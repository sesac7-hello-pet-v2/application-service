package hello.pet.applicationservice.facade;

import hello.pet.applicationservice.client.AnnouncementServiceClient;
import hello.pet.applicationservice.dto.response.AnnouncementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnnouncementFacade {

    private final AnnouncementServiceClient announcementServiceClient;

    public AnnouncementResponse getAnnouncement(Long announcementId) {
        return announcementServiceClient.getAnnouncementById(announcementId);
    }

    public void completeAnnouncement(Long announcementId) {
        announcementServiceClient.completeAnnouncement(announcementId);
    }
}
