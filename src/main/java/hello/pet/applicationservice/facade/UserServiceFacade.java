package hello.pet.applicationservice.facade;

import feign.FeignException;
import hello.pet.applicationservice.client.UserServiceClient;
import hello.pet.applicationservice.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceFacade {

    private final UserServiceClient userServiceClient;

    public UserResponse getUserDetail(Long userId) {
        try {
            return userServiceClient.getUserDetail(userId);
        } catch (FeignException.NotFound e) {
            log.warn("사용자를 찾을 수 없습니다. userId={}", userId);
            return null;
        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류 발생. userId={}", userId, e);
            return null;
        }
    }
}
