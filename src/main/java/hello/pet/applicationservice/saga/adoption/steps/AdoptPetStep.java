package hello.pet.applicationservice.saga.adoption.steps;

import hello.pet.applicationservice.facade.PetServiceFacade;
import hello.pet.applicationservice.saga.adoption.AdoptionSagaContext;
import hello.pet.applicationservice.saga.core.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 펫 서비스에 입양 완료 처리를 요청하는 마지막 단계다.
@Slf4j
@Component
@RequiredArgsConstructor
public class AdoptPetStep implements SagaStep<AdoptionSagaContext> {

    private final PetServiceFacade petServiceFacade;

    @Override
    public void execute(AdoptionSagaContext context) {
        log.info("펫 입양 처리 시작 - petId: {}", context.getPetId());

        if (context.getPetId() == null) {
            throw new IllegalStateException("펫 ID가 설정되지 않았습니다");
        }

        petServiceFacade.completeAdoption(
                context.getPetId(),
                context.getUserId(),
                context.getUserRole()
        );

        log.info("펫 입양 처리 성공 - petId: {}", context.getPetId());
    }

    @Override
    public void compensate(AdoptionSagaContext context) {
        // 현재 마지막 단계여서 보상 처리가 필요 없으므로, SagaStep 인터페이스 구현을 위해 빈 메서드로 남긴다.
    }

    @Override
    public String getName() {
        return "펫 입양 완료";
    }
}
