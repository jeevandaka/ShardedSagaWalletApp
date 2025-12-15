package com.example.ShardedSagaWallet.saga;

import com.example.ShardedSagaWallet.entities.SagaInstance;
import com.example.ShardedSagaWallet.entities.SagaStatus;
import com.example.ShardedSagaWallet.entities.SagaStep;
import com.example.ShardedSagaWallet.entities.StepStatus;
import com.example.ShardedSagaWallet.repository.SagaInstanceRepository;
import com.example.ShardedSagaWallet.repository.SagaStepRepository;
import com.example.ShardedSagaWallet.saga.steps.SagaStepFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestratorImpl implements SagaOrchestrator{

    private final ObjectMapper objectMapper;
    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;
    private final SagaStepFactory sagaStepFactory;

    @Override
    @Transactional
    public Long startSaga(SagaContext context) {
        try {
            String contextJson = objectMapper.writeValueAsString(context); // convert the context to a json as a string
            System.out.println("context in start saga " + contextJson);
            SagaInstance sagaInstance = SagaInstance
                    .builder()
                    .context(contextJson)
                    .status(SagaStatus.STARTED)
                    .build();

            sagaInstance = sagaInstanceRepository.save(sagaInstance);

            log.info("Started saga with id {}", sagaInstance.getId());

            log.info("Started saga with context {}", sagaInstance.getContext().getBytes(StandardCharsets.UTF_8));
            System.out.println("context " + sagaInstance.getContext());
            return sagaInstance.getId();

        } catch (Exception e) {
            log.error("Error starting saga", e);
            throw new RuntimeException("Error starting saga", e);
        }
    }

    @Override
    @Transactional
    public boolean executeStep(Long sagaInstanceId, String stepName) {

        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("Saga instance not found"));

        SagaStepInterface step = sagaStepFactory.getSagaStep(stepName);
        if(step == null) {
            log.error("Saga step not found for step name {}", stepName);
            throw new RuntimeException("Saga step not found");
        }

        SagaStep sagaStepDB = sagaStepRepository
                .findBySagaInstanceIdAndStepNameAndStatus(sagaInstanceId, stepName, StepStatus.PENDING)
                .orElse(
                        SagaStep.builder().sagaInstanceId(sagaInstanceId).stepName(stepName).status(StepStatus.PENDING).build()
                );

        if(sagaStepDB.getId() == null) {
            try{
                sagaStepDB = sagaStepRepository.save(sagaStepDB);
            }catch (Exception e){
                log.error("exception ocurred while saving step into db for first time " + e);
            }
        }

        log.info("sagaStepDB", sagaStepDB);

        try {
            log.info("fetching context from sagainstance", sagaStepDB);
            SagaContext sagaContext = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);
            sagaStepDB.markAsRunning();
            log.info("saving running step status in sagastep repo", sagaStepDB);
            sagaStepRepository.save(sagaStepDB); // updating the status to running in db

            log.info("sagaStepDB running", sagaStepDB);

            boolean success = step.execute(sagaContext);

            if(success) {
                sagaStepDB.markAsCompleted();
                sagaStepRepository.save(sagaStepDB); // updating the status to completed in db

                sagaInstance.setCurrentStep(stepName); // step we just completed
                sagaInstance.markAsRunning();
                sagaInstanceRepository.save(sagaInstance); // updating the status to running in db

                log.info("Step {} executed successfully", stepName);
                return true;
            } else {
                sagaStepDB.markAsFailed();
                sagaStepRepository.save(sagaStepDB); // updating the status to failed in db
                log.error("Step {} failed", stepName);
                return false;
            }

        } catch (Exception e) {
            sagaStepDB.markAsFailed();
            sagaStepRepository.save(sagaStepDB);
            log.error("Failed to execute step {}", stepName);
            log.error("Failed to execute step with exception", e);
            return false;
        }
    }

    @Transactional
    @Override
    public boolean compensateStep(Long sagaInstanceId, String stepName) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("Saga instance not found"));

        SagaStepInterface step = sagaStepFactory.getSagaStep(stepName);
        if(step == null) {
            log.error("Saga step not found to compensate for step name {}", stepName);
            throw new RuntimeException("Saga step not found");
        }

        SagaStep sagaStepDB = sagaStepRepository
                .findBySagaInstanceIdAndStepNameAndStatus(sagaInstanceId, stepName, StepStatus.COMPLETED)
                .orElse(null);

        if(sagaStepDB == null) {
            log.info("step not found in db so it is compensated " + stepName);
            return true;
        }

        try {
            SagaContext sagaContext = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);
            sagaStepDB.markAsCompensating();
            sagaStepRepository.save(sagaStepDB); // updating the status to compensating in db

            boolean success = step.compensate(sagaContext);

            if(success) {
                sagaStepDB.markAsCompensated();
                sagaStepRepository.save(sagaStepDB); // updating the status to compensated in db

                sagaInstance.setCurrentStep(stepName); // step we just completed
                sagaInstance.markAsCompensating();
                sagaInstanceRepository.save(sagaInstance); // updating the status to compensating in db

                log.info("Step {} compensated successfully", stepName);
                return true;
            } else {
                sagaStepDB.markAsFailed();
                sagaStepRepository.save(sagaStepDB); // updating the status to failed in db
                log.error("Step {} failed", stepName);
                return false;
            }

        } catch (Exception e) {
            sagaStepDB.markAsFailed();
            sagaStepRepository.save(sagaStepDB);
            log.error("Failed to execute step {}", stepName);
            return false;
        }

    }

    @Override
    public SagaInstance getSagaInstance(Long sagaInstanceId) {
        return sagaInstanceRepository.findById(sagaInstanceId).orElseThrow(() -> new RuntimeException(""));
    }

    @Transactional
    @Override
    public void compensateSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = getSagaInstance(sagaInstanceId);

        sagaInstance.markAsCompensating();
        sagaInstanceRepository.save(sagaInstance);
        boolean allCompensated = true;
        List<SagaStep> allCompletedList = sagaStepRepository.findCompletedStepsBySagaInstanceId(sagaInstanceId);
        for(SagaStep step : allCompletedList){
            boolean compensated = compensateStep(sagaInstanceId,step.getStepName());

            if(!compensated){
                log.info("saga step is not compensated " + step.getStepName());
                allCompensated = false;
            }
        }

        if(!allCompensated){
            log.error("saga is not compensated");
        }else{
            log.info("saga is compensated");
            sagaInstance.markAsCompensated();
            sagaInstanceRepository.save(sagaInstance);
        }
    }

    @Transactional
    @Override
    public void failSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId).orElseThrow(()->new RuntimeException(""));
        sagaInstance.markAsFailed();
        log.info("marked fail to saga");
        sagaInstanceRepository.save(sagaInstance);

        completeSaga(sagaInstanceId);
        log.info("compensated the saga");

    }

    @Transactional
    @Override
    public void completeSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId).orElseThrow(()->new RuntimeException(""));
        sagaInstance.markAsCompensated();
        sagaInstanceRepository.save(sagaInstance);

    }
}
