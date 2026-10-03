package lt.prifkodas.knygnesys.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsSummary {
    private int totalBooks;
    private int completed;
    private int reading;
    private int wantToRead;
}
