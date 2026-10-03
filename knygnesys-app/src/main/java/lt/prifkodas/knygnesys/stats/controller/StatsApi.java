package lt.prifkodas.knygnesys.stats.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lt.prifkodas.knygnesys.stats.dto.GenreDataPoint;
import lt.prifkodas.knygnesys.stats.dto.PagesDayDataPoint;
import lt.prifkodas.knygnesys.stats.dto.StatsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Statistika", description = "Skaitymo statistika")
@RequestMapping("/api/stats")
public interface StatsApi {

    @Operation(summary = "Gauti skaitymo statistiką")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistika grąžinta"),
            @ApiResponse(responseCode = "401", description = "Neprisijungęs")
    })
    @GetMapping
    ResponseEntity<StatsResponse> getStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "WEEK") String period,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    );

    @Operation(summary = "Gauti puslapių skaičių per dieną")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Duomenys grąžinti"),
            @ApiResponse(responseCode = "401", description = "Neprisijungęs")
    })
    @GetMapping("/pages")
    ResponseEntity<List<PagesDayDataPoint>> getPagesPerDay(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String startDate
    );

    @Operation(summary = "Gauti knygų pasiskirstymą pagal žanrus")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Duomenys grąžinti"),
            @ApiResponse(responseCode = "401", description = "Neprisijungęs")
    })
    @GetMapping("/genres")
    ResponseEntity<List<GenreDataPoint>> getGenreStats(
            @AuthenticationPrincipal UserDetails userDetails
    );
}
