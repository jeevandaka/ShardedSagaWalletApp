package com.example.ShardedSagaWallet.saga.steps;

import com.example.ShardedSagaWallet.entities.Transaction;
import com.example.ShardedSagaWallet.entities.TransactionStatus;
import com.example.ShardedSagaWallet.repository.TransactionRepository;
import com.example.ShardedSagaWallet.saga.SagaContext;
import com.example.ShardedSagaWallet.saga.SagaStepInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateTransactionStatusStep implements SagaStepInterface {

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public boolean execute(SagaContext context) {
        Long transactionId = context.getLong("transactionId");

        log.info("Updating transaction status for transaction {}", transactionId);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction Not Found"));

        context.put("originalTransactionStatus", transaction.getTransactionStatus());

        transaction.setTransactionStatus(TransactionStatus.SUCCESS);

        transactionRepository.save(transaction);
        log.info("Transaction status updated for transaction {}", transactionId);
        context.put("transactionStatusAfterUpdate" , transaction.getTransactionStatus());
        log.info("Update transaction step executed successfully");

        return true;
    }

    @Override
    @Transactional
    public boolean compensate(SagaContext context) {
        Long transactionId = context.getLong("transactionId");

        log.info("Compensating transaction status for transaction {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction Not Found"));

        TransactionStatus original = (TransactionStatus) context.get("originalTransactionStatus");

        transaction.setTransactionStatus(original);

        transactionRepository.save(transaction);

        context.put("statusAfterCompensation",transaction.getTransactionStatus());

        log.info("Compensated transaction status for transaction {}", transactionId);
        return false;
    }

    @Override
    public String getStepName() {
        return "";
    }
}
