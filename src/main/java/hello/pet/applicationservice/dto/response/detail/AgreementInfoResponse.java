package hello.pet.applicationservice.dto.response.detail;

import hello.pet.applicationservice.entity.info.agreement.AgreementInfo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgreementInfoResponse {
    private boolean agreedToAccuracy;
    private boolean agreedToCare;
    private boolean agreedToPrivacy;

    public static AgreementInfoResponse from(AgreementInfo agreementInfo) {
        return AgreementInfoResponse.builder()
                                    .agreedToAccuracy(agreementInfo.getAgreedToAccuracy())
                                    .agreedToCare(agreementInfo.getAgreedToCare())
                                    .agreedToPrivacy(agreementInfo.getAgreedToPrivacy())
                                    .build();
    }
}
