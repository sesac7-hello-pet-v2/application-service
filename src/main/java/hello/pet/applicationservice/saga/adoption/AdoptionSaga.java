package hello.pet.applicationservice.saga.adoption;

import hello.pet.applicationservice.dto.response.AnnouncementResponse;
import hello.pet.applicationservice.dto.response.ApplicationApprovalResponse;
import hello.pet.applicationservice.facade.AnnouncementFacade;
import hello.pet.applicationservice.saga.adoption.steps.ApproveApplicationStep;
import hello.pet.applicationservice.saga.adoption.steps.CompleteAnnouncementStep;
import hello.pet.applicationservice.saga.adoption.steps.MarkPetAdoptedStep;
import hello.pet.applicationservice.saga.core.SagaExecutionException;
import hello.pet.applicationservice.saga.core.SagaOrchestrator;
import hello.pet.applicationservice.saga.core.SagaStep;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 입양 승인 순서를 정의하고, 실행과 보상은 SagaOrchestrator에 맡긴다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdoptionSaga {

    private final SagaOrchestrator sagaOrchestrator;
    private final AnnouncementFacade announcementFacade;

    private final ApproveApplicationStep approveApplicationStep;
    private final CompleteAnnouncementStep completeAnnouncementStep;
    private final MarkPetAdoptedStep markPetAdoptedStep;

    // 전체를 한 트랜잭션으로 묶지 않는다. 각 Step의 커밋 후 다음 단계로 진행한다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ApplicationApprovalResponse execute(
            Long announcementId,
            Long applicationId,
            Long userId,
            String userRole) {

        log.info("입양 승인 Saga 시작 - announcementId: {}, applicationId: {}",
                announcementId, applicationId);

        validatePermission(announcementId, userId, userRole);

        // 요청마다 하나의 Context를 만들어 모든 Step이 공유한다.
        AdoptionSagaContext context = AdoptionSagaContext.builder()
                                                         .announcementId(announcementId)
                                                         .applicationId(applicationId)
                                                         .userId(userId)
                                                         .userRole(userRole)
                                                         .build();

        // 신청서 승인·거절 → 공고 완료 → 펫 입양 완료 순서로 실행한다.
        List<SagaStep<AdoptionSagaContext>> steps = Arrays.asList(
                approveApplicationStep,
                completeAnnouncementStep,
                markPetAdoptedStep
        );

        try {
            sagaOrchestrator.execute(steps, context);

            log.info("입양 승인 Saga 성공 - context: {}", context);

            return ApplicationApprovalResponse.of(announcementId, applicationId);

        } catch (SagaExecutionException e) {
            log.error("입양 승인 Saga 실패 - 보상 실패 여부: {}. context: {}",
                    e.hasCompensationFailures(), context, e);
            // 원래 실패와 보상 실패 정보가 담긴 예외를 그대로 전달한다.
            throw e;
        }
    }

    private void validatePermission(Long announcementId, Long userId, String userRole) {
        if (!"SHELTER".equals(userRole)) {
            throw new IllegalArgumentException(
                    "보호소 담당자만 입양 승인이 가능합니다. 현재 권한: " + userRole
            );
        }

        AnnouncementResponse announcement = announcementFacade.getAnnouncement(announcementId);

        if (!announcement.getShelterId().equals(userId)) {
            throw new IllegalArgumentException(
                    "다른 보호소의 공고는 승인할 수 없습니다. " +
                            "공고 보호소 ID: " + announcement.getShelterId() + ", 요청자 ID: " + userId
            );
        }
    }
}
