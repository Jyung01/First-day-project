package kr.co.firstdayproject.repository.member;

import kr.co.firstdayproject.entity.member.PersonalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonalProfileRepository extends JpaRepository<PersonalProfile, Long> {
}
