package lt.prifkodas.knygnesys.goal.service;

import lombok.RequiredArgsConstructor;
import lt.prifkodas.knygnesys.goal.Goal;
import lt.prifkodas.knygnesys.goal.dto.GoalResponse;
import lt.prifkodas.knygnesys.goal.dto.SetGoalRequest;
import lt.prifkodas.knygnesys.goal.repository.GoalRepository;
import lt.prifkodas.knygnesys.readinglist.repository.ReadingListRepository;
import lt.prifkodas.knygnesys.user.User;
import lt.prifkodas.knygnesys.user.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Year;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final ReadingListRepository readingListRepository;
    private final UserService userService;

    public GoalResponse getGoal(String username) {
        User user = userService.getByUsername(username);
        short currentYear = (short) Year.now().getValue();
        return goalRepository.findByUserIdAndYear(user.getId(), currentYear)
                .map(goal -> buildResponse(goal, user.getId(), currentYear))
                .orElse(new GoalResponse(null, 0, 0));
    }

    public GoalResponse setGoal(String username, SetGoalRequest request) {
        if (request.getTargetBooks() == null || request.getTargetBooks() < 1) {
            throw new IllegalArgumentException("Tikslas turi būti teigiamas skaičius");
        }
        User user = userService.getByUsername(username);
        short currentYear = (short) Year.now().getValue();
        Goal goal = goalRepository.findByUserIdAndYear(user.getId(), currentYear)
                .orElseGet(() -> {
                    Goal g = new Goal();
                    g.setUserId(user.getId());
                    g.setYear(currentYear);
                    return g;
                });
        goal.setTargetBooks(request.getTargetBooks());
        goalRepository.save(goal);
        return buildResponse(goal, user.getId(), currentYear);
    }

    public GoalResponse getGoalByUserId(Integer userId) {
        userService.getById(userId);
        short currentYear = (short) Year.now().getValue();
        return goalRepository.findByUserIdAndYear(userId, currentYear)
                .map(goal -> buildResponse(goal, userId, currentYear))
                .orElse(new GoalResponse(null, 0, 0));
    }

    private GoalResponse buildResponse(Goal goal, Integer userId, short year) {
        LocalDateTime yearStart = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime yearEnd = LocalDateTime.of(year + 1, 1, 1, 0, 0);
        int booksRead = readingListRepository.countFinishedByUserIdAndYear(userId, yearStart, yearEnd);
        int percent = Math.min(100, booksRead * 100 / goal.getTargetBooks());
        return new GoalResponse(goal.getTargetBooks(), booksRead, percent);
    }
}
