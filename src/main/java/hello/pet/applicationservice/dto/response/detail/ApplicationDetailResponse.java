package hello.pet.applicationservice.dto.response.detail;

import hello.pet.applicationservice.entity.Application;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationDetailResponse {
    private Long announcementId;
    private Long userId;
    private String reason;
    private HousingInfoResponse housing;
    private FamilyInfoResponse family;
    private CareInfoResponse care;
    private FinancialInfoResponse financial;
    private PetExperienceInfoResponse petExperience;
    private FuturePlanInfoResponse futurePlan;
    private AgreementInfoResponse agreement;
    private LocalDateTime createdAt;

    public static ApplicationDetailResponse from(Application application) {
        return ApplicationDetailResponse.builder()
                                        .announcementId(application.getAnnouncementId())
                                        .userId(application.getUserId())
                                        .reason(application.getReason())
                                        .housing(HousingInfoResponse.from(application.getHousingInfo()))
                                        .family(FamilyInfoResponse.from(application.getFamilyInfo()))
                                        .care(CareInfoResponse.from(application.getCareInfo()))
                                        .financial(FinancialInfoResponse.from(application.getFinancialInfo()))
                                        .petExperience(PetExperienceInfoResponse.from(
                                                application.getPetExperienceInfo())
                                        )
                                        .futurePlan(FuturePlanInfoResponse.from(application.getFuturePlanInfo()))
                                        .agreement(AgreementInfoResponse.from(application.getAgreementInfo()))
                                        .createdAt(application.getCreatedAt())
                                        .build();
    }
}
