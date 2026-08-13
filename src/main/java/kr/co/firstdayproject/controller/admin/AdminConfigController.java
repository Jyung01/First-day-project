package kr.co.firstdayproject.controller.admin;

import jakarta.servlet.http.HttpSession;
import kr.co.firstdayproject.service.admin.config.AdminSiteSettingService;
import lombok.RequiredArgsConstructor;
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

    // TODO: 실제 로그인 세션에서 관리자 ID를 담는 속성명으로 교체 필요.
    // 예: session.getAttribute("loginAdmin") 형태로 User/Admin 객체를 담고 있다면
    //     ((User) session.getAttribute("loginAdmin")).getUserId() 로 꺼내야 함.
    private static final String SESSION_ADMIN_ID_KEY = "loginAdminId";

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
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminSiteSettingService.updateBasicInfo(
                    getAdminUserId(session),
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
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminSiteSettingService.updateFooter(
                    getAdminUserId(session),
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
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminSiteSettingService.updateImage(
                    getAdminUserId(session),
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

    private Long getAdminUserId(HttpSession session) {
        Object adminId = session.getAttribute(SESSION_ADMIN_ID_KEY);
        return adminId == null ? null : (Long) adminId;
    }
}
