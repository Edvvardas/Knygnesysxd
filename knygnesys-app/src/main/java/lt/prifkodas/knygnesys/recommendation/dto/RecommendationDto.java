package lt.prifkodas.knygnesys.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationDto {
    private String title;
    private String author;
    private String coverUrl;
    private String olId;
}