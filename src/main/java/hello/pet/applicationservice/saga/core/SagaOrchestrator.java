package hello.pet.applicationservice.saga.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Saga 단계를 순서대로 실행하고, 실패하면 성공한 단계를 역순으로 보상한다.
@Slf4j
@Component
public class SagaOrchestrator {

    /**
     * 각 단계를 순서대로 실행하고, 실패하면 앞서 성공한 단계의 보상을 역순으로 시도한다.
     *
     * @param <T> 호출할 때 정해지는 Context 타입. 입양 승인에서는 AdoptionSagaContext가 된다.
     *            단계가 받는 타입과 전달하는 객체의 타입을 일치시켜, 잘못된 타입을 넘기면 컴파일 오류로 알려준다.
     * @param steps 순서대로 실행할 단계 목록. 모든 단계는 T 타입의 Context를 받는다.
     * @param context 실행과 보상에 필요한 정보를 담은 객체. 모든 단계에 같은 객체를 전달한다.
     */
    public <T> void execute(List<SagaStep<T>> steps, T context) {
        // 실행별 로그를 구분하는 ID
        String sagaId = UUID.randomUUID().toString();
        log.info("[SAGA-{}] Saga 시작", sagaId);
        log.info("[SAGA-{}] 총 {} 단계 실행 예정", sagaId, steps.size());

        // 성공한 단계만 기록한다. 예를 들어 3단계 실패 시 2단계 → 1단계 순서로 보상한다.
        // 이 목록은 JVM 메모리에만 있어 서버 재시작 후 복구에는 사용할 수 없다.
        List<SagaStep<T>> completedSteps = new ArrayList<>();

        for (int i = 0; i < steps.size(); i++) {
            SagaStep<T> step = steps.get(i);

            try {
                log.info("[SAGA-{}] Step {}/{}: {} 실행 시작", sagaId, i + 1, steps.size(), step.getName());

                step.execute(context);
                completedSteps.add(step);

                log.info("[SAGA-{}] Step {}/{}: {} 성공", sagaId, i + 1, steps.size(), step.getName());

            } catch (Exception e) {
                log.error("[SAGA-{}] Step {}/{}: {} 실패 - {}", sagaId, i + 1, steps.size(), step.getName(), e.getMessage());

                // 처음 발생한 예외를 실패 원인으로 보관하고, 보상 중 발생한 예외도 함께 기록한다.
                SagaExecutionException failure = new SagaExecutionException(
                        String.format("Saga 실행 실패 - Step: %s", step.getName()), e
                );
                compensate(completedSteps, context, sagaId, failure);
                throw failure;
            }
        }

        log.info("[SAGA-{}] Saga 완료", sagaId);
    }

    private <T> void compensate(List<SagaStep<T>> completedSteps, T context, String sagaId,
                                SagaExecutionException failure) {
        if (completedSteps.isEmpty()) {
            log.info("[SAGA-{}] 보상할 Step이 없습니다", sagaId);
            return;
        }

        log.warn("[SAGA-{}] 보상 시작 ({} 단계)", sagaId, completedSteps.size());

        // 이미 커밋된 작업을 역순으로 취소한다. 각 보상은 별도 작업이며, 보상 자체도 실패할 수 있다.
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            SagaStep<T> step = completedSteps.get(i);

            try {
                log.info("[SAGA-{}] 보상 {}/{}: {} 시작", sagaId, completedSteps.size() - i, completedSteps.size(), step.getName());

                step.compensate(context);

                log.info("[SAGA-{}] 보상 {}/{}: {} 완료", sagaId, completedSteps.size() - i, completedSteps.size(), step.getName());

            } catch (Exception e) {
                // addSuppressed()는 전달받은 예외를 던지는 대신, 기존 예외에 추가로 보관하는 Java 메서드다.
                // 여기서는 보상 실패를 failure에 추가하고, 다음 단계의 보상을 계속한다. 추가한 예외는 getSuppressed()로 확인한다.
                failure.addSuppressed(new IllegalStateException("보상 실패 - Step: " + step.getName(), e));
                log.error("[SAGA-{}] 보상 실패: {} - 나머지 보상을 계속 진행합니다.", sagaId, step.getName(), e);
            }
        }

        // 일부라도 보상에 실패했다면 완료로 기록하지 않는다.
        if (failure.hasCompensationFailures()) {
            log.error("[SAGA-{}] 보상 미완료 (실패 {}건)", sagaId, failure.getSuppressed().length);
        } else {
            log.warn("[SAGA-{}] 보상 완료", sagaId);
        }
    }
}
