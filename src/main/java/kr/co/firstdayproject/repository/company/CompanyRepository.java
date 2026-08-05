package kr.co.firstdayproject.repository.company;

import java.util.Collection;
import kr.co.firstdayproject.entity.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByBusinessNumberIn(Collection<String> businessNumbers);

    boolean existsByBusinessNumberInAndCompanyIdNot(
            Collection<String> businessNumbers,
            Long companyId
    );
}
