package hello.pet.applicationservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApplicationStatus {
    SUBMITTED("신청서 제출"),
    UNDER_REVIEW("검토 중"),
    APPROVED("승인"),
    REJECTED("거절");

    private final String label;
}
