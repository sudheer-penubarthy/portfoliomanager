package com.example.portfoliotracker.repository;

import com.example.portfoliotracker.entity.UserHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserHoldingRepository extends JpaRepository<UserHolding, Long> {
    Optional<UserHolding> findByUserIdAndSchemeCode(Long userId, String schemeCode);
    List<UserHolding> findByUserId(Long userId);
}
