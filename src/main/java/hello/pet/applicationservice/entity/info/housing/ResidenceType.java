package hello.pet.applicationservice.entity.info.housing;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResidenceType {
    OWNED("자가"),
    JEONSE("전세"),
    MONTHLY_RENT("월세"),
    TEMPORARY("임시 거주");

    private final String label;
}
