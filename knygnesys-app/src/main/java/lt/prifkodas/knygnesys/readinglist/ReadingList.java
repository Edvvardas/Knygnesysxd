package lt.prifkodas.knygnesys.readinglist;

import jakarta.persistence.*;
import lombok.Data;
import lt.prifkodas.knygnesys.book.Book;
import lt.prifkodas.knygnesys.user.User;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "reading_list")
public class ReadingList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @Column(name = "epub_path", length = 500)
    private String epubPath;

    @Column(name = "current_page")
    private Integer currentPage;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }
}
