package kr.co.firstdayproject.controller.auth;

import kr.co.firstdayproject.dto.auth.BusinessNumberAvailabilityResponse;
import kr.co.firstdayproject.dto.auth.LoginIdAvailabilityResponse;
import kr.co.firstdayproject.service.auth.CorporateSignupService;
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
    private final CorporateSignupService corporateSignupService;

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

    @GetMapping("/business-number-availability")
    public BusinessNumberAvailabilityResponse businessNumberAvailability(
            @RequestParam String businessNumber
    ) {
        boolean available = corporateSignupService
                .isBusinessNumberAvailable(businessNumber);
        return new BusinessNumberAvailabilityResponse(
                available,
                available
                        ? "사용 가능한 사업자등록번호입니다."
                        : "올바르지 않거나 이미 등록된 사업자등록번호입니다."
        );
    }
}
