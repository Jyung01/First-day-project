package kr.co.firstdayproject.controller.my;

import kr.co.firstdayproject.entity.member.PersonalProfile;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.my.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "kr.co.firstdayproject.controller.my")
@RequiredArgsConstructor
public class MySidebarAdvice {

    private static final String PERSONAL_ROLE = "ROLE_PERSONAL";

    private final MyPageService myPageService;

    @ModelAttribute
    public void addMySidebarProfile(
            Authentication authentication,
            Model model
    ) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)
                || authentication.getAuthorities().stream()
                        .noneMatch(authority -> PERSONAL_ROLE.equals(authority.getAuthority()))) {
            return;
        }

        User user = myPageService.getUser(userDetails.getUserId());
        PersonalProfile profile = myPageService.getProfile(userDetails.getUserId());

        model.addAttribute("personalProfile", profile);
        model.addAttribute("mySidebarProfileImageUrl", myPageService.getProfileImageDisplayUrl(profile));
        model.addAttribute("mySidebarName", user.getName());
    }
}
