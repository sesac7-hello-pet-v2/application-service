package hello.pet.applicationservice.dto.response;

/**
 * 공고 완료 요청의 처리 결과.
 *
 * @param changed 이번 요청에서 공고 상태를 변경했으면 true, 이미 완료된 상태였다면 false
 */
public record AnnouncementCompletionResponse(boolean changed) {
}
