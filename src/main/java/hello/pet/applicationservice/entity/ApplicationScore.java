package hello.pet.applicationservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "application_scores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationScore {

    @Id
    private Long applicationId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "application_id")
    private Application application;

    // ========== 카테고리별 점수 ==========
    @Column(nullable = false)
    private Integer housingScore = 0;        // 주거 환경 점수 (0-25)

    @Column(nullable = false)
    private Integer financialScore = 0;      // 경제력 점수 (0-20)

    @Column(nullable = false)
    private Integer experienceScore = 0;     // 경험 및 지식 점수 (0-20)

    @Column(nullable = false)
    private Integer careScore = 0;           // 돌봄 시간 및 환경 점수 (0-20)

    @Column(nullable = false)
    private Integer familyScore = 0;         // 가족 동의 점수 (0-15)

    // ========== 종합 점수 ==========
    @Column(nullable = false)
    private Integer totalScore = 0;          // 총점 (0-100)

    @Column(length = 10)
    private String grade;                    // 등급 (A, B, C, D, F)

    // ========== 즉시 탈락 여부 ==========
    @Column(nullable = false)
    private boolean hasDisqualifyingFactor = false;  // 즉시 탈락 요인 존재

    @Column(columnDefinition = "TEXT")
    private String disqualifyingReason;              // 탈락 사유

    // ========== 메타 정보 ==========
    @Column(nullable = false)
    private LocalDateTime calculatedAt;              // 점수 계산 일시

    @Builder
    public ApplicationScore(Application application,
                            Integer housingScore,
                            Integer financialScore,
                            Integer experienceScore,
                            Integer careScore,
                            Integer familyScore,
                            boolean hasDisqualifyingFactor,
                            String disqualifyingReason) {
        this.application = application;
        this.applicationId = application.getId();
        this.housingScore = housingScore;
        this.financialScore = financialScore;
        this.experienceScore = experienceScore;
        this.careScore = careScore;
        this.familyScore = familyScore;
        this.hasDisqualifyingFactor = hasDisqualifyingFactor;
        this.disqualifyingReason = disqualifyingReason;
        this.calculatedAt = LocalDateTime.now();

        calculateTotalScore();
        determineGrade();
    }

    private void calculateTotalScore() {
        if (hasDisqualifyingFactor) {
            this.totalScore = 0;
            return;
        }

        this.totalScore = housingScore + financialScore + experienceScore + careScore + familyScore;
    }

    /**
     * 등급 결정
     * A: 85점 이상 (매우 적합)
     * B: 70-84점 (적합)
     * C: 55-69점 (보통)
     * D: 40-54점 (재검토 필요)
     * F: 40점 미만 또는 즉시 탈락
     */
    private void determineGrade() {
        if (hasDisqualifyingFactor || totalScore < 40) {
            this.grade = "F";
        } else if (totalScore >= 85) {
            this.grade = "A";
        } else if (totalScore >= 70) {
            this.grade = "B";
        } else if (totalScore >= 55) {
            this.grade = "C";
        } else {
            this.grade = "D";
        }
    }

}
