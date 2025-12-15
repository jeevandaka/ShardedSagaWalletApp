package com.example.ShardedSagaWallet.saga;

import com.example.ShardedSagaWallet.entities.Transaction;
import com.example.ShardedSagaWallet.saga.steps.SagaStepFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferSagaService {

    private final TransactionService transactionService;
    private final SagaOrchestrator sagaOrchestrator;


    @Transactional
    public Long initiateTransfer(
            Long fromWalletId,
            Long toWalletId,
            BigDecimal amount,
            String description
    ) {
        log.info("Initiating transfer from wallet {} to wallet {} with amount {} and description {}", fromWalletId, toWalletId, amount, description);

        Transaction transaction = transactionService.createTransaction(fromWalletId, toWalletId, amount, description);

        SagaContext sagaContext = SagaContext.builder()
                .data(Map.ofEntries(
                        Map.entry("transactionId", transaction.getId()),
                        Map.entry("fromWalletId", fromWalletId),
                        Map.entry("toWalletId", toWalletId),
                        Map.entry("amount", amount),
                        Map.entry("description", description)
                ))
                .build();

        log.info("Saga context created with id {}", sagaContext.get("description"));

        Long sagaInstanceId = sagaOrchestrator.startSaga(sagaContext);
        log.info("Saga instance created with id {}", sagaInstanceId);

        transactionService.updateTransactionWithSagaInstanceId(transaction.getId(), sagaInstanceId);

        executeTransferSaga(sagaInstanceId);

        return sagaInstanceId;
    }

    public void executeTransferSaga(Long sagaInstanceId) {
        log.info("Executing transfer saga with id {}", sagaInstanceId);


        try {
            for(SagaStepFactory.SagaStepType step : SagaStepFactory.TransferMoneySagaSteps) {
                log.info("Going to execute step {}", step.toString());
                boolean success  = sagaOrchestrator.executeStep(sagaInstanceId, step.toString() );
                if(!success) {
                    log.error("Failed to execute step {}", step.toString());
                    sagaOrchestrator.failSaga(sagaInstanceId);
                    return;
                }

            }
            sagaOrchestrator.completeSaga(sagaInstanceId);
            log.info("Transfer saga completed with id {}", sagaInstanceId);
        } catch (Exception e) {
            log.error("Failed to execute transfer saga with id {}", sagaInstanceId, e);
            sagaOrchestrator.failSaga(sagaInstanceId);

        }
    }
}

