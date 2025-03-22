package api.indy.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private final BCryptPasswordEncoder encoder;
    private final List<UUID> tokens;

    public AuthService() {
        this.encoder = new BCryptPasswordEncoder();
        this.tokens = new ArrayList<>();
    }

    public UUID generateToken() {
        UUID uuid = UUID.randomUUID();
        while(this.tokens.contains(uuid)) uuid = UUID.randomUUID();

        this.tokens.add(uuid);

        return uuid;
    }

    public String hashPassword(String password) {
        return this.encoder.encode(password);
    }

    public boolean verifyPassword(String password, String hash) {
        return this.encoder.matches(password, hash);
    }

    public boolean verifyToken(UUID token) {
        return this.tokens.contains(token);
    }

    public List<UUID> tokens() {
        return this.tokens;
    }
}
