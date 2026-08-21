package kr.co.firstdayproject.security;

/**
 * 관리자 화면에서 "지금 로그인한 관리자 ID"를 꺼내는 공통 헬퍼.
 *
 * 관리자는 별도 테이블이 아니라 users 테이블의 userType='관리자' 행이고,
 * CustomUserDetailsService가 이를 ROLE_ADMIN으로 매핑한다.
 * 따라서 감사 컬럼(created_by / updated_by / answered_by 등,
 * 모두 users(user_id) FK)에 넣을 값은 CustomUserDetails.userId 그대로다.
 */
public final class AdminPrincipal {

    private AdminPrincipal() {
    }

    /**
     * @throws IllegalStateException 인증 정보가 없을 때.
     *         /admin/**는 SecurityConfig에서 hasRole("ADMIN")으로 막혀 있어 정상 흐름에서는 발생하지 않지만,
     *         잘못된 ID로 감사 기록을 남기는 것보다 실패시키는 편이 안전하다.
     */
    public static Long requireAdminId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new IllegalStateException("관리자 로그인이 필요합니다.");
        }
        return userDetails.getUserId();
    }
}
