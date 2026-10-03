package lt.prifkodas.knygnesys.recommendation.service;

import lt.prifkodas.knygnesys.recommendation.dto.OpenLibraryBookDoc;
import lt.prifkodas.knygnesys.recommendation.dto.OpenLibrarySearchResponse;
import lt.prifkodas.knygnesys.recommendation.dto.RecommendationDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Service
public class RecommendationsService {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://openlibrary.org")
            .defaultHeader(HttpHeaders.USER_AGENT, "Knygnesys/1.0")
            .build();

    public List<RecommendationDto> getPopularRecommendations(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 10));

        String url = UriComponentsBuilder
                .fromPath("/search.json")
                .queryParam("q", "popular books")
                .queryParam("limit", safeLimit)
                .toUriString();

        OpenLibrarySearchResponse response = restClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(OpenLibrarySearchResponse.class);

        if (response == null || response.getDocs() == null) {
            return Collections.emptyList();
        }

        return response.getDocs().stream()
                .limit(safeLimit)
                .map(this::toDto)
                .toList();
    }

    private RecommendationDto toDto(OpenLibraryBookDoc doc) {
        String author = "Unknown";
        if (doc.getAuthorName() != null && !doc.getAuthorName().isEmpty()) {
            author = doc.getAuthorName().get(0);
        }

        String coverUrl = null;
        if (doc.getCoverId() != null) {
            coverUrl = "https://covers.openlibrary.org/b/id/" + doc.getCoverId() + "-M.jpg";
        }

        return new RecommendationDto(
                doc.getTitle(),
                author,
                coverUrl,
                doc.getKey()
        );
    }
}