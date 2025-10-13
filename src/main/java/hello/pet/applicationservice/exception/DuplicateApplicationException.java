package hello.pet.applicationservice.exception;

public class DuplicateApplicationException extends RuntimeException {
    public DuplicateApplicationException() {
        super("해당 공고에 이미 신청하셨습니다.");
    }
}
