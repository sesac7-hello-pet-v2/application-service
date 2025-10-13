package hello.pet.applicationservice.controller;

import hello.pet.applicationservice.dto.request.ApplicationCreateRequest;
import hello.pet.applicationservice.dto.request.ApplicationPageRequest;
import hello.pet.applicationservice.dto.response.ApplicationApprovalResponse;
import hello.pet.applicationservice.dto.response.ApplicationResponse;
import hello.pet.applicationservice.dto.response.ShelterApplicationsPageResponse;
import hello.pet.applicationservice.dto.response.UserApplicationPageResponse;
import hello.pet.applicationservice.dto.response.detail.ApplicationDetailResponse;
import hello.pet.applicationservice.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDetailResponse> getApplication(@PathVariable Long id) {
        ApplicationDetailResponse response = applicationService.getApplication(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(
            @Valid @RequestBody ApplicationCreateRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        ApplicationResponse response = applicationService.createApplication(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        applicationService.deleteApplication(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user")
    public ResponseEntity<UserApplicationPageResponse> getUserApplications(
            @RequestHeader("X-User-Id") Long userId,
            @ModelAttribute @Valid ApplicationPageRequest request) {

        UserApplicationPageResponse response = applicationService.getUserApplications(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/announcement/{announcementId}")
    public ResponseEntity<ShelterApplicationsPageResponse> getShelterApplications(
            @PathVariable Long announcementId,
            @RequestHeader("X-User-Id") Long shelterId,
            @ModelAttribute @Valid ApplicationPageRequest request) {

        ShelterApplicationsPageResponse response =
                applicationService.getShelterApplications(announcementId, request, shelterId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/announcement/{announcementId}/{applicationId}/approve")
    public ResponseEntity<ApplicationApprovalResponse> approveApplication(
            @PathVariable Long announcementId,
            @PathVariable Long applicationId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole) {

        ApplicationApprovalResponse response =
                applicationService.processApplicationApproval(announcementId, applicationId, userId, userRole);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
