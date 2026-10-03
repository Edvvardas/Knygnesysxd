package lt.prifkodas.knygnesys.goal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lt.prifkodas.knygnesys.goal.Goal;
import lt.prifkodas.knygnesys.goal.repository.GoalRepository;
import lt.prifkodas.knygnesys.user.User;
import lt.prifkodas.knygnesys.user.service.UserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InactivitySchedulerService {

    private final GoalRepository goalRepository;
    private final UserService userService;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 0 * * *")
    public void checkInactivityAndNotify() {
        short currentYear = (short) Year.now().getValue();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

        List<Goal> goalsToNotify = goalRepository.findGoalsToNotify(currentYear, cutoff);
        log.info("Inactivity check: {} user(s) to notify", goalsToNotify.size());

        for (Goal goal : goalsToNotify) {
            try {
                User user = userService.getById(goal.getUserId());
                emailService.sendInactivityReminder(user.getEmail(), user.getUsername());
                goal.setNotifiedAt(LocalDateTime.now());
                goalRepository.save(goal);
                log.info("Notified user '{}' (id={})", user.getUsername(), user.getId());
            } catch (Exception e) {
                log.error("Failed to notify user for goal id={}: {}", goal.getId(), e.getMessage());
            }
        }
    }
}
