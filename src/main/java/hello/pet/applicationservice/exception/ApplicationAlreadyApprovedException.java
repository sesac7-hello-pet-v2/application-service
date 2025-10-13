package hello.pet.applicationservice.exception;

public class ApplicationAlreadyApprovedException extends RuntimeException {
    public ApplicationAlreadyApprovedException(Long id) {
        super("신청서(id=" + id + ")는 이미 승인되어 삭제할 수 없습니다.");
    }
}
