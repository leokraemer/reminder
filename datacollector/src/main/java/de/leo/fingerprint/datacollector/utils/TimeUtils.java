package de.leo.fingerprint.datacollector.utils;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

/**
 * Created by Leo on 4/24/2016.
 */
public class TimeUtils {

    public static String getCurrentTimeStr() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm-ss"));
    }

    public static String getTimeStr(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm-ss"));
    }

    public static LocalDateTime getDateFromString(String timeStr) {
        return LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm-ss"));
    }
}
