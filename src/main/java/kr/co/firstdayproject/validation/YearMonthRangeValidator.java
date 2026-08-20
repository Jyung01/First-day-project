package kr.co.firstdayproject.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Year;
import java.time.YearMonth;

/**
 * {@link YearMonthRange}의 실제 검사 로직.
 */
public class YearMonthRangeValidator implements ConstraintValidator<YearMonthRange, YearMonth> {

    private int minYear;
    private int maxYearsAfterNow;

    @Override
    public void initialize(YearMonthRange annotation) {
        this.minYear = annotation.minYear();
        this.maxYearsAfterNow = annotation.maxYearsAfterNow();
    }

    @Override
    public boolean isValid(YearMonth value, ConstraintValidatorContext context) {
        // 값 없음은 이 제약의 관심사가 아니다. 필수 여부는 @NotNull이 담당한다.
        if (value == null) {
            return true;
        }

        // 상한은 애플리케이션이 오래 떠 있어도 어긋나지 않도록 검사 시점 기준으로 계산한다.
        int maxYear = Year.now().getValue() + maxYearsAfterNow;

        return value.getYear() >= minYear && value.getYear() <= maxYear;
    }
}
