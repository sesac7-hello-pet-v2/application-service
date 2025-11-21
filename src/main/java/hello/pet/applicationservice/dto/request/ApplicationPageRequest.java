package hello.pet.applicationservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class ApplicationPageRequest {

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 10;

    // 점수 필터링
    @Min(0)
    @Max(100)
    private Integer minScore;

    // 정렬 옵션
    private String orderBy = "createdAt"; // createdAt(기본값) 또는 score

    public Pageable toPageable() {
        if ("score".equals(orderBy)) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "score.totalScore", "createdAt"));
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
