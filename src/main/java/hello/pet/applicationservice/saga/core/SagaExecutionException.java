package hello.pet.applicationservice.saga.core;

public class SagaExecutionException extends RuntimeException {

    /**
     * Saga 실행 단계에서 발생한 예외를 실패 원인으로 보관한다.
     */
    public SagaExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 실행 실패 후 보상 단계에서 추가로 발생한 예외가 기록되어 있는지 확인한다.
     */
    public boolean hasCompensationFailures() {
        // Java의 getSuppressed()로 addSuppressed()에 기록한 보상 예외가 있는지 확인한다.
        return getSuppressed().length > 0;
    }
}
