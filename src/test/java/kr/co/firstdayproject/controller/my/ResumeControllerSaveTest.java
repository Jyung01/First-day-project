package kr.co.firstdayproject.controller.my;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import kr.co.firstdayproject.dto.my.ResumeDto;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.my.ResumeService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

/**
 * 이력서 저장의 검증 실패 경로 테스트.
 *
 * <p>검증에서 걸린 요청이 서비스까지 내려가면 DB 제약에 부딪혀 500이 난다.
 * 폼으로 되돌리는 분기가 사라지지 않도록 고정한다.
 */
class ResumeControllerSaveTest {

    @Test
    void returnsFormWithoutSavingWhenValidationFailed() {
        ResumeService resumeService = mock(ResumeService.class);
        ResumeController controller = new ResumeController(resumeService);
        stubFormModel(resumeService);

        ResumeDto.FormRequest form = new ResumeDto.FormRequest();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "resumeForm");
        bindingResult.rejectValue("title", "NotBlank", "이력서 제목을 입력해주세요.");

        Model model = new ExtendedModelMap();
        String view = controller.save(userDetails(), form, bindingResult, "save", model);

        assertThat(view).isEqualTo("my/resume/form");
        verify(resumeService, never()).save(anyLong(), any());
    }

    @Test
    void exposesValidationMessagesToTheView() {
        ResumeService resumeService = mock(ResumeService.class);
        ResumeController controller = new ResumeController(resumeService);
        stubFormModel(resumeService);

        ResumeDto.FormRequest form = new ResumeDto.FormRequest();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "resumeForm");
        bindingResult.rejectValue("title", "NotBlank", "이력서 제목을 입력해주세요.");
        // 같은 문장이 여러 행에서 나올 수 있으므로 중복이 제거되어야 한다.
        bindingResult.rejectValue("careerType", "NotBlank", "이력서 제목을 입력해주세요.");

        Model model = new ExtendedModelMap();
        controller.save(userDetails(), form, bindingResult, "save", model);

        Object messages = model.getAttribute("validationErrors");
        assertThat(messages).isEqualTo(List.of("이력서 제목을 입력해주세요."));
    }

    @Test
    void savesAndRedirectsWhenValidationPassed() {
        ResumeService resumeService = mock(ResumeService.class);
        ResumeController controller = new ResumeController(resumeService);

        ResumeDto.FormRequest form = new ResumeDto.FormRequest();
        form.setTitle("신입 백엔드 이력서");
        form.setCareerType("신입");
        when(resumeService.save(anyLong(), any())).thenReturn(7L);

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "resumeForm");
        Model model = new ExtendedModelMap();

        String view = controller.save(userDetails(), form, bindingResult, "save", model);

        assertThat(view).isEqualTo("redirect:/my/resume/list?saved=created");
        verify(resumeService).save(anyLong(), any());
    }

    /** 검증 실패 후 폼을 다시 그리려면 부가 데이터가 필요하다. */
    private void stubFormModel(ResumeService resumeService) {
        User applicant = User.builder()
                .name("회원")
                .email("member@example.com")
                .phone("010-1234-5678")
                .build();

        when(resumeService.getApplicant(anyLong())).thenReturn(applicant);
        when(resumeService.getActiveSkills()).thenReturn(List.of());
    }

    /** 검증에 걸려도 방금 고른 기술이 사라지면 안 된다. */
    @Test
    void keepsSubmittedSkillsWhenValidationFailed() {
        ResumeService resumeService = mock(ResumeService.class);
        ResumeController controller = new ResumeController(resumeService);
        stubFormModel(resumeService);

        ResumeDto.FormRequest form = new ResumeDto.FormRequest();
        form.setResumeId(7L);
        form.setSkillIds(List.of(3L, 1L));

        List<ResumeDto.SkillChip> submittedChips = List.of(
                ResumeDto.SkillChip.builder().id(3L).name("Spring").build(),
                ResumeDto.SkillChip.builder().id(1L).name("Java").build()
        );
        when(resumeService.getSkillChipsByIds(List.of(3L, 1L))).thenReturn(submittedChips);

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "resumeForm");
        bindingResult.rejectValue("title", "NotBlank", "이력서 제목을 입력해주세요.");

        Model model = new ExtendedModelMap();
        controller.save(userDetails(), form, bindingResult, "save", model);

        assertThat(model.getAttribute("skillChips")).isEqualTo(submittedChips);
        // 저장된 기술을 다시 읽어오면 사용자의 선택을 덮어쓰게 된다.
        verify(resumeService, never()).getSkillChips(anyLong());
    }

    private CustomUserDetails userDetails() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(12L);
        return userDetails;
    }
}
