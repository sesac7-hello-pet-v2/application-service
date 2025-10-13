package hello.pet.applicationservice.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class AgreementInfoRequest {

    @NotNull(message = "사실에 근거하여 작성했음을 동의해야 합니다.")
    @AssertTrue(message = "사실에 근거하여 작성했음을 동의해야 합니다.")
    private Boolean agreedToAccuracy;

    @NotNull(message = "책임감 있는 돌봄 약속에 동의해야 합니다.")
    @AssertTrue(message = "책임감 있는 돌봄 약속에 동의해야 합니다.")
    private Boolean agreedToCare;

    @NotNull(message = "개인정보 수집 및 활용에 동의해야 합니다.")
    @AssertTrue(message = "개인정보 수집 및 활용에 동의해야 합니다.")
    private Boolean agreedToPrivacy;
}
