package kr.co.firstdayproject.controller.auth;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.co.firstdayproject.dto.auth.EmailVerificationConfirmRequest;
import kr.co.firstdayproject.dto.auth.EmailVerificationResponse;
import kr.co.firstdayproject.dto.auth.EmailVerificationSendRequest;
import kr.co.firstdayproject.service.auth.EmailVerificationException;
import kr.co.firstdayproject.service.auth.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email-verifications")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/send")
    public EmailVerificationResponse send(
            @Valid @RequestBody EmailVerificationSendRequest request,
            HttpSession session
    ) {
        emailVerificationService.sendVerificationCode(request.email(), session);
        return new EmailVerificationResponse(
                true,
                "인증번호를 발송했습니다. 이메일을 확인해주세요."
        );
    }

    @PostMapping("/verify")
    public EmailVerificationResponse verify(
            @Valid @RequestBody EmailVerificationConfirmRequest request,
            HttpSession session
    ) {
        emailVerificationService.verifyCode(
                request.email(),
                request.code(),
                session
        );
        return new EmailVerificationResponse(
                true,
                "이메일 인증이 완료되었습니다."
        );
    }

    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<EmailVerificationResponse> handleVerificationException(
            EmailVerificationException exception
    ) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(new EmailVerificationResponse(false, exception.getMessage()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<EmailVerificationResponse> handleValidationException(
            BindException exception
    ) {
        String message = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("요청값을 확인해주세요.");

        return ResponseEntity.badRequest()
                .body(new EmailVerificationResponse(false, message));
    }
}
