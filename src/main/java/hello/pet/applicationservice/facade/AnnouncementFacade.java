package hello.pet.applicationservice.facade;

import feign.FeignException;
import hello.pet.applicationservice.client.AnnouncementServiceClient;
import hello.pet.applicationservice.dto.response.AnnouncementResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnnouncementFacade {

    private final AnnouncementServiceClient announcementServiceClient;

    public AnnouncementResponse getAnnouncement(Long announcementId) {
        try {
            return announcementServiceClient.getAnnouncementById(announcementId);
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException("해당 번호의 공고를 찾을 수 없습니다. id=" + announcementId);
        }
    }

    public void completeAnnouncement(Long announcementId, Long userId) {
        try {
            announcementServiceClient.completeAnnouncement(announcementId, userId);
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException("해당 번호의 공고를 찾을 수 없습니다. id=" + announcementId);
        }
    }
}
