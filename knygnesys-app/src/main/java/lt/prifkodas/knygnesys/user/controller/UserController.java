package lt.prifkodas.knygnesys.user.controller;

import lombok.RequiredArgsConstructor;
import lt.prifkodas.knygnesys.auth.dto.AuthResponse;
import lt.prifkodas.knygnesys.user.User;
import lt.prifkodas.knygnesys.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<AuthResponse> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.getByUsername(userDetails.getUsername());
        return ResponseEntity.ok(new AuthResponse(user.getId(), user.getUsername(), user.getEmail()));
    }
}