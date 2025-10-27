package hello.pet.applicationservice.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {
    private Long id;
    private Long shelterId;
    private Long petId;
    private String breed;
    private String gender;
    private String health;
    private String personality;
    private int age;
    private String shelterName;
    private LocalDateTime createdAt;
    private LocalDateTime endDate;
    private String imageUrl;
    private String announcementStatus;
    private String animalType;
    private boolean alreadyApplied;
}
