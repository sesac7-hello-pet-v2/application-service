package hello.pet.applicationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "pet-service",
        url = "${PET_SERVICE_URL:http://localhost:8085}",
        path = "/v1/pets"
)
public interface PetServiceClient {

    @PatchMapping("/{petId}/mark-adopted")
    void markAsAdopted(@PathVariable("petId") Long petId,
                       @RequestHeader("X-User-Id") Long userId,
                       @RequestHeader("X-User-Role") String userRole);
}
