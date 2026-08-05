package kr.co.firstdayproject.dto.corp;

import jakarta.validation.constraints.Size;
import kr.co.firstdayproject.entity.member.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompanyAccountRequest {

    @Size(max = 100, message = "부서는 100자를 초과할 수 없습니다.")
    private String department;

    @Size(max = 100, message = "직책은 100자를 초과할 수 없습니다.")
    private String positionTitle;

    public static CompanyAccountRequest from(User user) {
        CompanyAccountRequest request = new CompanyAccountRequest();
        request.setDepartment(user.getDepartment());
        request.setPositionTitle(user.getPositionTitle());
        return request;
    }
}
