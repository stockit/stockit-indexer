package com.stockit.util

import java.util.Date

/**
 * Created by dmcquill on 3/28/15.
 */

object DateIntervalUtil {
    val MILLIS_IN_SECOND: Long = 1000
    val SECONDS_IN_MINUTE: Long = 60
    val MINUTES_IN_HOUR: Long = 60
    val HOURS_IN_DAY: Long = 24
    val DAYS_IN_YEAR: Long = 365

    val MILLIS_IN_YEAR = MILLIS_IN_SECOND * SECONDS_IN_MINUTE * MINUTES_IN_HOUR * HOURS_IN_DAY * DAYS_IN_YEAR
    val MILLIS_IN_DAY = MILLIS_IN_SECOND * SECONDS_IN_MINUTE * MINUTES_IN_HOUR * HOURS_IN_DAY
}

class DateIntervalUtil {

    def partitionDateRange(startDate: Date, endDate: Date, partitions: Int): Seq[(Date, Date)] = {
        (1 to partitions) map { (i: Int) =>
            val curMinDate = new Date(startDate.getTime() + (i - 1) * DateIntervalUtil.MILLIS_IN_YEAR + (if(i > 0) DateIntervalUtil.MILLIS_IN_DAY else 0 ))
            val curMaxDate = new Date(startDate.getTime() + i * DateIntervalUtil.MILLIS_IN_YEAR)

            ( curMinDate, ( if(curMaxDate.getTime() >= endDate.getTime()) endDate else curMaxDate ) )
        }
    }

}
