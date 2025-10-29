package hello.pet.applicationservice.client;

import hello.pet.applicationservice.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        url = "${USER_SERVICE_URL:http://localhost:8082}",
        path = "/internal/v1/users"
)
public interface UserServiceClient {

    @GetMapping("/{userId}")
    UserResponse getUserDetail(@PathVariable("userId") Long userId);
}
