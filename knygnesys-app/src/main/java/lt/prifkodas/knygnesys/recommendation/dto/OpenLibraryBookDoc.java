package lt.prifkodas.knygnesys.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OpenLibraryBookDoc {

    private String key;
    private String title;

    @JsonProperty("author_name")
    private List<String> authorName;

    @JsonProperty("cover_i")
    private Integer coverId;
}