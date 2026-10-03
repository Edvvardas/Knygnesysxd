package lt.prifkodas.knygnesys.book.repository;

import lt.prifkodas.knygnesys.book.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Integer> {
    Optional<Book> findByOlId(String olId);
}
