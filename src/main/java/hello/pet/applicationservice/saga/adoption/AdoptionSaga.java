package hello.pet.applicationservice.saga.adoption;

import hello.pet.applicationservice.dto.response.AnnouncementResponse;
import hello.pet.applicationservice.dto.response.ApplicationApprovalResponse;
import hello.pet.applicationservice.facade.AnnouncementFacade;
import hello.pet.applicationservice.entity.Application;
import hello.pet.applicationservice.repository.ApplicationRepository;
import jakarta.persistence.EntityNotFoundException;
import hello.pet.applicationservice.saga.adoption.steps.ApproveApplicationStep;
import hello.pet.applicationservice.saga.adoption.steps.CompleteAnnouncementStep;
import hello.pet.applicationservice.saga.adoption.steps.AdoptPetStep;
import hello.pet.applicationservice.saga.core.SagaExecutionException;
import hello.pet.applicationservice.saga.core.SagaOrchestrator;
import hello.pet.applicationservice.saga.core.SagaStep;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 입양 승인 단계와 실행 순서를 구성하고, 오케스트레이터에 실행을 요청한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class AdoptionSaga {

    private final SagaOrchestrator sagaOrchestrator;
    private final AnnouncementFacade announcementFacade;
    private final ApplicationRepository applicationRepository;

    private final ApproveApplicationStep approveApplicationStep;
    private final CompleteAnnouncementStep completeAnnouncementStep;
    private final AdoptPetStep adoptPetStep;

    /**
     * 승인 요청을 검증하고, 신청서 승인 → 공고 완료 → 펫 입양 완료 순서로 Saga를 실행한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ApplicationApprovalResponse execute(
            Long announcementId,
            Long applicationId,
            Long userId,
            String userRole) {

        log.info("입양 승인 Saga 시작 - announcementId: {}, applicationId: {}", announcementId, applicationId);

        validateApprovalRequest(announcementId, applicationId, userId, userRole);

        // 각 단계의 실행과 보상에 필요한 정보를 공유할 Context를 생성한다.
        AdoptionSagaContext context = AdoptionSagaContext.builder()
                                                         .announcementId(announcementId)
                                                         .applicationId(applicationId)
                                                         .userId(userId)
                                                         .userRole(userRole)
                                                         .build();

        // 신청서 승인 과정의 순서를 정의한다.
        List<SagaStep<AdoptionSagaContext>> steps = List.of(
                approveApplicationStep,
                completeAnnouncementStep,
                adoptPetStep
        );

        try {
            sagaOrchestrator.execute(steps, context);
            log.info("입양 승인 Saga 성공 - context: {}", context);
            return ApplicationApprovalResponse.of(announcementId, applicationId);

        } catch (SagaExecutionException e) {
            log.error("입양 승인 Saga 실패 - 보상 실패 여부: {}. context: {}", e.hasCompensationFailures(), context, e);
            throw e;
        }
    }

    private void validateApprovalRequest(Long announcementId, Long applicationId, Long userId, String userRole) {
        // 요청자가 보호소 담당자인지 확인한다.
        if (!"SHELTER".equals(userRole)) {
            throw new IllegalArgumentException(
                    "보호소 담당자만 입양 승인이 가능합니다. 현재 권한: " + userRole
            );
        }

        AnnouncementResponse announcement = announcementFacade.getAnnouncement(announcementId);

        // 요청자 본인의 보호소 공고인지 확인한다.
        if (!announcement.getShelterId().equals(userId)) {
            throw new IllegalArgumentException(
                    "다른 보호소의 공고는 승인할 수 없습니다. " +
                            "공고 보호소 ID: " + announcement.getShelterId() + ", 요청자 ID: " + userId
            );
        }

        // 해당 공고의 신청서인지 확인하고, 이미 승인되거나 거절된 신청서는 차단한다.
        Application selected = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("신청서를 찾을 수 없습니다."));
        selected.validateApproval(announcementId);

        // 공고 모집이 마감되어야 승인을 진행할 수 있다.
        if (!"CLOSED".equals(announcement.getAnnouncementStatus())) {
            throw new IllegalArgumentException("마감된 공고의 신청서만 승인할 수 있습니다.");
        }
    }
}
