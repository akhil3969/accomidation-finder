-- =====================================================================
--  Accommodation Finder - demo data
--
--  Run AFTER schema.sql:   mysql -u root -p < database/seed.sql
--
--  Every account below uses the password `password123`. The hash is a real
--  BCrypt hash of that string, so you can sign in with it straight away.
--
--  Note: the application also seeds this data automatically on an empty
--  database (see DataSeeder.java, controlled by app.seed.enabled). Use this
--  script when you want to load the data without starting the app.
-- =====================================================================

USE accommodation_finder;

SET @pw = '$2b$10$BKr3uNJszWQyL.py3yuUyOA6Mo6P6G7NxwVA.Fl8hpo23Ob3slnLi';

INSERT INTO users (id, name, email, password, phone, role, bio, enabled) VALUES
(1, 'Platform Admin',  'admin@accomfinder.app', @pw, '+33 6 00 00 00 00', 'ADMIN',    'Keeps the listings honest.', TRUE),
(2, 'Jean Dupont',     'jean@landlord.fr',      @pw, '+33 6 12 34 56 78', 'LANDLORD', 'Renting student-friendly flats around Paris since 2015.', TRUE),
(3, 'Marie Lefevre',   'marie@landlord.fr',     @pw, '+33 6 98 76 54 32', 'LANDLORD', 'Three quiet studios near the metro, ideal for MSc students.', TRUE),
(4, 'Kona Sai Akhil',  'akhil@epita.fr',        @pw, '+33 7 11 22 33 44', 'TENANT',   'MSc student at EPITA Paris looking for a studio.', TRUE),
(5, 'Sofia Rossi',     'sofia@student.fr',      @pw, '+33 7 55 66 77 88', 'TENANT',   'Erasmus student, non-smoker, tidy.', TRUE),
(6, 'Liam O''Connor',  'liam@student.fr',       @pw, '+33 7 99 88 77 66', 'TENANT',   'PhD candidate, needs a quiet place to work.', TRUE);

INSERT INTO rooms
(id, title, description, city, address, postal_code, price, deposit, size_sqm, bedrooms, bathrooms,
 max_guests, available, furnished, bills_included, available_from, min_stay_months, room_type,
 landlord_id, latitude, longitude) VALUES
(1, 'Bright studio 5 min from EPITA',
    'Fully renovated 22 m2 studio on a quiet street, five minutes on foot from the EPITA campus. Fibre internet, a proper desk and a separate kitchenette. Bills included.',
    'Le Kremlin-Bicetre', '12 Rue de la Liberte', '94270', 750.00, 750.00, 22, 1, 1, 1, TRUE, TRUE, TRUE,
    CURRENT_DATE + INTERVAL 7 DAY, 6, 'STUDIO', 2, 48.814500, 2.360700),
(2, 'Room in shared flat - Paris 13',
    'Large bedroom in a friendly three-bedroom flat. Shared living room and kitchen, two minutes from metro line 7.',
    'Paris', '45 Avenue d''Italie', '75013', 550.00, 550.00, 15, 1, 1, 1, TRUE, TRUE, FALSE,
    CURRENT_DATE + INTERVAL 7 DAY, 3, 'SHARED', 2, 48.827100, 2.360100),
(3, 'Modern one-bedroom in Villejuif',
    'Recently built apartment with a separate bedroom, a real living room and a small balcony. Tram T7 and metro line 7 within walking distance.',
    'Villejuif', '8 Rue Jean Jaures', '94800', 950.00, 950.00, 35, 2, 1, 2, TRUE, TRUE, TRUE,
    CURRENT_DATE + INTERVAL 7 DAY, 6, 'APARTMENT', 3, 48.794600, 2.365600),
(4, 'Cosy studio near Place d''Italie',
    'Compact but clever 18 m2 studio with a mezzanine bed, right by the shops and the metro.',
    'Paris', '3 Rue Bobillot', '75013', 820.00, 820.00, 18, 1, 1, 1, TRUE, TRUE, TRUE,
    CURRENT_DATE + INTERVAL 7 DAY, 6, 'STUDIO', 3, 48.828100, 2.355500),
(5, 'Quiet room in a house - Cachan',
    'Bedroom in a family house with garden access. Includes breakfast and laundry. RER B five minutes away.',
    'Cachan', '21 Avenue Carnot', '94230', 480.00, 480.00, 14, 1, 1, 1, TRUE, TRUE, TRUE,
    CURRENT_DATE + INTERVAL 7 DAY, 3, 'SHARED', 2, 48.795500, 2.331600),
(6, 'Spacious apartment for two - Ivry-sur-Seine',
    'Two-bedroom apartment ideal for a couple or two friends sharing. Bright living room and a fully equipped kitchen.',
    'Ivry-sur-Seine', '17 Rue Raspail', '94200', 1150.00, 1150.00, 52, 2, 1, 2, TRUE, TRUE, FALSE,
    CURRENT_DATE + INTERVAL 7 DAY, 6, 'APARTMENT', 3, 48.813600, 2.387400),
(7, 'Student studio - Gentilly',
    'Simple, clean and affordable studio in a student residence. Communal study room and bike storage on site.',
    'Gentilly', '5 Rue Benoit Malon', '94250', 620.00, 620.00, 19, 1, 1, 1, TRUE, TRUE, TRUE,
    CURRENT_DATE + INTERVAL 7 DAY, 6, 'STUDIO', 2, 48.813500, 2.341900),
(8, 'Whole house for a group - Arcueil',
    'Three-bedroom house with a garden, perfect for a group of students sharing. Twenty minutes to central Paris on the RER B.',
    'Arcueil', '40 Rue Emile Raspail', '94110', 1800.00, 1800.00, 90, 3, 2, 4, FALSE, TRUE, TRUE,
    CURRENT_DATE + INTERVAL 7 DAY, 6, 'HOUSE', 3, 48.806500, 2.334500);

INSERT INTO room_images (room_id, position, url) VALUES
(1, 0, 'https://picsum.photos/seed/studio-epita-1/900/600'),
(1, 1, 'https://picsum.photos/seed/studio-epita-2/900/600'),
(1, 2, 'https://picsum.photos/seed/studio-epita-3/900/600'),
(2, 0, 'https://picsum.photos/seed/shared-paris13-1/900/600'),
(2, 1, 'https://picsum.photos/seed/shared-paris13-2/900/600'),
(3, 0, 'https://picsum.photos/seed/villejuif-apt-1/900/600'),
(3, 1, 'https://picsum.photos/seed/villejuif-apt-2/900/600'),
(4, 0, 'https://picsum.photos/seed/italie-studio-1/900/600'),
(5, 0, 'https://picsum.photos/seed/cachan-room-1/900/600'),
(6, 0, 'https://picsum.photos/seed/ivry-apt-1/900/600'),
(7, 0, 'https://picsum.photos/seed/gentilly-studio-1/900/600'),
(8, 0, 'https://picsum.photos/seed/arcueil-house-1/900/600');

INSERT INTO room_amenities (room_id, amenity) VALUES
(1, 'Wi-Fi'), (1, 'Desk'), (1, 'Washing machine'), (1, 'Heating'), (1, 'Kitchenette'),
(2, 'Wi-Fi'), (2, 'Shared kitchen'), (2, 'Balcony'), (2, 'Elevator'),
(3, 'Wi-Fi'), (3, 'Balcony'), (3, 'Dishwasher'), (3, 'Elevator'), (3, 'Parking'),
(4, 'Wi-Fi'), (4, 'Heating'), (4, 'Kitchenette'),
(5, 'Wi-Fi'), (5, 'Garden'), (5, 'Breakfast'), (5, 'Laundry'),
(6, 'Wi-Fi'), (6, 'Dishwasher'), (6, 'Washing machine'), (6, 'Elevator'),
(7, 'Wi-Fi'), (7, 'Study room'), (7, 'Bike storage'), (7, 'Heating'),
(8, 'Wi-Fi'), (8, 'Garden'), (8, 'Parking'), (8, 'Washing machine'), (8, 'Dishwasher');

INSERT INTO bookings (id, room_id, tenant_id, check_in, check_out, guests, message, status, total_price) VALUES
(1, 1, 4, CURRENT_DATE + INTERVAL 14 DAY, CURRENT_DATE + INTERVAL 300 DAY, 1,
    'Hello! I start my MSc in September and would love to visit this week.', 'PENDING', 7150.00),
(2, 3, 5, CURRENT_DATE + INTERVAL 30 DAY, CURRENT_DATE + INTERVAL 210 DAY, 2,
    'Erasmus semester, references available on request.', 'CONFIRMED', 5700.00),
(3, 5, 6, CURRENT_DATE - INTERVAL 240 DAY, CURRENT_DATE - INTERVAL 30 DAY, 1,
    'Thanks for the quick reply.', 'COMPLETED', 3360.00),
(4, 7, 5, CURRENT_DATE + INTERVAL 7 DAY, CURRENT_DATE + INTERVAL 127 DAY, 1,
    'Is the study room open at weekends?', 'REJECTED', 2480.00);

INSERT INTO reviews (room_id, author_id, rating, comment) VALUES
(5, 6, 5, 'Very quiet, and the breakfast was a lovely surprise. Highly recommended.'),
(3, 5, 4, 'Great flat and a helpful landlady. The lift can be slow at rush hour.');

INSERT INTO favorites (user_id, room_id) VALUES
(4, 3), (4, 4), (5, 1);

INSERT INTO messages (sender_id, recipient_id, room_id, content, read_flag) VALUES
(4, 2, 1, 'Hi Jean, is the studio still available for September?', TRUE),
(2, 4, 1, 'Hello Akhil, yes it is. Would Saturday morning suit you for a visit?', TRUE),
(4, 2, 1, 'Saturday at 10 would be perfect, thank you!', FALSE);
