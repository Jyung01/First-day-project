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
    ORDER BY posting.viewCount DESC,
             posting.publishedAt DESC,
             posting.jobPostingId DESC
    """)
    List<MainJobListItem> findPopularRecruitingJobPostings(
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
      AND (
            :keyword IS NULL
            OR LOWER(posting.title)
                LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(company.companyName)
                LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
      AND (
            :categoryId IS NULL
            OR posting.jobCategoryId = :categoryId
      )
      AND (
            :parentCategoryId IS NULL
            OR category.parentId = :parentCategoryId
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
    ORDER BY posting.publishedAt DESC,
             posting.jobPostingId DESC
    """,
            countQuery = """
    SELECT COUNT(posting)
    FROM JobPosting posting
    JOIN Company company
      ON company.companyId = posting.companyId
    WHERE posting.status = '모집중'
      AND (
            :keyword IS NULL
            OR LOWER(posting.title)
                LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(company.companyName)
                LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
      AND (
            :categoryId IS NULL
            OR posting.jobCategoryId = :categoryId
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
    """
    )
    Page<JobListQueryItem> findRecruitingJobPostings(
            @Param("keyword") String keyword,
            @Param("parentCategoryId") Long parentCategoryId,
            @Param("categoryId") Long categoryId,
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
            + "posting.jobPostingId, company.companyName, posting.title, "
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
               AND jobPosting.status = '모집중'
            """)
    int closeRecruitingPostingsForWithdrawal(
            @Param("companyId") Long companyId,
            @Param("closedAt") LocalDateTime closedAt
    );
}
