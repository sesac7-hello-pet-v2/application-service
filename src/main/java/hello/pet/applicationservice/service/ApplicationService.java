package hello.pet.applicationservice.service;

import hello.pet.applicationservice.dto.response.AnnouncementResponse;
import hello.pet.applicationservice.exception.ForbiddenOperationException;
import hello.pet.applicationservice.facade.AnnouncementFacade;
import hello.pet.applicationservice.dto.request.ApplicationCreateRequest;
import hello.pet.applicationservice.dto.request.ApplicationPageRequest;
import hello.pet.applicationservice.dto.response.ApplicationApprovalResponse;
import hello.pet.applicationservice.dto.response.ApplicationResponse;
import hello.pet.applicationservice.dto.response.ShelterApplicationResponse;
import hello.pet.applicationservice.dto.response.ShelterApplicationsPageResponse;
import hello.pet.applicationservice.dto.response.UserApplicationPageResponse;
import hello.pet.applicationservice.dto.response.UserApplicationResponse;
import hello.pet.applicationservice.dto.response.detail.ApplicationDetailResponse;
import hello.pet.applicationservice.entity.Application;
import hello.pet.applicationservice.entity.ApplicationStatus;
import hello.pet.applicationservice.repository.ApplicationRepository;
import hello.pet.applicationservice.exception.AlreadyProcessedApplicationException;
import hello.pet.applicationservice.exception.AnnouncementAlreadyCompletedException;
import hello.pet.applicationservice.exception.AnnouncementApprovalPermissionException;
import hello.pet.applicationservice.exception.ApplicationAlreadyApprovedException;
import hello.pet.applicationservice.exception.DuplicateApplicationException;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final AnnouncementFacade announcementFacade;

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

        return ApplicationDetailResponse.from(application);
    }

    @Transactional(readOnly = true)
    public ShelterApplicationsPageResponse getShelterApplications(Long announcementId,
                                                                  ApplicationPageRequest request,
                                                                  Long shelterId) {

        AnnouncementResponse announcement = announcementFacade.getAnnouncement(announcementId);

        if (!announcement.getShelterId().equals(shelterId)) {
            throw new ForbiddenOperationException("해당 공고에 대한 접근 권한이 없습니다.");
        }

        Pageable pageable = request.toPageable();
        Page<Application> page = applicationRepository.findByAnnouncementId(announcementId, pageable);

        List<ShelterApplicationResponse> content = page.stream()
                                                       .map(ShelterApplicationResponse::from)
                                                       .toList();

        return ShelterApplicationsPageResponse.of(pageable, content, page.getTotalElements(), announcement);
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

        if ("COMPLETED".equals(announcement.getAnnouncementStatus())) {
            throw new AnnouncementAlreadyCompletedException(announcement.getId());
        }

        Optional<Application> existingApplication = applicationRepository.findByUserIdAndAnnouncementId(
                userId,
                announcement.getId()
        );

        if (existingApplication.isPresent()) {
            throw new DuplicateApplicationException();
        }

        Application application = request.toEntity(userId, announcement.getId());
        applicationRepository.save(application);

        return ApplicationResponse.from(application.getId());
    }

    public ApplicationApprovalResponse processApplicationApproval(Long announcementId,
                                                                  Long applicationId,
                                                                  Long userId,
                                                                  String userRole) {
        // 보호소의 공고 승인 권한 검증
        validateShelterApprovalPermission(announcementId, userId, userRole);

        // 해당 공고에 대해 신청서 승인 및 나머지 신청서 일괄 거절 처리
        approveAndRejectApplications(announcementId, applicationId);

        // 공고 상태를 완료로 변경
        announcementFacade.completeAnnouncement(announcementId);

        return ApplicationApprovalResponse.of(announcementId, applicationId);
    }

    private void validateShelterApprovalPermission(Long announcementId, Long userId, String userRole) {
        AnnouncementResponse announcement = announcementFacade.getAnnouncement(announcementId);

        boolean isShelter = "SHELTER".equals(userRole);
        boolean isShelterOwner = announcement.getShelterId().equals(userId);

        if (!(isShelter && isShelterOwner)) {
            throw new AnnouncementApprovalPermissionException();
        }
    }

    private void approveAndRejectApplications(Long announcementId, Long applicationId) {
        Application approvedApp = applicationRepository.findByIdAndAnnouncementIdAndStatus(
                                                               applicationId, announcementId, ApplicationStatus.PENDING
                                                       )
                                                       .orElseThrow(AlreadyProcessedApplicationException::new);

        approvedApp.approve();
        applicationRepository.bulkRejectApplications(announcementId, applicationId);
    }
}
