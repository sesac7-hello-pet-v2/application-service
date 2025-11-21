package hello.pet.applicationservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ScoreStatus {

    NORMAL("정상", "적합한 입양 신청자입니다"),
    WARNING("주의", "점수가 낮아 추가 검토가 필요합니다"),
    DISQUALIFIED("탈락", "즉시 탈락 사유가 있습니다");

    private final String label;
    private final String description;

    public static ScoreStatus determine(String grade, boolean hasDisqualifyingFactor) {
        if (hasDisqualifyingFactor) {
            return DISQUALIFIED;
        }

        // D, F 등급은 주의
        if ("D".equals(grade) || "F".equals(grade)) {
            return WARNING;
        }

        // A, B, C 등급은 정상
        return NORMAL;
    }
}
