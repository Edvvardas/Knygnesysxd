package lt.prifkodas.knygnesys.stats.controller;

import lombok.RequiredArgsConstructor;
import lt.prifkodas.knygnesys.stats.dto.GenreDataPoint;
import lt.prifkodas.knygnesys.stats.dto.PagesDayDataPoint;
import lt.prifkodas.knygnesys.stats.dto.StatsResponse;
import lt.prifkodas.knygnesys.stats.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StatsController implements StatsApi {

    private final StatsService statsService;

    @Override
    public ResponseEntity<StatsResponse> getStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "WEEK") String period,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(statsService.getStats(userDetails.getUsername(), period, date, startDate, endDate));
    }

    @Override
    public ResponseEntity<List<PagesDayDataPoint>> getPagesPerDay(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String startDate) {
        return ResponseEntity.ok(statsService.getPagesPerDay(userDetails.getUsername(), days, endDate, startDate));
    }

    @Override
    public ResponseEntity<List<GenreDataPoint>> getGenreStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(statsService.getGenreStats(userDetails.getUsername()));
    }
}
