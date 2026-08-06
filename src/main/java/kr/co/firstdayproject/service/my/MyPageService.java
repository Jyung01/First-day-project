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
import kr.co.firstdayproject.entity.coverletter.CoverLetter;
import kr.co.firstdayproject.entity.job.JobCategory;
import kr.co.firstdayproject.entity.job.UserDesiredJob;
import kr.co.firstdayproject.entity.job.UserDesiredJobId;
import kr.co.firstdayproject.entity.member.PersonalProfile;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.entity.resume.Resume;
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

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new MyPageException(null, "회원 정보를 찾을 수 없습니다."));
    }

    public PersonalProfile getProfile(Long userId) {
        return personalProfileRepository.findById(userId)
                .orElseGet(() -> PersonalProfile.builder().userId(userId).build());
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
            try {
                profile.setProfileImageUrl(
                        awsS3Service.upload(profileImage, "personal_profile")
                );
            } catch (IOException | RuntimeException exception) {
                throw new MyPageException(
                        "profileImage",
                        "프로필 이미지를 업로드하지 못했습니다. 다시 시도해주세요."
                );
            }
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
