package kr.co.firstdayproject.repository.application;

import kr.co.firstdayproject.entity.application.ApplicationMemo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationMemoRepository extends JpaRepository<ApplicationMemo, Long> {
}
