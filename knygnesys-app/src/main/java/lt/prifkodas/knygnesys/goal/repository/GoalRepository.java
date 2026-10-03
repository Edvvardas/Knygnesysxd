package lt.prifkodas.knygnesys.goal.repository;

import lt.prifkodas.knygnesys.goal.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Integer> {

    Optional<Goal> findByUserIdAndYear(Integer userId, Short year);

    @Query("""
            SELECT g FROM Goal g
            WHERE g.year = :year
              AND (g.notifiedAt IS NULL OR g.notifiedAt < :cutoff)
              AND NOT EXISTS (
                  SELECT 1 FROM ReadingList rl
                  WHERE rl.user.id = g.userId
                    AND (rl.addedAt >= :cutoff OR rl.completedAt >= :cutoff)
              )
            """)
    List<Goal> findGoalsToNotify(@Param("year") Short year, @Param("cutoff") LocalDateTime cutoff);
}
