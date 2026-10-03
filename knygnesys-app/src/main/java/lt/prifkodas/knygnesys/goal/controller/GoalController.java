package lt.prifkodas.knygnesys.goal.controller;

import lombok.RequiredArgsConstructor;
import lt.prifkodas.knygnesys.goal.dto.GoalResponse;
import lt.prifkodas.knygnesys.goal.dto.SetGoalRequest;
import lt.prifkodas.knygnesys.goal.service.GoalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GoalController implements GoalApi {

    private final GoalService goalService;

    @Override
    public ResponseEntity<GoalResponse> getGoal(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(goalService.getGoal(userDetails.getUsername()));
    }

    @Override
    public ResponseEntity<GoalResponse> getGoalByUserId(Integer userId) {
        return ResponseEntity.ok(goalService.getGoalByUserId(userId));
    }

    @Override
    public ResponseEntity<GoalResponse> setGoal(@AuthenticationPrincipal UserDetails userDetails, SetGoalRequest request) {
        return ResponseEntity.ok(goalService.setGoal(userDetails.getUsername(), request));
    }
}
