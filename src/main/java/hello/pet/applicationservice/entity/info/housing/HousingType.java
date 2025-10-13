package hello.pet.applicationservice.entity.info.housing;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HousingType {
    APARTMENT("아파트"),
    VILLA("빌라 / 연립"),
    OFFICETEL("오피스텔"),
    DETACHED_HOUSE("단독주택"),
    DORMITORY("기숙사 / 쉐어하우스"),
    MOBILE_HOME("이동식 주택 (컨테이너, 캠핑카 등)");

    private final String label;
}
