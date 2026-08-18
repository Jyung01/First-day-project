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

    long countByIdUserId(Long userId);

    @Query("""
            select count(s)
              from SavedJob s
              join JobPosting jp on jp.jobPostingId = s.id.jobPostingId
             where s.id.userId = :userId
               and jp.status = '모집중'
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
