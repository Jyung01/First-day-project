package kr.co.firstdayproject.service.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailSenderServiceTest {

    @Test
    void sendsPasswordResetSpecificSubjectAndBody() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailSenderService service = new EmailSenderService(
                mailSender,
                "no-reply@firstday.test"
        );

        service.sendPasswordResetCode(
                "user@example.com",
                "123456",
                5
        );

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getSubject()).contains("비밀번호 재설정");
        assertThat(message.getText())
                .contains("비밀번호 재설정을 위한 인증번호")
                .contains("123456")
                .doesNotContain("회원가입");
    }
}
