package hello.pet.applicationservice.dto.response.detail;

import hello.pet.applicationservice.entity.info.financial.FinancialInfo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FinancialInfoResponse {
    private String monthlyBudget;
    private String monthlyBudgetLabel;
    private boolean hasEmergencyFund;

    public static FinancialInfoResponse from(FinancialInfo financialInfo) {
        return FinancialInfoResponse.builder()
                                    .monthlyBudget(financialInfo.getMonthlyBudget().name())
                                    .monthlyBudgetLabel(financialInfo.getMonthlyBudget().getLabel())
                                    .hasEmergencyFund(financialInfo.isHasEmergencyFund())
                                    .build();
    }
}
