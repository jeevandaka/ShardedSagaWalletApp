package com.example.ShardedSagaWallet.entities;

public enum StepStatus {
    STARTED,
    RUNNING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
}
