package hello.pet.applicationservice.facade;

import feign.FeignException;
import hello.pet.applicationservice.client.PetServiceClient;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetServiceFacade {

    private final PetServiceClient petServiceClient;

    public void markAsAdopted(Long petId, Long userId, String userRole) {
        try {
            petServiceClient.markAsAdopted(petId, userId, userRole);
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException("해당 번호의 펫을 찾을 수 없습니다. id=" + petId);
        }
    }

    public void markAsAnnounced(Long petId, Long userId, String userRole) {
        try {
            petServiceClient.markAsAnnounced(petId, userId, userRole);
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException("해당 번호의 펫을 찾을 수 없습니다. id=" + petId);
        }
    }
}
