package kr.co.firstdayproject.service.corp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import kr.co.firstdayproject.dto.corp.job.JobPostingCreateRequest;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.entity.job.JobPostingSkill;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.job.JobPostingSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CorpJobService {

    private static final String DRAFT = "DRAFT";
    private static final String PUBLISH = "PUBLISH";
    private static final String REVIEW = "REVIEW";

    private final CompanyRepository companyRepository;
    private final JobPostingRepository jobPostingRepository;
    private final JobPostingSkillRepository jobPostingSkillRepository;
    private final CorpJobRequestValidator requestValidator;

    @Transactional
    public Long createJobPosting(
        Long companyId,
        JobPostingCreateRequest request
    ) {
        if (companyId == null) {
            throw new IllegalArgumentException("기업회원 로그인이 필요합니다.");
        }

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException(
                "기업 정보를 찾을 수 없습니다."
            ));

        boolean publish = PUBLISH.equals(request.getSubmitType());

        if (!publish && !DRAFT.equals(request.getSubmitType())) {
            throw new IllegalArgumentException("올바르지 않은 저장 방식입니다.");
        }

        if (publish) {
            requestValidator.validatePublishableCompany(company);
            requestValidator.validatePublishRequest(request);
        } else if (request.getJobCategoryId() != null) {
            requestValidator.validateCategory(request.getJobCategoryId());
        }

        List<Long> skillIds = requestValidator.normalizeAndValidateSkills(
            request.getSkillIds()
        );
        LocalDateTime now = LocalDateTime.now();

        JobPosting jobPosting = JobPosting.builder()
            .companyId(companyId)
            .jobCategoryId(request.getJobCategoryId())
            .title(request.getTitle().trim())
            .employmentType(blankToNull(request.getEmploymentType()))
            .careerType(blankToNull(request.getCareerType()))
            .minExperienceYears(request.getMinExperienceYears())
            .maxExperienceYears(request.getMaxExperienceYears())
            .educationLevel(blankToNull(request.getEducationLevel()))
            .workRegion(blankToNull(request.getWorkRegion()))
            .workAddress(joinNullableAddress(
                request.getAddress(),
                request.getAddressDetail()
            ))
            .salaryText(blankToNull(request.getSalaryText()))
            .salaryMin(request.getSalaryMin())
            .salaryMax(request.getSalaryMax())
            .headcount(request.getHeadcount())
            .applyStartAt(publish ? now : null)
            .applyEndAt(request.getApplyEndDate() == null
                ? null
                : request.getApplyEndDate().atTime(23, 59, 59))
            .introduction(blankToNull(request.getIntroduction()))
            .mainTasks(blankToNull(request.getMainTasks()))
            .qualifications(blankToNull(request.getQualifications()))
            .preferredConditions(blankToNull(
                request.getPreferredConditions()
            ))
            .benefitsJson(toBenefitsJson(request.getBenefits()))
            .processText(null)
            .status(publish ? "모집중" : "임시저장")
            .publishedAt(publish ? now : null)
            .build();

        JobPosting savedJobPosting = jobPostingRepository.save(jobPosting);

        List<JobPostingSkill> postingSkills = skillIds.stream()
            .map(skillId -> JobPostingSkill.of(
                savedJobPosting.getJobPostingId(),
                skillId
            ))
            .toList();

        jobPostingSkillRepository.saveAll(postingSkills);
        return savedJobPosting.getJobPostingId();
    }

    @Transactional
    public void updateJobPosting(
        Long companyId,
        Long jobPostingId,
        JobPostingCreateRequest request
    ) {
        if (companyId == null) {
            throw new IllegalArgumentException("기업회원 로그인이 필요합니다.");
        }

        JobPosting posting = jobPostingRepository
            .findByJobPostingIdAndCompanyId(jobPostingId, companyId)
            .orElseThrow(() -> new IllegalArgumentException(
                "채용공고를 찾을 수 없습니다."
            ));

        if (!Set.of("임시저장", "모집중", "숨김")
            .contains(posting.getStatus())) {
            throw new IllegalArgumentException(
                "수정할 수 없는 상태의 채용공고입니다."
            );
        }

        boolean publish = PUBLISH.equals(request.getSubmitType());
        boolean review = REVIEW.equals(request.getSubmitType());
        if (!publish && !review && !DRAFT.equals(request.getSubmitType())) {
            throw new IllegalArgumentException(
                "올바르지 않은 저장 방식입니다."
            );
        }
        if ("숨김".equals(posting.getStatus()) && !review) {
            throw new IllegalArgumentException(
                "숨김 공고는 수정 후 재검토를 요청해야 합니다."
            );
        }
        if (!"숨김".equals(posting.getStatus()) && review) {
            throw new IllegalArgumentException(
                "숨김 상태의 공고만 재검토를 요청할 수 있습니다."
            );
        }
        if ("모집중".equals(posting.getStatus()) && !publish) {
            throw new IllegalArgumentException(
                "모집 중인 공고는 임시저장으로 변경할 수 없습니다."
            );
        }

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException(
                "기업 정보를 찾을 수 없습니다."
            ));

        if (publish || review) {
            requestValidator.validatePublishableCompany(company);
            requestValidator.validatePublishRequest(request);
        } else if (request.getJobCategoryId() != null) {
            requestValidator.validateCategory(request.getJobCategoryId());
        }

        List<Long> skillIds = requestValidator.normalizeAndValidateSkills(
            request.getSkillIds()
        );
        boolean firstPublish = publish
            && "임시저장".equals(posting.getStatus());

        posting.setJobCategoryId(request.getJobCategoryId());
        posting.setTitle(request.getTitle().trim());
        posting.setEmploymentType(blankToNull(request.getEmploymentType()));
        posting.setCareerType(blankToNull(request.getCareerType()));
        posting.setMinExperienceYears(request.getMinExperienceYears());
        posting.setMaxExperienceYears(request.getMaxExperienceYears());
        posting.setEducationLevel(blankToNull(request.getEducationLevel()));
        posting.setWorkRegion(blankToNull(request.getWorkRegion()));
        posting.setWorkAddress(joinNullableAddress(
            request.getAddress(),
            request.getAddressDetail()
        ));
        posting.setSalaryText(blankToNull(request.getSalaryText()));
        posting.setSalaryMin(request.getSalaryMin());
        posting.setSalaryMax(request.getSalaryMax());
        posting.setHeadcount(request.getHeadcount());
        posting.setApplyEndAt(request.getApplyEndDate() == null
            ? null
            : request.getApplyEndDate().atTime(23, 59, 59));
        posting.setIntroduction(blankToNull(request.getIntroduction()));
        posting.setMainTasks(blankToNull(request.getMainTasks()));
        posting.setQualifications(blankToNull(request.getQualifications()));
        posting.setPreferredConditions(blankToNull(
            request.getPreferredConditions()
        ));
        posting.setBenefitsJson(toBenefitsJson(request.getBenefits()));
        posting.setStatus(review
            ? "재검토요청"
            : (publish ? "모집중" : "임시저장"));

        if (firstPublish) {
            LocalDateTime now = LocalDateTime.now();
            posting.setApplyStartAt(now);
            posting.setPublishedAt(now);
        }

        jobPostingSkillRepository.deleteByIdJobPostingId(jobPostingId);
        jobPostingSkillRepository.flush();
        jobPostingSkillRepository.saveAll(
            skillIds.stream()
                .map(skillId -> JobPostingSkill.of(
                    jobPostingId,
                    skillId
                ))
                .toList()
        );
    }

    private String joinNullableAddress(String address, String detail) {
        if (address == null || address.isBlank()) {
            return null;
        }

        String baseAddress = address.trim();
        String detailAddress = blankToNull(detail);
        String fullAddress = detailAddress == null
            ? baseAddress
            : baseAddress + " " + detailAddress;

        if (fullAddress.length() > 500) {
            throw new IllegalArgumentException(
                "근무 주소는 500자 이하로 입력해 주세요."
            );
        }

        return fullAddress;
    }

    private String toBenefitsJson(List<String> benefits) {
        List<String> normalizedBenefits = benefits == null
            ? List.of()
            : benefits.stream()
                .map(this::blankToNull)
                .filter(value -> value != null)
                .distinct()
                .toList();

        return normalizedBenefits.stream()
            .map(this::toJsonString)
            .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private String toJsonString(String value) {
        StringBuilder escaped = new StringBuilder("\"");

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);

            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }

        return escaped.append('"').toString();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
