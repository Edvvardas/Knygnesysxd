package lt.prifkodas.knygnesys.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenreDataPoint {
    private String genre;
    private int count;
}
