package hello.pet.applicationservice.entity;

import hello.pet.applicationservice.entity.info.agreement.AgreementInfo;
import hello.pet.applicationservice.entity.info.care.CareInfo;
import hello.pet.applicationservice.entity.info.experience.PetExperienceInfo;
import hello.pet.applicationservice.entity.info.family.FamilyInfo;
import hello.pet.applicationservice.entity.info.financial.FinancialInfo;
import hello.pet.applicationservice.entity.info.housing.HousingInfo;
import hello.pet.applicationservice.entity.info.plan.FuturePlanInfo;
import hello.pet.applicationservice.exception.AlreadyProcessedApplicationException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "announcement_id", nullable = false)
    private Long announcementId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Embedded
    private HousingInfo housingInfo;

    @Embedded
    private FamilyInfo familyInfo;

    @Embedded
    private CareInfo careInfo;

    @Embedded
    private FinancialInfo financialInfo;

    @Embedded
    private PetExperienceInfo petExperienceInfo;

    @Embedded
    private FuturePlanInfo futurePlanInfo;

    @Embedded
    private AgreementInfo agreementInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    @Builder
    public Application(Long userId, Long announcementId, String reason, HousingInfo housingInfo,
                       FamilyInfo familyInfo, CareInfo careInfo, FinancialInfo financialInfo,
                       PetExperienceInfo petExperienceInfo, FuturePlanInfo futurePlanInfo,
                       AgreementInfo agreementInfo) {
        this.userId = userId;
        this.announcementId = announcementId;
        this.reason = reason;
        this.housingInfo = housingInfo;
        this.familyInfo = familyInfo;
        this.careInfo = careInfo;
        this.financialInfo = financialInfo;
        this.petExperienceInfo = petExperienceInfo;
        this.futurePlanInfo = futurePlanInfo;
        this.agreementInfo = agreementInfo;
    }

    public void approve() {
        if (this.status != ApplicationStatus.PENDING) {
            throw new AlreadyProcessedApplicationException();
        }
        this.status = ApplicationStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
    }

    public void reject() {
        if (this.status == ApplicationStatus.PENDING) {
            this.status = ApplicationStatus.REJECTED;
            this.processedAt = LocalDateTime.now();
        }
    }
}
