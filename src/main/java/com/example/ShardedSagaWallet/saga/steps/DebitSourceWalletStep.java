package com.example.ShardedSagaWallet.saga.steps;

import com.example.ShardedSagaWallet.entities.Wallet;
import com.example.ShardedSagaWallet.repository.WalletRepository;
import com.example.ShardedSagaWallet.saga.SagaContext;
import com.example.ShardedSagaWallet.saga.SagaStepInterface;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DebitSourceWalletStep implements SagaStepInterface {

    private final WalletRepository walletRepository;
    private static final Logger log = LoggerFactory.getLogger(CreditDestinationWalletStep.class);

    @Transactional
    @Override
    public boolean execute(SagaContext context) {
        // 1. get amount and id from context
        Long id = context.getLong("fromWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        log.info("Debiting Source wallet {} with amount {}", id, amount);
        //2. fetch source wallet from database with lock
        Wallet sourceWallet = walletRepository.findByIdWithLock(id)
                .orElseThrow(()-> new RuntimeException("Wallet Not Found"));

        if(!sourceWallet.hasSufficientBalance(amount)){
            throw new RuntimeException("Insufficient balance");
        }

        log.info("wallet fetched with balance {}", sourceWallet.getBalance());
        context.put("originalFromWalletBalance",sourceWallet.getBalance());

        //3. debit amount from source wallet
        sourceWallet.debit(amount);

        //4. save into database
        walletRepository.save(sourceWallet);

        log.info("wallet saved with balance {}", sourceWallet.getBalance());

        //5. update the context
        context.put("fromWalletBalanceAfterDebit", sourceWallet.getBalance());
        return true;
    }

    @Transactional
    @Override
    public boolean compensate(SagaContext context) {
        // 1. get amount and id from context
        Long id = context.getLong("fromWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        log.info("Compensating Source wallet {} with amount {}", id, amount);
        //2. fetch source wallet from database with lock
        Wallet sourceWallet = walletRepository.findByIdWithLock(id)
                .orElseThrow(()-> new RuntimeException("Wallet Not Found"));

//        log.info("wallet fetched with balance {}", sourceWallet.getBalance());

        //3. debit amount from source wallet
        sourceWallet.credit(amount);

        //4. save into database
        walletRepository.save(sourceWallet);

//        log.info("wallet saved with balance {}", sourceWallet.getBalance());

        //5. update the context
        context.put("fromWalletBalanceAfterCompensation", sourceWallet.getBalance());
        return true;
    }

    @Override
    public String getStepName() {
        return "";
    }
}
