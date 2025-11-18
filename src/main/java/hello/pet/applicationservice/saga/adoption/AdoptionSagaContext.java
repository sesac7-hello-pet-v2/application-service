package hello.pet.applicationservice.saga.adoption;

import hello.pet.applicationservice.entity.Application;
import hello.pet.applicationservice.entity.ApplicationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 역할: Step 간 데이터 공유 저장소
 *
 * 데이터 종류:
 * 1. Saga 시작 시 받은 값 (final) - 변경 불가
 * 2. Step 실행 중 생성된 값 (@Setter) - 변경 가능
 *
 * 사용 목적:
 * - 정방향 실행: 각 Step이 필요한 데이터를 읽고 결과를 저장
 * - 역방향 보상: 실패 시 원래 상태로 되돌리기 위한 정보 참조
 */
@Getter
@Builder
public class AdoptionSagaContext {

    // ============ Saga 시작 시 받은 값 (변경 불가) ============
    private final Long announcementId;  // 공고 ID
    private final Long applicationId;   // 승인할 신청서 ID
    private final Long userId;          // 요청한 보호소 담당자 ID
    private final String userRole;      // 요청자 권한 (SHELTER 확인용)

    // ============ Saga 실행 중 생성되는 값 (변경 가능) ============
    // ============ Step 1: 신청서 승인 ============
    /**
     * DB에서 조회한 신청서 엔티티
     * 언제 저장: Step 1 실행 시 applicationRepository.findById() 결과
     * 어디서 사용: Step 1 보상 시 상태 복원을 위해 참조
     */
    @Setter
    private Application application;

    /**
     * 신청서에서 추출한 펫 ID
     * 언제 저장: Step 1에서 application.getPetId()로 추출
     * 어디서 사용: Step 4에서 pet-service의 /pets/{petId}/mark-adopted 호출 시
     */
    @Setter
    private Long petId;

    /**
     * 신청서의 원래 상태 (변경 전 백업)
     * 언제 저장: Step 1에서 상태를 APPROVED로 변경하기 직전
     * 어디서 사용: Step 1 보상에서 실패 시 원래 상태로 되돌림
     */
    @Setter
    private ApplicationStatus originalApplicationStatus;

    // ============ Step 2: 다른 신청서 거절 ============
    /**
     * REJECTED로 변경한 신청서들의 ID 목록
     * 언제 저장: Step 2에서 다른 신청서들을 거절 처리한 후
     * 어디서 사용: Step 2 보상에서 이 ID들의 상태를 SUBMITTED로 되돌림
     */
    @Setter
    private List<Long> rejectedApplicationIds = new ArrayList<>();

    // ============ Step 3: 공고 완료 처리 ============
    /**
     * announcement-service 호출 성공 여부 플래그
     * 언제 저장: Step 3에서 /announcements/{id}/complete 호출 성공 시 true
     * 어디서 사용: Step 3 보상에서 true면 /announcements/{id}/reopen 호출
     */
    @Setter
    private boolean announcementCompleted = false;

    // ============ Step 4: 펫 입양 완료 ============
    /**
     * pet-service 호출 성공 여부 플래그
     * 언제 저장: Step 4에서 /pets/{petId}/mark-adopted 호출 성공 시 true
     * 어디서 사용: Step 4 보상에서 true면 /pets/{petId}/mark-announced 호출
     */
    @Setter
    private boolean petMarkedAsAdopted = false;

    // ============ 디버깅 ============

    /**
     * 로그 출력용 상태 요약
     * 포함 정보: 공고ID, 신청서ID, 원래상태, 거절건수, 완료플래그들
     */
    @Override
    public String toString() {
        return String.format(
                "AdoptionSagaContext[announcementId=%d, applicationId=%d, " +
                        "originalStatus=%s, rejectedCount=%d, announcementCompleted=%s, petAdopted=%s]",
                announcementId,
                applicationId,
                originalApplicationStatus,
                rejectedApplicationIds != null ? rejectedApplicationIds.size() : 0,
                announcementCompleted,
                petMarkedAsAdopted
        );
    }
}
