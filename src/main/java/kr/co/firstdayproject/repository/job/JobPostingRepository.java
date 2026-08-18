package kr.co.firstdayproject.repository.job;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import kr.co.firstdayproject.dto.admin.job.AdminJobListItem;
import kr.co.firstdayproject.dto.job.JobListQueryItem;
import kr.co.firstdayproject.dto.job.MainJobListItem;
import kr.co.firstdayproject.entity.job.JobPosting;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    long countByJobCategoryId(Long jobCategoryId);

    long countByCompanyIdAndStatus(
            Long companyId,
            String status
    );

    long countByCompanyIdAndStatusIn(
            Long companyId,
            Collection<String> statuses
    );

    @Query("""
            select count(posting)
              from JobPosting posting
             where posting.companyId = :companyId
               and posting.status = '모집중'
               and posting.applyEndAt >= :now
               and posting.applyEndAt < :deadline
            """)
    long countCompanyJobsClosingSoon(
            @Param("companyId") Long companyId,
            @Param("now") LocalDateTime now,
            @Param("deadline") LocalDateTime deadline
    );

    @Query("""
            select coalesce(sum(posting.viewCount), 0)
              from JobPosting posting
             where posting.companyId = :companyId
            """)
    long sumViewCountByCompanyId(@Param("companyId") Long companyId);

    List<JobPosting> findByCompanyIdOrderByUpdatedAtDescJobPostingIdDesc(
            Long companyId,
            Pageable pageable
    );

    @Query("""
            select posting
              from JobPosting posting
             where posting.companyId = :companyId
               and posting.status <> '삭제'
             order by posting.updatedAt desc,
                      posting.jobPostingId desc
            """)
    List<JobPosting> findDashboardJobs(
            @Param("companyId") Long companyId,
            Pageable pageable
    );

    List<JobPosting> findByStatusOrderByPublishedAtDescJobPostingIdDesc(
            String status,
            Pageable pageable
    );

    Optional<JobPosting> findByJobPostingIdAndCompanyId(
        Long jobPostingId,
        Long companyId
    );

    Optional<JobPosting> findByJobPostingIdAndStatusIn(
        Long jobPostingId,
        Collection<String> statuses
    );

    // 일반회원 메인: 모집 중 채용공고 최신순
    @Query("""
    SELECT new kr.co.firstdayproject.dto.job.MainJobListItem(
        posting.jobPostingId,
        company.logoUrl,
        company.companyName,
        posting.title,
        posting.workRegion,
        posting.careerType,
        posting.employmentType,
        category.categoryName,
        posting.viewCount
    )
    FROM JobPosting posting
    JOIN Company company
      ON company.companyId = posting.companyId
    LEFT JOIN JobCategory category
      ON category.jobCategoryId = posting.jobCategoryId
    WHERE posting.status = '모집중'
      AND company.approvalStatus = '승인'
      AND company.companyStatus = '정상'
    ORDER BY posting.publishedAt DESC,
             posting.jobPostingId DESC
    """)
    List<MainJobListItem> findLatestRecruitingJobPostings(
            Pageable pageable
    );

    // 일반회원 메인: 모집 중 채용공고 인기순
    @Query("""
    SELECT new kr.co.firstdayproject.dto.job.MainJobListItem(
        posting.jobPostingId,
        company.logoUrl,
        company.companyName,
        posting.title,
        posting.workRegion,
        posting.careerType,
        posting.employmentType,
        category.categoryName,
        posting.viewCount
    )
    FROM JobPosting posting
    JOIN Company company
      ON company.companyId = posting.companyId
    LEFT JOIN JobCategory category
      ON category.jobCategoryId = posting.jobCategoryId
    WHERE posting.status = '모집중'
      AND company.approvalStatus = '승인'
      AND company.companyStatus = '정상'
    ORDER BY posting.viewCount DESC,
             posting.publishedAt DESC,
             posting.jobPostingId DESC
    """)
    List<MainJobListItem> findPopularRecruitingJobPostings(
            Pageable pageable
    );

    @Query("""
    SELECT new kr.co.firstdayproject.dto.job.MainJobListItem(
        posting.jobPostingId, company.logoUrl, company.companyName, posting.title,
        posting.workRegion, posting.careerType, posting.employmentType,
        category.categoryName, posting.viewCount
    )
    FROM JobPosting posting
    JOIN Company company ON company.companyId = posting.companyId
    LEFT JOIN JobCategory category ON category.jobCategoryId = posting.jobCategoryId
    WHERE posting.status = '모집중'
      AND company.approvalStatus = '승인'
      AND company.companyStatus = '정상'
      AND posting.jobCategoryId IN :categoryIds
    ORDER BY posting.publishedAt DESC, posting.viewCount DESC, posting.jobPostingId DESC
    """)
    List<MainJobListItem> findRecruitingJobPostingsByCategoryIds(
            @Param("categoryIds") List<Long> categoryIds,
            Pageable pageable
    );

    @Query(
            value = """
    SELECT new kr.co.firstdayproject.dto.job.JobListQueryItem(
        posting.jobPostingId,
        company.logoUrl,
        company.companyName,
        posting.title,
        posting.workRegion,
        posting.careerType,
        posting.employmentType,
        category.categoryName,
        posting.viewCount,
        COUNT(application.applicationId),
        posting.publishedAt,
        posting.applyEndAt
    )
    FROM JobPosting posting
    JOIN Company company
      ON company.companyId = posting.companyId
    LEFT JOIN JobCategory category
      ON category.jobCategoryId = posting.jobCategoryId
    LEFT JOIN Application application
      ON application.jobPostingId = posting.jobPostingId
     AND application.currentStatus <> '지원취소'
     AND application.currentStatus <> '채용종료'
    WHERE posting.status = '모집중'
      AND company.approvalStatus = '승인'
      AND company.companyStatus = '정상'
      AND (
            :keyword IS NULL
            OR LOWER(posting.title)
                LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(company.companyName)
                LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
      AND (
            :categoryFilter = false
            OR posting.jobCategoryId IN :categoryIds
      )
      AND (
            :parentCategoryId IS NULL
            OR category.parentId = :parentCategoryId
      )
      AND (
            :regionFilter = false
            OR SUBSTRING(
                posting.workRegion,
                1,
                LOCATE(' ', CONCAT(posting.workRegion, ' ')) - 1
            ) IN :regions
      )
      AND (
            :careerFilter = false
            OR posting.careerType IN :careers
      )
      AND (
            :educationFilter = false
            OR posting.educationLevel IN :educations
      )
      AND (
            :skillFilter = false
            OR EXISTS (
                SELECT postingSkill.id.skillId
                FROM JobPostingSkill postingSkill
                WHERE postingSkill.id.jobPostingId = posting.jobPostingId
                  AND postingSkill.id.skillId IN :skillIds
            )
      )
    GROUP BY
        posting.jobPostingId,
        company.logoUrl,
        company.companyName,
        posting.title,
        posting.workRegion,
        posting.careerType,
        posting.employmentType,
        category.categoryName,
        posting.viewCount,
        posting.publishedAt,
        posting.applyEndAt
    """,
            countQuery = """
    SELECT COUNT(posting)
    FROM JobPosting posting
    JOIN Company company
      ON company.companyId = posting.companyId
    WHERE posting.status = '모집중'
      AND company.approvalStatus = '승인'
      AND company.companyStatus = '정상'
      AND (
            :keyword IS NULL
            OR LOWER(posting.title)
                LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(company.companyName)
                LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
      AND (
            :categoryFilter = false
            OR posting.jobCategoryId IN :categoryIds
      )
      AND (
            :parentCategoryId IS NULL
            OR EXISTS (
                SELECT child.jobCategoryId
                FROM JobCategory child
                WHERE child.jobCategoryId = posting.jobCategoryId
                  AND child.parentId = :parentCategoryId
            )
      )
      AND (
            :regionFilter = false
            OR SUBSTRING(
                posting.workRegion,
                1,
                LOCATE(' ', CONCAT(posting.workRegion, ' ')) - 1
            ) IN :regions
      )
      AND (
            :careerFilter = false
            OR posting.careerType IN :careers
      )
      AND (
            :educationFilter = false
            OR posting.educationLevel IN :educations
      )
      AND (
            :skillFilter = false
            OR EXISTS (
                SELECT postingSkill.id.skillId
                FROM JobPostingSkill postingSkill
                WHERE postingSkill.id.jobPostingId = posting.jobPostingId
                  AND postingSkill.id.skillId IN :skillIds
            )
      )
    """
    )
    Page<JobListQueryItem> findRecruitingJobPostings(
            @Param("keyword") String keyword,
            @Param("parentCategoryId") Long parentCategoryId,
            @Param("categoryFilter") boolean categoryFilter,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("regionFilter") boolean regionFilter,
            @Param("regions") List<String> regions,
            @Param("careerFilter") boolean careerFilter,
            @Param("careers") List<String> careers,
            @Param("educationFilter") boolean educationFilter,
            @Param("educations") List<String> educations,
            @Param("skillFilter") boolean skillFilter,
            @Param("skillIds") List<Long> skillIds,
            Pageable pageable
    );

    @Query(
        value = "SELECT posting FROM JobPosting posting "
            + "WHERE posting.companyId = :companyId "
            + "AND posting.status <> '삭제' "
            + "AND (:status IS NULL OR posting.status = :status) "
            + "AND (:keyword IS NULL "
            + "OR LOWER(posting.title) "
            + "LIKE LOWER(CONCAT('%', :keyword, '%')))",
        countQuery = "SELECT COUNT(posting) FROM JobPosting posting "
            + "WHERE posting.companyId = :companyId "
            + "AND posting.status <> '삭제' "
            + "AND (:status IS NULL OR posting.status = :status) "
            + "AND (:keyword IS NULL "
            + "OR LOWER(posting.title) "
            + "LIKE LOWER(CONCAT('%', :keyword, '%')))"
    )
    Page<JobPosting> findCorpJobPostings(
        @Param("companyId") Long companyId,
        @Param("status") String status,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    long countByCompanyIdAndStatusNot(Long companyId, String excludedStatus);

    @Query(
        value = "SELECT new kr.co.firstdayproject.dto.admin.job."
            + "AdminJobListItem("
            + "posting.jobPostingId, company.companyName, "
            + "company.companyStatus, posting.title, "
            + "category.categoryName, posting.createdAt, "
            + "posting.applyEndAt, posting.status) "
            + "FROM JobPosting posting "
            + "JOIN Company company ON company.companyId = posting.companyId "
            + "LEFT JOIN JobCategory category "
            + "ON category.jobCategoryId = posting.jobCategoryId "
            + "WHERE posting.status IN :statuses "
            + "AND (:status IS NULL OR posting.status = :status) "
            + "AND (:categoryId IS NULL "
            + "OR posting.jobCategoryId = :categoryId) "
            + "AND (:parentCategoryId IS NULL "
            + "OR category.parentId = :parentCategoryId) "
            + "AND (:keyword IS NULL "
            + "OR LOWER(posting.title) "
            + "LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(company.companyName) "
            + "LIKE LOWER(CONCAT('%', :keyword, '%')))",
        countQuery = "SELECT COUNT(posting) FROM JobPosting posting "
            + "JOIN Company company ON company.companyId = posting.companyId "
            + "WHERE posting.status IN :statuses "
            + "AND (:status IS NULL OR posting.status = :status) "
            + "AND (:categoryId IS NULL "
            + "OR posting.jobCategoryId = :categoryId) "
            + "AND (:parentCategoryId IS NULL OR EXISTS ("
            + "SELECT child.jobCategoryId FROM JobCategory child "
            + "WHERE child.jobCategoryId = posting.jobCategoryId "
            + "AND child.parentId = :parentCategoryId)) "
            + "AND (:keyword IS NULL "
            + "OR LOWER(posting.title) "
            + "LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(company.companyName) "
            + "LIKE LOWER(CONCAT('%', :keyword, '%')))"
    )
    Page<AdminJobListItem> findAdminJobPostings(
        @Param("statuses") Collection<String> statuses,
        @Param("status") String status,
        @Param("parentCategoryId") Long parentCategoryId,
        @Param("categoryId") Long categoryId,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    long countByStatus(String status);

    long countByStatusIn(Collection<String> statuses);

    long countByStatusInAndCreatedAtBetween(
        Collection<String> statuses,
        LocalDateTime start,
        LocalDateTime end
    );

    long countByStatusAndPublishedAtBetween(
        String status,
        LocalDateTime start,
        LocalDateTime end
    );

    long countByStatusAndApplyEndAtBetween(
        String status,
        LocalDateTime start,
        LocalDateTime end
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JobPosting posting "
        + "SET posting.status = '마감', "
        + "posting.closeReason = :closeReason, "
        + "posting.closedAt = :closedAt "
        + "WHERE posting.status = '모집중' "
        + "AND posting.applyEndAt IS NOT NULL "
        + "AND posting.applyEndAt <= :closedAt")
    int closeExpiredPostings(
        @Param("closeReason") String closeReason,
        @Param("closedAt") LocalDateTime closedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JobPosting posting "
        + "SET posting.status = '모집중', "
        + "posting.publishedAt = :publishedAt "
        + "WHERE posting.status = '모집예정' "
        + "AND posting.applyStartAt IS NOT NULL "
        + "AND posting.applyStartAt <= :publishedAt")
    int publishScheduledPostings(
        @Param("publishedAt") LocalDateTime publishedAt
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE JobPosting jobPosting
               SET jobPosting.status = '마감',
                   jobPosting.closeReason = '기업탈퇴',
                   jobPosting.closedAt = :closedAt,
                   jobPosting.updatedAt = :closedAt
             WHERE jobPosting.companyId = :companyId
               AND jobPosting.status IN ('모집중', '모집예정')
            """)
    int closeRecruitingPostingsForWithdrawal(
            @Param("companyId") Long companyId,
            @Param("closedAt") LocalDateTime closedAt
    );

    /**
     * 주어진 공고 중 지금 외부에 노출해도 되는 것만 추린다.
     * 조건은 일반 공고 목록(findRecruitingJobPostings)과 동일하게 맞춘다 —
     * 목록에서 안 보이는 공고가 다른 경로로 새어 나가면 안 되기 때문이다.
     *
     * pgvector 검색 결과를 걸러내는 데 쓴다. 공고·기업 상태는 스케줄러나 관리자 조치로
     * 수시로 바뀌어 벡터 스토어 메타데이터에 복제하면 곧 낡으므로, 원본인 MySQL에 직접 묻는다.
     */
    @Query("""
            SELECT posting.jobPostingId
              FROM JobPosting posting
              JOIN Company company
                ON company.companyId = posting.companyId
             WHERE posting.jobPostingId IN :jobPostingIds
               AND posting.status = '모집중'
               AND company.approvalStatus = '승인'
               AND company.companyStatus = '정상'
            """)
    List<Long> findVisibleIdsIn(
            @Param("jobPostingIds") Collection<Long> jobPostingIds
    );

    /**
     * 임베딩 백필용 — 커서(jobPostingId) 이후 공고 ID를 오름차순으로 가져온다.
     * 공고 본문 전체를 메모리에 올리지 않으려고 ID만 조회한다.
     */
    @Query("""
            SELECT posting.jobPostingId
              FROM JobPosting posting
             WHERE posting.jobPostingId > :afterJobPostingId
             ORDER BY posting.jobPostingId ASC
            """)
    List<Long> findIdsAfter(
            @Param("afterJobPostingId") Long afterJobPostingId,
            Pageable pageable
    );

    long countByJobPostingIdGreaterThan(Long jobPostingId);
}
