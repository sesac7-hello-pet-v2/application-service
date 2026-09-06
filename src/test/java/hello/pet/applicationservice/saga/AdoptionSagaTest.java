package hello.pet.applicationservice.saga;

import hello.pet.applicationservice.dto.response.AnnouncementResponse;
import hello.pet.applicationservice.entity.Application;
import hello.pet.applicationservice.entity.ApplicationStatus;
import hello.pet.applicationservice.entity.info.agreement.AgreementInfo;
import hello.pet.applicationservice.entity.info.care.*;
import hello.pet.applicationservice.entity.info.experience.PetExperienceInfo;
import hello.pet.applicationservice.entity.info.family.*;
import hello.pet.applicationservice.entity.info.financial.*;
import hello.pet.applicationservice.entity.info.housing.*;
import hello.pet.applicationservice.entity.info.plan.FuturePlanInfo;
import hello.pet.applicationservice.facade.AnnouncementFacade;
import hello.pet.applicationservice.facade.PetServiceFacade;
import hello.pet.applicationservice.repository.ApplicationRepository;
import hello.pet.applicationservice.saga.adoption.AdoptionSagaContext;
import hello.pet.applicationservice.saga.adoption.steps.ApproveApplicationStep;
import hello.pet.applicationservice.service.ApplicationService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static hello.pet.applicationservice.entity.ApplicationStatus.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 실제 Spring 프록시와 DB 커밋을 검증한다. 원격 서비스 호출만 대체한다. */
@SpringBootTest
@ActiveProfiles("test")
class AdoptionSagaTest {
    @Autowired ApplicationService service;
    @Autowired ApproveApplicationStep applicationStep;
    @Autowired ApplicationRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @MockitoBean AnnouncementFacade announcementFacade;
    @MockitoBean PetServiceFacade petServiceFacade;

    private Long selected;
    private Long submitted;
    private Long reviewing;
    private Long alreadyRejected;
    private Long unrelated;
    private LocalDateTime rejectedAt;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        selected = save(1L, UNDER_REVIEW);
        submitted = save(1L, SUBMITTED);
        reviewing = save(1L, UNDER_REVIEW);
        alreadyRejected = save(1L, REJECTED);
        unrelated = save(2L, SUBMITTED);
        rejectedAt = repository.findById(alreadyRejected).orElseThrow().getProcessedAt();
        AnnouncementResponse announcement = mock(AnnouncementResponse.class);
        when(announcement.getShelterId()).thenReturn(10L);
        when(announcementFacade.getAnnouncement(1L)).thenReturn(announcement);
    }

    @Test
    void commitsAllApplicationChangesBeforeCallingAnnouncement() {
        doAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertApproved(); // 별도 DB 조회로 다음 단계 전에 커밋되었음을 확인
            return null;
        }).when(announcementFacade).completeAnnouncement(1L, 10L);

        approve();

        assertApproved();
        verify(petServiceFacade).markAsAdopted(5L, 10L, "SHELTER");
    }

    @Test
    void suspendsCallerTransactionWhileRunningSaga() {
        doAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertApproved();
            return null;
        }).when(announcementFacade).completeAnnouncement(1L, 10L);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            approve();
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            status.setRollbackOnly();
        });
        assertApproved();
    }

    @Test
    void announcementFailureRestoresBothPendingStatusesAndProcessingTimes() {
        doAnswer(call -> {
            assertApproved();
            throw new IllegalStateException("공고 변경 거부");
        }).when(announcementFacade).completeAnnouncement(1L, 10L);

        assertThatThrownBy(this::approve).isInstanceOf(RuntimeException.class);

        assertRestored();
        verifyNoInteractions(petServiceFacade);
        verify(announcementFacade, never()).reopenAnnouncement(anyLong(), anyLong());
    }

    @Test
    void petFailureCompensatesAnnouncementBeforeApplications() {
        doThrow(new IllegalStateException("펫 변경 거부"))
                .when(petServiceFacade).markAsAdopted(5L, 10L, "SHELTER");
        doAnswer(call -> {
            assertApproved(); // 공고 보상 시점에는 신청서 보상이 아직 실행되지 않았다.
            return null;
        }).when(announcementFacade).reopenAnnouncement(1L, 10L);

        assertThatThrownBy(this::approve).isInstanceOf(RuntimeException.class);

        verify(announcementFacade).reopenAnnouncement(1L, 10L);
        assertRestored();
    }

    @Test
    void bulkUpdateFailureRollsBackApprovalAndEarlierBatchAndDoesNotCallRemoteMutation() {
        jdbc.execute("alter table applications add constraint reject_failure check (id <> "
                + reviewing + " or status <> 'REJECTED')");
        try {
            assertThatThrownBy(this::approve).isInstanceOf(RuntimeException.class);
            assertRestored();
            verify(announcementFacade, never()).completeAnnouncement(anyLong(), anyLong());
            verifyNoInteractions(petServiceFacade);
        } finally {
            jdbc.execute("alter table applications drop constraint reject_failure");
        }
    }

    @Test
    void compensationCanBeRepeatedWithoutChangingRestoredData() {
        AdoptionSagaContext context = AdoptionSagaContext.builder()
                .announcementId(1L).applicationId(selected).userId(10L).userRole("SHELTER").build();
        applicationStep.execute(context);
        assertApproved();
        applicationStep.compensate(context);
        applicationStep.compensate(context);
        assertRestored();
    }

    @Test
    void compensationFailureRollsBackAllLocalRestorations() {
        AdoptionSagaContext context = AdoptionSagaContext.builder()
                .announcementId(1L).applicationId(selected).userId(10L).userRole("SHELTER").build();
        applicationStep.execute(context);
        // 다른 변경이 끼어들어 복원할 상태와 맞지 않는 경우 전체 보상을 롤백한다.
        jdbc.update("update applications set status = 'SUBMITTED' where id = ?", reviewing);

        assertThatThrownBy(() -> applicationStep.compensate(context))
                .isInstanceOf(IllegalStateException.class);

        assertStatus(selected, APPROVED);
        assertStatus(submitted, REJECTED);
        assertStatus(reviewing, SUBMITTED);
    }

    @Test
    void restoresExistingProcessingTimestamp() {
        LocalDateTime original = LocalDateTime.of(2025, 1, 2, 3, 4, 5);
        jdbc.update("update applications set processed_at = ? where id = ?", original, selected);
        AdoptionSagaContext context = AdoptionSagaContext.builder()
                .announcementId(1L).applicationId(selected).userId(10L).userRole("SHELTER").build();

        applicationStep.execute(context);
        applicationStep.compensate(context);

        Application restored = repository.findById(selected).orElseThrow();
        assertThat(restored.getStatus()).isEqualTo(UNDER_REVIEW);
        assertThat(restored.getProcessedAt()).isEqualTo(original);
    }

    @Test
    void manyApplicationsUseIdQueriesAndBulkUpdatesWithoutLoadingOtherEntities() {
        for (int i = 0; i < 501; i++) {
            save(1L, UNDER_REVIEW);
        }
        AdoptionSagaContext context = AdoptionSagaContext.builder()
                .announcementId(1L).applicationId(selected).userId(10L).userRole("SHELTER").build();
        org.hibernate.stat.Statistics statistics = entityManagerFactory
                .unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean wasEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            applicationStep.execute(context);
            assertThat(statistics.getEntityLoadCount()).isEqualTo(1);
            assertThat(statistics.getEntityUpdateCount()).isEqualTo(1);
            assertThat(context.getSubmittedApplicationIds()).containsExactly(submitted);
            assertThat(context.getUnderReviewApplicationIds()).hasSize(502);
            assertThat(repository.countByIdInAndStatus(context.getUnderReviewApplicationIds(), REJECTED))
                    .isEqualTo(502);
            applicationStep.compensate(context);
            assertThat(statistics.getEntityLoadCount()).isEqualTo(2);
            assertThat(repository.countByIdInAndStatus(context.getUnderReviewApplicationIds(), UNDER_REVIEW))
                    .isEqualTo(502);
            assertRestored();
        } finally {
            statistics.setStatisticsEnabled(wasEnabled);
        }
    }

    @Test
    void noOtherApplicationsStillCommitsAndCompensatesSelectedApplication() {
        repository.deleteById(submitted);
        repository.deleteById(reviewing);
        AdoptionSagaContext context = AdoptionSagaContext.builder()
                .announcementId(1L).applicationId(selected).userId(10L).userRole("SHELTER").build();
        applicationStep.execute(context);
        assertStatus(selected, APPROVED);
        assertThat(context.getSubmittedApplicationIds()).isEmpty();
        assertThat(context.getUnderReviewApplicationIds()).isEmpty();
        applicationStep.compensate(context);
        assertStatus(selected, UNDER_REVIEW);
        assertThat(repository.findById(selected).orElseThrow().getProcessedAt()).isNull();
        assertUntouched();
    }

    @Test
    void bulkRejectionPreservesOtherApplicationsProcessingTimes() {
        LocalDateTime original = LocalDateTime.of(2025, 1, 2, 3, 4, 5);
        jdbc.update("update applications set processed_at = ? where id = ?", original, reviewing);
        AdoptionSagaContext context = AdoptionSagaContext.builder()
                .announcementId(1L).applicationId(selected).userId(10L).userRole("SHELTER").build();
        applicationStep.execute(context);
        assertThat(repository.findById(reviewing).orElseThrow().getProcessedAt()).isEqualTo(original);
        applicationStep.compensate(context);
        assertThat(repository.findById(reviewing).orElseThrow().getProcessedAt()).isEqualTo(original);
        assertStatus(reviewing, UNDER_REVIEW);
    }

    @Test
    void wrongAnnouncementDoesNotChangeApplications() {
        assertThatThrownBy(() -> service.approveApplication(1L, unrelated, 10L, "SHELTER"))
                .isInstanceOf(RuntimeException.class);
        assertRestored();
        verify(announcementFacade, never()).completeAnnouncement(anyLong(), anyLong());
    }

    @Test
    void alreadyApprovedRequestContinuesWithoutChangingPreviousApproval() {
        LocalDateTime original = LocalDateTime.of(2025, 1, 2, 3, 4, 5);
        jdbc.update("update applications set status = 'APPROVED', processed_at = ? where id = ?",
                original, selected);

        approve();

        assertApproved();
        assertThat(repository.findById(selected).orElseThrow().getProcessedAt()).isEqualTo(original);
        verify(announcementFacade).completeAnnouncement(1L, 10L);
        verify(petServiceFacade).markAsAdopted(5L, 10L, "SHELTER");
    }

    @Test
    void retryFailureRestoresOnlyApplicationsChangedByThisAttempt() {
        LocalDateTime original = LocalDateTime.of(2025, 1, 2, 3, 4, 5);
        jdbc.update("update applications set status = 'APPROVED', processed_at = ? where id = ?",
                original, selected);
        doAnswer(call -> {
            assertApproved();
            throw new IllegalStateException("공고 변경 거부");
        }).when(announcementFacade).completeAnnouncement(1L, 10L);

        assertThatThrownBy(this::approve).isInstanceOf(RuntimeException.class);

        assertStatus(selected, APPROVED);
        assertThat(repository.findById(selected).orElseThrow().getProcessedAt()).isEqualTo(original);
        assertStatus(submitted, SUBMITTED);
        assertStatus(reviewing, UNDER_REVIEW);
        assertUntouched();
        verifyNoInteractions(petServiceFacade);
    }

    @Test
    void retryAfterCompletedApprovalDoesNotUndoPreviouslyRejectedApplications() {
        approve();
        LocalDateTime original = repository.findById(selected).orElseThrow().getProcessedAt();
        doThrow(new IllegalStateException("펫 요청 실패"))
                .when(petServiceFacade).markAsAdopted(5L, 10L, "SHELTER");

        assertThatThrownBy(this::approve).isInstanceOf(RuntimeException.class);

        assertApproved();
        assertThat(repository.findById(selected).orElseThrow().getProcessedAt()).isEqualTo(original);
    }

    @Test
    void rejectedApplicationCannotBeApproved() {
        assertThatThrownBy(() -> service.approveApplication(1L, alreadyRejected, 10L, "SHELTER"))
                .isInstanceOf(RuntimeException.class);
        assertRestored();
        verify(announcementFacade, never()).completeAnnouncement(anyLong(), anyLong());
    }

    private void approve() {
        service.approveApplication(1L, selected, 10L, "SHELTER");
    }

    private void assertApproved() {
        assertStatus(selected, APPROVED);
        assertStatus(submitted, REJECTED);
        assertStatus(reviewing, REJECTED);
        assertThat(repository.findById(selected).orElseThrow().getProcessedAt()).isNotNull();
        // 기존 벌크 거절과 동일하게 다른 신청서는 상태만 변경한다.
        assertThat(repository.findById(submitted).orElseThrow().getProcessedAt()).isNull();
        assertThat(repository.findById(reviewing).orElseThrow().getProcessedAt()).isNull();
        assertUntouched();
    }

    private void assertRestored() {
        assertStatus(selected, UNDER_REVIEW);
        assertStatus(submitted, SUBMITTED);
        assertStatus(reviewing, UNDER_REVIEW);
        for (Long id : java.util.List.of(selected, submitted, reviewing)) {
            assertThat(repository.findById(id).orElseThrow().getProcessedAt()).isNull();
        }
        assertUntouched();
    }

    private void assertUntouched() {
        assertStatus(unrelated, SUBMITTED);
        assertStatus(alreadyRejected, REJECTED);
        assertThat(repository.findById(alreadyRejected).orElseThrow().getProcessedAt()).isEqualTo(rejectedAt);
    }

    private void assertStatus(Long id, ApplicationStatus status) {
        assertThat(jdbc.queryForObject("select status from applications where id = ?", String.class, id))
                .isEqualTo(status.name());
    }

    private Long save(Long announcementId, ApplicationStatus status) {
        Application application = Application.builder()
                .userId(20L).announcementId(announcementId).petId(5L).reason("입양 신청")
                .agreementInfo(new AgreementInfo(true, true, true))
                .housingInfo(new HousingInfo(HousingType.values()[0], ResidenceType.values()[0], true,
                        PetLivingPlace.values()[0], HouseSizeRange.values()[0]))
                .familyInfo(new FamilyInfo(1, false, FamilyAgreement.values()[0], false))
                .careInfo(new CareInfo(AbsenceTime.values()[0], CareTime.values()[0]))
                .financialInfo(new FinancialInfo(MonthlyBudget.values()[0], true))
                .petExperienceInfo(new PetExperienceInfo(false, "없음"))
                .futurePlanInfo(new FuturePlanInfo(true, "돌봄 계획"))
                .build();
        application.changeStatus(status);
        return repository.saveAndFlush(application).getId();
    }
}
