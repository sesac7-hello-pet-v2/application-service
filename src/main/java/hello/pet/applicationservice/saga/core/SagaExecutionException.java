package hello.pet.applicationservice.saga.core;

/** getCause()는 원래 작업 실패, getSuppressed()는 단계 이름을 포함한 보상 실패 목록이다. */
public class SagaExecutionException extends RuntimeException {

    public SagaExecutionException(String message) {
        super(message);
    }

    public SagaExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    /** 보상할 단계가 없거나 모든 보상이 성공했다면 false. Saga 자체의 성공 여부는 아니다. */
    public boolean hasCompensationFailures() {
        return getSuppressed().length > 0;
    }
}
