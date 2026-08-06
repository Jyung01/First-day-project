package kr.co.firstdayproject.dto.my;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import kr.co.firstdayproject.entity.member.PersonalProfile;
import kr.co.firstdayproject.entity.member.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileEditRequest {

    @NotBlank(message = "이름을 입력해주세요.")
    private String memberName;

    @NotBlank(message = "휴대전화를 입력해주세요.")
    @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "휴대전화 형식이 올바르지 않습니다.")
    private String phone;

    private String postcode;

    private String address;

    private String addressDetail;

    @Size(max = 3, message = "희망 직무는 최대 3개까지 선택할 수 있습니다.")
    private List<Long> desiredJobIds;

    public static ProfileEditRequest from(User user, PersonalProfile profile) {
        ProfileEditRequest request = new ProfileEditRequest();
        request.setMemberName(user.getName());
        request.setPhone(user.getPhone());
        request.setPostcode(profile.getPostalCode());
        request.setAddress(profile.getAddressLine1());
        request.setAddressDetail(profile.getAddressLine2());
        return request;
    }
}
