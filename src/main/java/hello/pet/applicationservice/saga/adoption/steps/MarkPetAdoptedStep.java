package hello.pet.applicationservice.saga.adoption.steps;

import hello.pet.applicationservice.facade.PetServiceFacade;
import hello.pet.applicationservice.saga.adoption.AdoptionSagaContext;
import hello.pet.applicationservice.saga.core.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 펫 서비스에 입양 완료를 요청한다. 이 단계까지 성공하면 입양 승인 Saga가 끝난다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarkPetAdoptedStep implements SagaStep<AdoptionSagaContext> {

    private final PetServiceFacade petServiceFacade;

    @Override
    public void execute(AdoptionSagaContext context) throws Exception {
        log.info("펫 입양 처리 시작 - petId: {}", context.getPetId());

        if (context.getPetId() == null) {
            throw new IllegalStateException("펫 ID가 설정되지 않았습니다");
        }

        try {
            petServiceFacade.markAsAdopted(
                    context.getPetId(),
                    context.getUserId(),
                    context.getUserRole()
            );

            context.setPetMarkedAsAdopted(true);

            log.info("펫 입양 처리 성공 - petId: {}", context.getPetId());

        } catch (Exception e) {
            log.error("펫 입양 처리 실패 - petId: {}, 오류: {}",
                    context.getPetId(), e.getMessage());
            throw e;
        }
    }

    // 보상 동작은 정의되어 있지만, 현재 마지막 Step이므로 성공 후 호출되는 경로는 없다.
    @Override
    public void compensate(AdoptionSagaContext context) {
        try {
            log.warn("펫 입양 취소 시작 - petId: {}", context.getPetId());

            if (!context.isPetMarkedAsAdopted()) {
                log.info("펫 입양 처리가 되지 않았으므로 취소할 필요 없음");
                return;
            }

            if (context.getPetId() == null) {
                log.error("펫 ID가 없어 보상할 수 없습니다");
                return;
            }

            petServiceFacade.markAsAnnounced(
                    context.getPetId(),
                    context.getUserId(),
                    context.getUserRole()
            );

            log.warn("펫 입양 취소 성공 - petId: {}", context.getPetId());

        } catch (Exception e) {
            log.error("펫 입양 취소 실패 - petId: {}, 오류: {}. 수동 처리 필요!",
                    context.getPetId(), e.getMessage());
            throw e;
        }
    }

    @Override
    public String getName() {
        return "펫 입양 완료";
    }
}
