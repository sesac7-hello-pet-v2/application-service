package hello.pet.applicationservice.dto.request;

import hello.pet.applicationservice.entity.info.financial.MonthlyBudget;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class FinancialInfoRequest {

    @NotNull(message = "반려 동물을 위한 월 예산은 필수 입력 항목입니다.")
    private MonthlyBudget monthlyBudget;

    @NotNull(message = "반려 동물을 위한 비상금 보유 여부는 필수 입력 항목입니다.")
    private Boolean hasEmergencyFund;
}
