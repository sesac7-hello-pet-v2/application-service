package hello.pet.applicationservice.dto.request;

import hello.pet.applicationservice.entity.info.housing.HouseSizeRange;
import hello.pet.applicationservice.entity.info.housing.HousingType;
import hello.pet.applicationservice.entity.info.housing.PetLivingPlace;
import hello.pet.applicationservice.entity.info.housing.ResidenceType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class HousingInfoRequest {

    @NotNull(message = "주거 형태는 필수 입력 항목입니다.")
    private HousingType housingType;

    @NotNull(message = "거주 형태는 필수 입력 항목입니다.")
    private ResidenceType residenceType;

    @NotNull(message = "반려동물 허용 여부는 필수 입력 항목입니다.")
    private Boolean petAllowed;

    @NotNull(message = "반려동물 생활 공간은 필수 입력 항목입니다.")
    private PetLivingPlace petLivingPlace;

    @NotNull(message = "주거 면적은 필수 입력 항목입니다.")
    private HouseSizeRange houseSizeRange;
}
