package kr.co.firstdayproject.repository.company;

import java.util.Collection;
import kr.co.firstdayproject.entity.company.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByBusinessNumberIn(Collection<String> businessNumbers);

    boolean existsByBusinessNumberInAndCompanyIdNot(
            Collection<String> businessNumbers,
            Long companyId
    );

    @Query(
            value = """
                    select c
                    from Company c
                    where (
                        :status = 'ALL'
                        or (:status = 'PENDING'
                            and c.companyStatus = '정상'
                            and c.approvalStatus = '승인대기')
                        or (:status = 'APPROVED'
                            and c.companyStatus = '정상'
                            and c.approvalStatus = '승인')
                        or (:status = 'REJECTED'
                            and c.companyStatus = '정상'
                            and c.approvalStatus = '반려')
                        or (:status = 'SUSPENDED'
                            and c.companyStatus = '이용정지')
                        or (:status = 'WITHDRAWN'
                            and c.companyStatus = '탈퇴')
                    )
                    and (
                        :keyword is null
                        or lower(c.companyName) like lower(concat('%', :keyword, '%'))
                        or replace(c.businessNumber, '-', '')
                            like concat('%', replace(:keyword, '-', ''), '%')
                    )
                    order by
                        case
                            when c.reapplyRequestedAt is not null
                            then c.reapplyRequestedAt
                            else c.createdAt
                        end desc,
                        c.companyId desc
                    """,
            countQuery = """
                    select count(c)
                    from Company c
                    where (
                        :status = 'ALL'
                        or (:status = 'PENDING'
                            and c.companyStatus = '정상'
                            and c.approvalStatus = '승인대기')
                        or (:status = 'APPROVED'
                            and c.companyStatus = '정상'
                            and c.approvalStatus = '승인')
                        or (:status = 'REJECTED'
                            and c.companyStatus = '정상'
                            and c.approvalStatus = '반려')
                        or (:status = 'SUSPENDED'
                            and c.companyStatus = '이용정지')
                        or (:status = 'WITHDRAWN'
                            and c.companyStatus = '탈퇴')
                    )
                    and (
                        :keyword is null
                        or lower(c.companyName) like lower(concat('%', :keyword, '%'))
                        or replace(c.businessNumber, '-', '')
                            like concat('%', replace(:keyword, '-', ''), '%')
                    )
                    """
    )
    Page<Company> findAdminCompanies(
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    long countByApprovalStatusAndCompanyStatus(
            String approvalStatus,
            String companyStatus
    );

    long countByApprovalStatusAndCompanyStatusAndReapplyRequestedAtIsNull(
            String approvalStatus,
            String companyStatus
    );

    long countByApprovalStatusAndCompanyStatusAndReapplyRequestedAtIsNotNull(
            String approvalStatus,
            String companyStatus
    );
}
