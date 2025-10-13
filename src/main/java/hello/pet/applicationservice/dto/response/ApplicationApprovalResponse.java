package hello.pet.applicationservice.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationApprovalResponse {
    private Long announcementId;
    private Long applicationId;
    private LocalDateTime processedAt;
    private String message;

    public static ApplicationApprovalResponse of(Long announcementId, Long applicationId) {
        return ApplicationApprovalResponse.builder()
                                          .announcementId(announcementId)
                                          .applicationId(applicationId)
                                          .processedAt(LocalDateTime.now())
                                          .message("승인이 완료되었습니다.")
                                          .build();
    }
}
