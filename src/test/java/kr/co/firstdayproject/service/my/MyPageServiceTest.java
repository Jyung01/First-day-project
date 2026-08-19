package kr.co.firstdayproject.service.my;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import kr.co.firstdayproject.dao.my.MyPageDao;
import kr.co.firstdayproject.entity.member.PersonalProfile;
import kr.co.firstdayproject.entity.member.User;
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
import kr.co.firstdayproject.service.ai.UserProfileEmbeddingDeleteEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

class MyPageServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private ApplicationRepository applicationRepository;
    private ResumeRepository resumeRepository;
    private CoverLetterRepository coverLetterRepository;
    private PersonalProfileRepository personalProfileRepository;
    private AwsS3Service awsS3Service;
    private ApplicationEventPublisher eventPublisher;
    private MyPageService myPageService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        applicationRepository = mock(ApplicationRepository.class);
        resumeRepository = mock(ResumeRepository.class);
        coverLetterRepository = mock(CoverLetterRepository.class);
        personalProfileRepository = mock(PersonalProfileRepository.class);
        awsS3Service = mock(AwsS3Service.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        myPageService = new MyPageService(
                userRepository,
                personalProfileRepository,
                mock(JobCategoryRepository.class),
                mock(UserDesiredJobRepository.class),
                applicationRepository,
                resumeRepository,
                coverLetterRepository,
                mock(CoverLetterAiReviewRepository.class),
                mock(SavedJobRepository.class),
                mock(SavedCompanyRepository.class),
                awsS3Service,
                passwordEncoder,
                mock(MyPageDao.class),
                eventPublisher
        );
    }

    private PersonalProfile profileWithImage() {
        return PersonalProfile.builder()
                .userId(42L)
                .postalCode("06234")
                .addressLine1("서울시 강남구")
                .addressLine2("101동 202호")
                .profileImageUrl("personal_profile/abc.png")
                .build();
    }

    private User activeUser() {
        return User.builder()
                .userId(42L)
                .loginId("hong123")
                .passwordHash("encoded")
                .name("홍길동")
                .email("hong@example.com")
                .phone("010-1234-5678")
                .userType("개인")
                .accountStatus("정상")
                .build();
    }

    @Test
    void masksNameAndPhoneOnWithdrawal() {
        User user = activeUser();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw1234!", "encoded")).thenReturn(true);

        myPageService.withdraw(42L, "pw1234!");

        assertThat(user.getName()).isEqualTo("탈퇴한 회원");
        assertThat(user.getPhone()).isNull();
    }

    @Test
    void keepsLoginIdAndEmailOnWithdrawalForDuplicateSignupBlocking() {
        User user = activeUser();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw1234!", "encoded")).thenReturn(true);

        myPageService.withdraw(42L, "pw1234!");

        assertThat(user.getLoginId()).isEqualTo("hong123");
        assertThat(user.getEmail()).isEqualTo("hong@example.com");
    }

    @Test
    void marksAccountWithdrawn() {
        User user = activeUser();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw1234!", "encoded")).thenReturn(true);

        myPageService.withdraw(42L, "pw1234!");

        assertThat(user.getAccountStatus()).isEqualTo("탈퇴");
        assertThat(user.getWithdrawnAt()).isNotNull();
    }

    @Test
    void keepsPersonalDataWhenPasswordDoesNotMatch() {
        User user = activeUser();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> myPageService.withdraw(42L, "wrong"))
                .isInstanceOf(MyAccountException.class)
                .hasMessageContaining("현재 비밀번호가 일치하지 않습니다");

        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.getPhone()).isEqualTo("010-1234-5678");
        assertThat(user.getAccountStatus()).isEqualTo("정상");
    }

    @Test
    void terminatesInProgressApplicationsOnWithdrawal() {
        User user = activeUser();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw1234!", "encoded")).thenReturn(true);

        myPageService.withdraw(42L, "pw1234!");

        ArgumentCaptor<Collection<String>> statuses =
                ArgumentCaptor.forClass(Collection.class);
        verify(applicationRepository).terminateActiveApplicationsForMemberWithdrawal(
                eq(42L),
                statuses.capture(),
                any(LocalDateTime.class)
        );
        assertThat(statuses.getValue()).containsExactlyInAnyOrder(
                "지원완료", "서류검토중", "서류합격", "면접예정", "면접완료"
        );
    }

    @Test
    void keepsFinalPassAndHiredApplicationsOnWithdrawal() {
        User user = activeUser();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw1234!", "encoded")).thenReturn(true);

        myPageService.withdraw(42L, "pw1234!");

        ArgumentCaptor<Collection<String>> statuses =
                ArgumentCaptor.forClass(Collection.class);
        verify(applicationRepository).terminateActiveApplicationsForMemberWithdrawal(
                eq(42L),
                statuses.capture(),
                any(LocalDateTime.class)
        );
        assertThat(statuses.getValue())
                .doesNotContain("최종합격", "입사완료", "불합격", "지원취소");
    }

    @Test
    void recordsStatusHistoryBeforeTerminatingApplications() {
        User user = activeUser();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw1234!", "encoded")).thenReturn(true);

        myPageService.withdraw(42L, "pw1234!");

        // 이력이 먼저 남아야 from_status에 종료 직전 단계가 기록된다.
        InOrder inOrder = inOrder(applicationRepository);
        inOrder.verify(applicationRepository).recordMemberWithdrawalStatusHistory(
                eq(42L), anyCollection(), any(LocalDateTime.class)
        );
        inOrder.verify(applicationRepository)
                .terminateActiveApplicationsForMemberWithdrawal(
                        eq(42L), anyCollection(), any(LocalDateTime.class)
                );
    }

    @Test
    void doesNotTouchApplicationsWhenPasswordDoesNotMatch() {
        User user = activeUser();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> myPageService.withdraw(42L, "wrong"))
                .isInstanceOf(MyAccountException.class);

        verifyNoInteractions(applicationRepository);
    }

    @Test
    void hardDeletesResumesAndCoverLettersOnWithdrawal() {
        User user = activeUser();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw1234!", "encoded")).thenReturn(true);

        myPageService.withdraw(42L, "pw1234!");

        verify(resumeRepository).deleteAllByUserId(42L);
        verify(coverLetterRepository).deleteAllByUserId(42L);
    }

    @Test
    void publishesProfileEmbeddingDeleteEventOnWithdrawal() {
        User user = activeUser();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw1234!", "encoded")).thenReturn(true);

        myPageService.withdraw(42L, "pw1234!");

        verify(eventPublisher).publishEvent(new UserProfileEmbeddingDeleteEvent(42L));
    }

    @Test
    void clearsAddressAndProfileImageOnWithdrawal() {
        User user = activeUser();
        PersonalProfile profile = profileWithImage();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(personalProfileRepository.findById(42L)).thenReturn(Optional.of(profile));
        when(passwordEncoder.matches("pw1234!", "encoded")).thenReturn(true);

        myPageService.withdraw(42L, "pw1234!");

        assertThat(profile.getPostalCode()).isNull();
        assertThat(profile.getAddressLine1()).isNull();
        assertThat(profile.getAddressLine2()).isNull();
        assertThat(profile.getProfileImageUrl()).isNull();
        // 커밋된 뒤에 지워야 롤백 시 파일이 사라지지 않는다.
        verify(awsS3Service).synchronizePrivateReplacement("personal_profile/abc.png", null);
    }

    @Test
    void survivesMissingPersonalProfileOnWithdrawal() {
        User user = activeUser();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(personalProfileRepository.findById(42L)).thenReturn(Optional.empty());
        when(passwordEncoder.matches("pw1234!", "encoded")).thenReturn(true);

        myPageService.withdraw(42L, "pw1234!");

        assertThat(user.getAccountStatus()).isEqualTo("탈퇴");
        verifyNoInteractions(awsS3Service);
    }

    @Test
    void keepsDocumentsWhenPasswordDoesNotMatch() {
        User user = activeUser();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> myPageService.withdraw(42L, "wrong"))
                .isInstanceOf(MyAccountException.class);

        verifyNoInteractions(resumeRepository, coverLetterRepository, awsS3Service, eventPublisher);
    }

    @Test
    void rejectsAlreadyWithdrawnAccount() {
        User user = activeUser();
        user.setAccountStatus("탈퇴");
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> myPageService.withdraw(42L, "pw1234!"))
                .isInstanceOf(MyAccountException.class)
                .hasMessageContaining("이미 탈퇴 처리된 회원입니다");
    }
}
