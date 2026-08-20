package kr.co.firstdayproject.controller.my;

import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import kr.co.firstdayproject.dto.my.ResumeDto;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.entity.resume.Resume;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.my.ResumeService;
import kr.co.firstdayproject.validation.DateRanges;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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

        model.addAttribute("resumeForm", formRequest);
        addFormModel(model, userId, editMode, editMode ? resumeService.getSkillChips(id) : List.of());
        return "my/resume/form";
    }

    /**
     * 폼 화면에 필요한 부가 데이터를 채운다.
     * 최초 진입(GET)과 검증 실패 후 재표시가 같은 값을 쓰도록 한 곳에 모았다.
     *
     * <p>{@code resumeForm}은 여기서 넣지 않는다. GET은 조회 결과를, 검증 실패 시에는
     * 사용자가 입력한 값을 그대로 돌려줘야 하기 때문이다.
     */
    private void addFormModel(
            Model model,
            Long userId,
            boolean editMode,
            List<ResumeDto.SkillChip> skillChips
    ) {
        User applicant = resumeService.getApplicant(userId);

        model.addAttribute("activeMenu", "resumes");
        model.addAttribute("editMode", editMode);
        model.addAttribute("skillChips", skillChips);
        model.addAttribute("activeSkills", resumeService.getActiveSkills());
        model.addAttribute("memberName", applicant.getName());
        model.addAttribute("email", applicant.getEmail());
        model.addAttribute("phone", applicant.getPhone());

        // 연도 선택 목록. 서버 검증(@YearMonthRange)과 같은 상수를 써서 둘이 어긋나지 않게 한다.
        model.addAttribute("minYear", DateRanges.MIN_YEAR);
        model.addAttribute("maxYear", DateRanges.maxYear());
    }

    @PostMapping("/form")
    public String save(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute("resumeForm") ResumeDto.FormRequest resumeForm,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "save") String submitMode,
            Model model
    ) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        /*
         * 화면에서 걸러도 폼을 거치지 않은 요청은 그대로 들어온다.
         * DATE·DECIMAL(3,2)의 한계나 CHECK 제약을 넘는 값은 저장 시점에 DB가 거부해 500이 되므로,
         * 여기서 막고 입력 내용을 유지한 채 폼으로 되돌린다.
         */
        if (bindingResult.hasErrors()) {
            boolean editMode = resumeForm.getResumeId() != null;
            Long userId = userDetails.getUserId();

            model.addAttribute("validationErrors", collectMessages(bindingResult));
            /*
             * resumeForm은 @ModelAttribute가 이미 모델에 넣어둔다. 입력값이 그대로 유지된다.
             * 기술 칩만은 화면이 skillChips로 그리므로, DB가 아니라 방금 제출된 id로 다시 만든다.
             * 저장된 값을 조회하면 사용자가 방금 고른 기술이 사라진다.
             */
            addFormModel(model, userId, editMode,
                    resumeService.getSkillChipsByIds(resumeForm.getSkillIds()));
            return "my/resume/form";
        }

        // 저장 후 화면이 바뀌므로 목적지에서 완료 안내를 띄울 수 있게 결과를 넘긴다.
        String savedType = resumeForm.getResumeId() == null ? "created" : "updated";
        Long resumeId = resumeService.save(userDetails.getUserId(), resumeForm);

        if ("preview".equals(submitMode)) {
            return "redirect:/my/resume/detail?id=" + resumeId + "&saved=" + savedType;
        }
        return "redirect:/my/resume/list?saved=" + savedType;
    }

    /**
     * 검증 메시지를 화면에 뿌릴 문자열 목록으로 바꾼다.
     *
     * <p>이 폼은 {@code th:field} 대신 동적 인덱스({@code educations[0].major})로 이름을 만들기 때문에
     * Thymeleaf의 필드별 에러 표시를 쓸 수 없다. 대신 상단에 목록으로 모아 보여준다.
     * 같은 문장이 여러 행에서 반복될 수 있어 중복은 제거한다.
     */
    private List<String> collectMessages(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .map(this::toUserMessage)
                .distinct()
                .toList();
    }

    /**
     * 검증 에러 하나를 사용자에게 보여줄 문장으로 바꾼다.
     *
     * <p>날짜·숫자 칸에 형식이 맞지 않는 값이 오면 Bean Validation 이전에 바인딩이 먼저 실패한다.
     * 이때 Spring이 붙이는 기본 메시지는 영어 기술 문구라 그대로 노출할 수 없어 따로 갈아끼운다.
     */
    private String toUserMessage(ObjectError error) {
        if (isTypeMismatch(error)) {
            return "날짜와 숫자를 형식에 맞게 입력해주세요.";
        }

        String message = error.getDefaultMessage();

        return (message == null || message.isBlank())
                ? "입력값을 다시 확인해주세요."
                : message;
    }

    private boolean isTypeMismatch(ObjectError error) {
        String[] codes = error.getCodes();

        if (codes == null) {
            return false;
        }

        return Arrays.stream(codes).anyMatch(code -> code.startsWith("typeMismatch"));
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        resumeService.delete(id, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
