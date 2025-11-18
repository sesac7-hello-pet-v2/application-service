package hello.pet.applicationservice.saga.core;

/**
 * 역할: 모든 Saga Step이 구현해야 할 인터페이스
 *
 * 메서드:
 * - execute(T context): 정상 비즈니스 로직 수행
 * - compensate(T context): 실패 시 보상 로직 수행
 * - getName(): Step 이름 반환 (로깅용)
 *
 * 참고:
 * - 제네릭 타입 <T>로 다양한 Context 타입 지원
 *
 * @param <T> Saga 실행 중 공유되는 Context 타입
 */
public interface SagaStep<T> {

    void execute(T context) throws Exception;

    void compensate(T context);

    String getName();
}
