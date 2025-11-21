package hello.pet.applicationservice.service;

import hello.pet.applicationservice.dto.request.ApplicationCreateRequest;
import hello.pet.applicationservice.dto.request.ApplicationPageRequest;
import hello.pet.applicationservice.dto.response.AnnouncementApplicationResponse;
import hello.pet.applicationservice.dto.response.AnnouncementApplicationsPageResponse;
import hello.pet.applicationservice.dto.response.AnnouncementResponse;
import hello.pet.applicationservice.dto.response.ApplicationApprovalResponse;
import hello.pet.applicationservice.dto.response.ApplicationResponse;
import hello.pet.applicationservice.dto.response.UserApplicationPageResponse;
import hello.pet.applicationservice.dto.response.UserApplicationResponse;
import hello.pet.applicationservice.dto.response.detail.ApplicationDetailResponse;
import hello.pet.applicationservice.entity.Application;
import hello.pet.applicationservice.entity.ApplicationStatus;
import hello.pet.applicationservice.exception.AnnouncementAlreadyCompletedException;
import hello.pet.applicationservice.exception.ApplicationAlreadyApprovedException;
import hello.pet.applicationservice.exception.DuplicateApplicationException;
import hello.pet.applicationservice.exception.ForbiddenOperationException;
import hello.pet.applicationservice.dto.response.UserResponse;
import hello.pet.applicationservice.facade.AnnouncementFacade;
import hello.pet.applicationservice.facade.UserServiceFacade;
import hello.pet.applicationservice.repository.ApplicationRepository;
import hello.pet.applicationservice.saga.adoption.AdoptionSaga;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final AnnouncementFacade announcementFacade;
    private final UserServiceFacade userServiceFacade;
    private final AdoptionSaga adoptionSaga;
    private final ScoringService scoringService;

    public void deleteApplication(Long id, Long userId) {
        Application application = applicationRepository.findById(id)
                                                       .orElseThrow(() -> new EntityNotFoundException(
                                                               "해당 번호의 입양 신청서를 찾을 수 없습니다. id=" + id)
                                                       );

        if (!application.getUserId().equals(userId)) {
            throw new ForbiddenOperationException("입양 신청서를 삭제할 권한이 없습니다.");
        }

        if (application.getStatus() == ApplicationStatus.APPROVED) {
            throw new ApplicationAlreadyApprovedException(application.getId());
        }

        applicationRepository.delete(application);
    }

    @Transactional(readOnly = true)
    public ApplicationDetailResponse getApplication(Long id) {
        Application application = applicationRepository.findById(id)
                                                       .orElseThrow(() -> new EntityNotFoundException(
                                                               "해당 번호의 입양 신청서를 찾을 수 없습니다. id=" + id)
                                                       );

        UserResponse applicantUser = userServiceFacade.getUserDetail(application.getUserId());
        AnnouncementResponse announcement = announcementFacade.getAnnouncement(application.getAnnouncementId());
        UserResponse shelterUser = userServiceFacade.getUserDetail(announcement.getShelterId());

        return ApplicationDetailResponse.from(application, applicantUser, shelterUser);
    }

    @Transactional(readOnly = true)
    public AnnouncementApplicationsPageResponse getAnnouncementApplications(Long announcementId,
                                                                            ApplicationPageRequest request,
                                                                            Long shelterId) {

        AnnouncementResponse announcement = announcementFacade.getAnnouncement(announcementId);

        if (!announcement.getShelterId().equals(shelterId)) {
            throw new ForbiddenOperationException("해당 공고에 대한 접근 권한이 없습니다.");
        }

        Pageable pageable = request.toPageable();
        Page<Application> page = applicationRepository.findByAnnouncementId(announcementId, pageable);

        List<AnnouncementApplicationResponse> content = page.stream()
                                                            .map(application -> {
                                                                UserResponse user = userServiceFacade.getUserDetail(
                                                                        application.getUserId());
                                                                return AnnouncementApplicationResponse.from(application,
                                                                        user);
                                                            })
                                                            .toList();

        return AnnouncementApplicationsPageResponse.of(pageable, content, page.getTotalElements(), announcement);
    }

    @Transactional(readOnly = true)
    public UserApplicationPageResponse getUserApplications(Long userId, ApplicationPageRequest request) {
        Pageable pageable = request.toPageable();

        Page<Application> page = applicationRepository.findByUserId(userId, pageable);

        List<UserApplicationResponse> content = page.stream()
                                                    .map(app -> {
                                                        AnnouncementResponse announcement =
                                                                announcementFacade.getAnnouncement(
                                                                        app.getAnnouncementId()
                                                                );
                                                        return UserApplicationResponse.of(app, announcement);
                                                    })
                                                    .toList();

        return UserApplicationPageResponse.of(pageable, content, page.getTotalElements());
    }

    public ApplicationResponse createApplication(ApplicationCreateRequest request, Long userId) {
        AnnouncementResponse announcement = announcementFacade.getAnnouncement(request.getAnnouncementId());

        // OPEN 상태일 때만 신청 가능
        if (!"OPEN".equals(announcement.getAnnouncementStatus())) {
            throw new AnnouncementAlreadyCompletedException(announcement.getId());
        }

        Optional<Application> existingApplication = applicationRepository.findByUserIdAndAnnouncementId(
                userId,
                announcement.getId()
        );

        if (existingApplication.isPresent()) {
            throw new DuplicateApplicationException();
        }

        Application application = request.toEntity(userId, announcement.getId(), announcement.getPetId());
        Application savedApplication = applicationRepository.save(application);

        // 점수 자동 계산 (점수 계산 실패해도 신청서는 정상적으로 저장됨)
        try {
            scoringService.calculateAndSaveScore(savedApplication);
            log.info("Score calculated successfully for application ID: {}", savedApplication.getId());
        } catch (Exception e) {
            log.error("Failed to calculate score for application ID: {}, but application saved successfully",
                    savedApplication.getId(), e);
        }

        return ApplicationResponse.from(savedApplication.getId());
    }

    /**
     * Saga 패턴으로 입양 신청 승인 과정 처리
     */
    public ApplicationApprovalResponse approveApplication(Long announcementId,
                                                          Long applicationId,
                                                          Long userId,
                                                          String userRole) {
        log.info("입양 신청 승인 시작 (Saga 패턴) - announcementId: {}, applicationId: {}",
                announcementId, applicationId);

        // Saga 패턴으로 처리 위임
        return adoptionSaga.execute(announcementId, applicationId, userId, userRole);
    }

    /**
     * 특정 사용자가 특정 공고에 이미 지원했는지 여부를 확인
     * 프론트엔드에서 지원 상태 표시 및 중복 지원 방지 UI 처리를 위해 사용됨
     */
    @Transactional(readOnly = true)
    public boolean hasUserAppliedToAnnouncement(Long announcementId, Long userId) {
        return applicationRepository.findByUserIdAndAnnouncementId(userId, announcementId).isPresent();
    }

    /**
     * 공고 마감 시 해당 공고의 모든 신청 상태를 UNDER_REVIEW로 변경
     * announcement-service의 스케줄러에서 호출됨
     */
    public void updateApplicationsToUnderReviewForClosedAnnouncement(Long announcementId) {
        log.info("공고 ID {}의 모든 신청을 UNDER_REVIEW 상태로 변경 시작", announcementId);

        // 벌크 업데이트로 해당 공고의 모든 SUBMITTED 상태를 UNDER_REVIEW로 변경
        int updatedCount = applicationRepository.bulkUpdateStatus(
                announcementId,
                ApplicationStatus.SUBMITTED,
                ApplicationStatus.UNDER_REVIEW
        );

        if (updatedCount == 0) {
            log.info("공고 ID {}에 대기 중인 신청이 없습니다.", announcementId);
        } else {
            log.info("공고 ID {}의 신청 {}건을 UNDER_REVIEW 상태로 변경 완료", announcementId, updatedCount);
        }
    }
}
