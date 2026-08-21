package kr.co.firstdayproject.controller.admin;

import java.io.IOException;
import java.util.Map;
import kr.co.firstdayproject.dto.banner.BannerDTO;
import kr.co.firstdayproject.security.AdminPrincipal;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.banner.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/banner")
public class AdminBannerController {
    private final BannerService bannerService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("banners", bannerService.getAdminBanners());
        model.addAttribute("activeMenu", "banner");
        return "admin/banner/index";
    }

    @GetMapping("/{bannerId}") @ResponseBody
    public ResponseEntity<?> detail(@PathVariable Long bannerId) {
        try { return ResponseEntity.ok(bannerService.getBanner(bannerId)); }
        catch (RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @PostMapping("/register") @ResponseBody
    public ResponseEntity<?> register(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      @ModelAttribute BannerDTO banner, @RequestParam MultipartFile bannerFile) {
        try {
            bannerService.register(banner, bannerFile, AdminPrincipal.requireAdminId(userDetails));
            return ResponseEntity.ok(Map.of("success", true, "message", "배너가 등록되었습니다."));
        } catch (IOException | RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage())); }
    }

    @PostMapping("/{bannerId}") @ResponseBody
    public ResponseEntity<?> update(@PathVariable Long bannerId, @ModelAttribute BannerDTO banner,
                                    @RequestParam(required = false) MultipartFile bannerFile) {
        try {
            bannerService.update(bannerId, banner, bannerFile);
            return ResponseEntity.ok(Map.of("success", true, "message", "배너가 수정되었습니다."));
        } catch (IOException | RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage())); }
    }

    @PostMapping("/{bannerId}/toggle") @ResponseBody
    public ResponseEntity<?> toggle(@PathVariable Long bannerId) {
        try {
            boolean active = bannerService.toggleActive(bannerId);
            return ResponseEntity.ok(Map.of("success", true, "active", active, "message", active ? "배너가 노출됩니다." : "배너가 숨김 처리되었습니다."));
        } catch (RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage())); }
    }
}
