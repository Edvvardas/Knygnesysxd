package lt.prifkodas.knygnesys.readinglist.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddBookRequest {
    private String olId;
    private String title;
    private String author;
    private String coverUrl;
    private Integer pageCount;
    private String status;
    private String genre;
}
