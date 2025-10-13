package hello.pet.applicationservice.client;

import hello.pet.applicationservice.dto.response.AnnouncementResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "announcement-service")
public interface AnnouncementServiceClient {

    @GetMapping("/v1/announcements/{id}")
    AnnouncementResponse getAnnouncementById(@PathVariable Long id);

    @PatchMapping("/v1/announcements/{id}/complete")
    void completeAnnouncement(@PathVariable Long id);
}
