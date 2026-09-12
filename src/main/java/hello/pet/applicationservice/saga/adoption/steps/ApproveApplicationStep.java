package hello.pet.applicationservice.saga.adoption.steps;

import hello.pet.applicationservice.entity.Application;
import hello.pet.applicationservice.entity.ApplicationStatus;
import hello.pet.applicationservice.repository.ApplicationRepository;
import hello.pet.applicationservice.saga.adoption.AdoptionSagaContext;
import hello.pet.applicationservice.saga.core.SagaStep;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 선택 신청서 승인과 다른 신청서 거절을 하나의 로컬 트랜잭션으로 처리한다. */
@Component
@RequiredArgsConstructor
public class ApproveApplicationStep implements SagaStep<AdoptionSagaContext> {
    // IN 절은 500건씩 나누지만, 모든 배치는 함께 커밋하거나 롤백한다.
    private static final int BATCH_SIZE = 500;
    private final ApplicationRepository applicationRepository;

    @Override
    @Transactional
    public void execute(AdoptionSagaContext context) {
        Application selected = applicationRepository.findById(context.getApplicationId())
                .orElseThrow(() -> new EntityNotFoundException("신청서를 찾을 수 없습니다."));
        if (!selected.getAnnouncementId().equals(context.getAnnouncementId())) {
            throw new IllegalArgumentException("해당 공고의 신청서가 아닙니다.");
        }
        if (selected.getStatus() != ApplicationStatus.SUBMITTED
                && selected.getStatus() != ApplicationStatus.UNDER_REVIEW
                && selected.getStatus() != ApplicationStatus.APPROVED) {
            throw new IllegalStateException("승인 가능한 신청서 상태가 아닙니다.");
        }

        // 보상에 필요한 이전 값과 대상 ID만 저장한다. 다른 신청서 엔티티는 조회하지 않는다.
        context.setOriginalApplicationStatus(selected.getStatus());
        context.setOriginalProcessedAt(selected.getProcessedAt());
        context.setPetId(selected.getPetId());
        context.setSubmittedApplicationIds(applicationRepository.findOtherApplicationIds(
                context.getAnnouncementId(), ApplicationStatus.SUBMITTED, selected.getId()));
        context.setUnderReviewApplicationIds(applicationRepository.findOtherApplicationIds(
                context.getAnnouncementId(), ApplicationStatus.UNDER_REVIEW, selected.getId()));

        // 재요청에서는 이미 완료된 승인과 처리 시각을 유지한다.
        if (selected.getStatus() != ApplicationStatus.APPROVED) {
            selected.changeStatus(ApplicationStatus.APPROVED);
        }
        reject(context.getSubmittedApplicationIds(), ApplicationStatus.SUBMITTED);
        reject(context.getUnderReviewApplicationIds(), ApplicationStatus.UNDER_REVIEW);
    }

    // 이미 커밋된 신청서 변경을 새 로컬 트랜잭션으로 복원한다.
    @Override
    @Transactional
    public void compensate(AdoptionSagaContext context) {
        // 이번 실행에서 승인한 신청서만 복원한다.
        if (context.getOriginalApplicationStatus() != ApplicationStatus.APPROVED) {
            Application selected = applicationRepository.findById(context.getApplicationId())
                    .orElseThrow(() -> new EntityNotFoundException("보상할 신청서를 찾을 수 없습니다."));
            selected.restoreAfterAdoptionApproval(ApplicationStatus.APPROVED,
                    context.getOriginalApplicationStatus(), context.getOriginalProcessedAt());
        }
        restore(context.getSubmittedApplicationIds(), ApplicationStatus.SUBMITTED);
        restore(context.getUnderReviewApplicationIds(), ApplicationStatus.UNDER_REVIEW);
    }

    private void reject(List<Long> ids, ApplicationStatus originalStatus) {
        for (int start = 0; start < ids.size(); start += BATCH_SIZE) {
            List<Long> batch = ids.subList(start, Math.min(start + BATCH_SIZE, ids.size()));
            int updated = applicationRepository.bulkUpdateStatus(batch, originalStatus, ApplicationStatus.REJECTED);
            // 조회 이후 대상이 바뀌었다면 일부만 처리하지 않고 전체를 롤백한다.
            if (updated != batch.size()) {
                throw new IllegalStateException("거절 대상이 변경되어 신청서 처리를 취소합니다.");
            }
        }
    }

    private void restore(List<Long> ids, ApplicationStatus originalStatus) {
        for (int start = 0; start < ids.size(); start += BATCH_SIZE) {
            List<Long> batch = ids.subList(start, Math.min(start + BATCH_SIZE, ids.size()));
            int updated = applicationRepository.bulkUpdateStatus(batch, ApplicationStatus.REJECTED, originalStatus);
            // 이미 복원된 대상은 허용하되 삭제되거나 다른 상태인 대상은 보상 실패로 처리한다.
            if (updated != batch.size()
                    && applicationRepository.countByIdInAndStatus(batch, originalStatus) != batch.size()) {
                throw new IllegalStateException("신청서 상태가 변경되어 보상할 수 없습니다.");
            }
        }
    }

    @Override
    public String getName() {
        return "신청서 승인 및 다른 신청서 거절";
    }
}
