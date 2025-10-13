package hello.pet.applicationservice.dto.request;

import hello.pet.applicationservice.entity.Application;
import hello.pet.applicationservice.entity.info.agreement.AgreementInfo;
import hello.pet.applicationservice.entity.info.care.CareInfo;
import hello.pet.applicationservice.entity.info.experience.PetExperienceInfo;
import hello.pet.applicationservice.entity.info.family.FamilyInfo;
import hello.pet.applicationservice.entity.info.financial.FinancialInfo;
import hello.pet.applicationservice.entity.info.housing.HousingInfo;
import hello.pet.applicationservice.entity.info.plan.FuturePlanInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ApplicationCreateRequest {
    @NotNull
    private Long announcementId;

    @NotBlank(message = "신청 희망 사유는 필수 입력 항목입니다.")
    private String reason;

    @Valid
    private HousingInfoRequest housingInfo;

    @Valid
    private FamilyInfoRequest familyInfo;

    @Valid
    private CareInfoRequest careInfo;

    @Valid
    private FinancialInfoRequest financialInfo;

    @Valid
    private PetExperienceInfoRequest petExperienceInfo;

    @Valid
    private FuturePlanInfoRequest futurePlanInfo;

    @Valid
    private AgreementInfoRequest agreement;

    public Application toEntity(Long userId, Long announcementId) {
        return Application.builder()
                          .userId(userId)
                          .announcementId(announcementId)
                          .reason(reason)
                          .housingInfo(HousingInfo.from(housingInfo))
                          .familyInfo(FamilyInfo.from(familyInfo))
                          .careInfo(CareInfo.from(careInfo))
                          .financialInfo(FinancialInfo.from(financialInfo))
                          .petExperienceInfo(PetExperienceInfo.from(petExperienceInfo))
                          .futurePlanInfo(FuturePlanInfo.from(futurePlanInfo))
                          .agreementInfo(AgreementInfo.from(agreement))
                          .build();
    }
}
