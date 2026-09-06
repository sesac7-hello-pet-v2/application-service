package hello.pet.applicationservice.client;

import hello.pet.applicationservice.dto.response.AnnouncementResponse;
import hello.pet.applicationservice.dto.response.AnnouncementCompletionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "announcement-service",
        url = "${ANNOUNCEMENT_SERVICE_URL:http://localhost:8084}",
        path = "/v1/announcements"
)
public interface AnnouncementServiceClient {

    @GetMapping("/{id}")
    AnnouncementResponse getAnnouncementById(@PathVariable("id") Long id);

    @PatchMapping("/{id}/complete")
    AnnouncementCompletionResponse completeAnnouncement(@PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long userId);

    @PatchMapping("/{id}/cancel-completion")
    void cancelAnnouncementCompletion(@PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long userId);

    @PatchMapping("/{id}/reopen")
    void reopenAnnouncement(@PathVariable("id") Long id, @RequestHeader("X-User-Id") Long userId);
}
