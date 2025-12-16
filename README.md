# 🧩 Sharded Saga Wallet Application

This project implements a distributed wallet transfer system using the Saga Pattern (Orchestration-based) in Spring Boot (Java).
It ensures data consistency across multiple local transactions without using 2PC, and is designed to work with database sharding (ShardingSphere).

# 🚀 Key Features

- Orchestrated Saga Pattern for distributed transactions
- Persistent Saga State (SagaInstance, SagaStep)
- Pessimistic locking on wallet rows to prevent race conditions
- Compensation logic for rollback on failure
- Transaction lifecycle tracking
- Sharding-friendly repository design

<img width="1097" height="385" alt="saga drawio (1)" src="https://github.com/user-attachments/assets/4a3b2f18-1073-4f1d-8dbf-92d7aae135bb" />
