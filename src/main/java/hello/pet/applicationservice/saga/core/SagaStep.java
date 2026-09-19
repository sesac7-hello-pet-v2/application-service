package hello.pet.applicationservice.saga.core;

// Saga의 각 단계가 구현해야 하는 실행과 보상 메서드를 정의한다.
public interface SagaStep<T> {

    /**
     * T는 공유할 Context 타입이다. 입양 승인 Step에서는 AdoptionSagaContext를 사용한다.
     * 정상 작업을 수행한다. 실패는 예외로 오케스트레이터에 전달한다.
     */
    void execute(T context) throws Exception;

    /**
     * 이미 완료된 작업을 보상한다. DB 롤백 자체가 아니라 별도의 취소 작업이다.
     */
    void compensate(T context);

    /**
     * 실패한 단계를 식별하기 위한 로그·예외 메시지용 이름.
     */
    String getName();
}
