package lt.prifkodas.knygnesys.recommendation.dto;

import lombok.Data;

import java.util.List;

@Data
public class OpenLibrarySearchResponse {
    private List<OpenLibraryBookDoc> docs;
}