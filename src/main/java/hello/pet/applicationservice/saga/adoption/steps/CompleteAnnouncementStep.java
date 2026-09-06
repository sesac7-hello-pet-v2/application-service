package hello.pet.applicationservice.saga.adoption.steps;

import hello.pet.applicationservice.facade.AnnouncementFacade;
import hello.pet.applicationservice.saga.adoption.AdoptionSagaContext;
import hello.pet.applicationservice.saga.core.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Step 2: 공고 완료 처리
 *
 * 책임
 * - announcement-service에 공고 완료 요청
 * - 외부 서비스 호출이므로 트랜잭션 없음
 * - 보상 시 공고를 재오픈
 *
 * 참고
 * - 멱등성은 announcement-service에서 처리 (외부 서비스의 상태는 여기서 확인 불가)
 * - AnnouncementFacade를 통해 외부 서비스 호출 (에러 처리 위임)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompleteAnnouncementStep implements SagaStep<AdoptionSagaContext> {

    private final AnnouncementFacade announcementFacade;

    @Override
    public void execute(AdoptionSagaContext context) throws Exception {
        log.info("공고 완료 처리 시작 - announcementId: {}", context.getAnnouncementId());

        try {
            // 외부 서비스 호출: 공고 완료 처리
            announcementFacade.completeAnnouncement(
                    context.getAnnouncementId(),
                    context.getUserId()
            );

            // 성공 시 Context에 표시 (보상 시 참조용)
            context.setAnnouncementCompleted(true);

            log.info("공고 완료 처리 성공 - announcementId: {}", context.getAnnouncementId());

        } catch (Exception e) {
            log.error("공고 완료 처리 실패 - announcementId: {}, 오류: {}",
                    context.getAnnouncementId(), e.getMessage());
            throw e; // 상위(SagaOrchestrator)에서 처리
        }
    }

    @Override
    public void compensate(AdoptionSagaContext context) {
        try {
            log.warn("공고 완료 취소 시작 - announcementId: {}", context.getAnnouncementId());

            // 실제로 완료 처리가 되었는지 확인
            if (!context.isAnnouncementCompleted()) {
                log.info("공고 완료 처리가 되지 않았으므로 취소할 필요 없음");
                return;
            }

            // 외부 서비스 호출: 공고 재오픈
            announcementFacade.reopenAnnouncement(
                    context.getAnnouncementId(),
                    context.getUserId()
            );

            log.warn("공고 완료 취소 성공 - announcementId: {}", context.getAnnouncementId());

        } catch (Exception e) {
            // 보상 실패는 로그만 남기고 진행
            // 확장 계획 - 재시도, Dead Letter Queue, 수동 개입 알림 필요
            log.error("공고 완료 취소 실패 - announcementId: {}, 오류: {}. 수동 처리 필요!",
                    context.getAnnouncementId(), e.getMessage());
            throw e; // 상위(SagaOrchestrator)에서 처리
        }
    }

    @Override
    public String getName() {
        return "공고 완료 처리";
    }
}
