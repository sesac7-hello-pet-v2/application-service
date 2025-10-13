package hello.pet.applicationservice.entity.info.housing;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HouseSizeRange {
    LESS_THAN_33("33㎡ 미만 (10평 미만)"),
    FROM_33_TO_66("33㎡ 이상 ~ 66㎡ 미만 (10평 이상 ~ 20평 미만)"),
    FROM_66_TO_99("66㎡ 이상 ~ 99㎡ 미만 (20평 이상 ~ 30평 미만)"),
    OVER_99("99㎡ 이상 (30평 이상)");

    private final String label;
}

