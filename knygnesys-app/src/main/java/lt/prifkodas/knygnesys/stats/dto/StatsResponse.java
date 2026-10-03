package lt.prifkodas.knygnesys.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsResponse {
    private StatsSummary summary;
    private String period;
    private List<StatsDataPoint> dataPoints;
}
