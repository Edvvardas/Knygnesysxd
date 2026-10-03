package lt.prifkodas.knygnesys.readinglist.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LibraryBookDto {
    private Integer readingListId;
    private Integer bookId;
    private String olId;
    private String title;
    private String author;
    private String coverUrl;
    private Integer pageCount;
    private int currentPage;
    private String status;
    private String addedAt;
    private boolean hasEpub;
}
