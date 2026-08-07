package kr.co.firstdayproject.controller.my;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.co.firstdayproject.dto.my.MyAccountActionResponse;
import kr.co.firstdayproject.dto.my.MyPasswordChangeRequest;
import kr.co.firstdayproject.dto.my.MyWithdrawalRequest;
import kr.co.firstdayproject.dto.my.ProfileEditRequest;
import kr.co.firstdayproject.entity.member.PersonalProfile;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.job.JobService;
import kr.co.firstdayproject.service.my.MyAccountException;
import kr.co.firstdayproject.service.my.MyPageException;
import kr.co.firstdayproject.service.my.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/my")
@RequiredArgsConstructor
public class MyPageController {

    private final JobService jobService;
    private final MyPageService myPageService;

    @GetMapping({"", "/index"})
    public String index(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        Long userId = userDetails.getUserId();
        model.addAttribute("activeMenu", "home");
        model.addAttribute("dashboardStats", myPageService.getDashboardStats(userId));
        model.addAttribute("recentApplications", myPageService.getRecentApplications(userId));
        myPageService.getRecentResume(userId)
                .ifPresent(resume -> model.addAttribute("recentResume", resume));
        myPageService.getRecentCoverLetter(userId)
                .ifPresent(coverLetter -> model.addAttribute("recentCoverLetter", coverLetter));
        return "my/index";
    }

    @GetMapping("/profile-edit")
    public String profileEdit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        User user = myPageService.getUser(userDetails.getUserId());
        PersonalProfile profile = resolveProfile(model, userDetails.getUserId());

        model.addAttribute("profileForm", ProfileEditRequest.from(user, profile));
        addProfileEditModel(model, userDetails.getUserId(), user, profile);
        return "my/profile-edit";
    }

    @PostMapping("/profile-edit")
    public String updateProfileEdit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute("profileForm") ProfileEditRequest request,
            BindingResult bindingResult,
            @RequestParam(name = "profileImage", required = false)
            MultipartFile profileImage,
            Model model
    ) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        if (!bindingResult.hasErrors()) {
            try {
                myPageService.updateProfile(userDetails.getUserId(), request, profileImage);
                return "redirect:/my/profile-edit?saved=true";
            } catch (MyPageException exception) {
                if (exception.getField() != null && !"profileImage".equals(exception.getField())) {
                    bindingResult.rejectValue(
                            exception.getField(),
                            "profile.edit",
                            exception.getMessage()
                    );
                } else {
                    bindingResult.reject("profile.edit", exception.getMessage());
                }
            }
        }

        User user = myPageService.getUser(userDetails.getUserId());
        PersonalProfile profile = resolveProfile(model, userDetails.getUserId());
        addProfileEditModel(model, userDetails.getUserId(), user, profile);
        return "my/profile-edit";
    }

    /**
     * {@link MySidebarAdvice}가 이미 같은 요청에서 조회해둔 PersonalProfile이 있으면 재사용하고,
     * 없으면(예: 사이드바 대상이 아닌 경우) 새로 조회합니다.
     */
    private PersonalProfile resolveProfile(Model model, Long userId) {
        if (model.getAttribute("personalProfile") instanceof PersonalProfile cachedProfile) {
            return cachedProfile;
        }
        return myPageService.getProfile(userId);
    }

    private void addProfileEditModel(
            Model model,
            Long userId,
            User user,
            PersonalProfile profile
    ) {
        model.addAttribute("activeMenu", "edit");
        model.addAttribute("email", user.getEmail());
        model.addAttribute("profileImageUrl", resolveProfileImageUrl(model, profile));
        model.addAttribute("desiredJobs", myPageService.getDesiredJobs(userId));
        model.addAttribute(
                "jobCategoryGroups",
                jobService.getActiveJobCategoryGroups()
        );
    }

    /**
     * {@link MySidebarAdvice}가 이미 같은 요청에서 발급해둔 presigned URL이 있으면 재사용해서,
     * S3Presigner 서명 계산과 DB 조회가 요청당 두 번씩 일어나지 않도록 합니다.
     */
    private String resolveProfileImageUrl(Model model, PersonalProfile profile) {
        if (model.containsAttribute("personalProfile")) {
            return (String) model.getAttribute("mySidebarProfileImageUrl");
        }
        return myPageService.getProfileImageDisplayUrl(profile);
    }

    @PostMapping("/password-change")
    @ResponseBody
    public ResponseEntity<MyAccountActionResponse> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody MyPasswordChangeRequest request
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    MyAccountActionResponse.failure(
                            "로그인이 만료되었습니다. 다시 로그인해주세요."
                    )
            );
        }
        try {
            myPageService.changePassword(userDetails.getUserId(), request);
            return ResponseEntity.ok(MyAccountActionResponse.success(
                    "비밀번호가 변경되었습니다."
            ));
        } catch (MyAccountException exception) {
            return ResponseEntity.badRequest().body(
                    MyAccountActionResponse.failure(exception.getMessage())
            );
        }
    }

    @PostMapping("/withdraw")
    @ResponseBody
    public ResponseEntity<MyAccountActionResponse> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody MyWithdrawalRequest request,
            HttpServletRequest httpRequest
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    MyAccountActionResponse.failure(
                            "로그인이 만료되었습니다. 다시 로그인해주세요."
                    )
            );
        }
        try {
            myPageService.withdraw(userDetails.getUserId(), request.currentPassword());
        } catch (MyAccountException exception) {
            return ResponseEntity.badRequest().body(
                    MyAccountActionResponse.failure(exception.getMessage())
            );
        }

        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(MyAccountActionResponse.success(
                "회원 탈퇴가 완료되었습니다.",
                "/auth/login?accountStatus=withdrawn"
        ));
    }

    @GetMapping("/applications")
    public String applications(Model model) {
        model.addAttribute("activeMenu", "applications");
        return "my/applications";
    }

    @GetMapping("/applications/{applicationNo}")
    public String applicationDetail(
            @PathVariable Long applicationNo,
            @RequestParam(defaultValue = "APPLIED") String status,
            Model model
    ) {
        model.addAttribute("applicationNo", applicationNo);
        model.addAttribute("applicationStatus", status.toUpperCase());
        model.addAttribute("activeMenu", "applications");
        return "my/application-detail";
    }

    @GetMapping("/saved-jobs")
    public String savedJobs(Model model) {
        model.addAttribute("activeMenu", "interestJobs");
        return "my/saved-jobs";
    }

    @GetMapping("/saved-companies")
    public String savedCompanies(Model model) {
        model.addAttribute("activeMenu", "interestCompanies");
        return "my/saved-companies";
    }

    @GetMapping("/reviews")
    public String reviews(Model model) {
        model.addAttribute("activeMenu", "reviews");
        return "my/reviews";
    }
}
