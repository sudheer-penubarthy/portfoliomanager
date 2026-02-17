package com.sudheer.portfoliotracker.service;

import com.sudheer.portfoliotracker.api.dto.*;
import com.sudheer.portfoliotracker.enums.GoalStatus;
import com.sudheer.portfoliotracker.exception.ResourceNotFoundException;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.GoalEntity;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.GoalFundAlignmentEntity;
import com.sudheer.portfoliotracker.repository.GoalFundAlignmentRepository;
import com.sudheer.portfoliotracker.repository.GoalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final GoalFundAlignmentRepository alignmentRepository;
    private final PortfolioService portfolioService;

    public GoalService(GoalRepository goalRepository,
                      GoalFundAlignmentRepository alignmentRepository,
                      PortfolioService portfolioService) {
        this.goalRepository = goalRepository;
        this.alignmentRepository = alignmentRepository;
        this.portfolioService = portfolioService;
    }

    /**
     * Create a new goal for a user
     */
    @Transactional
    public GoalDto createGoal(Long userId, GoalDto goalDto) {
        log.info("Creating goal for user: {}, goal name: {}", userId, goalDto.getName());

        GoalEntity goal = GoalEntity.builder()
                .userId(userId)
                .name(goalDto.getName())
                .description(goalDto.getDescription())
                .targetAmount(goalDto.getTargetAmount())
                .targetDate(goalDto.getTargetDate())
                .currentValue(BigDecimal.ZERO)
                .status(GoalStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        GoalEntity saved = goalRepository.save(goal);
        log.debug("Goal created with ID: {}", saved.getId());

        return convertToDto(saved);
    }

    /**
     * Get all goals for a user
     */
    public List<GoalDto> getUserGoals(Long userId) {
        log.debug("Fetching goals for user: {}", userId);
        List<GoalEntity> goals = goalRepository.findByUserId(userId);
        return goals.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific goal by ID
     */
    public GoalDto getGoal(Long goalId, Long userId) {
        log.debug("Fetching goal: {} for user: {}", goalId, userId);
        GoalEntity goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", "id", goalId.toString()));
        return convertToDto(goal);
    }

    /**
     * Update a goal
     */
    @Transactional
    public GoalDto updateGoal(Long goalId, Long userId, GoalDto goalDto) {
        log.info("Updating goal: {} for user: {}", goalId, userId);
        GoalEntity goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", "id", goalId.toString()));

        goal.setName(goalDto.getName());
        goal.setDescription(goalDto.getDescription());
        goal.setTargetAmount(goalDto.getTargetAmount());
        goal.setTargetDate(goalDto.getTargetDate());
        goal.setStatus(goalDto.getStatus() != null ? goalDto.getStatus() : goal.getStatus());

        GoalEntity updated = goalRepository.save(goal);
        log.debug("Goal updated: {}", goalId);

        return convertToDto(updated);
    }

    /**
     * Delete a goal
     */
    @Transactional
    public void deleteGoal(Long goalId, Long userId) {
        log.info("Deleting goal: {} for user: {}", goalId, userId);
        GoalEntity goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", "id", goalId.toString()));

        // Delete alignments first
        alignmentRepository.deleteByGoalId(goalId);
        goalRepository.delete(goal);

        log.debug("Goal deleted: {}", goalId);
    }

    /**
     * Align funds/schemes to a goal
     */
    @Transactional
    public void alignFundsToGoal(Long goalId, Long userId, List<String> schemeCodes) {
        log.info("Aligning {} schemes to goal: {}", schemeCodes.size(), goalId);

        // Verify goal exists
        GoalEntity goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", "id", goalId.toString()));

        // Clear existing alignments
        alignmentRepository.deleteByGoalId(goalId);

        // Create new alignments
        for (String schemeCode : schemeCodes) {
            GoalFundAlignmentEntity alignment = GoalFundAlignmentEntity.builder()
                    .goalId(goalId)
                    .schemeCode(schemeCode)
                    .createdAt(LocalDateTime.now())
                    .build();
            alignmentRepository.save(alignment);
        }

        log.debug("Aligned {} schemes to goal: {}", schemeCodes.size(), goalId);
    }

    /**
     * Get fund alignments for a goal
     */
    public List<String> getGoalFundAlignments(Long goalId, Long userId) {
        log.debug("Fetching fund alignments for goal: {}", goalId);

        // Verify goal exists
        goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", "id", goalId.toString()));

        return alignmentRepository.findByGoalId(goalId)
                .stream()
                .map(GoalFundAlignmentEntity::getSchemeCode)
                .collect(Collectors.toList());
    }

    /**
     * Calculate goal progress and tracking status
     */
    @Transactional
    public GoalDto getGoalWithProgress(Long goalId, Long userId) {
        log.debug("Calculating progress for goal: {}", goalId);
        GoalEntity goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", "id", goalId.toString()));

        // Update current value from portfolio
        List<String> alignedSchemes = alignmentRepository.findByGoalId(goalId)
                .stream()
                .map(GoalFundAlignmentEntity::getSchemeCode)
                .collect(Collectors.toList());

        if (!alignedSchemes.isEmpty()) {
            BigDecimal currentValue = portfolioService.calculatePortfolioValueForSchemes(userId, alignedSchemes);
            goal.setCurrentValue(currentValue);
            goalRepository.save(goal);
        }

        return convertToDto(goal);
    }

    /**
     * Get all goals with their tracking status
     */
    public List<GoalDto> getAllGoalsWithTracking(Long userId) {
        log.debug("Fetching all goals with tracking for user: {}", userId);
        List<GoalEntity> goals = goalRepository.findByUserId(userId);

        return goals.stream()
                .map(goal -> {
                    // Update current value
                    List<String> alignedSchemes = alignmentRepository.findByGoalId(goal.getId())
                            .stream()
                            .map(GoalFundAlignmentEntity::getSchemeCode)
                            .collect(Collectors.toList());

                    if (!alignedSchemes.isEmpty()) {
                        BigDecimal currentValue = portfolioService.calculatePortfolioValueForSchemes(userId, alignedSchemes);
                        goal.setCurrentValue(currentValue);
                    }

                    return convertToDto(goal);
                })
                .collect(Collectors.toList());
    }

    /**
     * Convert entity to DTO with computed fields
     */
    private GoalDto convertToDto(GoalEntity entity) {
        BigDecimal remaining = entity.getTargetAmount().subtract(entity.getCurrentValue());
        Double progress = entity.getTargetAmount().doubleValue() > 0
                ? (entity.getCurrentValue().doubleValue() / entity.getTargetAmount().doubleValue()) * 100
                : 0.0;

        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), entity.getTargetDate());

        String trackingStatus = calculateTrackingStatus(entity, progress, daysRemaining);

        return GoalDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .targetAmount(entity.getTargetAmount())
                .targetDate(entity.getTargetDate())
                .currentValue(entity.getCurrentValue())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .remainingAmount(remaining)
                .progressPercentage(progress)
                .daysRemaining(daysRemaining)
                .trackingStatus(trackingStatus)
                .build();
    }

    /**
     * Calculate tracking status based on progress and time remaining
     */
    private String calculateTrackingStatus(GoalEntity goal, Double progress, long daysRemaining) {
        if (progress >= 100) {
            return "OVERACHIEVED";
        }

        if (daysRemaining < 0) {
            return "COMPLETED";
        }

        // Expected progress based on time elapsed
        if (daysRemaining == 0) {
            return progress >= 100 ? "COMPLETED" : "AT_RISK";
        }

        long totalDays = ChronoUnit.DAYS.between(goal.getCreatedAt().toLocalDate(), goal.getTargetDate());
        long daysElapsed = totalDays - daysRemaining;

        if (totalDays > 0) {
            double expectedProgress = (daysElapsed * 100.0) / totalDays;

            if (progress >= expectedProgress) {
                return "ON_TRACK";
            } else {
                return "AT_RISK";
            }
        }

        return "ACTIVE";
    }
}

