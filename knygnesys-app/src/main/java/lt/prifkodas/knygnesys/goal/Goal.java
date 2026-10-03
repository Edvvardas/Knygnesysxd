package lt.prifkodas.knygnesys.goal;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "goals")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private Short year;

    @Column(name = "target_books", nullable = false)
    private Integer targetBooks;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;
}
