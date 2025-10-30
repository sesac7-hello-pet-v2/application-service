package hello.pet.applicationservice.dto.response;

public enum AnnouncementStatus {
    OPEN,           // 공고 중
    CLOSED,         // 마감됨 (신청 기간 종료)
    COMPLETED,      // 입양 완료
    DELETED         // 삭제됨
}