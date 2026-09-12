package hello.pet.applicationservice.saga.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 역할: Saga 패턴의 중앙 조율자(Orchestrator)
 * - Step 리스트를 순차적으로 실행
 * - 실패 시 완료된 Step들을 역순으로 보상
 * - 각 Step의 실행/보상 과정 로깅
 * - Saga ID 생성 및 추적
 */
@Slf4j
@Component
public class SagaOrchestrator {

    /**
     * Saga를 실행하고 실패 시 자동으로 보상 처리
     *
     * @param steps   실행할 Step 목록
     * @param context Step 간 공유할 데이터
     * @param <T>     Context 타입
     * @throws SagaExecutionException Saga 실행 실패 시
     */
    public <T> void execute(List<SagaStep<T>> steps, T context) throws SagaExecutionException {
        // 실행 ID 생성 (로그 추적용)
        String sagaId = generateSagaId();
        log.info("[SAGA-{}] ========== Saga 시작 ==========", sagaId);
        log.info("[SAGA-{}] 총 {} 단계 실행 예정", sagaId, steps.size());

        // 성공적으로 실행된 Step 저장 (보상용)
        List<SagaStep<T>> completedSteps = new ArrayList<>();

        // 각 Step 순차 실행
        for (int i = 0; i < steps.size(); i++) {
            SagaStep<T> step = steps.get(i);

            try {
                log.info("[SAGA-{}] Step {}/{}: {} 실행 시작",
                        sagaId, i + 1, steps.size(), step.getName());

                // Step 실행
                step.execute(context);

                // 성공한 Step 기록
                completedSteps.add(step);

                log.info("[SAGA-{}] Step {}/{}: {} ✅ 성공",
                        sagaId, i + 1, steps.size(), step.getName());

            } catch (Exception e) {
                log.error("[SAGA-{}] Step {}/{}: {} ❌ 실패 - {}",
                        sagaId, i + 1, steps.size(), step.getName(), e.getMessage());

                // 원래 실패를 유지하면서 보상 실패도 같은 예외에 추가한다.
                SagaExecutionException failure = new SagaExecutionException(
                        String.format("Saga 실행 실패 - Step: %s", step.getName()), e
                );
                compensate(completedSteps, context, sagaId, failure);
                throw failure;
            }
        }

        log.info("[SAGA-{}] ========== Saga 완료 ==========", sagaId);
    }

    /**
     * 완료된 Step들을 역순으로 보상 처리한다.
     *
     * 보상 원칙
     * - 역순 실행: 마지막으로 성공한 Step부터 보상
     * - 실패 수집: 개별 보상 실패를 보존하고 나머지 보상은 계속 진행
     * - 상세 로깅: 모든 보상 과정을 기록
     *
     * @param completedSteps 성공한 Step 목록
     * @param context        Saga 컨텍스트
     * @param sagaId         Saga 실행 ID
     * @param failure        원래 작업 실패와 보상 실패를 함께 전달할 예외
     */
    private <T> void compensate(List<SagaStep<T>> completedSteps, T context, String sagaId,
                                SagaExecutionException failure) {
        if (completedSteps.isEmpty()) {
            log.info("[SAGA-{}] 보상할 Step이 없습니다", sagaId);
            return;
        }

        log.warn("[SAGA-{}] ========== 보상 시작 ({} 단계) ==========",
                sagaId, completedSteps.size());

        // 역순으로 보상 실행
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            SagaStep<T> step = completedSteps.get(i);

            try {
                log.info("[SAGA-{}] 보상 {}/{}: {} 시작",
                        sagaId, completedSteps.size() - i, completedSteps.size(), step.getName());

                step.compensate(context);

                log.info("[SAGA-{}] 보상 {}/{}: {} ✅ 완료",
                        sagaId, completedSteps.size() - i, completedSteps.size(), step.getName());

            } catch (Exception e) {
                failure.addSuppressed(new IllegalStateException("보상 실패 - Step: " + step.getName(), e));
                log.error("[SAGA-{}] 보상 실패: {} - 나머지 보상을 계속 진행합니다.",
                        sagaId, step.getName(), e);
            }
        }

        if (failure.hasCompensationFailures()) {
            log.error("[SAGA-{}] ========== 보상 미완료 (실패 {}건) ==========",
                    sagaId, failure.getSuppressed().length);
        } else {
            log.warn("[SAGA-{}] ========== 보상 완료 ==========", sagaId);
        }
    }

    /**
     * Saga 실행 ID 생성 (간단한 타임스탬프 기반)
     *
     * @return 8자리 Saga ID
     */
    private String generateSagaId() {
        return String.valueOf(System.currentTimeMillis()).substring(6);
    }
}
