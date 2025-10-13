package hello.pet.applicationservice.exception;

import hello.pet.applicationservice.dto.request.PetExperienceInfoRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PetExperienceInfoValidator implements
        ConstraintValidator<ValidPetExperience, PetExperienceInfoRequest> {

    @Override
    public boolean isValid(PetExperienceInfoRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        // hasPetExperience가 null인 경우 → null 여부는 @NotNull이 검증하므로 여기서는 검증하지 않음
        if (value.getHasPetExperience() == null) {
            return true;
        }

        if (value.getHasPetExperience()) {
            return value.getExperienceDetails() != null && !value.getExperienceDetails().trim().isEmpty();
        }

        return true;
    }
}
