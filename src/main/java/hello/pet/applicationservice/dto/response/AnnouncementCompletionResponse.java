package hello.pet.applicationservice.dto.response;

/** 공고 서비스가 완료 처리 트랜잭션에서 반환한 보상 정보. */
public record AnnouncementCompletionResponse(boolean changed) {
}
