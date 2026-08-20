package kr.co.firstdayproject.validation;

import java.time.Year;

/**
 * 이력서 등에서 사람이 입력하는 연도의 허용 범위.
 *
 * <p>{@link YearMonthRange}의 기본값과 화면의 연도 선택 목록이 같은 값을 쓰도록 여기에 모았다.
 * 둘이 어긋나면 화면에 없는 연도를 서버가 받거나, 고를 수 있는 연도를 서버가 거부하게 된다.
 */
public final class DateRanges {

    /** 허용하는 가장 이른 연도. */
    public static final int MIN_YEAR = 1900;

    /** 현재 연도로부터 몇 년 뒤까지 허용할지. 졸업 예정일이 미래일 수 있어 여유를 둔다. */
    public static final int MAX_YEARS_AFTER_NOW = 10;

    private DateRanges() {
    }

    /** 허용하는 가장 늦은 연도. 해가 바뀌어도 맞도록 호출 시점에 계산한다. */
    public static int maxYear() {
        return Year.now().getValue() + MAX_YEARS_AFTER_NOW;
    }
}
