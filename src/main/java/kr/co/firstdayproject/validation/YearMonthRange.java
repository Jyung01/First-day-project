package kr.co.firstdayproject.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link java.time.YearMonth} 값이 현실적인 연도 범위 안에 있는지 검사한다.
 *
 * <p>{@code <input type="month">}은 브라우저에서 연도 자릿수를 막지 않아 131212년 같은 값이
 * 그대로 넘어온다. MySQL {@code DATE}는 9999년까지만 담을 수 있어 저장 시점에 터지므로,
 * 컨트롤러 앞단에서 걸러 500 대신 폼 에러로 돌려준다.
 *
 * <p>{@code null}은 통과시킨다. 필수 여부는 {@code @NotNull}로 따로 표현한다.
 */
@Documented
@Constraint(validatedBy = YearMonthRangeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface YearMonthRange {

    String message() default "날짜를 올바르게 입력해주세요.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** 허용하는 가장 이른 연도. */
    int minYear() default DateRanges.MIN_YEAR;

    /**
     * 현재 연도로부터 몇 년 뒤까지 허용할지.
     * 졸업 예정일처럼 미래 날짜가 정상인 경우가 있어 여유를 둔다.
     */
    int maxYearsAfterNow() default DateRanges.MAX_YEARS_AFTER_NOW;
}
