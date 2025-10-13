package hello.pet.applicationservice.dto.response.detail;

import hello.pet.applicationservice.entity.info.family.FamilyInfo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FamilyInfoResponse {
    private int numberOfHousehold;
    private boolean hasChildUnder13;
    private String familyAgreement;
    private String familyAgreementLabel;
    private boolean hasPetAllergy;

    public static FamilyInfoResponse from(FamilyInfo familyInfo) {
        return FamilyInfoResponse.builder()
                                 .numberOfHousehold(familyInfo.getNumberOfHousehold())
                                 .hasChildUnder13(familyInfo.isHasChildUnder13())
                                 .familyAgreement(familyInfo.getFamilyAgreement().name())
                                 .familyAgreementLabel(familyInfo.getFamilyAgreement().getLabel())
                                 .hasPetAllergy(familyInfo.isHasPetAllergy())
                                 .build();
    }
}
