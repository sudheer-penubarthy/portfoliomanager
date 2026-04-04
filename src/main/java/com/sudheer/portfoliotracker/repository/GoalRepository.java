package com.sudheer.portfoliotracker.repository;

import com.sudheer.portfoliotracker.api.dto.GoalOptionDto;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.GoalEntity;
import com.sudheer.portfoliotracker.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<GoalEntity, Long> {
    List<GoalEntity> findByUserIdOrderByTargetDateAsc(Long userId);

    @Query("""
            select new com.sudheer.portfoliotracker.api.dto.GoalOptionDto(g.id, g.name)
            from GoalEntity g
            where g.userId = :userId
            order by g.targetDate asc, g.id asc
            """)
    List<GoalOptionDto> findGoalOptionsByUserId(Long userId);

    List<GoalEntity> findByUserIdAndStatus(Long userId, GoalStatus status);

    Optional<GoalEntity> findByIdAndUserId(Long goalId, Long userId);

    List<GoalEntity> findByUserIdAndTargetDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}

