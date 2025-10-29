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
    private String userName;
    private String phoneNumber;
    private String email;

    public static AnnouncementApplicationResponse from(Application application, UserResponse user) {
        return AnnouncementApplicationResponse.builder()
                                              .applicationId(application.getId())
                                              .applicationStatusLabel(application.getStatus().getLabel())
                                              .userId(application.getUserId())
                                              .userName(user.getUsername())
                                              .phoneNumber(user.getPhoneNumber())
                                              .email(user.getEmail())
                                              .build();
    }
}
