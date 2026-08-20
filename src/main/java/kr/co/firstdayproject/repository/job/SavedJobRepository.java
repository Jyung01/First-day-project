package kr.co.firstdayproject.repository.job;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import kr.co.firstdayproject.entity.job.SavedJob;
import kr.co.firstdayproject.entity.job.SavedJobId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, SavedJobId> {

    @Query("""
            select count(savedJob)
              from SavedJob savedJob
              join JobPosting posting
                on posting.jobPostingId = savedJob.id.jobPostingId
              join Company company
                on company.companyId = posting.companyId
             where savedJob.id.userId = :userId
               and posting.status not in ('임시저장', '숨김', '재검토요청', '삭제')
               and company.approvalStatus = '승인'
               and company.companyStatus = '정상'
            """)
    long countVisibleByUserId(@Param("userId") Long userId);

    @Query("""
            select count(s)
              from SavedJob s
              join JobPosting jp on jp.jobPostingId = s.id.jobPostingId
              join Company company on company.companyId = jp.companyId
             where s.id.userId = :userId
               and jp.status = '모집중'
               and company.approvalStatus = '승인'
               and company.companyStatus = '정상'
               and jp.applyEndAt is not null
               and jp.applyEndAt between :now and :deadline
            """)
    long countDeadlineSoon(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            @Param("deadline") LocalDateTime deadline
    );

    boolean existsByIdUserIdAndIdJobPostingId(
            Long userId,
            Long jobPostingId
    );

    void deleteByIdUserIdAndIdJobPostingId(
            Long userId,
            Long jobPostingId
    );

    @Query("""
            select savedJob.id.jobPostingId
              from SavedJob savedJob
             where savedJob.id.userId = :userId
               and savedJob.id.jobPostingId in :jobPostingIds
            """)
    List<Long> findSavedJobPostingIds(
            @Param("userId") Long userId,
            @Param("jobPostingIds") Collection<Long> jobPostingIds
    );
}
