package hello.pet.applicationservice.saga.adoption;

import java.util.List;
import java.time.LocalDateTime;
import hello.pet.applicationservice.entity.ApplicationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 입양 승인 Saga의 Step들이 공유하는 실행 데이터와 보상 정보를 보관한다.
 *
 * AdoptionSaga가 요청마다 생성하고, SagaOrchestrator가 같은 인스턴스를 각 Step에 전달한다.
 * 신청서 Step은 펫 ID와 변경 전 정보를 저장하고, 후속 Step은 이를 읽어 외부 서비스를 호출한다.
 * 후속 단계가 실패하면 각 Step의 compensate()가 이 정보를 사용해 앞선 변경을 복원한다.
 *
 * 실행 순서와 보상 호출은 SagaOrchestrator의 책임이며, 이 클래스는 데이터를 보관하는 역할만 한다.
 * 메모리에만 존재하므로 서버 재시작 후 Saga를 재개하기 위한 영속 기록은 아니다.
 */
@Getter
@Builder
@ToString
public class AdoptionSagaContext {
    /** 입양 승인 대상 공고 ID. 공고 서비스 호출과 다른 신청서 조회에 사용한다. */
    private final Long announcementId;
    /** 승인할 신청서 ID. 해당 신청서의 변경과 보상에 사용한다. */
    private final Long applicationId;
    /** 승인 요청자의 ID. 외부 서비스 호출 시 사용자 정보로 전달한다. */
    private final Long userId;
    /** 승인 요청자의 역할. 외부 서비스의 권한 검증에 사용한다. */
    private final String userRole;

    /** 신청서 Step에서 추출하며, 펫 Step이 입양 완료 요청에 사용한다. */
    @Setter
    private Long petId;

    /** 선택한 신청서의 실행 전 상태. 이미 APPROVED였다면 기존 승인을 보상 대상에서 제외한다. */
    @Setter
    private ApplicationStatus originalApplicationStatus;

    /** 선택한 신청서의 승인 전 처리 시각. 보상 시 복원하며, 기존 값이 없으면 null이다. */
    @Setter
    private LocalDateTime originalProcessedAt;

    /**
     * 거절 전에 SUBMITTED였던 다른 신청서의 ID 목록.
     * 이 ID들만 벌크 거절하고, 보상 시 SUBMITTED로 복원한다.
     * 다른 신청서는 상태만 변경하므로 처리 시각을 별도로 보관하지 않는다.
     */
    @Setter
    @Builder.Default
    @ToString.Exclude
    private List<Long> submittedApplicationIds = List.of();

    /**
     * 거절 전에 UNDER_REVIEW였던 다른 신청서의 ID 목록.
     * 이 ID들만 벌크 거절하고, 보상 시 UNDER_REVIEW로 복원한다.
     * 전체 엔티티 대신 ID만 보관하며, 대량 ID가 로그에 출력되지 않도록 제외한다.
     */
    @Setter
    @Builder.Default
    @ToString.Exclude
    private List<Long> underReviewApplicationIds = List.of();

    /** 공고 서비스가 이번 호출에서 실제로 완료 상태로 변경했을 때만 true. */
    @Setter
    private boolean announcementCompleted;

    /**
     * 펫 입양 완료 호출이 정상 반환하면 true. 펫 Step의 보상 메서드에서 확인한다.
     * 현재는 마지막 Step이므로 성공 이후 후속 Step 실패로 보상되는 경로는 없다.
     */
    @Setter
    private boolean petMarkedAsAdopted;
}
