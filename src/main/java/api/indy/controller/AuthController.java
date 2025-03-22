package api.indy.controller;

import api.indy.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/generate-token")
    public ResponseEntity<Map<String, String>> generateToken() {
        return new ResponseEntity<>(Map.of("token", authService.generateToken().toString()), HttpStatus.OK);
    }

    @PostMapping("/verify/{token}")
    public ResponseEntity<Map<String, String>> verifyToken(@PathVariable("token") String token) {
        if(authService.verifyToken(UUID.fromString(token)))
            return new ResponseEntity<>(Map.of("message", "Token is valid", "status", "200"), HttpStatus.OK);
        else return new ResponseEntity<>(Map.of("message", "Token is invalid", "status", "401"), HttpStatus.OK);
    }
}
