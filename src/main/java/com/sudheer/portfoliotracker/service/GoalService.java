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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    @Transactional(readOnly = true)
    public List<GoalDto> getUserGoals(Long userId) {
        log.debug("Fetching goals for user: {}", userId);
        List<GoalEntity> goals = goalRepository.findByUserIdOrderByTargetDateAsc(userId);
        return goals.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GoalOptionDto> getUserGoalOptions(Long userId) {
        log.debug("Fetching goal options for user: {}", userId);
        return goalRepository.findGoalOptionsByUserId(userId);
    }

    /**
     * Get a specific goal by ID
     */
    @Transactional(readOnly = true)
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
                    .allocationPercentage(BigDecimal.valueOf(100))
                    .createdAt(LocalDateTime.now())
                    .build();
            alignmentRepository.save(alignment);
        }

        log.debug("Aligned {} schemes to goal: {}", schemeCodes.size(), goalId);
    }

    /**
     * Get fund alignments for a goal
     */
    @Transactional(readOnly = true)
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

    public List<SchemeGoalAllocationDto> getSchemeGoalAllocations(Long userId, String schemeCode) {
        Map<Long, GoalEntity> goalsById = goalRepository.findByUserIdOrderByTargetDateAsc(userId).stream()
                .collect(Collectors.toMap(GoalEntity::getId, goal -> goal, (left, right) -> left, LinkedHashMap::new));

        if (goalsById.isEmpty()) {
            return List.of();
        }

        return alignmentRepository.findByGoalIdInAndSchemeCode(List.copyOf(goalsById.keySet()), schemeCode).stream()
                .map(alignment -> SchemeGoalAllocationDto.builder()
                        .goalId(alignment.getGoalId())
                        .goalName(goalsById.containsKey(alignment.getGoalId()) ? goalsById.get(alignment.getGoalId()).getName() : null)
                        .allocationPercentage(alignment.getAllocationPercentage())
                        .build())
                .sorted((left, right) -> String.valueOf(left.getGoalName()).compareToIgnoreCase(String.valueOf(right.getGoalName())))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<SchemeGoalAllocationDto> updateSchemeGoalAllocations(Long userId, String schemeCode, List<SchemeGoalAllocationDto> allocations) {
        List<SchemeGoalAllocationDto> normalizedAllocations = allocations == null ? List.of() : allocations.stream()
                .filter(Objects::nonNull)
                .filter(dto -> dto.getGoalId() != null)
                .filter(dto -> dto.getAllocationPercentage() != null)
                .filter(dto -> dto.getAllocationPercentage().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        Map<Long, GoalEntity> goalsById = goalRepository.findByUserIdOrderByTargetDateAsc(userId).stream()
                .collect(Collectors.toMap(GoalEntity::getId, goal -> goal, (left, right) -> left, LinkedHashMap::new));
        Set<Long> allowedGoalIds = goalsById.keySet();

        BigDecimal totalAllocation = BigDecimal.ZERO;
        Set<Long> seenGoalIds = new java.util.HashSet<>();
        for (SchemeGoalAllocationDto allocation : normalizedAllocations) {
            if (!allowedGoalIds.contains(allocation.getGoalId())) {
                throw new ResourceNotFoundException("Goal", "id", String.valueOf(allocation.getGoalId()));
            }
            if (!seenGoalIds.add(allocation.getGoalId())) {
                throw new IllegalArgumentException("Duplicate goal allocation submitted for goal id " + allocation.getGoalId());
            }
            if (allocation.getAllocationPercentage().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Allocation percentage cannot exceed 100 for a single goal");
            }
            totalAllocation = totalAllocation.add(allocation.getAllocationPercentage());
        }

        if (totalAllocation.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Total allocation percentage for a fund cannot exceed 100");
        }

        if (!allowedGoalIds.isEmpty()) {
            alignmentRepository.deleteByGoalIdInAndSchemeCode(List.copyOf(allowedGoalIds), schemeCode);
        }

        for (SchemeGoalAllocationDto allocation : normalizedAllocations) {
            alignmentRepository.save(GoalFundAlignmentEntity.builder()
                    .goalId(allocation.getGoalId())
                    .schemeCode(schemeCode)
                    .allocationPercentage(allocation.getAllocationPercentage())
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return getSchemeGoalAllocations(userId, schemeCode);
    }

    /**
     * Calculate goal progress and tracking status
     */
    @Transactional(readOnly = true)
    public GoalDto getGoalWithProgress(Long goalId, Long userId) {
        log.debug("Calculating progress for goal: {}", goalId);
        GoalEntity goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", "id", goalId.toString()));

        Map<Long, BigDecimal> goalValuesById = calculateGoalCurrentValues(userId, List.of(goal));
        goal.setCurrentValue(goalValuesById.getOrDefault(goalId, BigDecimal.ZERO));

        return convertToDto(goal);
    }

    /**
     * Get all goals with their tracking status
     */
    @Transactional(readOnly = true)
    public List<GoalDto> getAllGoalsWithTracking(Long userId) {
        log.debug("Fetching all goals with tracking for user: {}", userId);
        List<GoalEntity> goals = goalRepository.findByUserIdOrderByTargetDateAsc(userId);
        Map<Long, BigDecimal> goalValuesById = calculateGoalCurrentValues(userId, goals);

        return goals.stream()
                .map(goal -> {
                    goal.setCurrentValue(goalValuesById.getOrDefault(goal.getId(), BigDecimal.ZERO));
                    return convertToDto(goal);
                })
                .collect(Collectors.toList());
    }

    private Map<Long, BigDecimal> calculateGoalCurrentValues(Long userId, List<GoalEntity> goals) {
        if (goals == null || goals.isEmpty()) {
            return Map.of();
        }

        List<Long> goalIds = goals.stream()
                .map(GoalEntity::getId)
                .filter(Objects::nonNull)
                .toList();

        if (goalIds.isEmpty()) {
            return Map.of();
        }

        List<GoalFundAlignmentEntity> alignments = alignmentRepository.findByGoalIdIn(goalIds);
        if (alignments.isEmpty()) {
            return Map.of();
        }

        Map<String, BigDecimal> schemeValues = portfolioService.calculatePortfolioValuesForSchemes(
                userId,
                alignments.stream()
                        .map(GoalFundAlignmentEntity::getSchemeCode)
                        .filter(Objects::nonNull)
                        .toList()
        );

        Map<Long, BigDecimal> goalValuesById = new HashMap<>();
        for (GoalFundAlignmentEntity alignment : alignments) {
            BigDecimal schemeValue = schemeValues.get(alignment.getSchemeCode());
            if (schemeValue == null) {
                continue;
            }

            BigDecimal allocationPercentage = alignment.getAllocationPercentage() == null
                    ? BigDecimal.valueOf(100)
                    : alignment.getAllocationPercentage();

            BigDecimal allocatedValue = schemeValue
                    .multiply(allocationPercentage)
                    .divide(BigDecimal.valueOf(100), 8, java.math.RoundingMode.HALF_UP);

            goalValuesById.merge(alignment.getGoalId(), allocatedValue, BigDecimal::add);
        }

        return goalValuesById;
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

