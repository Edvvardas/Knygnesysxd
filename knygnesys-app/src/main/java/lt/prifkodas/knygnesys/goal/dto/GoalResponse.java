package lt.prifkodas.knygnesys.goal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoalResponse {
    private Integer targetBooks;
    private int booksRead;
    private int progressPercent;
}
