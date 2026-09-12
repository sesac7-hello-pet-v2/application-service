package hello.pet.applicationservice.saga.core;

/** cause는 원래 단계 실패, suppressed 예외는 단계 이름을 포함한 보상 실패를 담는다. */
public class SagaExecutionException extends RuntimeException {

    public SagaExecutionException(String message) {
        super(message);
    }

    public SagaExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    /** 보상할 단계가 없거나 모든 보상이 성공한 경우 false를 반환한다. */
    public boolean hasCompensationFailures() {
        return getSuppressed().length > 0;
    }
}
