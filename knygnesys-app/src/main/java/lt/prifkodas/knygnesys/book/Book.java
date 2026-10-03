package lt.prifkodas.knygnesys.book;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ol_id", nullable = false, unique = true, length = 50)
    private String olId;

    @Column(nullable = false)
    private String title;

    private String author;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String genre;
}
