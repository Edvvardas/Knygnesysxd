package lt.prifkodas.knygnesys.readinglist.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProgressRequest {
    private int currentPage;
}
