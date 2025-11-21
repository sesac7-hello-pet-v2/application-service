package hello.pet.applicationservice.dto.response;

import hello.pet.applicationservice.entity.Application;
import hello.pet.applicationservice.entity.ApplicationScore;
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

    // 점수 정보
    private Integer totalScore;
    private String grade;
    private String scoreStatus;

    public static AnnouncementApplicationResponse from(Application application,
                                                       UserResponse user,
                                                       ApplicationScore score) {
        AnnouncementApplicationResponseBuilder builder = AnnouncementApplicationResponse
                .builder()
                .applicationId(application.getId())
                .applicationStatusLabel(application.getStatus().getLabel())
                .userId(application.getUserId())
                .userName(user.getUsername())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail());

        if (score != null) {
            builder.totalScore(score.getTotalScore())
                   .grade(score.getGrade())
                   .scoreStatus(score.getStatus().getLabel());
        }

        return builder.build();
    }
}
