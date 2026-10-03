package lt.prifkodas.knygnesys.recommendation.controller;

import lombok.RequiredArgsConstructor;
import lt.prifkodas.knygnesys.recommendation.dto.RecommendationDto;
import lt.prifkodas.knygnesys.recommendation.service.RecommendationsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationsController {

    private final RecommendationsService recommendationsService;

    @GetMapping("/popular")
    public ResponseEntity<List<RecommendationDto>> getPopularRecommendations(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(recommendationsService.getPopularRecommendations(limit));
    }
}