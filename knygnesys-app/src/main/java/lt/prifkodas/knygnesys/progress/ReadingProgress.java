package lt.prifkodas.knygnesys.progress;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "reading_progress")
public class ReadingProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "book_id", nullable = false)
    private Integer bookId;

    @Column(name = "current_page", nullable = false)
    private Integer currentPage;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ReadingProgress(Integer userId, Integer bookId, Integer currentPage) {
        this.userId = userId;
        this.bookId = bookId;
        this.currentPage = currentPage;
    }

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }
}
