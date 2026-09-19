package hello.pet.applicationservice.saga.adoption.steps;

import hello.pet.applicationservice.facade.AnnouncementFacade;
import hello.pet.applicationservice.dto.response.AnnouncementCompletionResponse;
import hello.pet.applicationservice.saga.adoption.AdoptionSagaContext;
import hello.pet.applicationservice.saga.core.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompleteAnnouncementStep implements SagaStep<AdoptionSagaContext> {

    private final AnnouncementFacade announcementFacade;

    @Override
    public void execute(AdoptionSagaContext context) {
        log.info("공고 완료 처리 시작 - announcementId: {}", context.getAnnouncementId());

        AnnouncementCompletionResponse result = announcementFacade.completeAnnouncement(
                context.getAnnouncementId(),
                context.getUserId()
        );

        if (result == null) {
            throw new IllegalStateException("공고 완료 응답에 올바른 보상 정보가 없습니다.");
        }
        context.setAnnouncementChanged(result.changed());

        log.info("공고 완료 처리 성공 - announcementId: {}", context.getAnnouncementId());
    }

    @Override
    public void compensate(AdoptionSagaContext context) {
        log.warn("공고 상태 복원 시작 - announcementId: {}", context.getAnnouncementId());

        if (!context.isAnnouncementChanged()) {
            log.info("이번 요청에서 공고 상태를 변경하지 않았으므로 복원하지 않습니다.");
            return;
        }

        announcementFacade.cancelAnnouncementCompletion(
                context.getAnnouncementId(),
                context.getUserId()
        );

        log.warn("공고 상태 복원 완료 - announcementId: {}", context.getAnnouncementId());
    }

    @Override
    public String getName() {
        return "공고 완료 처리";
    }
}
