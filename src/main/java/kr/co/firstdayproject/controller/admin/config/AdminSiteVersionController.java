package kr.co.firstdayproject.controller.admin.config;

import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.admin.config.AdminSiteVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/config/version")
public class AdminSiteVersionController {

    private final AdminSiteVersionService adminSiteVersionService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        model.addAttribute("activeMenu", "version");
        model.addAttribute("versionPage", adminSiteVersionService.getVersions(page));
        return "admin/config/version";
    }

    @PostMapping
    public String create(
            @AuthenticationPrincipal CustomUserDetails admin,
            @RequestParam String versionName,
            @RequestParam String changeNotes,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminSiteVersionService.create(
                    admin == null ? null : admin.getUserId(),
                    versionName,
                    changeNotes
            );
            redirectAttributes.addFlashAttribute(
                    "versionMessage",
                    "사이트 버전이 등록되었습니다."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("versionError", exception.getMessage());
        }

        return "redirect:/admin/config/version";
    }

    @PostMapping("/{siteVersionId}")
    public String update(
            @PathVariable Long siteVersionId,
            @RequestParam String versionName,
            @RequestParam String changeNotes,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminSiteVersionService.update(siteVersionId, versionName, changeNotes);
            redirectAttributes.addFlashAttribute(
                    "versionMessage",
                    "사이트 버전이 수정되었습니다."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("versionError", exception.getMessage());
        }

        return "redirect:/admin/config/version";
    }
}
