package hello.pet.applicationservice.dto.response.detail;

import hello.pet.applicationservice.dto.response.UserResponse;
import hello.pet.applicationservice.entity.Application;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationDetailResponse {
    private Long announcementId;
    private Long userId;
    private String userName;
    private String phoneNumber;
    private String email;
    private String shelterName;
    private String reason;
    private HousingInfoResponse housing;
    private FamilyInfoResponse family;
    private CareInfoResponse care;
    private FinancialInfoResponse financial;
    private PetExperienceInfoResponse petExperience;
    private FuturePlanInfoResponse futurePlan;
    private AgreementInfoResponse agreement;
    private LocalDateTime createdAt;

    public static ApplicationDetailResponse from(Application application, UserResponse applicantUser,
                                                 UserResponse shelterUser) {
        return ApplicationDetailResponse.builder()
                                        .announcementId(application.getAnnouncementId())
                                        .userId(application.getUserId())
                                        .userName(applicantUser.getUsername())
                                        .phoneNumber(applicantUser.getPhoneNumber())
                                        .email(applicantUser.getEmail())
                                        .shelterName(shelterUser.getUsername())
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
