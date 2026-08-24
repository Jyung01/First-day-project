package kr.co.firstdayproject.service.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import kr.co.firstdayproject.entity.job.SavedJob;
import kr.co.firstdayproject.entity.job.SavedJobId;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.job.SavedJobRepository;
import kr.co.firstdayproject.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class SavedJobServiceTest {

    @Mock
    private SavedJobRepository savedJobRepository;

    @Mock
    private JobPostingRepository jobPostingRepository;

    @InjectMocks
    private SavedJobService savedJobService;

    @Test
    void 존재하지_않는_공고를_등록하려_하면_ResourceNotFound() {
        Authentication authentication = personalAuthentication(7L);
        SavedJobId savedJobId = new SavedJobId(7L, 999L);

        when(savedJobRepository.existsById(savedJobId)).thenReturn(false);
        when(jobPostingRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() ->
                savedJobService.toggleSavedJob(999L, authentication)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("조회할 수 없는 채용공고입니다.");

        verify(savedJobRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 등록되지_않은_공고를_누르면_등록된다() {
        Authentication authentication = personalAuthentication(7L);
        SavedJobId savedJobId = new SavedJobId(7L, 10L);

        when(savedJobRepository.existsById(savedJobId)).thenReturn(false);
        when(jobPostingRepository.existsById(10L)).thenReturn(true);

        boolean bookmarked =
                savedJobService.toggleSavedJob(10L, authentication);

        assertThat(bookmarked).isTrue();
        verify(savedJobRepository).save(org.mockito.ArgumentMatchers.any(SavedJob.class));
    }

    /**
     * 해제는 이미 등록된 행을 지우는 것이라 공고 존재 확인이 필요 없다.
     * 공고가 삭제된 뒤에도 사용자가 관심공고를 해제할 수 있어야 한다.
     */
    @Test
    void 이미_등록된_공고를_누르면_공고_확인_없이_해제된다() {
        Authentication authentication = personalAuthentication(7L);
        SavedJobId savedJobId = new SavedJobId(7L, 10L);

        when(savedJobRepository.existsById(savedJobId)).thenReturn(true);

        boolean bookmarked =
                savedJobService.toggleSavedJob(10L, authentication);

        assertThat(bookmarked).isFalse();
        verify(savedJobRepository).deleteById(savedJobId);
        verify(jobPostingRepository, never()).existsById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void 개인회원이_아니면_AccessDenied() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);

        assertThatThrownBy(() ->
                savedJobService.toggleSavedJob(10L, authentication)
        )
                .isInstanceOf(AccessDeniedException.class);

        verify(jobPostingRepository, never()).existsById(org.mockito.ArgumentMatchers.anyLong());
    }

    private Authentication personalAuthentication(Long userId) {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_PERSONAL")
        )).when(authentication).getAuthorities();
        when(userDetails.getUserId()).thenReturn(userId);

        return authentication;
    }
}
