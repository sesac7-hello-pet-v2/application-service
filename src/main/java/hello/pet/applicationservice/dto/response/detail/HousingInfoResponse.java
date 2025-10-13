package hello.pet.applicationservice.dto.response.detail;

import hello.pet.applicationservice.entity.info.housing.HousingInfo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HousingInfoResponse {
    private String housingType;
    private String housingTypeLabel;
    private String residenceType;
    private String residenceTypeLabel;
    private boolean petAllowed;
    private String petLivingPlace;
    private String petLivingPlaceLabel;
    private String houseSizeRange;
    private String houseSizeRangeLabel;

    public static HousingInfoResponse from(HousingInfo housingInfo) {
        return HousingInfoResponse.builder()
                                  .housingType(housingInfo.getHousingType().name())
                                  .housingTypeLabel(housingInfo.getHousingType().getLabel())
                                  .residenceType(housingInfo.getResidenceType().name())
                                  .residenceTypeLabel(housingInfo.getResidenceType().getLabel())
                                  .petAllowed(housingInfo.isPetAllowed())
                                  .petLivingPlace(housingInfo.getPetLivingPlace().name())
                                  .petLivingPlaceLabel(housingInfo.getPetLivingPlace().getLabel())
                                  .houseSizeRange(housingInfo.getHouseSizeRange().name())
                                  .houseSizeRangeLabel(housingInfo.getHouseSizeRange().getLabel())
                                  .build();
    }
}
