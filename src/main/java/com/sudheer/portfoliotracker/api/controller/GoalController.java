package com.sudheer.portfoliotracker.api.controller;

import com.sudheer.portfoliotracker.api.dto.GoalDto;
import com.sudheer.portfoliotracker.api.dto.GoalOptionDto;
import com.sudheer.portfoliotracker.api.dto.SchemeGoalAllocationDto;
import com.sudheer.portfoliotracker.api.dto.UpdateSchemeGoalAllocationsRequest;
import com.sudheer.portfoliotracker.service.GoalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    /**
     * Create a new goal for the current user
     *
     * @param userId  the user ID (typically from authentication context)
     * @param goalDto the goal data
     * @return 201 Created with location header and goal data
     */
    @PostMapping
    public ResponseEntity<GoalDto> createGoal(
            @RequestParam Long userId,
            @Valid @RequestBody GoalDto goalDto) {
        log.info("Creating goal for user: {}, goal name: {}", userId, goalDto.getName());
        GoalDto created = goalService.createGoal(userId, goalDto);
        return ResponseEntity.created(URI.create("/api/goals/" + created.getId())).body(created);
    }

    /**
     * Get all goals for a user
     *
     * @param userId the user ID
     * @return 200 OK with list of goals
     */
    @GetMapping
    public ResponseEntity<List<GoalDto>> getUserGoals(@RequestParam Long userId) {
        log.debug("Fetching goals for user: {}", userId);
        List<GoalDto> goals = goalService.getUserGoals(userId);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/options")
    public ResponseEntity<List<GoalOptionDto>> getUserGoalOptions(@RequestParam Long userId) {
        log.debug("Fetching goal options for user: {}", userId);
        return ResponseEntity.ok(goalService.getUserGoalOptions(userId));
    }

    /**
     * Get a specific goal
     *
     * @param goalId the goal ID
     * @param userId the user ID
     * @return 200 OK with goal data, or 404 Not Found
     */
    @GetMapping("/{goalId}")
    public ResponseEntity<GoalDto> getGoal(
            @PathVariable Long goalId,
            @RequestParam Long userId) {
        log.debug("Fetching goal: {} for user: {}", goalId, userId);
        GoalDto goal = goalService.getGoal(goalId, userId);
        return ResponseEntity.ok(goal);
    }

    /**
     * Update a goal
     *
     * @param goalId  the goal ID
     * @param userId  the user ID
     * @param goalDto the updated goal data
     * @return 200 OK with updated goal data, or 404 Not Found
     */
    @PutMapping("/{goalId}")
    public ResponseEntity<GoalDto> updateGoal(
            @PathVariable Long goalId,
            @RequestParam Long userId,
            @Valid @RequestBody GoalDto goalDto) {
        log.info("Updating goal: {} for user: {}", goalId, userId);
        GoalDto updated = goalService.updateGoal(goalId, userId, goalDto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a goal
     *
     * @param goalId the goal ID
     * @param userId the user ID
     * @return 204 No Content, or 404 Not Found
     */
    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable Long goalId,
            @RequestParam Long userId) {
        log.info("Deleting goal: {} for user: {}", goalId, userId);
        goalService.deleteGoal(goalId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Align funds/schemes to a goal
     *
     * @param goalId      the goal ID
     * @param userId      the user ID
     * @param schemeCodes list of scheme codes to align
     * @return 200 OK
     */
    @PostMapping("/{goalId}/alignments")
    public ResponseEntity<Void> alignFundsToGoal(
            @PathVariable Long goalId,
            @RequestParam Long userId,
            @Valid @RequestBody Map<String, List<String>> body) {
        log.info("Aligning funds to goal: {}", goalId);
        List<String> schemeCodes = body.get("schemeCodes");
        goalService.alignFundsToGoal(goalId, userId, schemeCodes);
        return ResponseEntity.ok().build();
    }

    /**
     * Get fund alignments for a goal
     *
     * @param goalId the goal ID
     * @param userId the user ID
     * @return 200 OK with list of scheme codes
     */
    @GetMapping("/{goalId}/alignments")
    public ResponseEntity<List<String>> getGoalAlignments(
            @PathVariable Long goalId,
            @RequestParam Long userId) {
        log.debug("Fetching alignments for goal: {}", goalId);
        List<String> alignments = goalService.getGoalFundAlignments(goalId, userId);
        return ResponseEntity.ok(alignments);
    }

    @GetMapping("/schemes/{schemeCode}/alignments")
    public ResponseEntity<List<SchemeGoalAllocationDto>> getSchemeGoalAllocations(
            @PathVariable String schemeCode,
            @RequestParam Long userId) {
        log.debug("Fetching goal allocations for user: {}, scheme: {}", userId, schemeCode);
        return ResponseEntity.ok(goalService.getSchemeGoalAllocations(userId, schemeCode));
    }

    @PutMapping("/schemes/{schemeCode}/alignments")
    public ResponseEntity<List<SchemeGoalAllocationDto>> updateSchemeGoalAllocations(
            @PathVariable String schemeCode,
            @RequestParam Long userId,
            @Valid @RequestBody UpdateSchemeGoalAllocationsRequest request) {
        log.info("Updating goal allocations for user: {}, scheme: {}", userId, schemeCode);
        return ResponseEntity.ok(goalService.updateSchemeGoalAllocations(userId, schemeCode, request.getAllocations()));
    }

    /**
     * Get goal with current progress and tracking status
     *
     * @param goalId the goal ID
     * @param userId the user ID
     * @return 200 OK with goal data including progress
     */
    @GetMapping("/{goalId}/progress")
    public ResponseEntity<GoalDto> getGoalProgress(
            @PathVariable Long goalId,
            @RequestParam Long userId) {
        log.debug("Fetching progress for goal: {}", goalId);
        GoalDto goal = goalService.getGoalWithProgress(goalId, userId);
        return ResponseEntity.ok(goal);
    }

    /**
     * Get all goals with tracking status
     *
     * @param userId the user ID
     * @return 200 OK with list of goals with tracking info
     */
    @GetMapping("/tracking/all")
    public ResponseEntity<List<GoalDto>> getAllGoalsWithTracking(@RequestParam Long userId) {
        log.debug("Fetching all goals with tracking for user: {}", userId);
        List<GoalDto> goals = goalService.getAllGoalsWithTracking(userId);
        return ResponseEntity.ok(goals);
    }
}

