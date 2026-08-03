package kr.co.firstdayproject.controller.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @GetMapping("/login")
    public String login() { return "auth/login"; }

    @GetMapping("/member-type")
    public String memberType() { return "auth/member-type"; }

    @GetMapping("/personal-terms")
    public String personalTerms() { return "auth/personal-terms"; }

    @GetMapping("/personal-signup")
    public String personalSignup() { return "auth/personal-signup"; }

    @GetMapping("/personal-complete")
    public String personalComplete() { return "auth/personal-complete"; }

    @GetMapping("/corporate-terms")
    public String corporateTerms() { return "auth/corporate-terms"; }

    @GetMapping("/corporate-signup")
    public String corporateSignup() { return "auth/corporate-signup"; }

    @GetMapping("/corporate-complete")
    public String corporateComplete() { return "auth/corporate-complete"; }

    @GetMapping("/find-id")
    public String findId() { return "auth/find-id"; }

    @GetMapping("/reset-password")
    public String resetPassword() { return "auth/reset-password"; }
}
