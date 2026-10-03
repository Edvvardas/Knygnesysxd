package lt.prifkodas.knygnesys.readinglist.repository;

import lt.prifkodas.knygnesys.readinglist.ReadingList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReadingListRepository extends JpaRepository<ReadingList, Integer> {
    List<ReadingList> findAllByUserId(Integer userId);
    Optional<ReadingList> findByUserIdAndBookId(Integer userId, Integer bookId);
    Optional<ReadingList> findByIdAndUserId(Integer id, Integer userId);
    List<ReadingList> findByUserIdAndAddedAtAfter(Integer userId, LocalDateTime since);

    @Query("SELECT COUNT(rl) FROM ReadingList rl WHERE rl.user.id = :userId AND rl.status = 'FINISHED' AND rl.completedAt >= :from AND rl.completedAt < :to")
    int countFinishedByUserIdAndYear(@Param("userId") Integer userId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT rl FROM ReadingList rl
            WHERE rl.user.id = :userId
            AND (:title IS NULL OR :title = '' OR LOWER(rl.book.title) LIKE LOWER(CONCAT('%', :title, '%')))
            AND (:author IS NULL OR :author = '' OR LOWER(rl.book.author) LIKE LOWER(CONCAT('%', :author, '%')))
            AND (:progress IS NULL OR COALESCE(rl.currentPage, 0) > :progress)
            """)
    List<ReadingList> findByUserIdWithFilters(
            @Param("userId") Integer userId,
            @Param("title") String title,
            @Param("author") String author,
            @Param("progress") Integer progress
    );
}
