package kr.co.firstdayproject.dto.my;

import java.util.List;
import kr.co.firstdayproject.util.PageHandler;

public record MyApplicationListResult(
        List<MyApplicationListItem> applications,
        MyApplicationFilterCounts counts,
        String filter,
        PageHandler pageHandler
) {
}
