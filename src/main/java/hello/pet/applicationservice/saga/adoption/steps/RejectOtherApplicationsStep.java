package hello.pet.applicationservice.saga.adoption.steps;

import hello.pet.applicationservice.entity.ApplicationStatus;
import hello.pet.applicationservice.repository.ApplicationRepository;
import hello.pet.applicationservice.saga.adoption.AdoptionSagaContext;
import hello.pet.applicationservice.saga.core.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Step 2: 같은 공고의 다른 신청서들을 거절
 *
 * 책임
 * - 승인된 신청서를 제외한 모든 신청서를 일괄 거절
 * - 보상을 위해 거절된 신청서 ID를 저장
 * - 벌크 업데이트로 성능 최적화
 *
 * 참고
 * - ApplicationRepository 직접 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RejectOtherApplicationsStep implements SagaStep<AdoptionSagaContext> {

    private final ApplicationRepository applicationRepository;

    @Override
    @Transactional
    public void execute(AdoptionSagaContext context) throws Exception {
        log.info("다른 신청서 거절 시작 - announcementId: {}, 제외할 applicationId: {}",
                context.getAnnouncementId(), context.getApplicationId());

        // 거절 처리 전, 거절할 신청서 ID 목록 조회 (보상용)
        List<Long> targetApplicationIds = applicationRepository.findOtherApplicationIds(
                context.getAnnouncementId(),
                ApplicationStatus.SUBMITTED,
                context.getApplicationId()  // 승인된 신청서 제외
        );

        if (targetApplicationIds.isEmpty()) {
            log.info("거절할 다른 신청서가 없습니다.");
            return;
        }

        // Context에 거절된 신청서 ID 저장 (보상용)
        context.setRejectedApplicationIds(targetApplicationIds);

        // 벌크 업데이트로 일괄 거절 처리
        int updatedCount = applicationRepository.bulkRejectApplications(
                context.getAnnouncementId(),
                context.getApplicationId()
        );

        log.info("다른 신청서 거절 완료 - {} 건 처리", updatedCount);
    }

    @Override
    @Transactional
    public void compensate(AdoptionSagaContext context) {
        try {
            log.warn("다른 신청서 거절 취소 시작");

            // 보상할 데이터가 없으면 스킵
            if (context.getRejectedApplicationIds() == null || context.getRejectedApplicationIds().isEmpty()) {
                log.info("거절 취소할 신청서가 없습니다.");
                return;
            }

            // 거절된 신청서들을 SUBMITTED로 복원
            int restoredCount = applicationRepository.bulkUpdateStatus(
                    context.getRejectedApplicationIds(),
                    ApplicationStatus.REJECTED,  // 현재 상태
                    ApplicationStatus.SUBMITTED  // 변경할 상태
            );

            log.warn("다른 신청서 거절 취소 완료 - {} 건 복원 (총 {} 건 중)",
                    restoredCount, context.getRejectedApplicationIds().size());

        } catch (Exception e) {
            // 보상 실패는 로그만 남기고 진행
            log.error("다른 신청서 거절 취소 실패 - 오류: {}", e.getMessage());
            throw e; // 상위(SagaOrchestrator)에서 처리
        }
    }

    @Override
    public String getName() {
        return "다른 신청서 거절";
    }
}
