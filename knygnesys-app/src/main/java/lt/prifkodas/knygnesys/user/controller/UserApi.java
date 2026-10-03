package lt.prifkodas.knygnesys.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lt.prifkodas.knygnesys.auth.dto.AuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Vartotojai", description = "Vartotojų valdymas")
@RequestMapping("/api/users")
public interface UserApi {

    @Operation(summary = "Gauti prisijungusį vartotoją", description = "Grąžina šiuo metu prisijungusio vartotojo duomenis")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vartotojas rastas"),
            @ApiResponse(responseCode = "401", description = "Vartotojas neprisijungęs")
    })
    @GetMapping("/me")
    ResponseEntity<AuthResponse> getMe(@AuthenticationPrincipal UserDetails userDetails);
}