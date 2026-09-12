package hello.pet.applicationservice.saga.core;

/** Saga의 한 단계. T는 실행과 보상에 공유할 Context 타입이다. */
public interface SagaStep<T> {

    /** 정상 작업을 수행한다. 실패는 예외로 오케스트레이터에 전달한다. */
    void execute(T context) throws Exception;

    /** 이미 완료된 작업을 보상한다. DB 롤백 자체가 아니라 별도의 취소 작업이다. */
    void compensate(T context);

    /** 실패한 단계를 식별하기 위한 로그·예외 메시지용 이름. */
    String getName();
}
