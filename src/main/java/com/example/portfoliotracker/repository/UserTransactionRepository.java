package com.example.portfoliotracker.repository;

import com.example.portfoliotracker.entity.UserTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserTransactionRepository extends JpaRepository<UserTransaction, Long> {
    List<UserTransaction> findByUserIdAndTxnDateBetween(Long userId, LocalDate from, LocalDate to);
    boolean existsByUserIdAndSourceReference(Long userId, String sourceReference);
}
