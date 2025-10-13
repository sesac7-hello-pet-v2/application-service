package hello.pet.applicationservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationResponse {
    private Long applicationId;
    private String message;

    public static ApplicationResponse from(Long applicationId) {
        return ApplicationResponse.builder()
                                  .applicationId(applicationId)
                                  .message("신청이 완료되었습니다.")
                                  .build();
    }
}
