package hello.pet.applicationservice.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hello.pet.applicationservice.entity.Application;
import hello.pet.applicationservice.entity.ApplicationStatus;
import hello.pet.applicationservice.repository.ApplicationRepository;
import hello.pet.applicationservice.saga.adoption.AdoptionSagaContext;
import hello.pet.applicationservice.saga.adoption.steps.ApproveApplicationStep;
import hello.pet.applicationservice.saga.adoption.steps.CompleteAnnouncementStep;
import hello.pet.applicationservice.saga.adoption.steps.MarkPetAdoptedStep;
import hello.pet.applicationservice.saga.adoption.steps.RejectOtherApplicationsStep;
import hello.pet.applicationservice.saga.core.SagaOrchestrator;
import hello.pet.applicationservice.saga.core.SagaStep;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Saga 패턴 테스트
 *
 * - Saga 실행 흐름 검증
 * - 보상 트랜잭션 동작 확인
 * - 멱등성 테스트
 */
@ExtendWith(MockitoExtension.class)
class AdoptionSagaTest {

    @Mock
    private SagaOrchestrator sagaOrchestrator;

    @Mock
    private ApproveApplicationStep approveApplicationStep;

    @Mock
    private RejectOtherApplicationsStep rejectOtherApplicationsStep;

    @Mock
    private CompleteAnnouncementStep completeAnnouncementStep;

    @Mock
    private MarkPetAdoptedStep markPetAdoptedStep;

    @Mock
    private ApplicationRepository applicationRepository;

    private AdoptionSagaContext context;

    @BeforeEach
    void setUp() {
        // 테스트용 Context 생성
        context = AdoptionSagaContext.builder()
                                     .announcementId(1L)
                                     .applicationId(100L)
                                     .userId(10L)
                                     .userRole("SHELTER")
                                     .build();
    }

    @Test
    @DisplayName("Saga 정상 실행 - 모든 Step 성공")
    void testSagaSuccessfulExecution() throws Exception {
        // given
        // Context는 이미 setUp에서 초기화됨

        // when
        // Step들을 순차적으로 실행
        approveApplicationStep.execute(context);
        rejectOtherApplicationsStep.execute(context);
        completeAnnouncementStep.execute(context);
        markPetAdoptedStep.execute(context);

        // then
        verify(approveApplicationStep, times(1)).execute(context);
        verify(rejectOtherApplicationsStep, times(1)).execute(context);
        verify(completeAnnouncementStep, times(1)).execute(context);
        verify(markPetAdoptedStep, times(1)).execute(context);
    }

    @Test
    @DisplayName("Step 3 실패 시 보상 - Step 2, 1 보상 실행")
    void testCompensationOnStep3Failure() throws Exception {
        // given
        // completeAnnouncementStep.execute()가 호출되면 무조건 예외를 발생시켜라
        doThrow(new RuntimeException("공고 완료 실패")).when(completeAnnouncementStep).execute(any());

        // when
        try {
            approveApplicationStep.execute(context);
            rejectOtherApplicationsStep.execute(context);
            completeAnnouncementStep.execute(context); // 실패 발생
        } catch (RuntimeException e) {
            // 보상 실행 (역순)
            rejectOtherApplicationsStep.compensate(context);
            approveApplicationStep.compensate(context);
        }

        // then
        // 실행 검증
        verify(approveApplicationStep, times(1)).execute(context);
        verify(rejectOtherApplicationsStep, times(1)).execute(context);
        verify(completeAnnouncementStep, times(1)).execute(context);

        // 보상 검증
        verify(rejectOtherApplicationsStep, times(1)).compensate(context);
        verify(approveApplicationStep, times(1)).compensate(context);

        // Step 4는 실행되지 않음
        verify(markPetAdoptedStep, never()).execute(any());
    }

    @Test
    @DisplayName("멱등성 테스트 - 이미 승인된 신청서")
    void testIdempotency_AlreadyApproved() throws Exception {
        // given
        Application mockApplication = mock(Application.class);
        when(mockApplication.getStatus()).thenReturn(ApplicationStatus.APPROVED);

        when(applicationRepository.findById(100L))
                .thenReturn(java.util.Optional.of(mockApplication));

        // when
        ApproveApplicationStep step = new ApproveApplicationStep(applicationRepository);
        step.execute(context);

        // then
        // 이미 APPROVED 상태이므로 상태 변경을 하지 않음
        verify(mockApplication, never()).changeStatus(any());
    }

    @Test
    @DisplayName("보상 멱등성 - 이미 원래 상태인 경우")
    void testCompensationIdempotency() throws Exception {
        // given
        context.setOriginalApplicationStatus(ApplicationStatus.SUBMITTED);

        Application mockApplication = mock(Application.class);
        when(mockApplication.getStatus()).thenReturn(ApplicationStatus.SUBMITTED);
        when(applicationRepository.findById(100L))
                .thenReturn(java.util.Optional.of(mockApplication));

        // when
        ApproveApplicationStep step = new ApproveApplicationStep(applicationRepository);
        step.compensate(context);

        // then
        // 이미 SUBMITTED 상태이므로 변경하지 않음
        verify(mockApplication, never()).changeStatus(any());
    }

    @Test
    @DisplayName("Context 데이터 전달 확인")
    void testContextDataSharing() {
        // given
        Long petId = 5L;
        ApplicationStatus originalStatus = ApplicationStatus.SUBMITTED;
        List<Long> rejectedIds = List.of(101L, 102L, 103L);

        // when
        context.setPetId(petId);
        context.setOriginalApplicationStatus(originalStatus);
        context.setRejectedApplicationIds(rejectedIds);
        context.setAnnouncementCompleted(true);
        context.setPetMarkedAsAdopted(true);

        // then
        assertThat(context.getPetId()).isEqualTo(petId);
        assertThat(context.getOriginalApplicationStatus()).isEqualTo(originalStatus);
        assertThat(context.getRejectedApplicationIds()).hasSize(3);
        assertThat(context.isAnnouncementCompleted()).isTrue();
        assertThat(context.isPetMarkedAsAdopted()).isTrue();
    }

    @Test
    @DisplayName("Saga Orchestrator 호출 검증")
    void testSagaOrchestratorInvocation() throws Exception {
        // given
        List<SagaStep<AdoptionSagaContext>> mockSteps = List.of(
                approveApplicationStep,
                rejectOtherApplicationsStep,
                completeAnnouncementStep,
                markPetAdoptedStep
        );

        // when
        sagaOrchestrator.execute(mockSteps, context);

        // then
        verify(sagaOrchestrator).execute(eq(mockSteps), eq(context));
    }
}
