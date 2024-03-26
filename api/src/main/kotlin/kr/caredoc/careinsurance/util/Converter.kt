package kr.caredoc.careinsurance.util

import org.apache.commons.lang3.time.FastDateFormat
import java.text.ParseException
import java.util.*

class Converter {
    companion object {
        @JvmStatic
        fun toStr(obj: Any?): String {
            return toStr(obj, "")
        }

        @JvmStatic
        fun toStr(obj: Any?, defaultValue: String): String {
            if (obj == null) return defaultValue
            if ("".equals(obj.toString())) return defaultValue
            return obj.toString()
        }

        @JvmStatic
        fun toInt(obj: Any?, defaultValue: Int): Int {
            return if (obj != null) {
                if ("".equals(obj.toString())) defaultValue
                else obj.toString().toInt()
            } else {
                defaultValue
            }
        }

        @JvmStatic
        fun toInt(obj: Any?): Int {
            return toInt(obj, 0)
        }


        @JvmStatic
        fun toLong(obj: Any?): Long {
            return toLong(obj, 0L)
        }

        @JvmStatic
        fun toLong(obj: Any?, defaultValue: Long): Long {
            if (obj == null || "".equals(obj.toString())) return defaultValue
            return Math.round(obj.toString().toDouble())
        }


        @JvmStatic
        fun dateToStr(pattern: String?, date: Date?): String {
            if (date == null) return ""
            return FastDateFormat.getInstance(pattern, Locale.KOREA).format(date)
        }

        @JvmStatic
        fun dateToStr(pattern: String?): String {
            return dateToStr(pattern, Date())
        }

        @JvmStatic
        fun dateToStr(date: Date?): String {
            return dateToStr("yyyy-MM-dd", date)
        }

        @JvmStatic
        @Throws(ParseException::class)
        fun toDate(obj: Any?, pattern: String?): Date {
            val tmp = toStr(obj!!)
            val res = FastDateFormat.getInstance(pattern, Locale.KOREA).parse(tmp)
            return res
        }

        @JvmStatic
        @Throws(ParseException::class)
        fun toDate(obj: Any?): Date {
            return toDate(obj, "yyyy-MM-dd")
        }

        @JvmStatic
        fun toDouble(obj: Any?): Double {
            return toDouble(obj, 0.0)
        }

        @JvmStatic
        fun toDouble(obj: Any?, defaultValue: Double): Double {
            if (obj == null) return defaultValue
            val tmp = toStr(obj!!)
            if ("".equals(tmp)) return defaultValue
            else return tmp.toDouble()
        }
    }
}