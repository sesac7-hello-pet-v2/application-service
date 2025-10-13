package hello.pet.applicationservice.dto.request;

import hello.pet.applicationservice.entity.info.family.FamilyAgreement;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class FamilyInfoRequest {
    @Min(value = 1, message = "가구원 수는 1명 이상이어야 합니다.")
    private int numberOfHousehold;

    @NotNull(message = "만 13세 미만 자녀 여부는 필수 입력 항목입니다.")
    private Boolean hasChildUnder13;

    @NotNull(message = "가족 동의 여부는 필수 입력 항목입니다.")
    private FamilyAgreement familyAgreement;

    @NotNull(message = "반려동물 알레르기 여부는 필수 입력 항목입니다.")
    private Boolean hasPetAllergy;
}
