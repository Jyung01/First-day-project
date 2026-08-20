package kr.co.firstdayproject.service.my;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import kr.co.firstdayproject.dto.my.ResumeDto;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.entity.resume.ResumeCareer;
import kr.co.firstdayproject.entity.resume.ResumeEducation;
import kr.co.firstdayproject.repository.job.SkillRepository;
import kr.co.firstdayproject.repository.member.UserRepository;
import kr.co.firstdayproject.repository.resume.ResumeCareerRepository;
import kr.co.firstdayproject.repository.resume.ResumeEducationRepository;
import kr.co.firstdayproject.repository.resume.ResumeProjectRepository;
import kr.co.firstdayproject.repository.resume.ResumeRepository;
import kr.co.firstdayproject.repository.resume.ResumeSkillRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;


/**
 * 선택하지 않은 select가 빈 문자열로 저장되지 않는지 확인한다.
 *
 * <p>graduation_status·employment_type은 {@code NULL 또는 정해진 값}만 허용하는 CHECK 제약이 있어,
 * 빈 문자열이 그대로 내려가면 저장 시점에 제약 위반으로 500이 난다.
 */
class ResumeServiceSaveTest {

    @Test
    void storesUnselectedGraduationStatusAsNull() {
        Fixture fixture = new Fixture();

        ResumeDto.FormRequest.EducationItem education = new ResumeDto.FormRequest.EducationItem();
        education.setSchoolName("첫출근대학교");
        education.setGraduationStatus("");
        education.setDegree("");
        education.setMajor("");

        fixture.service.save(12L, formWith(List.of(education), List.of()));

        ArgumentCaptor<ResumeEducation> saved = ArgumentCaptor.forClass(ResumeEducation.class);
        verify(fixture.educationRepository).save(saved.capture());

        assertThat(saved.getValue().getGraduationStatus()).isNull();
        assertThat(saved.getValue().getDegree()).isNull();
        assertThat(saved.getValue().getMajor()).isNull();
    }

    @Test
    void storesUnselectedEmploymentTypeAsNull() {
        Fixture fixture = new Fixture();

        ResumeDto.FormRequest.CareerItem career = new ResumeDto.FormRequest.CareerItem();
        career.setCompanyName("첫출근");
        career.setStartDate(YearMonth.of(2020, 3));
        career.setEmploymentType("");
        career.setPositionTitle("");

        fixture.service.save(12L, formWith(List.of(), List.of(career)));

        ArgumentCaptor<ResumeCareer> saved = ArgumentCaptor.forClass(ResumeCareer.class);
        verify(fixture.careerRepository).save(saved.capture());

        assertThat(saved.getValue().getEmploymentType()).isNull();
        assertThat(saved.getValue().getPositionTitle()).isNull();
    }

    @Test
    void keepsSelectedValues() {
        Fixture fixture = new Fixture();

        ResumeDto.FormRequest.EducationItem education = new ResumeDto.FormRequest.EducationItem();
        education.setSchoolName("첫출근대학교");
        education.setGraduationStatus("졸업");

        fixture.service.save(12L, formWith(List.of(education), List.of()));

        ArgumentCaptor<ResumeEducation> saved = ArgumentCaptor.forClass(ResumeEducation.class);
        verify(fixture.educationRepository).save(saved.capture());

        assertThat(saved.getValue().getGraduationStatus()).isEqualTo("졸업");
    }

    private ResumeDto.FormRequest formWith(
            List<ResumeDto.FormRequest.EducationItem> educations,
            List<ResumeDto.FormRequest.CareerItem> careers
    ) {
        ResumeDto.FormRequest form = new ResumeDto.FormRequest();
        form.setTitle("신입 백엔드 이력서");
        form.setCareerType("신입");
        form.setEducations(new java.util.ArrayList<>(educations));
        form.setCareers(new java.util.ArrayList<>(careers));
        return form;
    }

    /** ResumeService가 의존하는 8개를 한 번에 묶어둔다. */
    private static final class Fixture {
        final ResumeRepository resumeRepository = mock(ResumeRepository.class);
        final ResumeCareerRepository careerRepository = mock(ResumeCareerRepository.class);
        final ResumeEducationRepository educationRepository = mock(ResumeEducationRepository.class);
        final ResumeProjectRepository projectRepository = mock(ResumeProjectRepository.class);
        final ResumeSkillRepository skillLinkRepository = mock(ResumeSkillRepository.class);
        final SkillRepository skillRepository = mock(SkillRepository.class);
        final UserRepository userRepository = mock(UserRepository.class);
        final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        final ResumeService service;

        Fixture() {
            User user = User.builder()
                    .name("회원")
                    .email("member@example.com")
                    .phone("010-1234-5678")
                    .build();
            when(userRepository.findById(any())).thenReturn(Optional.of(user));

            service = new ResumeService(
                    resumeRepository,
                    careerRepository,
                    educationRepository,
                    projectRepository,
                    skillLinkRepository,
                    skillRepository,
                    userRepository,
                    eventPublisher
            );
        }
    }
}
