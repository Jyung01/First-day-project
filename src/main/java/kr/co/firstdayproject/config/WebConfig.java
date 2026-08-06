package kr.co.firstdayproject.config;

import java.text.ParseException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        /*
         * Spring의 Jsr310DateTimeFormatAnnotationFormatterFactory는
         * YearMonth를 지원 대상에 포함하지 않아 <input type="month"> 값을
         * @DateTimeFormat(pattern = "yyyy-MM")만으로는 바인딩할 수 없다.
         */
        registry.addFormatter(new Formatter<YearMonth>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

            @Override
            public YearMonth parse(String text, Locale locale) throws ParseException {
                if (text == null || text.isBlank()) {
                    return null;
                }
                return YearMonth.parse(text, formatter);
            }

            @Override
            public String print(YearMonth yearMonth, Locale locale) {
                return yearMonth == null ? "" : formatter.format(yearMonth);
            }
        });
    }
}
