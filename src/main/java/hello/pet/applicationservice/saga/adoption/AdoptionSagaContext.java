package hello.pet.applicationservice.saga.adoption;

import java.util.List;
import java.time.LocalDateTime;
import hello.pet.applicationservice.entity.ApplicationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 입양 승인 한 건을 처리하는 동안 각 Step이 공유할 요청 정보와 복원 정보를 보관한다.
@Getter
@Setter
@Builder
@ToString
public class AdoptionSagaContext {
    // 승인 대상과 요청자 정보
    private final Long announcementId;
    private final Long applicationId;
    private final Long userId;
    private final String userRole;

    // 입양 처리할 펫의 ID
    private Long petId;

    // 보상 시 복원할 신청서 상태
    private ApplicationStatus originalApplicationStatus;

    // 보상 시 복원할 신청서 처리 시각
    private LocalDateTime originalProcessedAt;

    // 접수 상태에서 거절할 신청서 ID. 보상 시 이전 상태로 복원하는 데 사용
    @Builder.Default
    @ToString.Exclude
    private List<Long> submittedApplicationIds = List.of();

    // 심사 중 상태에서 거절할 신청서 ID. 보상 시 이전 상태로 복원하는 데 사용
    @Builder.Default
    @ToString.Exclude
    private List<Long> underReviewApplicationIds = List.of();

    // 이번 요청의 공고 변경 여부. 공고 보상이 필요한지 판단
    private boolean announcementChanged;
}
