package hello.pet.applicationservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApplicationStatus {
    PENDING("대기"),
    APPROVED("승인"),
    REJECTED("거절"),
    CANCELLED("취소");

    private final String label;
}
