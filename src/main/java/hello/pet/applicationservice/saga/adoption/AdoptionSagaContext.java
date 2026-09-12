package hello.pet.applicationservice.saga.adoption;

import java.util.List;
import java.time.LocalDateTime;
import hello.pet.applicationservice.entity.ApplicationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** Step 사이에 전달할 값과 보상 정보를 담는다. 메모리에만 있어 재시작 후에는 복구할 수 없다. */
@Getter
@Builder
@ToString
public class AdoptionSagaContext {
    // 입양 승인 요청 정보
    private final Long announcementId;
    private final Long applicationId;
    private final Long userId;
    private final String userRole;

    // 신청서에서 추출해 펫 서비스 호출에 사용한다.
    @Setter
    private Long petId;

    // 선택 신청서의 변경 전 값. 이미 APPROVED였다면 기존 승인을 보상하지 않는다.
    @Setter
    private ApplicationStatus originalApplicationStatus;

    @Setter
    private LocalDateTime originalProcessedAt;

    // 이번에 거절할 대상을 이전 상태별 ID로 보관하고, 보상 시 각 상태로 되돌린다.
    @Setter
    @Builder.Default
    @ToString.Exclude
    private List<Long> submittedApplicationIds = List.of();

    @Setter
    @Builder.Default
    @ToString.Exclude
    private List<Long> underReviewApplicationIds = List.of();

    // 공고 서비스가 이번 요청에서 실제로 변경했다고 응답한 경우에만 true.
    @Setter
    private boolean announcementCompleted;

    // 펫 호출이 정상 반환했는지 기록한다. 현재 마지막 Step이라 성공 후 보상 경로는 없다.
    @Setter
    private boolean petMarkedAsAdopted;
}
