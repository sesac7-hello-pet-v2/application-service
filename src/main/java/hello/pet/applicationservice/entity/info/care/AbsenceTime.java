package hello.pet.applicationservice.entity.info.care;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AbsenceTime {

    ONE_TO_THREE("1~3시간"),
    FOUR_TO_SIX("4~6시간"),
    SEVEN_TO_NINE("7~9시간"),
    TEN_OR_MORE("10시간 이상");

    private final String label;
}
