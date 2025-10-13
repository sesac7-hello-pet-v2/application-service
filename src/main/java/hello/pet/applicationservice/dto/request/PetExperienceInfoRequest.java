package hello.pet.applicationservice.dto.request;

import hello.pet.applicationservice.exception.ValidPetExperience;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@ValidPetExperience
public class PetExperienceInfoRequest {

    @NotNull(message = "반려동물 양육 경험 여부는 필수 입력 항목입니다.")
    private Boolean hasPetExperience;

    private String experienceDetails;
}
