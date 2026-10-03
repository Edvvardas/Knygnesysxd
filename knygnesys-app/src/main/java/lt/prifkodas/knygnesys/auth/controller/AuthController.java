package lt.prifkodas.knygnesys.auth.controller;

import lombok.RequiredArgsConstructor;
import lt.prifkodas.knygnesys.auth.dto.AuthResponse;
import lt.prifkodas.knygnesys.auth.dto.LoginRequest;
import lt.prifkodas.knygnesys.auth.dto.RegisterRequest;
import lt.prifkodas.knygnesys.auth.service.AuthService;
import lt.prifkodas.knygnesys.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<AuthResponse> register(RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new AuthResponse(user.getId(), user.getUsername(), user.getEmail()));
    }

    @Override
    public ResponseEntity<AuthResponse> login(LoginRequest request) {
        User user = authService.login(request);
        return ResponseEntity
                .ok(new AuthResponse(user.getId(), user.getUsername(), user.getEmail()));
    }

    @Override
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }
}
