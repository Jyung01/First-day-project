package kr.co.firstdayproject.service.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailSenderService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String from
    ) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendVerificationCode(
            String recipient,
            String code,
            long expirationMinutes
    ) {
        send(
                recipient,
                "[첫출근] 회원가입 이메일 인증번호 안내",
                """
                        첫출근 회원가입 이메일 인증번호입니다.

                        인증번호: %s

                        인증번호는 %d분 동안 유효합니다.
                        본인이 요청하지 않았다면 이 메일을 무시해주세요.
                        """.formatted(code, expirationMinutes)
        );
    }

    public void sendPasswordResetCode(
            String recipient,
            String code,
            long expirationMinutes
    ) {
        send(
                recipient,
                "[첫출근] 비밀번호 재설정 인증번호 안내",
                """
                        첫출근 비밀번호 재설정을 위한 인증번호입니다.

                        인증번호: %s

                        인증번호는 %d분 동안 유효합니다.
                        비밀번호 재설정을 요청하지 않았다면 이 메일을 무시해주세요.
                        """.formatted(code, expirationMinutes)
        );
    }

    private void send(String recipient, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }
}
