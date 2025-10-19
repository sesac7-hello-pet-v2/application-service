package hello.pet.applicationservice.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Pageable;

@Getter
@Builder
public class AnnouncementApplicationsPageResponse {
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
    private Long announcementId;
    private LocalDateTime announcementCreatedAt;
    private List<AnnouncementApplicationResponse> applications;

    public static AnnouncementApplicationsPageResponse of(Pageable pageable
            , List<AnnouncementApplicationResponse> content
            , long totalElements
            , AnnouncementResponse announcement) {

        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        return AnnouncementApplicationsPageResponse.builder()
                                              .page(pageable.getPageNumber())
                                              .size(pageable.getPageSize())
                                              .totalElements(totalElements)
                                              .totalPages(totalPages)
                                              .announcementId(announcement.getId())
                                              .announcementCreatedAt(announcement.getCreatedAt())
                                              .applications(content)
                                              .build();
    }
}
