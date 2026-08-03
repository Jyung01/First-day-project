package kr.co.firstdayproject.repository.company;

import kr.co.firstdayproject.entity.company.SavedCompany;
import kr.co.firstdayproject.entity.company.SavedCompanyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SavedCompanyRepository extends JpaRepository<SavedCompany, SavedCompanyId> {
}
