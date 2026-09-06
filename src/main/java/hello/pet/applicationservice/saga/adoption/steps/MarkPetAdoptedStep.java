package hello.pet.applicationservice.saga.adoption.steps;

import hello.pet.applicationservice.facade.PetServiceFacade;
import hello.pet.applicationservice.saga.adoption.AdoptionSagaContext;
import hello.pet.applicationservice.saga.core.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Step 3: 펫 입양 완료 처리
 *
 * 책임
 * - pet-service에 펫 입양 처리 요청
 * - 외부 서비스 호출이므로 트랜잭션 없음
 * - 보상 시 펫 상태를 공고 중으로 복원
 *
 * 참고
 * - 멱등성은 pet-service에서 처리 (외부 서비스의 상태는 여기서 확인 불가)
 * - PetServiceFacade를 통해 외부 서비스 호출 (에러 처리 위임)
 * - 이 Step이 성공하면 전체 Saga가 완료됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarkPetAdoptedStep implements SagaStep<AdoptionSagaContext> {

    private final PetServiceFacade petServiceFacade;

    @Override
    public void execute(AdoptionSagaContext context) throws Exception {
        log.info("펫 입양 처리 시작 - petId: {}", context.getPetId());

        // 펫 ID가 없으면 오류
        if (context.getPetId() == null) {
            throw new IllegalStateException("펫 ID가 설정되지 않았습니다");
        }

        try {
            // 외부 서비스 호출: 펫 입양 처리
            petServiceFacade.markAsAdopted(
                    context.getPetId(),
                    context.getUserId(),
                    context.getUserRole()
            );

            // 성공 시 Context에 표시 (보상 시 참조용)
            context.setPetMarkedAsAdopted(true);

            log.info("펫 입양 처리 성공 - petId: {}", context.getPetId());

        } catch (Exception e) {
            log.error("펫 입양 처리 실패 - petId: {}, 오류: {}",
                    context.getPetId(), e.getMessage());
            throw e; // 상위(SagaOrchestrator)에서 처리
        }
    }

    @Override
    public void compensate(AdoptionSagaContext context) {
        try {
            log.warn("펫 입양 취소 시작 - petId: {}", context.getPetId());

            // 실제로 입양 처리가 되었는지 확인
            if (!context.isPetMarkedAsAdopted()) {
                log.info("펫 입양 처리가 되지 않았으므로 취소할 필요 없음");
                return;
            }

            // 펫 ID가 없으면 보상 불가
            if (context.getPetId() == null) {
                log.error("펫 ID가 없어 보상할 수 없습니다");
                return;
            }

            // 외부 서비스 호출: 펫 상태 복원
            petServiceFacade.markAsAnnounced(
                    context.getPetId(),
                    context.getUserId(),
                    context.getUserRole()
            );

            log.warn("펫 입양 취소 성공 - petId: {}", context.getPetId());

        } catch (Exception e) {
            // 보상 실패는 로그만 남기고 진행
            // 확장 계획 - 재시도, Dead Letter Queue, 수동 개입 알림 필요
            log.error("펫 입양 취소 실패 - petId: {}, 오류: {}. 수동 처리 필요!",
                    context.getPetId(), e.getMessage());
            throw e; // 상위(SagaOrchestrator)에서 처리
        }
    }

    @Override
    public String getName() {
        return "펫 입양 완료";
    }
}
