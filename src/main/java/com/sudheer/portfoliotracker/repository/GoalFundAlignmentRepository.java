package com.sudheer.portfoliotracker.repository;

import com.sudheer.portfoliotracker.infrastructure.persistence.entity.GoalFundAlignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalFundAlignmentRepository extends JpaRepository<GoalFundAlignmentEntity, Long> {
    List<GoalFundAlignmentEntity> findByGoalId(Long goalId);
    List<GoalFundAlignmentEntity> findByGoalIdIn(List<Long> goalIds);
    List<GoalFundAlignmentEntity> findByGoalIdInAndSchemeCode(List<Long> goalIds, String schemeCode);

    List<GoalFundAlignmentEntity> findBySchemeCode(String schemeCode);

    void deleteByGoalId(Long goalId);
    void deleteByGoalIdInAndSchemeCode(List<Long> goalIds, String schemeCode);
}

