package kr.co.firstdayproject.service.corp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApplicationStatusTransitionPolicyTest {

    @Test
    void 현재_단계의_다음_상태와_불합격만_허용한다() {
        assertThat(ApplicationStatusTransitionPolicy
                .getAllowedNextStatuses("서류검토중"))
                .containsExactly("서류합격", "불합격");
        assertThat(ApplicationStatusTransitionPolicy.canTransition(
                "서류검토중",
                "면접예정"
        )).isFalse();
    }

    @Test
    void 종료_상태에서는_상태를_변경할_수_없다() {
        assertThat(ApplicationStatusTransitionPolicy
                .getAllowedNextStatuses("입사완료"))
                .isEmpty();
        assertThat(ApplicationStatusTransitionPolicy
                .getAllowedNextStatuses("불합격"))
                .isEmpty();
        assertThat(ApplicationStatusTransitionPolicy
                .getAllowedNextStatuses("지원취소"))
                .isEmpty();
        assertThat(ApplicationStatusTransitionPolicy
                .getAllowedNextStatuses("채용종료"))
                .isEmpty();
    }
}
