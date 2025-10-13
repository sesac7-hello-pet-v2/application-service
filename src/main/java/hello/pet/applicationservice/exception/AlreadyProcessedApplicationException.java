package hello.pet.applicationservice.exception;

public class AlreadyProcessedApplicationException extends RuntimeException {
    public AlreadyProcessedApplicationException() {
        super("이미 처리 완료된 신청서입니다. 다시 승인할 수 없습니다.");
    }
}
