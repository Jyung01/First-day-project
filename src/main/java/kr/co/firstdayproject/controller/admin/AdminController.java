package kr.co.firstdayproject.controller.admin;

import kr.co.firstdayproject.dto.banner.BannerDTO;
import kr.co.firstdayproject.service.AwsS3.AwsS3Service;
import kr.co.firstdayproject.service.banner.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final AwsS3Service s3UploadService;
    private final BannerService bannerService;

    @GetMapping({"", "/index"})
    public String index(Model model) {
        model.addAttribute("activeMenu", "dashboard");
        return "admin/index";
    }

    @GetMapping("/banner")
    public String banner(Model model) {
        model.addAttribute("activeMenu", "banner");
        return "admin/banner/index";
    }

    // 배너 등록
    @PostMapping("/banner/register")
    public ResponseEntity<Void> register(
            @ModelAttribute BannerDTO bannerDTO,
            @RequestParam MultipartFile bannerFile) throws IOException {

        Long userId = 1L; // 로그인 구현 전 임시

        bannerService.register(bannerDTO, bannerFile, userId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/review")
    public String review(Model model) {
        model.addAttribute("activeMenu", "review");
        return "admin/review/index";
    }

    @GetMapping("/report")
    public String report(Model model) {
        model.addAttribute("activeMenu", "report");
        return "admin/report/index";
    }

    // salary 메서드 삭제됨 -> AdminSalaryController 가 전담
    // notice 메서드 삭제됨 -> AdminNoticeController 가 전담

    @GetMapping("/cs/faq")
    public String faq(Model model) {
        model.addAttribute("activeMenu", "cs");
        return "admin/cs/faq";
    }

}