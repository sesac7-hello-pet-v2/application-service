package hello.pet.applicationservice.dto.request;

import hello.pet.applicationservice.exception.ValidFuturePlan;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@ValidFuturePlan
public class FuturePlanInfoRequest {

    @NotNull(message = "향후 계획 보유 여부는 필수 입력 항목입니다.")
    private Boolean hasFuturePlan;

    private String planDetails;
}
