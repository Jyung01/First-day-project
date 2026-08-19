package kr.co.firstdayproject.repository.member;

import kr.co.firstdayproject.entity.member.User;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update User u
            set u.lastLoginAt = :lastLoginAt
            where u.userId = :userId
            """)
    int updateLastLoginAt(
            @Param("userId") Long userId,
            @Param("lastLoginAt") LocalDateTime lastLoginAt
    );

    Optional<User> findByNameAndEmailIgnoreCase(String name, String email);

    Optional<User> findByLoginIdAndEmailIgnoreCase(String loginId, String email);

    boolean existsByLoginIdIgnoreCase(String loginId);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findFirstByCompanyIdAndUserTypeOrderByUserIdAsc(
            Long companyId,
            String userType
    );

    List<User> findByCompanyIdInAndUserTypeOrderByUserIdAsc(
            Collection<Long> companyIds,
            String userType
    );

    List<User> findByCompanyIdAndUserTypeOrderByUserIdAsc(
            Long companyId,
            String userType
    );

    @Query("""
            select u
            from User u
            where u.userType = :userType
              and u.accountStatus in :accountStatuses
              and (
                  :keyword is null
                  or lower(u.loginId) like lower(concat('%', :keyword, '%'))
                  or lower(u.name) like lower(concat('%', :keyword, '%'))
                  or lower(u.email) like lower(concat('%', :keyword, '%'))
                  or lower(u.phone) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<User> findAdminMembers(
            @Param("userType") String userType,
            @Param("accountStatuses") Collection<String> accountStatuses,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    long countByUserType(String userType);

    long countByUserTypeAndAccountStatus(String userType, String accountStatus);

    long countByUserTypeAndCreatedAtBetween(
            String userType,
            LocalDateTime start,
            LocalDateTime end
    );
}
