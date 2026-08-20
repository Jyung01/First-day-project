package kr.co.firstdayproject.service.my;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import kr.co.firstdayproject.dto.my.ResumeDto;
import kr.co.firstdayproject.entity.job.Skill;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.entity.resume.Resume;
import kr.co.firstdayproject.entity.resume.ResumeCareer;
import kr.co.firstdayproject.entity.resume.ResumeEducation;
import kr.co.firstdayproject.entity.resume.ResumeProject;
import kr.co.firstdayproject.entity.resume.ResumeSkill;
import kr.co.firstdayproject.entity.resume.ResumeSkillId;
import kr.co.firstdayproject.exception.AccessDeniedException;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.repository.job.SkillRepository;
import kr.co.firstdayproject.repository.member.UserRepository;
import kr.co.firstdayproject.repository.resume.ResumeCareerRepository;
import kr.co.firstdayproject.repository.resume.ResumeEducationRepository;
import kr.co.firstdayproject.repository.resume.ResumeProjectRepository;
import kr.co.firstdayproject.repository.resume.ResumeRepository;
import kr.co.firstdayproject.repository.resume.ResumeSkillRepository;
import kr.co.firstdayproject.service.ai.UserProfileEmbeddingSyncEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeService {

    private static final int MAX_SKILLS = 10;
    private static final String CURRENTLY_EMPLOYED = "재직";

    private final ResumeRepository resumeRepository;
    private final ResumeCareerRepository resumeCareerRepository;
    private final ResumeEducationRepository resumeEducationRepository;
    private final ResumeProjectRepository resumeProjectRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<ResumeDto.ListItem> findMyList(Long userId) {
        List<Resume> resumes = resumeRepository.findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId);

        List<ResumeDto.ListItem> result = new ArrayList<>();
        for (Resume resume : resumes) {
            result.add(ResumeDto.ListItem.builder()
                    .id(resume.getResumeId())
                    .title(resume.getTitle())
                    .summary(buildSummary(resume))
                    .build());
        }
        return result;
    }

    private String buildSummary(Resume resume) {
        if (resume.getSummary() == null || resume.getSummary().isBlank()) {
            return resume.getCareerType();
        }
        return resume.getCareerType() + " · " + resume.getSummary();
    }

    /** 소유권 검증까지 포함한 단건 조회 (상세/수정 화면에서 공통으로 사용) */
    public Resume getMine(Long resumeId, Long userId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 이력서입니다."));

        if (!resume.getUserId().equals(userId) || resume.getDeletedAt() != null) {
            throw new AccessDeniedException("접근 권한이 없습니다.");
        }
        return resume;
    }

    @Transactional
    public void delete(Long resumeId, Long userId) {
        Resume resume = getMine(resumeId, userId);
        resume.setDeletedAt(LocalDateTime.now());
        eventPublisher.publishEvent(new UserProfileEmbeddingSyncEvent(userId));
    }

    public List<ResumeCareer> getCareers(Long resumeId) {
        return resumeCareerRepository.findByResumeIdOrderByDisplayOrderAsc(resumeId);
    }

    public List<ResumeEducation> getEducations(Long resumeId) {
        return resumeEducationRepository.findByResumeIdOrderByDisplayOrderAsc(resumeId);
    }

    public List<ResumeProject> getProjects(Long resumeId) {
        return resumeProjectRepository.findByResumeIdOrderByDisplayOrderAsc(resumeId);
    }

    /** 이력서에 등록된 기술 스택 이름 목록 (표시 순서 유지) */
    public List<String> getSkillNames(Long resumeId) {
        return getSkillChips(resumeId).stream()
                .map(ResumeDto.SkillChip::getName)
                .toList();
    }

    /** 이력서에 등록된 기술 스택 id+이름 목록 (표시 순서 유지); 수정 폼의 기술 칩 렌더링에 사용 */
    public List<ResumeDto.SkillChip> getSkillChips(Long resumeId) {
        List<ResumeSkill> resumeSkills = resumeSkillRepository.findByIdResumeIdOrderByDisplayOrderAsc(resumeId);
        if (resumeSkills.isEmpty()) {
            return List.of();
        }

        List<Long> skillIds = resumeSkills.stream()
                .map(resumeSkill -> resumeSkill.getId().getSkillId())
                .toList();
        Map<Long, String> nameBySkillId = skillRepository.findAllById(skillIds).stream()
                .collect(Collectors.toMap(Skill::getSkillId, Skill::getSkillName));

        List<ResumeDto.SkillChip> result = new ArrayList<>();
        for (ResumeSkill resumeSkill : resumeSkills) {
            Long skillId = resumeSkill.getId().getSkillId();
            String name = nameBySkillId.get(skillId);
            if (name != null) {
                result.add(ResumeDto.SkillChip.builder().id(skillId).name(name).build());
            }
        }
        return result;
    }

    /**
     * 전달받은 id 목록으로 기술 칩을 만든다 (전달된 순서 유지).
     *
     * <p>검증에 걸려 폼을 다시 보여줄 때 쓴다. 이때 DB를 조회하면 저장되어 있던 이전 기술이
     * 나와서, 사용자가 방금 고른 선택이 사라진다. 제출된 값 그대로를 되돌려주기 위한 경로다.
     */
    public List<ResumeDto.SkillChip> getSkillChipsByIds(List<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return List.of();
        }

        Map<Long, String> nameBySkillId = skillRepository.findAllById(skillIds).stream()
                .collect(Collectors.toMap(Skill::getSkillId, Skill::getSkillName));

        List<ResumeDto.SkillChip> result = new ArrayList<>();
        for (Long skillId : skillIds) {
            String name = nameBySkillId.get(skillId);
            // 존재하지 않거나 비활성화된 id는 조용히 버린다. 저장 로직도 같은 기준으로 거른다.
            if (name != null) {
                result.add(ResumeDto.SkillChip.builder().id(skillId).name(name).build());
            }
        }
        return result;
    }

    /** 기술 선택 모달에 표시할 활성 기술 전체 목록 (그룹·기술 depth 순서 유지) */
    public List<Skill> getActiveSkills() {
        return skillRepository.findByIsActiveTrue(Sort.by(
                "depth",
                "parentId",
                "displayOrder",
                "skillId"
        ));
    }

    /** 이력서 작성·수정 폼의 읽기 전용 담당자 정보 (계정 기준) */
    public User getApplicant(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다."));
    }

    /** 이력서 등록 폼 초기값; 학력은 최소 1건이 필요하므로 빈 카드를 하나 채워둔다. */
    public ResumeDto.FormRequest getNewFormData() {
        ResumeDto.FormRequest form = new ResumeDto.FormRequest();
        form.setCareerType("신입");
        form.getEducations().add(new ResumeDto.FormRequest.EducationItem());
        return form;
    }

    /** 이력서 수정 폼 초기값 (기존 데이터 채움) */
    public ResumeDto.FormRequest getFormData(Long resumeId, Long userId) {
        Resume resume = getMine(resumeId, userId);

        ResumeDto.FormRequest form = new ResumeDto.FormRequest();
        form.setResumeId(resume.getResumeId());
        form.setTitle(resume.getTitle());
        form.setCareerType(resume.getCareerType());
        form.setSummary(resume.getSummary());

        for (ResumeEducation education : getEducations(resumeId)) {
            ResumeDto.FormRequest.EducationItem item = new ResumeDto.FormRequest.EducationItem();
            item.setSchoolName(education.getSchoolName());
            item.setMajor(education.getMajor());
            item.setDegree(education.getDegree());
            item.setAdmissionDate(toYearMonth(education.getAdmissionDate()));
            item.setGraduationDate(toYearMonth(education.getGraduationDate()));
            item.setGraduationStatus(education.getGraduationStatus());
            item.setGpaScore(education.getGpaScore());
            item.setGpaScale(education.getGpaScale());
            form.getEducations().add(item);
        }

        for (ResumeCareer career : getCareers(resumeId)) {
            ResumeDto.FormRequest.CareerItem item = new ResumeDto.FormRequest.CareerItem();
            item.setCompanyName(career.getCompanyName());
            item.setPositionTitle(career.getPositionTitle());
            item.setEmploymentType(career.getEmploymentType());
            item.setStartDate(toYearMonth(career.getStartDate()));
            item.setEndDate(toYearMonth(career.getEndDate()));
            item.setEmploymentStatus(Boolean.TRUE.equals(career.getIsCurrent()) ? "재직" : "퇴사");
            item.setDescription(career.getDescription());
            form.getCareers().add(item);
        }

        for (ResumeProject project : getProjects(resumeId)) {
            ResumeDto.FormRequest.ProjectItem item = new ResumeDto.FormRequest.ProjectItem();
            item.setProjectName(project.getProjectName());
            item.setRoleText(project.getRoleText());
            item.setDescription(project.getDescription());
            form.getProjects().add(item);
        }

        List<Long> skillIds = getSkillChips(resumeId).stream()
                .map(ResumeDto.SkillChip::getId)
                .collect(Collectors.toCollection(ArrayList::new));
        form.setSkillIds(skillIds);

        if (form.getEducations().isEmpty()) {
            form.getEducations().add(new ResumeDto.FormRequest.EducationItem());
        }

        return form;
    }

    private YearMonth toYearMonth(java.time.LocalDate date) {
        return date == null ? null : YearMonth.from(date);
    }

    /** 이력서 등록/수정 저장; resumeId 유무로 신규/기존을 분기한다. */
    @Transactional
    public Long save(Long userId, ResumeDto.FormRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("이력서 제목을 입력해주세요.");
        }
        if (request.getCareerType() == null || request.getCareerType().isBlank()) {
            throw new IllegalArgumentException("경력 구분을 선택해주세요.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        Resume resume;

        if (request.getResumeId() != null) {
            resume = getMine(request.getResumeId(), userId);
            resume.setTitle(request.getTitle());
            resume.setCareerType(request.getCareerType());
            resume.setSummary(request.getSummary());
            resume.setApplicantName(user.getName());
            resume.setEmail(user.getEmail());
            resume.setPhone(user.getPhone());
            resume.setUpdatedAt(now);

            resumeEducationRepository.deleteAll(resumeEducationRepository.findByResumeIdOrderByDisplayOrderAsc(resume.getResumeId()));
            resumeCareerRepository.deleteAll(resumeCareerRepository.findByResumeIdOrderByDisplayOrderAsc(resume.getResumeId()));
            resumeProjectRepository.deleteAll(resumeProjectRepository.findByResumeIdOrderByDisplayOrderAsc(resume.getResumeId()));
            resumeSkillRepository.deleteAll(resumeSkillRepository.findByIdResumeIdOrderByDisplayOrderAsc(resume.getResumeId()));
        } else {
            resume = Resume.builder()
                    .userId(userId)
                    .title(request.getTitle())
                    .careerType(request.getCareerType())
                    .summary(request.getSummary())
                    .applicantName(user.getName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .status("사용중")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            resumeRepository.save(resume);
        }

        saveEducations(resume.getResumeId(), request.getEducations());
        saveCareers(resume.getResumeId(), request.getCareers());
        saveProjects(resume.getResumeId(), request.getProjects());
        saveSkills(resume.getResumeId(), request.getSkillIds());
        eventPublisher.publishEvent(new UserProfileEmbeddingSyncEvent(userId));

        return resume.getResumeId();
    }

    /**
     * 빈 문자열을 null로 바꾼다.
     *
     * <p>"학위 선택" 같은 select의 기본 옵션은 {@code value=""}라 선택하지 않으면 빈 문자열이 온다.
     * 그런데 graduation_status·employment_type은 {@code NULL 또는 정해진 값}만 허용하는 CHECK 제약이
     * 걸려 있어, 빈 문자열이 그대로 들어가면 저장 시점에 제약 위반으로 터진다.
     * 선택하지 않았다는 뜻은 null이므로 여기서 맞춰준다.
     */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private void saveEducations(Long resumeId, List<ResumeDto.FormRequest.EducationItem> items) {
        if (items == null) {
            return;
        }

        int order = 0;
        for (ResumeDto.FormRequest.EducationItem item : items) {
            if (item.getSchoolName() == null || item.getSchoolName().isBlank()) {
                continue;
            }

            ResumeEducation education = ResumeEducation.builder()
                    .resumeId(resumeId)
                    .schoolName(item.getSchoolName())
                    .major(blankToNull(item.getMajor()))
                    .degree(blankToNull(item.getDegree()))
                    .admissionDate(item.getAdmissionDate() != null ? item.getAdmissionDate().atDay(1) : null)
                    .graduationDate(item.getGraduationDate() != null ? item.getGraduationDate().atDay(1) : null)
                    .graduationStatus(blankToNull(item.getGraduationStatus()))
                    .gpaScore(item.getGpaScore())
                    .gpaScale(item.getGpaScale())
                    .displayOrder(order++)
                    .build();
            resumeEducationRepository.save(education);
        }
    }

    private void saveCareers(Long resumeId, List<ResumeDto.FormRequest.CareerItem> items) {
        if (items == null) {
            return;
        }

        int order = 0;
        for (ResumeDto.FormRequest.CareerItem item : items) {
            if (item.getCompanyName() == null || item.getCompanyName().isBlank() || item.getStartDate() == null) {
                continue;
            }

            boolean current = CURRENTLY_EMPLOYED.equals(item.getEmploymentStatus());

            ResumeCareer career = ResumeCareer.builder()
                    .resumeId(resumeId)
                    .companyName(item.getCompanyName())
                    .positionTitle(blankToNull(item.getPositionTitle()))
                    .employmentType(blankToNull(item.getEmploymentType()))
                    .startDate(item.getStartDate().atDay(1))
                    .endDate(current || item.getEndDate() == null ? null : item.getEndDate().atDay(1))
                    .isCurrent(current)
                    .description(item.getDescription())
                    .displayOrder(order++)
                    .build();
            resumeCareerRepository.save(career);
        }
    }

    private void saveProjects(Long resumeId, List<ResumeDto.FormRequest.ProjectItem> items) {
        if (items == null) {
            return;
        }

        int order = 0;
        for (ResumeDto.FormRequest.ProjectItem item : items) {
            if (item.getProjectName() == null || item.getProjectName().isBlank()) {
                continue;
            }

            ResumeProject project = ResumeProject.builder()
                    .resumeId(resumeId)
                    .projectName(item.getProjectName())
                    .roleText(blankToNull(item.getRoleText()))
                    .description(item.getDescription())
                    .displayOrder(order++)
                    .build();
            resumeProjectRepository.save(project);
        }
    }

    private void saveSkills(Long resumeId, List<Long> skillIds) {
        if (skillIds == null) {
            return;
        }

        Set<Long> unique = new LinkedHashSet<>(skillIds);
        int order = 0;
        for (Long skillId : unique) {
            if (order >= MAX_SKILLS) {
                break;
            }

            ResumeSkill resumeSkill = ResumeSkill.builder()
                    .id(ResumeSkillId.builder().resumeId(resumeId).skillId(skillId).build())
                    .displayOrder(order++)
                    .build();
            resumeSkillRepository.save(resumeSkill);
        }
    }
}
