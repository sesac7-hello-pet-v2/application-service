package hello.pet.applicationservice.entity.info.plan;

import hello.pet.applicationservice.dto.request.FuturePlanInfoRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FuturePlanInfo {

    @Column(nullable = false)
    private boolean hasFuturePlan;

    @Column(columnDefinition = "TEXT")
    private String planDetails;

    public static FuturePlanInfo from(FuturePlanInfoRequest request) {
        return new FuturePlanInfo(
                request.getHasFuturePlan(),
                request.getPlanDetails()
        );
    }
}
