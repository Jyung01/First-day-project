package kr.co.firstdayproject.controller.salary;

import kr.co.firstdayproject.dto.salary.SalaryRecordsDTO;
import kr.co.firstdayproject.service.salary.SalaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import kr.co.firstdayproject.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import kr.co.firstdayproject.util.PageHandler;

@Controller
@RequiredArgsConstructor
@RequestMapping("/salary")
public class SalaryController {

    private final SalaryService salaryService;

    @GetMapping({"", "/list"})
    public String list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "companySize", required = false) String companySize,
                       @RequestParam(value = "industry", required = false) String industry,
                       @RequestParam(value = "career", required = false) String career,
                       @RequestParam(value = "sort", defaultValue = "salary") String sort,
                       @RequestParam(value = "page", defaultValue = "1") int page,
                       Model model) {
        int pageSize = 6;
        Map<String, Object> search = new HashMap<>();
        search.put("keyword", keyword == null ? null : keyword.trim());
        search.put("companySize", companySize);
        search.put("industry", industry);
        search.put("career", career);
        search.put("sort", sort);

        int total = salaryService.getSalaryCompanyCount(search);
        PageHandler pageHandler = new PageHandler(Math.max(page, 1), total, pageSize);
        search.put("offset", pageHandler.getOffset());
        search.put("pageSize", pageSize);

        model.addAttribute("salaryList", salaryService.getSalaryList(search));
        model.addAttribute("summary", salaryService.getSalarySummary());
        model.addAttribute("industryList", salaryService.getSalaryIndustryList());
        model.addAttribute("companySizeList", salaryService.getSalaryCompanySizeList());
        model.addAttribute("pageHandler", pageHandler);
        model.addAttribute("keyword", keyword);
        model.addAttribute("companySize", companySize);
        model.addAttribute("industry", industry);
        model.addAttribute("career", career);
        model.addAttribute("sort", sort);
        model.addAttribute("companyCount", total);

        return "salary/list";
    }

    @GetMapping("/detail")
    public String detail(@RequestParam Long companyId, Model model) {
        SalaryRecordsDTO companySalary = salaryService.getSalaryCompanyDetail(companyId);
        if (companySalary == null) {
            return "redirect:/salary/list";
        }
        model.addAttribute("companySalary", companySalary);
        model.addAttribute("careerStats", salaryService.getSalaryCareerStats(companyId));
        model.addAttribute("jobStats", salaryService.getSalaryJobStats(companyId));
        return "salary/detail";
    }

    @GetMapping("/create")
    public String create(@RequestParam(required = false) Long companyId,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         Model model) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        if (!"개인".equals(userDetails.getUserType())) {
            return "redirect:/salary/list";
        }
        SalaryRecordsDTO form = new SalaryRecordsDTO();
        form.setCompanyId(companyId);
        form.setEmploymentStatus("현직원");
        form.setEmploymentType("정규직");
        form.setCareerYears(0);
        form.setSalaryYear(java.time.Year.now().getValue());
        addSalaryCreateOptions(model);
        model.addAttribute("salaryRecordsDTO", form);
        return "salary/create";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute SalaryRecordsDTO dto,
                         @RequestParam(defaultValue = "false") boolean agree,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        if (!"개인".equals(userDetails.getUserType())) {
            redirectAttributes.addFlashAttribute("errorMessage", "개인회원만 연봉정보를 등록할 수 있습니다.");
            return "redirect:/salary/list";
        }
        try {
            salaryService.createSalaryRecord(userDetails.getUserId(), dto, agree);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/salary/create?companyId=" + (dto.getCompanyId() == null ? "" : dto.getCompanyId());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "연봉정보 등록 중 오류가 발생했습니다.");
            return "redirect:/salary/create?companyId=" + (dto.getCompanyId() == null ? "" : dto.getCompanyId());
        }
        redirectAttributes.addFlashAttribute("successMessage", "연봉정보가 등록되었습니다.");
        return "redirect:/salary/detail?companyId=" + dto.getCompanyId();
    }

    private void addSalaryCreateOptions(Model model) {
        model.addAttribute("companyOptions", salaryService.getSalaryCompanyOptions());
        model.addAttribute("jobCategoryOptions", salaryService.getSalaryJobCategoryOptions());
        model.addAttribute("currentYear", java.time.Year.now().getValue());
    }

    @GetMapping("/edit")
    public String edit(@RequestParam Long salaryRecordId,
                       @AuthenticationPrincipal CustomUserDetails userDetails,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/auth/login";
        try {
            model.addAttribute("salaryRecordsDTO",
                    salaryService.getSalaryRecordForEdit(userDetails.getUserId(), salaryRecordId));
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/salary/my-list";
        }
        addSalaryCreateOptions(model);
        return "salary/edit";
    }

    @PostMapping("/edit")
    public String edit(@ModelAttribute SalaryRecordsDTO dto,
                       @RequestParam(defaultValue = "false") boolean agree,
                       @AuthenticationPrincipal CustomUserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/auth/login";
        try {
            salaryService.updateSalaryRecord(userDetails.getUserId(), dto, agree);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/salary/edit?salaryRecordId=" + dto.getSalaryRecordId();
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "연봉정보 수정 중 오류가 발생했습니다.");
            return "redirect:/salary/edit?salaryRecordId=" + dto.getSalaryRecordId();
        }
        redirectAttributes.addFlashAttribute("successMessage", "연봉정보가 수정되었습니다.");
        return "redirect:/salary/detail?companyId=" + dto.getCompanyId();
    }

    @GetMapping("/my-list")
    public String myList(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/auth/login";
        model.addAttribute("salaryList", salaryService.getMySalaryList(userDetails.getUserId()));
        return "salary/my-list";
    }

    @PostMapping("/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> delete(
            @RequestParam Long salaryRecordId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            salaryService.deleteSalaryRecord(userDetails.getUserId(), salaryRecordId);
            return ResponseEntity.ok(Map.of("message", "연봉정보가 삭제되었습니다."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "연봉정보 삭제 중 오류가 발생했습니다."));
        }
    }
}
