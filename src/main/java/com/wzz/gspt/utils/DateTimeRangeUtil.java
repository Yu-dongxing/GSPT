package com.wzz.gspt.utils;

import com.wzz.gspt.enums.ResultCode;
import com.wzz.gspt.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 日期时间范围解析工具类
 */
public final class DateTimeRangeUtil {

    /**
     * 支持的日期时间格式
     */
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    );

    /**
     * 支持的日期格式
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 私有构造方法，禁止实例化
     */
    private DateTimeRangeUtil() {
    }

    /**
     * 解析时间范围字符串
     *
     * @param timeString 时间字符串
     * @param endOfDay 是否按结束时间解析
     * @return 解析后的时间
     */
    public static LocalDateTime parseDateTime(String timeString, boolean endOfDay) {
        if (!StringUtils.hasText(timeString)) {
            return null;
        }

        String trimmed = timeString.trim();
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            LocalDate date = LocalDate.parse(trimmed, DATE_FORMATTER);
            return endOfDay ? LocalDateTime.of(date, LocalTime.of(23, 59, 59)) : date.atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }

        throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "时间格式不正确: " + trimmed);
    }
}
