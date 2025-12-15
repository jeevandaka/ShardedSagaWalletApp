package com.example.ShardedSagaWallet.saga.steps;

import com.example.ShardedSagaWallet.entities.Wallet;
import com.example.ShardedSagaWallet.repository.WalletRepository;
import com.example.ShardedSagaWallet.saga.SagaContext;
import com.example.ShardedSagaWallet.saga.SagaStepInterface;
import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditDestinationWalletStep implements SagaStepInterface {

    private static final Logger log = LoggerFactory.getLogger(CreditDestinationWalletStep.class);
    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public boolean execute(SagaContext context) {
        // 1. get the destination wallet id from the context
        Long toWalletId = context.getLong("toWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        log.info("Crediting destination wallet {} with amount {}", toWalletId, amount);

        // 2. Fetch the destination wallet from the database with the lock
        Wallet wallet = walletRepository.findByIdWithLock(toWalletId)
                .orElseThrow(() -> new RuntimeException("wallet not found"));

        log.info("wallet fetched with balance {}", wallet.getBalance());

        context.put("originalToWalletBalance",wallet.getBalance());

        // 3. Credit the destination wallet
//        wallet.credit(amount);
//        walletRepository.save(wallet);
//
        walletRepository.updateBalanceByUserId(toWalletId, wallet.getBalance().add(amount));

        log.info("wallet saved with balance {}", wallet.getBalance());
        context.put("toWalletBalanceAfterCredit", wallet.getBalance());

        // 4. Update the context with the change -> already done above

        log.info("Credit destination wallet executed successfully");
        return true;
    }

    @Transactional
    @Override
    public boolean compensate(SagaContext context) {
        // 1. get the destination wallet id from the context
        Long toWalletId = context.getLong("toWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        log.info("Compensating destination wallet {} with amount {}", toWalletId, amount);

        // 2. Fetch the destination wallet from the database with the lock
        Wallet wallet = walletRepository.findByIdWithLock(toWalletId)
                .orElseThrow(() -> new RuntimeException("wallet not found"));

        log.info("wallet fetched with balance {} for compensation", wallet.getBalance());

        //context.put("originalToWalletBalance",wallet.getBalance());

        // 3. Debit from the destination wallet
//        wallet.debit(amount);
//        walletRepository.save(wallet);

        walletRepository.updateBalanceByUserId(toWalletId, wallet.getBalance().subtract(amount));

        context.put("toWalletBalanceAfterCreditCompensation", wallet.getBalance());

        // 4. Update the context with the change -> already done above

        log.info("Compensating Credit destination wallet executed successfully");
        return true;
    }

    @Override
    public String getStepName() {
        return "";
    }
}
