package kr.co.firstdayproject.repository.salary;

import kr.co.firstdayproject.entity.salary.SalaryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {
}
