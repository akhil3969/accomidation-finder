package com.accommodationfinder.config;

import com.accommodationfinder.model.*;
import com.accommodationfinder.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Populates an empty database with realistic demo content so the app is usable
 * the moment it starts. Controlled by {@code app.seed.enabled} (off in prod).
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      RoomRepository roomRepository,
                      BookingRepository bookingRepository,
                      ReviewRepository reviewRepository,
                      FavoriteRepository favoriteRepository,
                      MessageRepository messageRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.favoriteRepository = favoriteRepository;
        this.messageRepository = messageRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains data - skipping seed");
            return;
        }

        log.info("Seeding demo data (all demo accounts use the password '{}')", DEMO_PASSWORD);

        User admin = user("Platform Admin", "admin@accomfinder.app", Role.ADMIN,
                "+33 6 00 00 00 00", "Keeps the listings honest.");
        User jean = user("Jean Dupont", "jean@landlord.fr", Role.LANDLORD,
                "+33 6 12 34 56 78", "Renting student-friendly flats around Paris since 2015.");
        User marie = user("Marie Lefevre", "marie@landlord.fr", Role.LANDLORD,
                "+33 6 98 76 54 32", "Three quiet studios near the metro, ideal for MSc students.");
        User akhil = user("Kona Sai Akhil", "akhil@epita.fr", Role.TENANT,
                "+33 7 11 22 33 44", "MSc student at EPITA Paris looking for a studio.");
        User sofia = user("Sofia Rossi", "sofia@student.fr", Role.TENANT,
                "+33 7 55 66 77 88", "Erasmus student, non-smoker, tidy.");
        User liam = user("Liam O'Connor", "liam@student.fr", Role.TENANT,
                "+33 7 99 88 77 66", "PhD candidate, needs a quiet place to work.");

        // Landlords: one verified, one still waiting, so the moderation queue is not empty.
        jean.setVerificationStatus(VerificationStatus.VERIFIED);
        jean.setVerifiedAt(java.time.LocalDateTime.now().minusMonths(2));
        marie.setVerificationStatus(VerificationStatus.PENDING);

        // Tenants: the arrival profile is what makes the aid estimates specific rather than generic.
        newcomer(akhil, "India", false, LocalDate.now().minusYears(23), LocalDate.now().minusDays(10),
                true, false, "EPITA, Le Kremlin-Bicetre");
        newcomer(sofia, "Italy", true, LocalDate.now().minusYears(21), LocalDate.now().minusDays(40),
                true, true, "Sorbonne Universite, Paris");
        newcomer(liam, "Ireland", true, LocalDate.now().minusYears(28), LocalDate.now().minusMonths(14),
                true, false, "Universite Paris-Saclay");

        userRepository.saveAll(List.of(admin, jean, marie, akhil, sofia, liam));

        Room r1 = room(jean, "Bright studio 5 min from EPITA",
                "Fully renovated 22 m2 studio on a quiet street, five minutes on foot from the EPITA campus. "
                        + "Fibre internet, a proper desk and a separate kitchenette. Bills included.",
                "Le Kremlin-Bicetre", "12 Rue de la Liberte", "94270",
                750, 22, RoomType.STUDIO, 48.8145, 2.3607, true, true,
                Set.of("Wi-Fi", "Desk", "Washing machine", "Heating", "Kitchenette"),
                "studio-epita");

        Room r2 = room(jean, "Room in shared flat - Paris 13",
                "Large bedroom in a friendly three-bedroom flat. Shared living room and kitchen, "
                        + "two minutes from metro line 7. Perfect for a student who likes company.",
                "Paris", "45 Avenue d'Italie", "75013",
                550, 15, RoomType.SHARED, 48.8271, 2.3601, true, false,
                Set.of("Wi-Fi", "Shared kitchen", "Balcony", "Elevator"),
                "shared-paris13");

        Room r3 = room(marie, "Modern one-bedroom in Villejuif",
                "Recently built apartment with a separate bedroom, a real living room and a small balcony. "
                        + "Tram T7 and metro line 7 within walking distance.",
                "Villejuif", "8 Rue Jean Jaures", "94800",
                950, 35, RoomType.APARTMENT, 48.7946, 2.3656, true, true,
                Set.of("Wi-Fi", "Balcony", "Dishwasher", "Elevator", "Parking"),
                "villejuif-apt");

        Room r4 = room(marie, "Cosy studio near Place d'Italie",
                "Compact but clever 18 m2 studio with a mezzanine bed, right by the shops and the metro. "
                        + "Ideal for a first year in Paris.",
                "Paris", "3 Rue Bobillot", "75013",
                820, 18, RoomType.STUDIO, 48.8281, 2.3555, true, true,
                Set.of("Wi-Fi", "Heating", "Kitchenette"),
                "italie-studio");

        Room r5 = room(jean, "Quiet room in a house - Cachan",
                "Bedroom in a family house with garden access. Includes breakfast and laundry. "
                        + "RER B five minutes away, twelve minutes to Denfert-Rochereau.",
                "Cachan", "21 Avenue Carnot", "94230",
                480, 14, RoomType.SHARED, 48.7955, 2.3316, true, true,
                Set.of("Wi-Fi", "Garden", "Breakfast", "Laundry"),
                "cachan-room");

        Room r6 = room(marie, "Spacious apartment for two - Ivry-sur-Seine",
                "Two-bedroom apartment ideal for a couple or two friends sharing. Bright living room, "
                        + "fully equipped kitchen, and a supermarket downstairs.",
                "Ivry-sur-Seine", "17 Rue Raspail", "94200",
                1150, 52, RoomType.APARTMENT, 48.8136, 2.3874, true, false,
                Set.of("Wi-Fi", "Dishwasher", "Washing machine", "Elevator"),
                "ivry-apt");

        Room r7 = room(jean, "Student studio - Gentilly",
                "Simple, clean and affordable studio in a student residence. Communal study room and "
                        + "bike storage on site.",
                "Gentilly", "5 Rue Benoit Malon", "94250",
                620, 19, RoomType.STUDIO, 48.8135, 2.3419, true, true,
                Set.of("Wi-Fi", "Study room", "Bike storage", "Heating"),
                "gentilly-studio");

        Room r8 = room(marie, "Whole house for a group - Arcueil",
                "Three-bedroom house with a garden, perfect for a group of students sharing. "
                        + "Twenty minutes to central Paris on the RER B.",
                "Arcueil", "40 Rue Emile Raspail", "94110",
                1800, 90, RoomType.HOUSE, 48.8065, 2.3345, false, true,
                Set.of("Wi-Fi", "Garden", "Parking", "Washing machine", "Dishwasher"),
                "arcueil-house");

        r2.setChargesAmount(BigDecimal.valueOf(45));
        r6.setChargesAmount(BigDecimal.valueOf(70));
        r8.setChargesAmount(BigDecimal.valueOf(120));
        r1.setVerified(true);
        r3.setVerified(true);

        // A listing that trips every scam heuristic, so the safety features are visible in
        // the demo without anyone having to fake one by hand.
        Room bait = room(marie, "Beautiful large studio Paris centre - urgent",
                "I am currently abroad for work so I cannot show the flat. Send the deposit by "
                        + "Western Union and I will courier the keys to you. Trust me, first come "
                        + "first served. Contact me on WhatsApp only.",
                "Paris", "1 Rue de Rivoli", "75001",
                290, 40, RoomType.STUDIO, 48.8566, 2.3522, true, true,
                Set.of("Wi-Fi"),
                "too-good");
        bait.setImages(List.of());

        roomRepository.saveAll(List.of(r1, r2, r3, r4, r5, r6, r7, r8, bait));

        // --- bookings -------------------------------------------------------
        Booking b1 = booking(r1, akhil, LocalDate.now().plusDays(14), LocalDate.now().plusMonths(10),
                1, "Hello! I start my MSc in September and would love to visit this week.",
                BookingStatus.PENDING);
        Booking b2 = booking(r3, sofia, LocalDate.now().plusDays(30), LocalDate.now().plusMonths(6),
                2, "Erasmus semester, references available on request.", BookingStatus.CONFIRMED);
        Booking b3 = booking(r5, liam, LocalDate.now().minusMonths(8), LocalDate.now().minusMonths(1),
                1, "Thanks for the quick reply.", BookingStatus.COMPLETED);
        Booking b4 = booking(r7, sofia, LocalDate.now().plusDays(7), LocalDate.now().plusMonths(4),
                1, "Is the study room open at weekends?", BookingStatus.REJECTED);
        bookingRepository.saveAll(List.of(b1, b2, b3, b4));

        // --- reviews (only from tenants who actually stayed) -----------------
        reviewRepository.saveAll(List.of(
                review(r5, liam, 5, "Very quiet, and the breakfast was a lovely surprise. Highly recommended."),
                review(r3, sofia, 4, "Great flat and a helpful landlady. The lift can be slow at rush hour.")));

        // --- favourites -----------------------------------------------------
        favoriteRepository.saveAll(List.of(
                new Favorite(akhil, r3),
                new Favorite(akhil, r4),
                new Favorite(sofia, r1)));

        // --- a sample conversation ------------------------------------------
        messageRepository.saveAll(List.of(
                message(akhil, jean, r1, "Hi Jean, is the studio still available for September?"),
                message(jean, akhil, r1, "Hello Akhil, yes it is. Would Saturday morning suit you for a visit?"),
                message(akhil, jean, r1, "Saturday at 10 would be perfect, thank you!")));

        log.info("Seeded {} users, {} rooms, {} bookings",
                userRepository.count(), roomRepository.count(), bookingRepository.count());
        log.info("Sign in as tenant  -> akhil@epita.fr   / {}", DEMO_PASSWORD);
        log.info("Sign in as landlord-> jean@landlord.fr / {}", DEMO_PASSWORD);
        log.info("Sign in as admin   -> admin@accomfinder.app / {}", DEMO_PASSWORD);
    }

    // ------------------------------------------------------------- factories

    /** Fills in the arrival details the aid estimator and checklist read. */
    private void newcomer(User user, String nationality, boolean euNational, LocalDate born,
                          LocalDate arrived, boolean student, boolean scholarship, String university) {
        user.setNationality(nationality);
        user.setEuNational(euNational);
        user.setDateOfBirth(born);
        user.setArrivalDate(arrived);
        user.setStudent(student);
        user.setScholarshipHolder(scholarship);
        user.setUniversity(university);
        user.setProfileCompleted(true);
    }

    private User user(String name, String email, Role role, String phone, String bio) {
        User user = new User(name, email, passwordEncoder.encode(DEMO_PASSWORD), role);
        user.setPhone(phone);
        user.setBio(bio);
        user.setAvatarUrl("https://i.pravatar.cc/150?u=" + email);
        return user;
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private Room room(User landlord, String title, String description, String city, String address,
                      String postalCode, int price, int sizeSqm, RoomType type,
                      double latitude, double longitude, boolean available, boolean billsIncluded,
                      Set<String> amenities, String imageSeed) {
        Room room = new Room();
        room.setLandlord(landlord);
        room.setTitle(title);
        room.setDescription(description);
        room.setCity(city);
        room.setAddress(address);
        room.setPostalCode(postalCode);
        room.setPrice(BigDecimal.valueOf(price));
        room.setDeposit(BigDecimal.valueOf(price));
        room.setSizeSqm(sizeSqm);
        room.setRoomType(type);
        room.setBedrooms(type == RoomType.HOUSE ? 3 : type == RoomType.APARTMENT ? 2 : 1);
        room.setBathrooms(type == RoomType.HOUSE ? 2 : 1);
        room.setMaxGuests(type == RoomType.HOUSE ? 4 : type == RoomType.APARTMENT ? 2 : 1);
        room.setLatitude(BigDecimal.valueOf(latitude));
        room.setLongitude(BigDecimal.valueOf(longitude));
        room.setAvailable(available);
        room.setFurnished(true);
        room.setBillsIncluded(billsIncluded);
        room.setAvailableFrom(LocalDate.now().plusDays(7));
        room.setMinStayMonths(type == RoomType.SHARED ? 3 : 6);
        room.setAmenities(amenities);
        room.setImages(List.of(
                "https://picsum.photos/seed/" + imageSeed + "-1/900/600",
                "https://picsum.photos/seed/" + imageSeed + "-2/900/600",
                "https://picsum.photos/seed/" + imageSeed + "-3/900/600"));
        return room;
    }

    private Booking booking(Room room, User tenant, LocalDate checkIn, LocalDate checkOut,
                            int guests, String note, BookingStatus status) {
        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setTenant(tenant);
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setGuests(guests);
        booking.setMessage(note);
        booking.setStatus(status);
        long days = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        booking.setTotalPrice(room.getPrice()
                .multiply(BigDecimal.valueOf(days))
                .divide(BigDecimal.valueOf(30), 2, java.math.RoundingMode.HALF_UP));
        return booking;
    }

    private Review review(Room room, User author, int rating, String comment) {
        Review review = new Review();
        review.setRoom(room);
        review.setAuthor(author);
        review.setRating(rating);
        review.setComment(comment);
        return review;
    }

    private Message message(User sender, User recipient, Room room, String content) {
        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setRoom(room);
        message.setContent(content);
        return message;
    }
}
