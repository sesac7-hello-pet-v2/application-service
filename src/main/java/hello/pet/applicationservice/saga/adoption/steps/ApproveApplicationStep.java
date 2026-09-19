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

@Component
@RequiredArgsConstructor
public class ApproveApplicationStep implements SagaStep<AdoptionSagaContext> {
    private final ApplicationRepository applicationRepository;

    /**
     * 승인과 거절을 하나의 로컬 트랜잭션으로 처리해 함께 커밋하거나 롤백한다.
     */
    @Override
    @Transactional
    public void execute(AdoptionSagaContext context) {
        Application selected = applicationRepository.findById(context.getApplicationId())
                .orElseThrow(() -> new EntityNotFoundException("신청서를 찾을 수 없습니다."));

        // 조회한 신청서가 승인 가능한 상태인지 다시 검증한다.
        selected.validateApproval(context.getAnnouncementId());

        context.setOriginalApplicationStatus(selected.getStatus());
        context.setOriginalProcessedAt(selected.getProcessedAt());
        context.setPetId(selected.getPetId());
        context.setSubmittedApplicationIds(applicationRepository.findOtherApplicationIds(
                context.getAnnouncementId(), ApplicationStatus.SUBMITTED, selected.getId()));
        context.setUnderReviewApplicationIds(applicationRepository.findOtherApplicationIds(
                context.getAnnouncementId(), ApplicationStatus.UNDER_REVIEW, selected.getId()));

        selected.changeStatus(ApplicationStatus.APPROVED);
        reject(context.getSubmittedApplicationIds(), ApplicationStatus.SUBMITTED);
        reject(context.getUnderReviewApplicationIds(), ApplicationStatus.UNDER_REVIEW);
    }

    /**
     * 이번 승인 과정에서 변경한 신청서들을 별도 트랜잭션으로 복원한다.
     */
    @Override
    @Transactional
    public void compensate(AdoptionSagaContext context) {
        Application selected = applicationRepository.findById(context.getApplicationId())
                .orElseThrow(() -> new EntityNotFoundException("보상할 신청서를 찾을 수 없습니다."));

        // 승인한 신청서의 이전 상태 복원
        selected.restorePreviousState(context.getOriginalApplicationStatus(), context.getOriginalProcessedAt());

        // 거절한 신청서들의 이전 상태 복원
        restore(context.getSubmittedApplicationIds(), ApplicationStatus.SUBMITTED);
        restore(context.getUnderReviewApplicationIds(), ApplicationStatus.UNDER_REVIEW);
    }

    private void reject(List<Long> ids, ApplicationStatus originalStatus) {
        if (ids.isEmpty()) {
            return;
        }

        int updated = applicationRepository.bulkUpdateStatus(ids, originalStatus, ApplicationStatus.REJECTED);

        // 거절 대상 수와 실제 변경 수가 다르면 예외를 발생시켜 승인·거절 전체를 롤백한다.
        if (updated != ids.size()) {
            throw new IllegalStateException("거절 대상이 변경되어 신청서 처리를 취소합니다.");
        }
    }

    private void restore(List<Long> ids, ApplicationStatus originalStatus) {
        if (ids.isEmpty()) {
            return;
        }

        int updated = applicationRepository.bulkUpdateStatus(ids, ApplicationStatus.REJECTED, originalStatus);

        // 이미 복원된 대상은 허용하되 삭제되거나 다른 상태인 대상은 보상 실패로 처리한다.
        if (updated != ids.size()
                && applicationRepository.countByIdInAndStatus(ids, originalStatus) != ids.size()) {
            throw new IllegalStateException("신청서 상태가 변경되어 보상할 수 없습니다.");
        }
    }

    @Override
    public String getName() {
        return "신청서 승인 및 다른 신청서 거절";
    }
}
