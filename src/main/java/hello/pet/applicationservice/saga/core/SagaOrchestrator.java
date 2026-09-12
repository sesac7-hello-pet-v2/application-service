package hello.pet.applicationservice.saga.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 업무 내용을 몰라도 Step을 순서대로 실행하고, 실패하면 완료된 Step을 역순으로 보상한다. */
@Slf4j
@Component
public class SagaOrchestrator {

    public <T> void execute(List<SagaStep<T>> steps, T context) throws SagaExecutionException {
        String sagaId = generateSagaId();
        log.info("[SAGA-{}] ========== Saga 시작 ==========", sagaId);
        log.info("[SAGA-{}] 총 {} 단계 실행 예정", sagaId, steps.size());

        // 정상 반환한 단계만 보상 대상으로 기록한다. 실행 기록은 메모리에만 존재한다.
        List<SagaStep<T>> completedSteps = new ArrayList<>();

        for (int i = 0; i < steps.size(); i++) {
            SagaStep<T> step = steps.get(i);

            try {
                log.info("[SAGA-{}] Step {}/{}: {} 실행 시작",
                        sagaId, i + 1, steps.size(), step.getName());

                // 로컬 트랜잭션 Step은 Spring 프록시의 커밋까지 성공해야 정상 반환한다.
                step.execute(context);

                completedSteps.add(step);

                log.info("[SAGA-{}] Step {}/{}: {} ✅ 성공",
                        sagaId, i + 1, steps.size(), step.getName());

            } catch (Exception e) {
                log.error("[SAGA-{}] Step {}/{}: {} ❌ 실패 - {}",
                        sagaId, i + 1, steps.size(), step.getName(), e.getMessage());

                // 원래 작업 실패를 cause로 유지하고, 보상 실패는 이 예외에 추가한다.
                SagaExecutionException failure = new SagaExecutionException(
                        String.format("Saga 실행 실패 - Step: %s", step.getName()), e
                );
                compensate(completedSteps, context, sagaId, failure);
                throw failure;
            }
        }

        log.info("[SAGA-{}] ========== Saga 완료 ==========", sagaId);
    }

    private <T> void compensate(List<SagaStep<T>> completedSteps, T context, String sagaId,
                                SagaExecutionException failure) {
        if (completedSteps.isEmpty()) {
            log.info("[SAGA-{}] 보상할 Step이 없습니다", sagaId);
            return;
        }

        log.warn("[SAGA-{}] ========== 보상 시작 ({} 단계) ==========",
                sagaId, completedSteps.size());

        // 가장 최근에 완료한 작업부터 취소한다.
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            SagaStep<T> step = completedSteps.get(i);

            try {
                log.info("[SAGA-{}] 보상 {}/{}: {} 시작",
                        sagaId, completedSteps.size() - i, completedSteps.size(), step.getName());

                step.compensate(context);

                log.info("[SAGA-{}] 보상 {}/{}: {} ✅ 완료",
                        sagaId, completedSteps.size() - i, completedSteps.size(), step.getName());

            } catch (Exception e) {
                // 보상 실패를 수집하고, 다음 보상도 계속 실행한다.
                failure.addSuppressed(new IllegalStateException("보상 실패 - Step: " + step.getName(), e));
                log.error("[SAGA-{}] 보상 실패: {} - 나머지 보상을 계속 진행합니다.",
                        sagaId, step.getName(), e);
            }
        }

        // 일부라도 보상에 실패했다면 완료로 기록하지 않는다.
        if (failure.hasCompensationFailures()) {
            log.error("[SAGA-{}] ========== 보상 미완료 (실패 {}건) ==========",
                    sagaId, failure.getSuppressed().length);
        } else {
            log.warn("[SAGA-{}] ========== 보상 완료 ==========", sagaId);
        }
    }

    // 현재 실행의 로그를 묶는 표시용 ID이며, 영속적인 작업 식별자는 아니다.
    private String generateSagaId() {
        return String.valueOf(System.currentTimeMillis()).substring(6);
    }
}
