package kr.co.firstdayproject.repository.member;

import kr.co.firstdayproject.entity.member.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByNameAndEmailIgnoreCase(String name, String email);

    Optional<User> findByLoginIdAndEmailIgnoreCase(String loginId, String email);

    boolean existsByLoginIdIgnoreCase(String loginId);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findFirstByCompanyIdAndUserTypeOrderByUserIdAsc(
        Long companyId,
        String userType
    );
}
