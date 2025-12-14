package com.example.ShardedSagaWallet.config;

import com.example.ShardedSagaWallet.saga.SagaStepInterface;
import com.example.ShardedSagaWallet.saga.steps.CreditDestinationWalletStep;
import com.example.ShardedSagaWallet.saga.steps.DebitSourceWalletStep;
import com.example.ShardedSagaWallet.saga.steps.SagaStepFactory;
import com.example.ShardedSagaWallet.saga.steps.UpdateTransactionStatusStep;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SagaConfiguration {

    @Bean
    public Map<String, SagaStepInterface> sagaStepMap(
            DebitSourceWalletStep debitSourceWalletStep,
            CreditDestinationWalletStep creditDestinationWalletStep,
            UpdateTransactionStatusStep updateTransactionStatus
    ) {
        Map<String, SagaStepInterface> map = new HashMap<>();
        map.put(SagaStepFactory.SagaStepType.DEBIT_SOURCE_WALLET_STEP.toString(), debitSourceWalletStep);
        map.put(SagaStepFactory.SagaStepType.CREDIT_DESTINATION_WALLET_STEP.toString(), creditDestinationWalletStep);
        map.put(SagaStepFactory.SagaStepType.UPDATE_TRANSACTION_STATUS_STEP.toString(), updateTransactionStatus);
        return map;
    }

}
