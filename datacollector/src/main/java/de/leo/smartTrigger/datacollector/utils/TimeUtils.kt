package de.leo.smartTrigger.datacollector.utils

import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Created by Leo on 4/24/2016.
 */
object TimeUtils {

    val currentTimeStr: String
        get() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm-ss"))

    fun getTimeStr(date: LocalDateTime): String {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm-ss"))
    }

    fun getDateFromString(timeStr: String): ZonedDateTime {
        return LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm-ss"))
            .atZone(ZoneId.systemDefault())
    }

    fun ZonedDateTime.toEpochMillis(): Long = TimeUnit.SECONDS.toMillis(toEpochSecond())
}
