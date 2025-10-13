package hello.pet.applicationservice.dto.response.detail;

import hello.pet.applicationservice.entity.info.plan.FuturePlanInfo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FuturePlanInfoResponse {
    private boolean hasFuturePlan;
    private String planDetails;

    public static FuturePlanInfoResponse from(FuturePlanInfo futurePlanInfo) {
        return FuturePlanInfoResponse.builder()
                                     .hasFuturePlan(futurePlanInfo.isHasFuturePlan())
                                     .planDetails(futurePlanInfo.getPlanDetails())
                                     .build();
    }
}
