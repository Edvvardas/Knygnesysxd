package lt.prifkodas.knygnesys.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsDataPoint {
    private String label;
    private int booksAdded;
    private int booksCompleted;
}
