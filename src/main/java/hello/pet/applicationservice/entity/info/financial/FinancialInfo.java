package hello.pet.applicationservice.entity.info.financial;

import hello.pet.applicationservice.dto.request.FinancialInfoRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FinancialInfo {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonthlyBudget monthlyBudget;

    @Column(nullable = false)
    private boolean hasEmergencyFund;

    public static FinancialInfo from(FinancialInfoRequest request) {
        return new FinancialInfo(
                request.getMonthlyBudget(),
                request.getHasEmergencyFund()
        );
    }
}
