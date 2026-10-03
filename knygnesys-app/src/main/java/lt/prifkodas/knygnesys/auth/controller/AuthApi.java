package lt.prifkodas.knygnesys.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lt.prifkodas.knygnesys.auth.dto.AuthResponse;
import lt.prifkodas.knygnesys.auth.dto.LoginRequest;
import lt.prifkodas.knygnesys.auth.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Autentifikacija", description = "Registracija, prisijungimas ir atsijungimas")
@RequestMapping("/api/auth")
public interface AuthApi {

    @Operation(summary = "Registracija", description = "Sukuria naują vartotoją sistemoje")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vartotojas sėkmingai užregistruotas"),
            @ApiResponse(responseCode = "400", description = "El. paštas arba vartotojo vardas jau užimtas")
    })
    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request);

    @Operation(summary = "Prisijungimas", description = "Prisijungia prie sistemos ir nustato sesijos slapuką")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sėkmingai prisijungta"),
            @ApiResponse(responseCode = "400", description = "Neteisingas slaptažodis"),
            @ApiResponse(responseCode = "404", description = "Vartotojas nerastas")
    })
    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request);

    @Operation(summary = "Atsijungimas", description = "Sunaikina vartotojo sesiją")
    @ApiResponse(responseCode = "204", description = "Sėkmingai atsijungta")
    @PostMapping("/logout")
    ResponseEntity<Void> logout();
}
