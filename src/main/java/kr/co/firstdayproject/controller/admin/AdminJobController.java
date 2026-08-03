package kr.co.firstdayproject.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/job")
public class AdminJobController {

    @GetMapping({"", "/list"})
    public String list(Model model) {
        model.addAttribute("activeMenu", "job");
        return "admin/job/index";
    }

    @GetMapping("/{jobId}")
    public String detail(
        @PathVariable String jobId,
        Model model
    ) {
        boolean reviewRequested = "J-2077".equals(jobId);
        boolean closed = "J-2078".equals(jobId);
        boolean hidden = "J-2079".equals(jobId);
        String jobStatus = reviewRequested
            ? "재검토 요청"
            : closed
                ? "마감"
                : hidden ? "숨김" : "게시 중";

        model.addAttribute("activeMenu", "job");
        model.addAttribute("jobId", jobId);
        model.addAttribute("reviewRequested", reviewRequested);
        model.addAttribute("closed", closed);
        model.addAttribute("hidden", hidden);
        model.addAttribute(
            "canHide",
            !reviewRequested && !closed && !hidden
        );
        model.addAttribute("jobStatus", jobStatus);
        model.addAttribute(
            "companyName",
            reviewRequested
                ? "데이터포지"
                : hidden ? "브릿지웍스" : "코드웨이브"
        );
        model.addAttribute(
            "jobTitle",
            reviewRequested
                ? "AI 엔지니어"
                : hidden ? "서비스 기획자" : "백엔드 개발자 채용"
        );

        return "admin/job/detail";
    }
}
