package hello.pet.applicationservice.client;

import hello.pet.applicationservice.dto.response.AnnouncementResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "announcement-service",
        url = "http://announcement-service:8084",
        path = "/v1/announcements"
)
public interface AnnouncementServiceClient {

    @GetMapping("/{id}")
    AnnouncementResponse getAnnouncementById(@PathVariable("id") Long id);

    @PatchMapping("/{id}/complete")
    void completeAnnouncement(@PathVariable("id") Long id);
}
