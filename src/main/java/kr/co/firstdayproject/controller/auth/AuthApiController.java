package kr.co.firstdayproject.controller.auth;

import kr.co.firstdayproject.dto.auth.LoginIdAvailabilityResponse;
import kr.co.firstdayproject.service.auth.PersonalSignupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final PersonalSignupService personalSignupService;

    @GetMapping("/login-id-availability")
    public LoginIdAvailabilityResponse loginIdAvailability(
            @RequestParam String loginId
    ) {
        boolean available = personalSignupService.isLoginIdAvailable(loginId);
        return new LoginIdAvailabilityResponse(
                available,
                available
                        ? "사용 가능한 아이디입니다."
                        : "사용할 수 없거나 이미 사용 중인 아이디입니다."
        );
    }
}
