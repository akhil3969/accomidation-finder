package com.accommodationfinder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Turns a written address into coordinates using the French government's Base Adresse
 * Nationale.
 *
 * <p>Chosen over Google or Mapbox for three reasons: it needs no API key, so the project
 * runs the moment you clone it; it is the authoritative source for French addresses, which
 * is the only country we cover; and it returns the INSEE commune code, which is what
 * actually identifies a French commune - two towns can share a name, and postcodes span
 * several communes.
 *
 * <p>Geocoding matters here beyond putting a pin on a map: without coordinates we cannot
 * tell someone how far a flat is from their university, and without the postcode we cannot
 * work out which CAF rent-ceiling zone applies.
 */
@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final boolean enabled;

    public GeocodingService(ObjectMapper objectMapper,
                            @Value("${app.geocoding.url:https://api-adresse.data.gouv.fr}") String baseUrl,
                            @Value("${app.geocoding.enabled:true}") boolean enabled) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.enabled = enabled;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** What the national address database knows about one address. */
    public record GeocodeResult(
            BigDecimal latitude,
            BigDecimal longitude,
            String label,
            String postcode,
            String city,
            String inseeCode,
            double confidence
    ) {
    }

    /**
     * Looks up an address. Returns empty rather than throwing: a listing that cannot be
     * geocoded should still save, just without a map pin.
     */
    public Optional<GeocodeResult> geocode(String address, String postcode, String city) {
        if (!enabled) {
            return Optional.empty();
        }

        String query = String.join(" ",
                        address == null ? "" : address.trim(),
                        postcode == null ? "" : postcode.trim(),
                        city == null ? "" : city.trim())
                .trim()
                .replaceAll("\\s+", " ");

        if (query.length() < 3) {
            return Optional.empty();
        }

        try {
            StringBuilder url = new StringBuilder(baseUrl)
                    .append("/search/?limit=1&q=")
                    .append(URLEncoder.encode(query, StandardCharsets.UTF_8));
            if (postcode != null && postcode.matches("\\d{5}")) {
                url.append("&postcode=").append(postcode);
            }

            HttpRequest request = HttpRequest.newBuilder(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "AccommodationFinder/1.0 (student project)")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Address lookup returned HTTP {} for '{}'", response.statusCode(), query);
                return Optional.empty();
            }

            JsonNode features = objectMapper.readTree(response.body()).path("features");
            if (!features.isArray() || features.isEmpty()) {
                return Optional.empty();
            }

            JsonNode feature = features.get(0);
            JsonNode coordinates = feature.path("geometry").path("coordinates");
            JsonNode properties = feature.path("properties");

            // GeoJSON is [longitude, latitude] - the opposite order to how people say it.
            return Optional.of(new GeocodeResult(
                    BigDecimal.valueOf(coordinates.get(1).asDouble()),
                    BigDecimal.valueOf(coordinates.get(0).asDouble()),
                    properties.path("label").asText(null),
                    properties.path("postcode").asText(null),
                    properties.path("city").asText(null),
                    properties.path("citycode").asText(null),
                    properties.path("score").asDouble(0)));

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Could not geocode '{}': {}", query, ex.getMessage());
            return Optional.empty();
        }
    }

    /** Distance in kilometres between two points, straight line. */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int earthRadiusKm = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Rough walking time, at a real-world 4.5 km/h rather than an optimistic 5. */
    public static int walkMinutes(double metres) {
        return (int) Math.max(1, Math.round(metres / 75.0));
    }
}
