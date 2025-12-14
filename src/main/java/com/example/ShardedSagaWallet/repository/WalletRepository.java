package com.example.ShardedSagaWallet.repository;

import com.example.ShardedSagaWallet.entities.Wallet;
import com.example.ShardedSagaWallet.saga.SagaContext;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet,Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :id")
    Optional<Wallet> findByIdWithLock(@Param("id") Long id);

    void updateBalanceByUserId(Long userId, BigDecimal add);

    List<Wallet> findByUserId(Long userId);
}
