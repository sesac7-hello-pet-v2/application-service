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
    @NotNull(message = "주거 정보는 필수 입력 항목입니다.")
    private HousingInfoRequest housingInfo;

    @Valid
    @NotNull(message = "가족 정보는 필수 입력 항목입니다.")
    private FamilyInfoRequest familyInfo;

    @Valid
    @NotNull(message = "돌봄 정보는 필수 입력 항목입니다.")
    private CareInfoRequest careInfo;

    @Valid
    @NotNull(message = "재정 정보는 필수 입력 항목입니다.")
    private FinancialInfoRequest financialInfo;

    @Valid
    @NotNull(message = "반려동물 경험 정보는 필수 입력 항목입니다.")
    private PetExperienceInfoRequest petExperienceInfo;

    @Valid
    @NotNull(message = "향후 계획 정보는 필수 입력 항목입니다.")
    private FuturePlanInfoRequest futurePlanInfo;

    @Valid
    @NotNull(message = "동의 정보는 필수 입력 항목입니다.")
    private AgreementInfoRequest agreement;

    public Application toEntity(Long userId, Long announcementId, Long petId) {
        return Application.builder()
                          .userId(userId)
                          .announcementId(announcementId)
                          .petId(petId)
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
