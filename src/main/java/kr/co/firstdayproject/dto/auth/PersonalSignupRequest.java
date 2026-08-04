package kr.co.firstdayproject.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PersonalSignupRequest {

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

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다.")
    private String memberName;

    @NotBlank(message = "휴대전화를 입력해주세요.")
    @Pattern(
            regexp = "^010-\\d{4}-\\d{4}$",
            message = "휴대전화는 010-0000-0000 형식으로 입력해주세요."
    )
    private String phone;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$",
            message = "도메인을 포함한 올바른 이메일을 입력해주세요."
    )
    @Size(max = 254, message = "이메일은 254자를 초과할 수 없습니다.")
    private String email;

    @Size(max = 3, message = "희망 직무는 최대 3개까지 선택할 수 있습니다.")
    private List<Long> desiredJobIds = new ArrayList<>();

    @Size(max = 20, message = "우편번호는 20자를 초과할 수 없습니다.")
    private String postcode;

    @Size(max = 255, message = "기본주소는 255자를 초과할 수 없습니다.")
    private String address;

    @Size(max = 255, message = "상세주소는 255자를 초과할 수 없습니다.")
    private String addressDetail;

    @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordMatched() {
        return password == null || password.equals(passwordConfirm);
    }

    @AssertTrue(message = "주소 찾기를 통해 우편번호와 기본주소를 함께 입력해주세요.")
    public boolean isAddressComplete() {
        boolean postcodeEmpty = postcode == null || postcode.isBlank();
        boolean addressEmpty = address == null || address.isBlank();
        boolean detailEmpty = addressDetail == null || addressDetail.isBlank();

        if (postcodeEmpty && addressEmpty && detailEmpty) {
            return true;
        }
        return !postcodeEmpty && !addressEmpty;
    }
}
