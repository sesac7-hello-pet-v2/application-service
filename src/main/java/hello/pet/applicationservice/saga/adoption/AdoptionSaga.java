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

/**
 * 역할: Saga의 진입점이자 조정자
 * - Saga Steps 정의 및 실행 관리
 * - SagaOrchestrator에게 실행 위임
 * - AdoptionSagaContext 생성 및 초기화
 *
 * 실행 순서
 * - 신청서 승인 및 다른 신청서 거절 (하나의 로컬 트랜잭션)
 * - 공고 상태 완료 처리 (외부 서비스)
 * - 펫 입양 처리 (외부 서비스)
 *
 * 실패 시 위 단계를 역순으로 보상 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdoptionSaga {

    private final SagaOrchestrator sagaOrchestrator;
    private final AnnouncementFacade announcementFacade;

    // Saga Steps
    private final ApproveApplicationStep approveApplicationStep;
    private final CompleteAnnouncementStep completeAnnouncementStep;
    private final MarkPetAdoptedStep markPetAdoptedStep;

    /**
     * 입양 승인 Saga 실행
     *
     * @param announcementId 공고 ID
     * @param applicationId  신청서 ID
     * @param userId         사용자 ID
     * @param userRole       사용자 역할
     * @return 승인 응답
     * @throws SagaExecutionException Saga 실행 실패 시
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ApplicationApprovalResponse execute(
            Long announcementId,
            Long applicationId,
            Long userId,
            String userRole) {

        log.info("입양 승인 Saga 시작 - announcementId: {}, applicationId: {}",
                announcementId, applicationId);

        // 권한 검증 (Saga 실행 전)
        validatePermission(announcementId, userId, userRole);

        // Saga Context 생성
        AdoptionSagaContext context = AdoptionSagaContext.builder()
                                                         .announcementId(announcementId)
                                                         .applicationId(applicationId)
                                                         .userId(userId)
                                                         .userRole(userRole)
                                                         .build();

        // Saga Steps 실핸 순서 정의
        List<SagaStep<AdoptionSagaContext>> steps = Arrays.asList(
                approveApplicationStep,           // Step 1: 신청서 승인 및 다른 신청서 거절
                completeAnnouncementStep,         // Step 2: 공고 완료
                markPetAdoptedStep                // Step 3: 펫 입양 처리
        );

        try {
            // Saga 실행
            sagaOrchestrator.execute(steps, context);

            log.info("입양 승인 Saga 성공 - context: {}", context);

            // 응답 생성
            return ApplicationApprovalResponse.of(announcementId, applicationId);

        } catch (SagaExecutionException e) {
            log.error("입양 승인 Saga 실패 - 보상 시도 종료. context: {}", context, e);
            throw new RuntimeException("입양 승인 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private void validatePermission(Long announcementId, Long userId, String userRole) {
        // 1. 보호소 권한 확인 (DB 조회 전에 먼저 체크)
        if (!"SHELTER".equals(userRole)) {
            throw new IllegalArgumentException(
                    "보호소 담당자만 입양 승인이 가능합니다. 현재 권한: " + userRole
            );
        }

        // 2. 공고 정보 조회 (명시적 타입 사용)
        AnnouncementResponse announcement = announcementFacade.getAnnouncement(announcementId);

        // 3. 공고 소유권 확인
        if (!announcement.getShelterId().equals(userId)) {
            throw new IllegalArgumentException(
                    "다른 보호소의 공고는 승인할 수 없습니다. " +
                            "공고 보호소 ID: " + announcement.getShelterId() + ", 요청자 ID: " + userId
            );
        }
    }
}
