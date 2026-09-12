package hello.pet.applicationservice.saga;

import hello.pet.applicationservice.saga.core.SagaExecutionException;
import hello.pet.applicationservice.saga.core.SagaOrchestrator;
import hello.pet.applicationservice.saga.core.SagaStep;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(OutputCaptureExtension.class)
class SagaOrchestratorTest {
    private final SagaOrchestrator orchestrator = new SagaOrchestrator();
    private final List<String> trace = new ArrayList<>();

    @Test
    void successfulStepsDoNotCompensate() {
        orchestrator.execute(List.of(step("A"), step("B")), trace);
        assertThat(trace).containsExactly("execute:A", "execute:B");
    }

    @Test
    void successfulCompensationKeepsOriginalCause(CapturedOutput output) {
        RuntimeException original = new IllegalArgumentException("업무 실패");
        SagaExecutionException failure = assertThrows(SagaExecutionException.class,
                () -> orchestrator.execute(List.of(step("A"), step("B"),
                        new TestStep("C", original, null), step("D")), trace));
        assertThat(trace).containsExactly("execute:A", "execute:B", "execute:C", "compensate:B", "compensate:A");
        assertThat(failure.getCause()).isSameAs(original);
        assertThat(failure.hasCompensationFailures()).isFalse();
        assertThat(output.getOut()).contains("보상 완료").doesNotContain("보상 미완료");
    }

    @Test
    void compensationFailureDoesNotStopRemainingCompensations(CapturedOutput output) {
        RuntimeException original = new IllegalArgumentException("업무 실패");
        RuntimeException compensation = new IllegalStateException("공고 복원 실패");
        SagaExecutionException failure = assertThrows(SagaExecutionException.class,
                () -> orchestrator.execute(List.of(step("A"), new TestStep("B", null, compensation),
                        new TestStep("C", original, null)), trace));
        assertThat(trace).containsExactly("execute:A", "execute:B", "execute:C", "compensate:B", "compensate:A");
        assertThat(failure.getCause()).isSameAs(original);
        assertThat(failure.hasCompensationFailures()).isTrue();
        assertThat(failure.getSuppressed()).hasSize(1);
        assertThat(failure.getSuppressed()[0]).hasMessageContaining("B").hasCause(compensation);
        assertThat(output.getOut()).contains("보상 미완료").doesNotContain("========== 보상 완료");
    }

    @Test
    void retainsEveryCompensationFailureInReverseOrder() {
        RuntimeException first = new IllegalStateException("A 실패");
        RuntimeException second = new IllegalStateException("B 실패");
        RuntimeException original = new IllegalArgumentException("업무 실패");
        SagaExecutionException failure = assertThrows(SagaExecutionException.class,
                () -> orchestrator.execute(List.of(new TestStep("A", null, first),
                        new TestStep("B", null, second), new TestStep("C", original, null)), trace));
        assertThat(failure.getCause()).isSameAs(original);
        assertThat(failure.getSuppressed()).hasSize(2);
        assertThat(failure.getSuppressed()[0]).hasMessageContaining("B").hasCause(second);
        assertThat(failure.getSuppressed()[1]).hasMessageContaining("A").hasCause(first);
    }

    @Test
    void firstStepFailureHasNothingToCompensate(CapturedOutput output) {
        RuntimeException original = new IllegalStateException("첫 단계 실패");
        SagaExecutionException failure = assertThrows(SagaExecutionException.class,
                () -> orchestrator.execute(List.of(new TestStep("A", original, null), step("B")), trace));
        assertThat(trace).containsExactly("execute:A");
        assertThat(failure.getCause()).isSameAs(original);
        assertThat(failure.hasCompensationFailures()).isFalse();
        assertThat(output.getOut()).contains("보상할 Step이 없습니다").doesNotContain("========== 보상 완료");
    }

    private TestStep step(String name) {
        return new TestStep(name, null, null);
    }

    private record TestStep(String name, RuntimeException executionFailure,
                            RuntimeException compensationFailure) implements SagaStep<List<String>> {
        @Override
        public void execute(List<String> context) {
            context.add("execute:" + name);
            if (executionFailure != null) throw executionFailure;
        }

        @Override
        public void compensate(List<String> context) {
            context.add("compensate:" + name);
            if (compensationFailure != null) throw compensationFailure;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
