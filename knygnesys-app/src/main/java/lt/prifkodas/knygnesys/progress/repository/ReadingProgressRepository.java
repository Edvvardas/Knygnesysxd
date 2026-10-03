package lt.prifkodas.knygnesys.progress.repository;

import lt.prifkodas.knygnesys.progress.ReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Integer> {
    List<ReadingProgress> findByUserIdAndUpdatedAtAfter(Integer userId, LocalDateTime since);
}
