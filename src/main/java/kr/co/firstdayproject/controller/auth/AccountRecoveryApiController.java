package kr.co.firstdayproject.controller.auth;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.co.firstdayproject.dto.auth.AccountRecoveryResponse;
import kr.co.firstdayproject.dto.auth.FindIdRequest;
import kr.co.firstdayproject.dto.auth.FindIdResponse;
import kr.co.firstdayproject.dto.auth.PasswordResetCodeRequest;
import kr.co.firstdayproject.dto.auth.PasswordResetRequest;
import kr.co.firstdayproject.dto.auth.VerifiedEmail;
import kr.co.firstdayproject.service.auth.AccountRecoveryException;
import kr.co.firstdayproject.service.auth.AccountRecoveryService;
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
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AccountRecoveryApiController {

    private final AccountRecoveryService accountRecoveryService;

    @PostMapping("/find-id")
    public FindIdResponse findId(
            @Valid @RequestBody FindIdRequest request
    ) {
        return accountRecoveryService.findId(request.name(), request.email());
    }

    @PostMapping("/password-reset/code")
    public AccountRecoveryResponse sendPasswordResetCode(
            @Valid @RequestBody PasswordResetCodeRequest request,
            HttpSession session
    ) {
        accountRecoveryService.sendPasswordResetCode(
                request.loginId(),
                request.email(),
                session
        );
        return new AccountRecoveryResponse(
                true,
                "인증번호를 발송했습니다. 이메일을 확인해주세요."
        );
    }

    @PostMapping("/password-reset")
    public AccountRecoveryResponse resetPassword(
            @Valid @RequestBody PasswordResetRequest request,
            HttpSession session
    ) {
        VerifiedEmail verifiedEmail = session.getAttribute(
                EmailVerificationService.VERIFIED_EMAIL_SESSION_KEY
        ) instanceof VerifiedEmail state ? state : null;

        accountRecoveryService.resetPassword(request, verifiedEmail);
        session.removeAttribute(EmailVerificationService.VERIFIED_EMAIL_SESSION_KEY);
        return new AccountRecoveryResponse(true, "비밀번호가 변경되었습니다.");
    }

    @ExceptionHandler(AccountRecoveryException.class)
    public ResponseEntity<AccountRecoveryResponse> handleRecoveryException(
            AccountRecoveryException exception
    ) {
        return ResponseEntity.status(exception.getStatus()).body(
                new AccountRecoveryResponse(false, exception.getMessage())
        );
    }

    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<AccountRecoveryResponse> handleEmailException(
            EmailVerificationException exception
    ) {
        return ResponseEntity.status(exception.getStatus()).body(
                new AccountRecoveryResponse(false, exception.getMessage())
        );
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<AccountRecoveryResponse> handleValidationException(
            BindException exception
    ) {
        String message = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("요청값을 확인해주세요.");
        return ResponseEntity.badRequest().body(
                new AccountRecoveryResponse(false, message)
        );
    }
}
