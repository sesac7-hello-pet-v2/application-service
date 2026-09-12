package hello.pet.applicationservice.saga.adoption.steps;

import hello.pet.applicationservice.facade.AnnouncementFacade;
import hello.pet.applicationservice.dto.response.AnnouncementCompletionResponse;
import hello.pet.applicationservice.saga.adoption.AdoptionSagaContext;
import hello.pet.applicationservice.saga.core.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 공고 서비스에 완료를 요청하고, 실패 시 이번에 변경한 공고만 CLOSED로 보상한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompleteAnnouncementStep implements SagaStep<AdoptionSagaContext> {

    private final AnnouncementFacade announcementFacade;

    @Override
    public void execute(AdoptionSagaContext context) throws Exception {
        log.info("공고 완료 처리 시작 - announcementId: {}", context.getAnnouncementId());

        try {
            AnnouncementCompletionResponse result = announcementFacade.completeAnnouncement(
                    context.getAnnouncementId(),
                    context.getUserId()
            );

            if (result == null) {
                throw new IllegalStateException("공고 완료 응답에 올바른 보상 정보가 없습니다.");
            }
            // HTTP 성공 여부가 아니라 실제 변경 여부로 보상 대상을 구분한다.
            context.setAnnouncementCompleted(result.changed());

            log.info("공고 완료 처리 성공 - announcementId: {}", context.getAnnouncementId());

        } catch (Exception e) {
            log.error("공고 완료 처리 실패 - announcementId: {}, 오류: {}",
                    context.getAnnouncementId(), e.getMessage());
            throw e;
        }
    }

    @Override
    public void compensate(AdoptionSagaContext context) {
        try {
            log.warn("공고 완료 취소 시작 - announcementId: {}", context.getAnnouncementId());

            // 재요청 전에 이미 완료된 공고는 취소하지 않는다.
            if (!context.isAnnouncementCompleted()) {
                log.info("공고 완료 처리가 되지 않았으므로 취소할 필요 없음");
                return;
            }

            announcementFacade.cancelAnnouncementCompletion(
                    context.getAnnouncementId(),
                    context.getUserId()
            );

            log.warn("공고 완료 취소 성공 - announcementId: {}", context.getAnnouncementId());

        } catch (Exception e) {
            log.error("공고 완료 취소 실패 - announcementId: {}, 오류: {}. 수동 처리 필요!",
                    context.getAnnouncementId(), e.getMessage());
            throw e;
        }
    }

    @Override
    public String getName() {
        return "공고 완료 처리";
    }
}
