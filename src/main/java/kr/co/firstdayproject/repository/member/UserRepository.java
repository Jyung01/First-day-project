package kr.co.firstdayproject.repository.member;

import kr.co.firstdayproject.entity.member.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginIdIgnoreCase(String loginId);

    boolean existsByEmailIgnoreCase(String email);
}
