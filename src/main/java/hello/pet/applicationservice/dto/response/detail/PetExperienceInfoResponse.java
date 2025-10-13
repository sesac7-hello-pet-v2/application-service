package hello.pet.applicationservice.dto.response.detail;

import hello.pet.applicationservice.entity.info.experience.PetExperienceInfo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PetExperienceInfoResponse {
    private boolean hasPetExperience;
    private String experienceDetails;

    public static PetExperienceInfoResponse from(PetExperienceInfo petExperienceInfo) {
        return PetExperienceInfoResponse.builder()
                                        .hasPetExperience(petExperienceInfo.isHasPetExperience())
                                        .experienceDetails(petExperienceInfo.getExperienceDetails())
                                        .build();
    }
}
