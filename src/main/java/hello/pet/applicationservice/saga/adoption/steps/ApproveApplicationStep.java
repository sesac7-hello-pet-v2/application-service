package hello.pet.applicationservice.saga.adoption.steps;

import hello.pet.applicationservice.entity.Application;
import hello.pet.applicationservice.entity.ApplicationStatus;
import hello.pet.applicationservice.repository.ApplicationRepository;
import hello.pet.applicationservice.saga.adoption.AdoptionSagaContext;
import hello.pet.applicationservice.saga.core.SagaStep;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Step 1: 입양 신청서 승인
 *
 * 책임
 * - 신청서 상태를 APPROVED로 변경
 * - 보상을 위해 기존 상태 저장
 * - 멱등성 보장
 *
 * 참고
 * - ApplicationRepository 직접 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApproveApplicationStep implements SagaStep<AdoptionSagaContext> {

    private final ApplicationRepository applicationRepository;

    @Override
    @Transactional
    public void execute(AdoptionSagaContext context) throws Exception {
        log.info("신청서 승인 시작 - applicationId: {}", context.getApplicationId());

        // 신청서 조회
        Application application = applicationRepository.findById(context.getApplicationId())
                                                       .orElseThrow(() -> new EntityNotFoundException(
                                                               "신청서를 찾을 수 없습니다. ID: " + context.getApplicationId()
                                                       ));

        // Context에 저장 (다음 Step에서 사용)
        context.setApplication(application);
        context.setPetId(application.getPetId());

        // 원본 상태 저장 (보상용)
        context.setOriginalApplicationStatus(application.getStatus());

        // 멱등성 체크: 이미 승인된 경우 스킵
        if (application.getStatus() == ApplicationStatus.APPROVED) {
            log.info("이미 승인된 신청서입니다. 스킵합니다. applicationId: {}",
                    context.getApplicationId());
            return;
        }

        // 상태 변경
        application.changeStatus(ApplicationStatus.APPROVED);
        applicationRepository.save(application);

        log.info("신청서 승인 완료 - applicationId: {}, 이전 상태: {}",
                context.getApplicationId(), context.getOriginalApplicationStatus());
    }

    @Override
    @Transactional
    public void compensate(AdoptionSagaContext context) {
        try {
            log.warn("신청서 승인 취소 시작 - applicationId: {}", context.getApplicationId());

            // 보상할 데이터가 없으면 스킵
            if (context.getOriginalApplicationStatus() == null) {
                log.warn("원본 상태 정보가 없습니다. 보상을 스킵합니다.");
                return;
            }

            Application application = applicationRepository.findById(context.getApplicationId())
                                                           .orElse(null);

            if (application == null) {
                log.error("신청서를 찾을 수 없어 보상할 수 없습니다. ID: {}", context.getApplicationId());
                return;
            }

            // 멱등성 체크: 이미 원래 상태면 스킵
            if (application.getStatus() == context.getOriginalApplicationStatus()) {
                log.info("이미 복원된 상태입니다. 스킵합니다. status: {}", application.getStatus());
                return;
            }

            // 상태 복원
            application.changeStatus(context.getOriginalApplicationStatus());
            applicationRepository.save(application);

            log.warn("신청서 승인 취소 완료 - applicationId: {}, 복원된 상태: {}",
                    context.getApplicationId(), context.getOriginalApplicationStatus());

        } catch (Exception e) {
            // 보상 실패는 로그만 남기고 진행
            log.error("신청서 승인 취소 실패 - applicationId: {}, 오류: {}",
                    context.getApplicationId(), e.getMessage());
            throw e; // 상위(SagaOrchestrator)에서 처리
        }
    }

    @Override
    public String getName() {
        return "신청서 승인";
    }
}
