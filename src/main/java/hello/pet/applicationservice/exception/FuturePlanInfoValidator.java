package hello.pet.applicationservice.exception;

import hello.pet.applicationservice.dto.request.FuturePlanInfoRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FuturePlanInfoValidator implements
        ConstraintValidator<ValidFuturePlan, FuturePlanInfoRequest> {

    @Override
    public boolean isValid(FuturePlanInfoRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        // hasFuturePlan이 null인 경우 → null 여부는 @NotNull이 검증하므로 여기서는 검증하지 않음
        if (value.getHasFuturePlan() == null) {
            return true;
        }

        if (value.getHasFuturePlan()) {
            return value.getPlanDetails() != null && !value.getPlanDetails().trim().isEmpty();
        }

        return true;
    }
}
