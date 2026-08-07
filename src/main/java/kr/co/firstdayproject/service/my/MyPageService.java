package kr.co.firstdayproject.service.my;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import kr.co.firstdayproject.dto.my.ApplicationSummaryProjection;
import kr.co.firstdayproject.dto.my.CoverLetterDashboardView;
import kr.co.firstdayproject.dto.my.DashboardStats;
import kr.co.firstdayproject.dto.my.MyPasswordChangeRequest;
import kr.co.firstdayproject.dto.my.ProfileEditRequest;
import kr.co.firstdayproject.dto.my.RecentApplicationView;
import kr.co.firstdayproject.dto.job.JobDTO;
import kr.co.firstdayproject.dto.company.CompanyDTO;
import kr.co.firstdayproject.dto.company.CompanyReviewsDTO;
import kr.co.firstdayproject.dto.company.InterviewReviewsDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import kr.co.firstdayproject.dao.my.MyPageDao;
import kr.co.firstdayproject.entity.coverletter.CoverLetter;
import kr.co.firstdayproject.entity.job.JobCategory;
import kr.co.firstdayproject.entity.job.UserDesiredJob;
import kr.co.firstdayproject.entity.job.UserDesiredJobId;
import kr.co.firstdayproject.entity.member.PersonalProfile;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.entity.resume.Resume;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.repository.application.ApplicationRepository;
import kr.co.firstdayproject.repository.company.SavedCompanyRepository;
import kr.co.firstdayproject.repository.coverletter.CoverLetterAiReviewRepository;
import kr.co.firstdayproject.repository.coverletter.CoverLetterRepository;
import kr.co.firstdayproject.repository.job.JobCategoryRepository;
import kr.co.firstdayproject.repository.job.SavedJobRepository;
import kr.co.firstdayproject.repository.job.UserDesiredJobRepository;
import kr.co.firstdayproject.repository.member.PersonalProfileRepository;
import kr.co.firstdayproject.repository.member.UserRepository;
import kr.co.firstdayproject.repository.resume.ResumeRepository;
import kr.co.firstdayproject.service.AwsS3.AwsS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])\\S{8,64}$"
    );
    private static final String WITHDRAWN_STATUS = "탈퇴";
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAXIMUM_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> IN_PROGRESS_APPLICATION_STATUSES =
            Set.of("지원완료", "서류검토중", "서류합격", "면접예정", "면접완료");
    private static final long DEADLINE_SOON_DAYS = 7;
    private static final long NEW_POSTING_DAYS = 7;

    private final UserRepository userRepository;
    private final PersonalProfileRepository personalProfileRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final UserDesiredJobRepository userDesiredJobRepository;
    private final ApplicationRepository applicationRepository;
    private final ResumeRepository resumeRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final CoverLetterAiReviewRepository coverLetterAiReviewRepository;
    private final SavedJobRepository savedJobRepository;
    private final SavedCompanyRepository savedCompanyRepository;
    private final AwsS3Service awsS3Service;
    private final PasswordEncoder passwordEncoder;
    private final MyPageDao myPageDao;

    public List<JobDTO> getSavedJobList(Long userId, String filter, int offset, int pageSize) {
        return myPageDao.selectSavedJobList(userId, normalizeSavedJobFilter(filter), offset, pageSize);
    }

    public int getSavedJobCount(Long userId, String filter) {
        return myPageDao.selectSavedJobCount(userId, normalizeSavedJobFilter(filter));
    }

    @Transactional
    public void removeSavedJob(Long userId, Long jobPostingId) {
        if (jobPostingId == null || myPageDao.deleteSavedJob(userId, jobPostingId) != 1) {
            throw new MyPageException(null, "관심 공고를 찾을 수 없습니다.");
        }
    }

    private String normalizeSavedJobFilter(String filter) {
        return Set.of("all", "open", "deadline").contains(filter) ? filter : "all";
    }

    public List<CompanyDTO> getSavedCompanyList(
            Long userId, String sort, int offset, int pageSize) {
        return myPageDao.selectSavedCompanyList(
                userId, normalizeSavedCompanySort(sort), offset, pageSize);
    }

    public int getSavedCompanyCount(Long userId) {
        return myPageDao.selectSavedCompanyCount(userId);
    }

    @Transactional
    public void removeSavedCompany(Long userId, Long companyId) {
        if (companyId == null || myPageDao.deleteSavedCompany(userId, companyId) != 1) {
            throw new MyPageException(null, "관심 기업을 찾을 수 없습니다.");
        }
    }

    private String normalizeSavedCompanySort(String sort) {
        return Set.of("recent", "name", "jobs").contains(sort) ? sort : "recent";
    }

    public List<CompanyReviewsDTO> getMyCompanyReviews(Long userId) {
        return myPageDao.selectMyCompanyReviews(userId);
    }

    public List<InterviewReviewsDTO> getMyInterviewReviews(Long userId) {
        return myPageDao.selectMyInterviewReviews(userId);
    }

    public int getMyCompanyReviewCount(Long userId) {
        return myPageDao.selectMyCompanyReviewCount(userId);
    }

    public int getMyInterviewReviewCount(Long userId) {
        return myPageDao.selectMyInterviewReviewCount(userId);
    }

    public CompanyReviewsDTO getMyCompanyReview(Long userId, Long reviewId) {
        CompanyReviewsDTO review = myPageDao.selectMyCompanyReview(userId, reviewId);
        if (review == null) throw new MyPageException(null, "수정할 기업리뷰를 찾을 수 없습니다.");
        return review;
    }

    public InterviewReviewsDTO getMyInterviewReview(Long userId, Long reviewId) {
        InterviewReviewsDTO review = myPageDao.selectMyInterviewReview(userId, reviewId);
        if (review == null) throw new MyPageException(null, "수정할 면접후기를 찾을 수 없습니다.");
        return review;
    }

    @Transactional
    public void updateMyCompanyReview(Long userId, CompanyReviewsDTO dto) {
        getMyCompanyReview(userId, dto.getCompanyReviewId());
        validateRating(dto.getCareerGrowthRating());
        validateRating(dto.getWorkSatisfactionRating());
        validateRating(dto.getCompensationRating());
        validateRating(dto.getCultureRating());
        if (isBlank(dto.getPros()) || isBlank(dto.getCons()) || isBlank(dto.getSummary()))
            throw new MyPageException(null, "장점, 단점, 한 줄 요약을 모두 입력해주세요.");
        double avg = (dto.getCareerGrowthRating() + dto.getWorkSatisfactionRating()
                + dto.getCompensationRating() + dto.getCultureRating()) / 4.0;
        dto.setOverallRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        dto.setAuthorUserId(userId);
        if (myPageDao.updateMyCompanyReview(dto) != 1)
            throw new MyPageException(null, "기업리뷰 수정에 실패했습니다.");
    }

    @Transactional
    public void updateMyInterviewReview(Long userId, InterviewReviewsDTO dto) {
        getMyInterviewReview(userId, dto.getInterviewReviewId());
        if (!Set.of("대면면접", "화상면접", "전화면접", "기타").contains(dto.getInterviewType()))
            throw new MyPageException(null, "올바른 면접 방식을 선택해주세요.");
        if (!Set.of("쉬움", "보통", "어려움").contains(dto.getDifficulty()))
            throw new MyPageException(null, "올바른 난이도를 선택해주세요.");
        if (isBlank(dto.getContent())) throw new MyPageException(null, "면접 내용을 입력해주세요.");
        dto.setAuthorUserId(userId);
        if (myPageDao.updateMyInterviewReview(dto) != 1)
            throw new MyPageException(null, "면접후기 수정에 실패했습니다.");
    }

    @Transactional
    public void deleteMyReview(Long userId, String type, Long reviewId) {
        int changed = "company".equals(type)
                ? myPageDao.deleteMyCompanyReview(userId, reviewId)
                : "interview".equals(type)
                    ? myPageDao.deleteMyInterviewReview(userId, reviewId) : 0;
        if (changed != 1) throw new MyPageException(null, "삭제할 후기를 찾을 수 없습니다.");
    }

    private void validateRating(Double rating) {
        if (rating == null || rating < 1 || rating > 5 || rating % 1 != 0)
            throw new MyPageException(null, "평점은 1점부터 5점 사이의 정수여야 합니다.");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("회원 정보를 찾을 수 없습니다."));
    }

    public PersonalProfile getProfile(Long userId) {
        return personalProfileRepository.findById(userId)
                .orElseGet(() -> PersonalProfile.builder().userId(userId).build());
    }

    /**
     * 비공개 버킷에 저장된 프로필 이미지 storageKey로부터, 화면에 표시할 임시 접근 URL을 발급합니다.
     * 호출 시점마다 새로 발급되므로 저장하거나 재사용하지 않아야 합니다.
     */
    public String getProfileImageDisplayUrl(PersonalProfile profile) {
        return awsS3Service.getPresignedUrl(profile.getProfileImageUrl());
    }

    public List<JobCategory> getDesiredJobs(Long userId) {
        return userDesiredJobRepository.findJobCategoriesByUserId(userId);
    }

    public DashboardStats getDashboardStats(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        long applicationCount = applicationRepository.countByApplicantUserId(userId);
        long applicationInProgressCount = applicationRepository
                .countByApplicantUserIdAndCurrentStatusIn(userId, IN_PROGRESS_APPLICATION_STATUSES);

        long likedJobCount = savedJobRepository.countByIdUserId(userId);
        long likedJobDeadlineSoonCount = savedJobRepository.countDeadlineSoon(
                userId, now, now.plusDays(DEADLINE_SOON_DAYS));

        long likedCompanyCount = savedCompanyRepository.countByIdUserId(userId);
        long likedCompanyNewJobCount = savedCompanyRepository.countNewJobPostings(
                userId, now.minusDays(NEW_POSTING_DAYS));

        long resumeCount = resumeRepository.countByUserIdAndDeletedAtIsNull(userId);
        long coverLetterCount = coverLetterRepository.countByUserIdAndDeletedAtIsNull(userId);

        return new DashboardStats(
                applicationCount,
                applicationInProgressCount,
                likedJobCount,
                likedJobDeadlineSoonCount,
                likedCompanyCount,
                likedCompanyNewJobCount,
                resumeCount,
                coverLetterCount
        );
    }

    public List<RecentApplicationView> getRecentApplications(Long userId) {
        return applicationRepository
                .findRecentByApplicantUserId(userId, PageRequest.of(0, 3))
                .stream()
                .map(this::toRecentApplicationView)
                .toList();
    }

    private RecentApplicationView toRecentApplicationView(ApplicationSummaryProjection projection) {
        String companyName = projection.getCompanyName();
        return new RecentApplicationView(
                projection.getApplicationId(),
                companyName,
                companyName == null || companyName.isBlank()
                        ? "?"
                        : companyName.substring(0, 1),
                projection.getJobTitle(),
                resolveStatusLabel(projection.getCurrentStatus()),
                resolveStatusVariant(projection.getCurrentStatus())
        );
    }

    private String resolveStatusLabel(String status) {
        return switch (status) {
            case "지원완료" -> "지원 완료";
            case "서류검토중" -> "서류 검토 중";
            case "서류합격" -> "서류 합격";
            case "면접예정" -> "면접 예정";
            case "면접완료" -> "면접 완료";
            case "최종합격" -> "최종 합격";
            case "입사완료" -> "입사 완료";
            case "불합격" -> "불합격";
            case "지원취소" -> "지원 취소";
            case "채용종료" -> "채용 종료";
            default -> status;
        };
    }

    private String resolveStatusVariant(String status) {
        return switch (status) {
            case "서류검토중" -> "review";
            case "면접예정", "면접완료" -> "interview";
            case "서류합격", "최종합격", "입사완료" -> "success";
            case "불합격", "지원취소", "채용종료" -> "closed";
            default -> "done";
        };
    }

    public Optional<Resume> getRecentResume(Long userId) {
        return resumeRepository.findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId);
    }

    public Optional<CoverLetterDashboardView> getRecentCoverLetter(Long userId) {
        return coverLetterRepository
                .findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId)
                .map(this::toCoverLetterDashboardView);
    }

    private CoverLetterDashboardView toCoverLetterDashboardView(CoverLetter letter) {
        return new CoverLetterDashboardView(
                letter.getCoverLetterId(),
                letter.getTitle(),
                letter.getUpdatedAt(),
                coverLetterAiReviewRepository.existsByCoverLetterId(letter.getCoverLetterId())
        );
    }

    @Transactional
    public void updateProfile(
            Long userId,
            ProfileEditRequest request,
            MultipartFile profileImage
    ) {
        List<Long> desiredJobIds = validateDesiredJobs(request.getDesiredJobIds());

        User user = getUser(userId);
        user.setName(request.getMemberName().trim());
        user.setPhone(request.getPhone().trim());
        user.setUpdatedAt(LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();
        PersonalProfile profile = personalProfileRepository.findById(userId)
                .orElseGet(() -> PersonalProfile.builder()
                        .userId(userId)
                        .createdAt(now)
                        .build());
        profile.setPostalCode(trimToNull(request.getPostcode()));
        profile.setAddressLine1(trimToNull(request.getAddress()));
        profile.setAddressLine2(trimToNull(request.getAddressDetail()));

        if (profileImage != null && !profileImage.isEmpty()) {
            validateProfileImage(profileImage);
            String previousImageKey = profile.getProfileImageUrl();
            try {
                profile.setProfileImageUrl(
                        awsS3Service.uploadPrivate(profileImage, "personal_profile")
                );
            } catch (IOException | RuntimeException exception) {
                throw new MyPageException(
                        "profileImage",
                        "프로필 이미지를 업로드하지 못했습니다. 다시 시도해주세요."
                );
            }
            awsS3Service.deletePrivate(previousImageKey);
        }
        profile.setUpdatedAt(now);
        personalProfileRepository.save(profile);

        replaceDesiredJobs(userId, desiredJobIds, now);
    }

    private List<Long> validateDesiredJobs(List<Long> submittedIds) {
        if (submittedIds == null || submittedIds.isEmpty()) {
            return List.of();
        }

        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(submittedIds));
        if (distinctIds.size() > 3) {
            throw new MyPageException(
                    "desiredJobIds",
                    "희망 직무는 최대 3개까지 선택할 수 있습니다."
            );
        }

        long activeCount = jobCategoryRepository
                .findAllByJobCategoryIdInAndIsActiveTrueAndDepth(distinctIds, 2)
                .size();
        if (activeCount != distinctIds.size()) {
            throw new MyPageException(
                    "desiredJobIds",
                    "선택할 수 없는 희망 직무가 포함되어 있습니다."
            );
        }
        return distinctIds;
    }

    private void replaceDesiredJobs(
            Long userId,
            List<Long> desiredJobIds,
            LocalDateTime now
    ) {
        userDesiredJobRepository.deleteByIdUserId(userId);
        userDesiredJobRepository.flush();

        List<UserDesiredJob> desiredJobs = new ArrayList<>();
        for (int index = 0; index < desiredJobIds.size(); index++) {
            desiredJobs.add(UserDesiredJob.builder()
                    .id(UserDesiredJobId.builder()
                            .userId(userId)
                            .jobCategoryId(desiredJobIds.get(index))
                            .build())
                    .displayOrder(index)
                    .createdAt(now)
                    .build());
        }
        userDesiredJobRepository.saveAll(desiredJobs);
    }

    @Transactional
    public void changePassword(Long userId, MyPasswordChangeRequest request) {
        User user = getUser(userId);
        String currentPassword = request.currentPassword();
        String newPassword = request.newPassword();

        if (currentPassword == null || currentPassword.isBlank()) {
            throw new MyAccountException("현재 비밀번호를 입력해주세요.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new MyAccountException("현재 비밀번호가 일치하지 않습니다.");
        }
        if (newPassword == null || !PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new MyAccountException(
                    "비밀번호는 8~64자의 영문, 숫자, 특수문자를 포함해야 합니다."
            );
        }
        if (!newPassword.equals(request.newPasswordConfirm())) {
            throw new MyAccountException("새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new MyAccountException("현재 비밀번호와 다른 비밀번호를 입력해주세요.");
        }

        LocalDateTime now = LocalDateTime.now();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(now);
        user.setUpdatedAt(now);
    }

    @Transactional
    public void withdraw(Long userId, String currentPassword) {
        User user = getUser(userId);
        if (WITHDRAWN_STATUS.equals(user.getAccountStatus())) {
            throw new MyAccountException("이미 탈퇴 처리된 회원입니다.");
        }
        if (currentPassword == null
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new MyAccountException("현재 비밀번호가 일치하지 않습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        user.setAccountStatus(WITHDRAWN_STATUS);
        user.setWithdrawnAt(now);
        user.setUpdatedAt(now);
    }

    private void validateProfileImage(MultipartFile profileImage) {
        if (!ALLOWED_IMAGE_TYPES.contains(profileImage.getContentType())) {
            throw new MyPageException(
                    "profileImage",
                    "JPG 또는 PNG 이미지만 등록할 수 있습니다."
            );
        }
        if (profileImage.getSize() > MAXIMUM_IMAGE_SIZE) {
            throw new MyPageException(
                    "profileImage",
                    "프로필 이미지는 5MB 이하로 등록해주세요."
            );
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
