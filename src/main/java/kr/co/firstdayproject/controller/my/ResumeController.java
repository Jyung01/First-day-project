package kr.co.firstdayproject.controller.my;

import java.util.List;
import kr.co.firstdayproject.dto.my.ResumeDto;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.entity.resume.Resume;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.my.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/my/resume")
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping({"", "/list"})
    public String list(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("activeMenu", "resumes");
        model.addAttribute("resumes", resumeService.findMyList(userDetails.getUserId()));
        return "my/resume/list";
    }

    @GetMapping("/detail")
    public String detail(
            @RequestParam(required = false) Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        if (id == null) {
            return "redirect:/my/resume/list";
        }
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        Long userId = userDetails.getUserId();
        Resume resume = resumeService.getMine(id, userId);

        model.addAttribute("activeMenu", "resumes");
        model.addAttribute("resume", resume);
        model.addAttribute("careers", resumeService.getCareers(id));
        model.addAttribute("educations", resumeService.getEducations(id));
        model.addAttribute("projects", resumeService.getProjects(id));
        model.addAttribute("skills", resumeService.getSkillNames(id));
        return "my/resume/detail";
    }

    @GetMapping("/form")
    public String form(
            @RequestParam(required = false) Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        Long userId = userDetails.getUserId();
        boolean editMode = id != null;

        ResumeDto.FormRequest formRequest = editMode
                ? resumeService.getFormData(id, userId)
                : resumeService.getNewFormData();
        List<ResumeDto.SkillChip> skillChips = editMode
                ? resumeService.getSkillChips(id)
                : List.of();
        User applicant = resumeService.getApplicant(userId);

        model.addAttribute("activeMenu", "resumes");
        model.addAttribute("editMode", editMode);
        model.addAttribute("resumeForm", formRequest);
        model.addAttribute("skillChips", skillChips);
        model.addAttribute("activeSkills", resumeService.getActiveSkills());
        model.addAttribute("memberName", applicant.getName());
        model.addAttribute("email", applicant.getEmail());
        model.addAttribute("phone", applicant.getPhone());
        return "my/resume/form";
    }

    @PostMapping("/form")
    public String save(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute ResumeDto.FormRequest resumeForm,
            @RequestParam(defaultValue = "save") String submitMode
    ) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        Long resumeId = resumeService.save(userDetails.getUserId(), resumeForm);

        if ("preview".equals(submitMode)) {
            return "redirect:/my/resume/detail?id=" + resumeId;
        }
        return "redirect:/my/resume/list";
    }
}
