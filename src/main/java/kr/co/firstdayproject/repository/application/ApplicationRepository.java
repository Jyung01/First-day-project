package kr.co.firstdayproject.repository.application;

import kr.co.firstdayproject.dto.my.ApplicationSummaryProjection;
import kr.co.firstdayproject.dto.my.MyApplicationListProjection;
import kr.co.firstdayproject.dto.my.MyApplicationDetailProjection;
import kr.co.firstdayproject.dto.corp.applicant.CorpApplicantJobOption;
import kr.co.firstdayproject.dto.corp.applicant.CorpApplicantDetailProjection;
import kr.co.firstdayproject.dto.corp.applicant.CorpApplicantListProjection;
import kr.co.firstdayproject.dto.corp.dashboard.CorpDashboardApplicantProjection;
import kr.co.firstdayproject.entity.application.Application;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByApplicantUserIdAndJobPostingIdAndCurrentStatusNot(
            Long applicantUserId,
            Long jobPostingId,
            String excludedStatus
    );

    Optional<Application> findByApplicationIdAndApplicantUserId(
            Long applicationId,
            Long applicantUserId
    );

    long countByJobPostingIdAndCurrentStatusNot(
        Long jobPostingId,
        String excludedStatus
    );

    long countByJobPostingIdAndCurrentStatusNotIn(
            Long jobPostingId,
            Collection<String> excludedStatuses
    );

    long countByApplicantUserId(Long userId);

    long countByApplicantUserIdAndCurrentStatusIn(
        Long userId,
        Collection<String> statuses
    );

    @Query("""
            select count(a)
              from Application a
              join JobPosting jp on jp.jobPostingId = a.jobPostingId
             where jp.companyId = :companyId
               and a.appliedAt >= :from
               and a.appliedAt < :to
            """)
    long countCompanyApplicantsBetween(
            @Param("companyId") Long companyId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            select count(a)
              from Application a
              join JobPosting jp on jp.jobPostingId = a.jobPostingId
             where jp.companyId = :companyId
               and a.currentStatus = :status
            """)
    long countCompanyApplicantsByStatus(
            @Param("companyId") Long companyId,
            @Param("status") String status
    );

    @Query("""
            select a.applicationId as applicationId,
                   u.name as applicantName,
                   jp.title as jobTitle,
                   a.appliedAt as appliedAt,
                   a.currentStatus as currentStatus
              from Application a
              join JobPosting jp on jp.jobPostingId = a.jobPostingId
              join User u on u.userId = a.applicantUserId
             where jp.companyId = :companyId
             order by a.appliedAt desc, a.applicationId desc
            """)
    List<CorpDashboardApplicantProjection> findRecentCompanyApplicants(
            @Param("companyId") Long companyId,
            Pageable pageable
    );

    @Query("""
            select a.applicationId as applicationId,
                   c.companyName as companyName,
                   c.logoUrl as companyLogoUrl,
                   c.companyStatus as companyStatus,
                   jp.title as jobTitle,
                   a.currentStatus as currentStatus,
                   a.appliedAt as appliedAt,
                   coalesce(max(h.changedAt), a.updatedAt)
                       as latestChangedAt
              from Application a
              join JobPosting jp
                on jp.jobPostingId = a.jobPostingId
              join Company c
                on c.companyId = jp.companyId
              left join ApplicationStatusHistory h
                on h.applicationId = a.applicationId
             where a.applicantUserId = :userId
               and a.currentStatus in :statuses
             group by a.applicationId,
                      c.companyName,
                      c.logoUrl,
                      c.companyStatus,
                      jp.title,
                      a.currentStatus,
                      a.appliedAt,
                      a.updatedAt
             order by a.appliedAt desc,
                      a.applicationId desc
            """)
    List<MyApplicationListProjection> findMyApplicationList(
            @Param("userId") Long userId,
            @Param("statuses") Collection<String> statuses,
            Pageable pageable
    );

    @Query("""
            select a.applicationId as applicationId,
                   u.name as applicantName,
                   jp.title as jobTitle,
                   a.currentStatus as currentStatus,
                   a.appliedAt as appliedAt,
                   a.resumeSnapshotJson as resumeSnapshotJson
              from Application a
              join JobPosting jp
                on jp.jobPostingId = a.jobPostingId
              join User u
                on u.userId = a.applicantUserId
             where jp.companyId = :companyId
               and (:jobPostingId is null
                    or jp.jobPostingId = :jobPostingId)
               and (:status is null
                    or a.currentStatus = :status)
               and (:keyword is null
                    or lower(u.name) like lower(concat('%', :keyword, '%')))
             order by a.appliedAt desc,
                      a.applicationId desc
            """)
    List<CorpApplicantListProjection> findCorpApplicantList(
            @Param("companyId") Long companyId,
            @Param("jobPostingId") Long jobPostingId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            select count(a)
              from Application a
              join JobPosting jp
                on jp.jobPostingId = a.jobPostingId
              join User u
                on u.userId = a.applicantUserId
             where jp.companyId = :companyId
               and (:jobPostingId is null
                    or jp.jobPostingId = :jobPostingId)
               and (:status is null
                    or a.currentStatus = :status)
               and (:keyword is null
                    or lower(u.name) like lower(concat('%', :keyword, '%')))
            """)
    long countCorpApplicantList(
            @Param("companyId") Long companyId,
            @Param("jobPostingId") Long jobPostingId,
            @Param("status") String status,
            @Param("keyword") String keyword
    );

    @Query("""
            select count(a)
              from Application a
              join JobPosting jp
                on jp.jobPostingId = a.jobPostingId
             where jp.companyId = :companyId
            """)
    long countCorpApplicants(@Param("companyId") Long companyId);

    @Query("""
            select count(a)
              from Application a
              join JobPosting jp
                on jp.jobPostingId = a.jobPostingId
             where jp.companyId = :companyId
               and a.currentStatus = '지원완료'
            """)
    long countCorpWaitingApplicants(
            @Param("companyId") Long companyId
    );

    @Query("""
            select count(a)
              from Application a
              join JobPosting jp
                on jp.jobPostingId = a.jobPostingId
             where jp.companyId = :companyId
               and a.appliedAt >= :startAt
               and a.appliedAt < :endAt
            """)
    long countCorpApplicantsAppliedBetween(
            @Param("companyId") Long companyId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Query("""
            select distinct jp.jobPostingId as jobPostingId,
                            jp.title as title
              from Application a
              join JobPosting jp
                on jp.jobPostingId = a.jobPostingId
             where jp.companyId = :companyId
             order by jp.title,
                      jp.jobPostingId
            """)
    List<CorpApplicantJobOption> findCorpApplicantJobOptions(
            @Param("companyId") Long companyId
    );

    @Query("""
            select a.applicationId as applicationId,
                   a.applicantUserId as applicantUserId,
                   u.name as applicantName,
                   u.email as applicantEmail,
                   u.phone as applicantPhone,
                   jp.title as jobTitle,
                   a.currentStatus as currentStatus,
                   a.appliedAt as appliedAt,
                   a.resumeSnapshotJson as resumeSnapshotJson,
                   a.coverLetterSnapshotJson as coverLetterSnapshotJson
              from Application a
              join JobPosting jp
                on jp.jobPostingId = a.jobPostingId
              join User u
                on u.userId = a.applicantUserId
             where a.applicationId = :applicationId
               and jp.companyId = :companyId
            """)
    Optional<CorpApplicantDetailProjection> findCorpApplicantDetail(
            @Param("companyId") Long companyId,
            @Param("applicationId") Long applicationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a
              from Application a
              join JobPosting jp
                on jp.jobPostingId = a.jobPostingId
             where a.applicationId = :applicationId
               and jp.companyId = :companyId
            """)
    Optional<Application> findCorpApplicationForUpdate(
            @Param("companyId") Long companyId,
            @Param("applicationId") Long applicationId
    );

    @Query("""
            select a.applicationId as applicationId,
                   a.jobPostingId as jobPostingId,
                   c.companyName as companyName,
                   c.logoUrl as companyLogoUrl,
                   c.companyStatus as companyStatus,
                   jp.title as jobTitle,
                   a.currentStatus as currentStatus,
                   a.appliedAt as appliedAt,
                   a.resumeSnapshotJson as resumeSnapshotJson,
                   a.coverLetterSnapshotJson as coverLetterSnapshotJson
              from Application a
              join JobPosting jp on jp.jobPostingId = a.jobPostingId
              join Company c on c.companyId = jp.companyId
             where a.applicationId = :applicationId
               and a.applicantUserId = :userId
            """)
    Optional<MyApplicationDetailProjection> findMyApplicationDetail(
            @Param("userId") Long userId,
            @Param("applicationId") Long applicationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a
              from Application a
             where a.applicationId = :applicationId
               and a.applicantUserId = :userId
            """)
    Optional<Application> findMyApplicationForUpdate(
            @Param("userId") Long userId,
            @Param("applicationId") Long applicationId
    );

    @Query("""
            select a.applicationId as applicationId,
                   c.companyName as companyName,
                   jp.title as jobTitle,
                   a.currentStatus as currentStatus,
                   a.appliedAt as appliedAt
              from Application a
              join JobPosting jp on jp.jobPostingId = a.jobPostingId
              join Company c on c.companyId = jp.companyId
             where a.applicantUserId = :userId
             order by a.appliedAt desc
            """)
    List<ApplicationSummaryProjection> findRecentByApplicantUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query(value = """
            SELECT COUNT(*)
              FROM applications a
              JOIN job_postings jp
                ON jp.job_posting_id = a.job_posting_id
             WHERE jp.company_id = :companyId
               AND jp.status = '모집중'
               AND a.current_status IN (:statuses)
            """, nativeQuery = true)
    long countActiveApplicantsOfRecruitingCompany(
            @Param("companyId") Long companyId,
            @Param("statuses") Collection<String> statuses
    );

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE applications a
              JOIN job_postings jp
                ON jp.job_posting_id = a.job_posting_id
               SET a.current_status = '채용종료',
                   a.termination_reason = '기업탈퇴',
                   a.terminated_at = :terminatedAt,
                   a.updated_at = :terminatedAt
             WHERE jp.company_id = :companyId
               AND jp.status = '모집중'
               AND a.current_status IN (:statuses)
            """, nativeQuery = true)
    int terminateActiveApplicationsForCompanyWithdrawal(
            @Param("companyId") Long companyId,
            @Param("statuses") Collection<String> statuses,
            @Param("terminatedAt") LocalDateTime terminatedAt
    );
}
