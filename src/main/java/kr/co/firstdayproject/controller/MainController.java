package kr.co.firstdayproject.controller;

import kr.co.firstdayproject.service.banner.BannerService;
import kr.co.firstdayproject.service.job.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController {

    public final BannerService bannerService;
    private final JobService jobService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("banners",
                bannerService.getActiveBanners());
        model.addAttribute("jobPostingList",
                jobService.getJobPostingList());

        return "index";
    }
}
