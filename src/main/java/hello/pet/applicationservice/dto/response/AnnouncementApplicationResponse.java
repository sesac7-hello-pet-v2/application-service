package hello.pet.applicationservice.dto.response;

import hello.pet.applicationservice.entity.Application;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnnouncementApplicationResponse {
    private Long applicationId;
    private String applicationStatusLabel;
    private Long userId;

    public static AnnouncementApplicationResponse from(Application application) {
        return AnnouncementApplicationResponse.builder()
                                         .applicationId(application.getId())
                                         .applicationStatusLabel(application.getStatus().getLabel())
                                         .userId(application.getUserId())
                                         .build();
    }
}
