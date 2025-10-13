package hello.pet.applicationservice.entity.info.care;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CareTime {

    ONE_OR_LESS("1시간 이하"),
    TWO_TO_THREE("2~3시간"),
    FOUR_OR_MORE("4시간 이상");

    private final String label;
}
