package com.accommodationfinder.support;

import com.accommodationfinder.model.Role;
import com.accommodationfinder.model.Room;
import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.RoomRepository;
import com.accommodationfinder.repository.UserRepository;
import com.accommodationfinder.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Builds users and rooms for integration tests. A plain class rather than a bean, so
 * component scanning never picks it up into the application context.
 */
public class TestFixtures {

    public static final String PASSWORD = "password123";

    private final UserRepository users;
    private final RoomRepository rooms;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public TestFixtures(UserRepository users, RoomRepository rooms, PasswordEncoder encoder, JwtService jwtService) {
        this.users = users;
        this.rooms = rooms;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public User user(Role role, boolean emailVerified) {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.fr";
        User user = new User("Test " + role.name().charAt(0) + role.name().substring(1).toLowerCase(),
                email, encoder.encode(PASSWORD), role);
        user.setPhone("+33 6 11 22 33 44");
        user.setEmailVerified(emailVerified);
        return users.save(user);
    }

    public Room room(User landlord) {
        Room room = new Room();
        room.setTitle("Studio near the canal");
        room.setCity("Paris");
        room.setPrice(BigDecimal.valueOf(850));
        room.setMaxGuests(2);
        room.setAvailable(true);
        room.setLandlord(landlord);
        return rooms.save(room);
    }

    public String bearer(User user) {
        return "Bearer " + jwtService.generateToken(users.findById(user.getId()).orElseThrow());
    }
}
