package lt.prifkodas.knygnesys.goal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lt.prifkodas.knygnesys.goal.dto.GoalResponse;
import lt.prifkodas.knygnesys.goal.dto.SetGoalRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tikslas", description = "Metinis skaitymo tikslas")
@RequestMapping("/api/goals")
public interface GoalApi {

    @Operation(summary = "Gauti šių metų tikslą")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tikslas grąžintas"),
            @ApiResponse(responseCode = "401", description = "Neprisijungęs")
    })
    @GetMapping
    ResponseEntity<GoalResponse> getGoal(@AuthenticationPrincipal UserDetails userDetails);

    @Operation(summary = "Gauti vartotojo šių metų tikslą pagal ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tikslas grąžintas"),
            @ApiResponse(responseCode = "404", description = "Vartotojas nerastas"),
            @ApiResponse(responseCode = "401", description = "Neprisijungęs")
    })
    @GetMapping("/{userId}")
    ResponseEntity<GoalResponse> getGoalByUserId(@PathVariable Integer userId);

    @Operation(summary = "Nustatyti arba atnaujinti tikslą")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tikslas išsaugotas"),
            @ApiResponse(responseCode = "400", description = "Netinkamas tikslas"),
            @ApiResponse(responseCode = "401", description = "Neprisijungęs")
    })
    @PostMapping
    ResponseEntity<GoalResponse> setGoal(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SetGoalRequest request
    );
}
