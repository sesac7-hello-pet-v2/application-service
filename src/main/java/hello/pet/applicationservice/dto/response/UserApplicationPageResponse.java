package hello.pet.applicationservice.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Pageable;

@Getter
@Builder
public class UserApplicationPageResponse {
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
    private List<UserApplicationResponse> applications;

    public static UserApplicationPageResponse of(
            Pageable pageable
            , List<UserApplicationResponse> content
            , long totalElements
    ) {
        int totalPages = (int) Math.ceil((double) totalElements / (double) pageable.getPageSize());

        return UserApplicationPageResponse.builder()
                                          .page(pageable.getPageNumber())
                                          .size(pageable.getPageSize())
                                          .totalElements(totalElements)
                                          .totalPages(totalPages)
                                          .applications(content)
                                          .build();
    }
}
