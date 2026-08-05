package kr.co.firstdayproject.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CorporateSignupRequest {

    @NotBlank(message = "아이디를 입력해주세요.")
    @Pattern(
            regexp = "^[A-Za-z0-9]{6,20}$",
            message = "아이디는 영문과 숫자 6~20자로 입력해주세요."
    )
    private String memberId;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])\\S{8,64}$",
            message = "비밀번호는 8~64자의 영문, 숫자, 특수문자를 포함해야 합니다."
    )
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String passwordConfirm;

    @NotBlank(message = "담당자 이름을 입력해주세요.")
    @Size(max = 100, message = "담당자 이름은 100자를 초과할 수 없습니다.")
    private String managerName;

    @NotBlank(message = "담당자 연락처를 입력해주세요.")
    @Pattern(
            regexp = "^010-\\d{4}-\\d{4}$",
            message = "담당자 연락처는 010-0000-0000 형식으로 입력해주세요."
    )
    private String managerPhone;

    @NotBlank(message = "담당자 이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$",
            message = "도메인을 포함한 올바른 이메일을 입력해주세요."
    )
    @Size(max = 254, message = "이메일은 254자를 초과할 수 없습니다.")
    private String managerEmail;

    @NotBlank(message = "사업자등록번호를 입력해주세요.")
    @Pattern(
            regexp = "^\\d{3}-?\\d{2}-?\\d{5}$",
            message = "사업자등록번호 10자리를 정확히 입력해주세요."
    )
    private String businessNumber;

    @NotBlank(message = "기업명을 입력해주세요.")
    @Size(max = 200, message = "기업명은 200자를 초과할 수 없습니다.")
    private String companyName;

    @NotBlank(message = "업종을 선택해주세요.")
    @Size(max = 100, message = "업종은 100자를 초과할 수 없습니다.")
    private String industry;

    @NotBlank(message = "기업 규모를 선택해주세요.")
    @Pattern(
            regexp = "^(스타트업|중소기업|중견기업|대기업|공공기관|외국계기업)$",
            message = "올바른 기업 규모를 선택해주세요."
    )
    private String companySize;

    @NotBlank(message = "기업 주소를 검색해주세요.")
    @Size(max = 20, message = "우편번호는 20자를 초과할 수 없습니다.")
    private String postcode;

    @NotBlank(message = "기업 주소를 검색해주세요.")
    @Size(max = 255, message = "기본주소는 255자를 초과할 수 없습니다.")
    private String address;

    @Size(max = 255, message = "상세주소는 255자를 초과할 수 없습니다.")
    private String addressDetail;

    @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordMatched() {
        return password == null || password.equals(passwordConfirm);
    }
}
