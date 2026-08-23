package com.accommodationfinder.service;

import com.accommodationfinder.dto.NewcomerDtos.CommuteEstimate;
import com.accommodationfinder.dto.NewcomerDtos.NearbyPlace;
import com.accommodationfinder.dto.NewcomerDtos.NeighbourhoodInfo;
import com.accommodationfinder.model.Room;
import com.accommodationfinder.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Answers "what is it actually like around here?" using OpenStreetMap data.
 *
 * <p>Somebody who has lived in a city all their life reads an address and instantly knows
 * whether it is well connected, whether there are shops, whether it is a twenty-minute walk
 * from the last metro. Somebody who landed three days ago reads the same address and learns
 * nothing from it. That asymmetry is what this closes.
 *
 * <p>Results are cached in memory per listing. Overpass is a free, community-funded service
 * and hammering it for every page view would be both slow and rude.
 */
@Service
public class NeighbourhoodService {

    private static final Logger log = LoggerFactory.getLogger(NeighbourhoodService.class);
    private static final int SEARCH_RADIUS_METRES = 900;
    private static final long CACHE_TTL_MS = Duration.ofHours(12).toMillis();

    private record CacheEntry(NeighbourhoodInfo info, long storedAt) {
    }

    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String overpassUrl;
    private final boolean enabled;

    public NeighbourhoodService(ObjectMapper objectMapper,
                                @Value("${app.overpass.url:https://overpass-api.de/api/interpreter}") String overpassUrl,
                                @Value("${app.overpass.enabled:true}") boolean enabled) {
        this.objectMapper = objectMapper;
        this.overpassUrl = overpassUrl;
        this.enabled = enabled;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public NeighbourhoodInfo forRoom(Room room, User viewer) {
        if (room.getLatitude() == null || room.getLongitude() == null) {
            return unavailable(room, "This listing has no map position yet, so we cannot show "
                    + "what is nearby. The landlord can fix that by adding an address.");
        }

        CacheEntry cached = cache.get(room.getId());
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.storedAt() < CACHE_TTL_MS) {
            return withCommute(cached.info(), room, viewer);
        }

        double lat = room.getLatitude().doubleValue();
        double lon = room.getLongitude().doubleValue();

        List<NearbyPlace> transport = new ArrayList<>();
        List<NearbyPlace> essentials = new ArrayList<>();
        boolean available = false;

        if (enabled) {
            try {
                JsonNode elements = queryOverpass(lat, lon);
                for (JsonNode element : elements) {
                    NearbyPlace place = toPlace(element, lat, lon);
                    if (place == null) {
                        continue;
                    }
                    if (isTransport(place.type())) {
                        transport.add(place);
                    } else {
                        essentials.add(place);
                    }
                }
                available = true;
            } catch (Exception ex) {
                log.warn("Could not load neighbourhood data for room {}: {}", room.getId(), ex.getMessage());
            }
        }

        transport.sort(Comparator.comparing(NearbyPlace::metresAway));
        essentials.sort(Comparator.comparing(NearbyPlace::metresAway));

        List<NearbyPlace> topTransport = transport.stream().limit(6).toList();
        List<NearbyPlace> topEssentials = dedupeByType(essentials);

        Integer walkToTransport = topTransport.isEmpty() ? null : topTransport.get(0).walkMinutes();

        NeighbourhoodInfo info = new NeighbourhoodInfo(
                room.getId(),
                room.getCity(),
                topTransport,
                topEssentials,
                walkToTransport,
                null,
                buildHighlights(topTransport, topEssentials, walkToTransport),
                available,
                available
                        ? "Places within a 15 minute walk, from OpenStreetMap."
                        : "We could not reach the map service just now. Try again in a moment.");

        if (available) {
            cache.put(room.getId(), new CacheEntry(info, now));
        }
        return withCommute(info, room, viewer);
    }

    // ------------------------------------------------------------------ queries

    private JsonNode queryOverpass(double lat, double lon) throws Exception {
        // Ask for the handful of things that actually decide whether a place is liveable.
        String query = """
                [out:json][timeout:20];
                (
                  node["railway"="station"](around:%1$d,%2$f,%3$f);
                  node["railway"="subway_entrance"](around:%1$d,%2$f,%3$f);
                  node["highway"="bus_stop"](around:%1$d,%2$f,%3$f);
                  node["station"="subway"](around:%1$d,%2$f,%3$f);
                  node["shop"="supermarket"](around:%1$d,%2$f,%3$f);
                  node["shop"="bakery"](around:%1$d,%2$f,%3$f);
                  node["amenity"="pharmacy"](around:%1$d,%2$f,%3$f);
                  node["amenity"="hospital"](around:%1$d,%2$f,%3$f);
                  node["amenity"="library"](around:%1$d,%2$f,%3$f);
                  node["amenity"="bank"](around:%1$d,%2$f,%3$f);
                  node["amenity"="laundry"](around:%1$d,%2$f,%3$f);
                );
                out body 60;
                """.formatted(SEARCH_RADIUS_METRES, lat, lon);

        HttpRequest request = HttpRequest.newBuilder(URI.create(overpassUrl))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "AccommodationFinder/1.0 (student project)")
                .POST(HttpRequest.BodyPublishers.ofString("data=" + java.net.URLEncoder.encode(
                        query, StandardCharsets.UTF_8)))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("Overpass returned HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body()).path("elements");
    }

    private NearbyPlace toPlace(JsonNode element, double fromLat, double fromLon) {
        JsonNode tags = element.path("tags");
        double lat = element.path("lat").asDouble();
        double lon = element.path("lon").asDouble();
        if (lat == 0 || lon == 0) {
            return null;
        }

        String type = resolveType(tags);
        if (type == null) {
            return null;
        }

        String name = tags.path("name").asText("");
        if (name.isBlank()) {
            name = label(type);
        }

        int metres = (int) Math.round(
                GeocodingService.distanceKm(fromLat, fromLon, lat, lon) * 1000);

        return new NearbyPlace(name, type, label(type), metres,
                GeocodingService.walkMinutes(metres), lat, lon);
    }

    private String resolveType(JsonNode tags) {
        if ("station".equals(tags.path("railway").asText())) return "train_station";
        if ("subway_entrance".equals(tags.path("railway").asText())) return "metro";
        if ("subway".equals(tags.path("station").asText())) return "metro";
        if ("bus_stop".equals(tags.path("highway").asText())) return "bus";
        String shop = tags.path("shop").asText();
        if ("supermarket".equals(shop)) return "supermarket";
        if ("bakery".equals(shop)) return "bakery";
        String amenity = tags.path("amenity").asText();
        return switch (amenity) {
            case "pharmacy" -> "pharmacy";
            case "hospital" -> "hospital";
            case "library" -> "library";
            case "bank" -> "bank";
            case "laundry" -> "laundry";
            default -> null;
        };
    }

    private String label(String type) {
        return switch (type) {
            case "train_station" -> "Train / RER station";
            case "metro" -> "Metro";
            case "bus" -> "Bus stop";
            case "supermarket" -> "Supermarket";
            case "bakery" -> "Bakery";
            case "pharmacy" -> "Pharmacy";
            case "hospital" -> "Hospital";
            case "library" -> "Library";
            case "bank" -> "Bank";
            case "laundry" -> "Launderette";
            default -> type;
        };
    }

    private boolean isTransport(String type) {
        return List.of("train_station", "metro", "bus").contains(type);
    }

    /** Keep the closest of each kind, so the list reads as a summary and not a directory. */
    private List<NearbyPlace> dedupeByType(List<NearbyPlace> places) {
        Map<String, NearbyPlace> nearest = new LinkedHashMap<>();
        for (NearbyPlace place : places) {
            nearest.putIfAbsent(place.type(), place);
        }
        return new ArrayList<>(nearest.values());
    }

    // ---------------------------------------------------------------- commute

    /** Distance from the listing to where the viewer studies, if we know it. */
    private NeighbourhoodInfo withCommute(NeighbourhoodInfo info, Room room, User viewer) {
        if (viewer == null
                || viewer.getUniversityLatitude() == null
                || viewer.getUniversityLongitude() == null
                || room.getLatitude() == null) {
            return info;
        }

        double km = GeocodingService.distanceKm(
                room.getLatitude().doubleValue(), room.getLongitude().doubleValue(),
                viewer.getUniversityLatitude().doubleValue(),
                viewer.getUniversityLongitude().doubleValue());

        // Public transport in a dense city averages roughly 18 km/h door to door once you
        // count waiting and walking at both ends. Under 1.5 km, walking usually wins.
        int minutes = km < 1.5
                ? GeocodingService.walkMinutes(km * 1000)
                : (int) Math.round((km / 18.0) * 60) + 8;

        CommuteEstimate commute = new CommuteEstimate(
                viewer.getUniversity(),
                Math.round(km * 10) / 10.0,
                minutes,
                km < 1.5 ? "walking" : "public transport",
                km < 1.5
                        ? "Close enough to walk - no transport pass needed for this trip."
                        : "Estimated from the straight-line distance. Check the real route on "
                          + "the transport operator's app before deciding.");

        return new NeighbourhoodInfo(
                info.roomId(), info.city(), info.transport(), info.essentials(),
                info.walkMinutesToTransport(), commute, info.highlights(),
                info.dataAvailable(), info.dataNote());
    }

    // -------------------------------------------------------------- highlights

    private List<String> buildHighlights(List<NearbyPlace> transport,
                                         List<NearbyPlace> essentials,
                                         Integer walkToTransport) {
        List<String> highlights = new ArrayList<>();

        if (walkToTransport != null) {
            if (walkToTransport <= 5) {
                highlights.add("Public transport is " + walkToTransport
                        + " minutes' walk away - about as good as it gets.");
            } else if (walkToTransport <= 12) {
                highlights.add("Nearest stop is a " + walkToTransport + " minute walk. Fine in "
                        + "summer, less fun in February rain.");
            } else {
                highlights.add("The nearest stop is " + walkToTransport + " minutes away. Worth "
                        + "checking how often it runs at night.");
            }
        } else {
            highlights.add("We found no transport stop within a 15 minute walk. Check carefully "
                    + "how you would get to class.");
        }

        boolean hasSupermarket = essentials.stream().anyMatch(p -> "supermarket".equals(p.type()));
        if (hasSupermarket) {
            essentials.stream()
                    .filter(p -> "supermarket".equals(p.type()))
                    .findFirst()
                    .ifPresent(shop -> highlights.add("A supermarket is " + shop.walkMinutes()
                            + " minutes away, which matters more than you would think when you "
                            + "are carrying a week's shopping."));
        } else {
            highlights.add("No supermarket within walking distance - budget for the time, or a "
                    + "delivery service.");
        }

        if (essentials.stream().anyMatch(p -> "pharmacy".equals(p.type()))) {
            highlights.add("There is a pharmacy nearby. In France the pharmacist is usually the "
                    + "first person you see about anything minor.");
        }
        if (essentials.stream().anyMatch(p -> "laundry".equals(p.type()))) {
            highlights.add("There is a launderette close by, useful if the flat has no machine.");
        }

        return highlights;
    }

    private NeighbourhoodInfo unavailable(Room room, String note) {
        return new NeighbourhoodInfo(room.getId(), room.getCity(), List.of(), List.of(),
                null, null, List.of(), false, note);
    }

    /** Lets an admin clear the cache after editing a listing's address. */
    public void evict(Long roomId) {
        cache.remove(roomId);
    }
}
