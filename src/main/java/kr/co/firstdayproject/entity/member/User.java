package kr.co.firstdayproject.entity.member;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 개인·기업·관리자 공통 로그인 계정
 * DB table: users
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long userId;
    /** 기업회원이 소속된 기업; 개인·관리자는 NULL */
    @Column(name = "company_id")
    private Long companyId;
    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    /** 개인회원 이름 또는 기업 담당자 이름 */
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    /** 로그인 계정 이메일; 기업회원은 담당자 이메일 */
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    /** 계정 연락처; 기업회원은 담당자 연락처 */
    @Column(name = "phone", length = 30)
    private String phone;
    /** 기업 담당자 부서; 개인·관리자는 NULL */
    @Column(name = "department", length = 100)
    private String department;
    /** 기업 담당자 직급·직책; 개인·관리자는 NULL */
    @Column(name = "position_title", length = 100)
    private String positionTitle;
    /** 개인/기업/관리자 */
    @Column(name = "user_type", nullable = false, length = 10)
    private String userType;
    /** 정상/이용정지/탈퇴 */
    @Column(name = "account_status", nullable = false, length = 10)
    private String accountStatus;
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;
}
