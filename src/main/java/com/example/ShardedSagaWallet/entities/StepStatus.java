package com.example.ShardedSagaWallet.entities;

public enum StepStatus {
    PENDING,
    STARTED,
    RUNNING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
}
