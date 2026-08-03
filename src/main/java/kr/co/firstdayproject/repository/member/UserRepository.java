package kr.co.firstdayproject.repository.member;

import kr.co.firstdayproject.entity.member.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
