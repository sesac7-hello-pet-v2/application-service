package hello.pet.applicationservice.entity.info.financial;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MonthlyBudget {

    UNDER_50K("5만 원 이하"),
    FROM_50K_TO_100K("5만 원 이상 ~ 10만 원 이하"),
    FROM_100K_TO_200K("10만 원 이상 ~ 20만 원 이하"),
    OVER_200K("20만 원 이상");

    private final String label;
}
