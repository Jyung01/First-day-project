package kr.co.firstdayproject.controller.admin;

import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.admin.config.AdminSiteSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final AdminSiteSettingService adminSiteSettingService;

    @GetMapping("/site-setting")
    public String siteSetting(Model model) {
        model.addAttribute("activeMenu", "siteSetting");
        model.addAttribute(
                "siteSetting",
                adminSiteSettingService.getSiteSettingView()
        );
        return "admin/config/site-setting";
    }

    @PostMapping("/site-setting/basic")
    public String updateBasicInfo(
            @RequestParam String serviceName,
            @RequestParam String supportEmail,
            @RequestParam String supportPhone,
            @RequestParam String serviceHours,
            @RequestParam(required = false) String serviceDescription,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminSiteSettingService.updateBasicInfo(
                    userDetails.getUserId(),
                    serviceName,
                    supportEmail,
                    supportPhone,
                    serviceHours,
                    serviceDescription
            );
            redirectAttributes.addFlashAttribute(
                    "configMessage",
                    "기본정보가 저장되었습니다."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "configError",
                    exception.getMessage()
            );
        }

        return "redirect:/admin/config/site-setting";
    }

    @PostMapping("/site-setting/footer")
    public String updateFooter(
            @RequestParam String companyName,
            @RequestParam String businessNumber,
            @RequestParam String companyAddress,
            @RequestParam(required = false) String copyrightText,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminSiteSettingService.updateFooter(
                    userDetails.getUserId(),
                    companyName,
                    businessNumber,
                    companyAddress,
                    copyrightText
            );
            redirectAttributes.addFlashAttribute(
                    "configMessage",
                    "푸터 설정이 저장되었습니다."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "configError",
                    exception.getMessage()
            );
        }

        return "redirect:/admin/config/site-setting";
    }

    @PostMapping("/site-setting/image")
    public String updateImage(
            @RequestParam String imageType,
            @RequestParam MultipartFile imageFile,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminSiteSettingService.updateImage(
                    userDetails.getUserId(),
                    imageType,
                    imageFile
            );
            redirectAttributes.addFlashAttribute(
                    "configMessage",
                    "이미지가 저장되었습니다."
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "configError",
                    exception.getMessage()
            );
        }

        return "redirect:/admin/config/site-setting";
    }

    @GetMapping("/version")
    public String version(Model model) {
        model.addAttribute("activeMenu", "version");
        return "admin/config/version";
    }

    // "/policy" 매핑은 AdminPolicyController(/admin/config/policy)로 이전했습니다.
    // 여기 남겨두면 두 컨트롤러가 같은 경로를 잡아 "Ambiguous handler methods" 500 에러가 납니다.
}