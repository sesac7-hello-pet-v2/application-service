package hello.pet.applicationservice.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserApplicationResponse {
    private Long applicationId;
    private Long announcementId;
    private String applicationStatusLabel;
    private LocalDateTime submittedAt;
    private String petImageUrl;

    public static UserApplicationResponse of(hello.pet.applicationservice.entity.Application application,
                                             AnnouncementResponse announcement) {
        return UserApplicationResponse.builder()
                                      .applicationId(application.getId())
                                      .announcementId(announcement.getId())
                                      .applicationStatusLabel(application.getStatus().getLabel())
                                      .submittedAt(application.getCreatedAt())
                                      .petImageUrl(announcement.getImageUrl())
                                      .build();
    }
}
