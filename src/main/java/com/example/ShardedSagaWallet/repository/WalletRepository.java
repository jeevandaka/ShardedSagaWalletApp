package com.example.ShardedSagaWallet.repository;

import com.example.ShardedSagaWallet.entities.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet,Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w from wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdWithLock(Long id);
}
