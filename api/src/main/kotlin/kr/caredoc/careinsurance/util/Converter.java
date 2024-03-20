package kr.caredoc.careinsurance.util;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Converter {

	/**
	 * "" 값을 null 로
	 * @param obj
	 * @return
	 */
	public static String blankToNull(Object obj) {
		String str = toStr(obj);
		return "".equals(str) ? null : str;
	}

	public static String toStr(Object obj) {
		return toStr(obj, "");
	}
	
	public static String toStr(Object obj, String defaultValue) {
		if (obj == null) return defaultValue;
		if ("".equals(obj.toString())) return defaultValue;
		return obj.toString();
	}
	
	public static int toInt(Object obj, int defaultValue) {
		if (obj != null) {
			if ("".equals(obj.toString())) return defaultValue;
			else return Integer.parseInt(obj.toString());
		}
		else {
			return defaultValue;
		}
	}
	
	public static int toInt(Object obj) {
		return toInt(obj, 0);
	}
	
	public static Long toLong(Object obj) {
		return toLong(obj, 0l);
	}

	public static Long toLong(Object obj, long defaultValue) {
		if (obj == null || "".equals(obj.toString())) return defaultValue;
		return Math.round(Double.parseDouble(obj.toString()));
	}
	
	public static boolean isEmpty(Object obj) {
		if( obj instanceof String ) {
			return obj == null || "".equals(obj.toString().trim());
		} else if( obj instanceof List ) {
			return obj == null || ((List)obj).isEmpty();
		} else if( obj instanceof Map ) {
			return obj == null || ((Map)obj).isEmpty();
		} else if( obj instanceof Object[] ) {
			return obj == null || Array.getLength(obj) == 0;
		} else {
			return obj == null;
		}
	}
	
	public static boolean isNotEmpty(Object obj) {
		return !isEmpty(obj);
	}
	
	public static String[] split(Object obj, String exp) {
		if (isEmpty(obj)) return null;
		return toStr(obj).split(exp, -1);
	}
	
	public static int inArray(String[] arr, Object val) {
		if (arr != null) {
			int i = 0;
			for (String str : arr) {
				if (str.equals(Converter.toStr(val))) {
					return i;
				}
				i++;
			}
		}
		return -1;
	}
	
	public static String decimalFormat(Object inVal, String pattern) {
		if ("".equals(toStr(inVal))) {
			return "0";
		}
		
		DecimalFormat form = new DecimalFormat(pattern);
		return form.format(inVal);
	}

	public static String addComma(Object inVal) {
		return decimalFormat(inVal, "#,###");
	}
	
	public static int inList(List<Map<String, Object>> list, String fieldName, Object val) {
		if (!isEmpty(list)) {
			int i = 0;
			for (Map<String, Object> map : list) {
				if (StringUtils.equals(toStr(map.get(fieldName)), toStr(val))) {
					return i;
				}
				i++;
			}
		}
		return -1;
	}

	public static int inList(List<String> list, Object val) {
		if (!isEmpty(list)) {
			int i = 0;
			for (String str : list) {
				if (StringUtils.equals(toStr(str), toStr(val))) {
					return i;
				}
				i++;
			}
		}
		return -1;
	}
	
	public static String inListStr(List<Map<String, Object>> list, String searchField, Object val, String outField) {
		if (!isEmpty(list)) {
			int i = 0;
			for (Map<String, Object> map : list) {
				if (StringUtils.equals(toStr(map.get(searchField)), toStr(val))) {
					return Converter.toStr(map.get(outField));
				}
				i++;
			}
		}
		return "";
	}
	
	public static String lpad(Object obj, int len) {
		return lpad(obj, len, "0");
	}
	
	/**
	 * obj 앞에 len 기준으로 미달되는 길이는 앞에다가 div를 붙혀준다.
	 * @param obj
	 * @param len
	 * @param div
	 * @return
	 */
	public static String lpad(Object obj, Integer len, String div) {
		String val = toStr(obj);
		String res;
		if (val.length() < len) {
			res = val;
			for (int k=len; k>val.length(); k--) {
				res = div + res;
			}
			return res;
		}
		else return val;
	}
	
	public static String dateToStr(String pattern, Date date) {
		if (date == null) return "";
		return FastDateFormat.getInstance(pattern, Locale.KOREA).format(date);
	}
	
	public static String dateToStr(String pattern) {
		return dateToStr(pattern, new Date());
	}
	
	public static String dateToStr(Date date) {
		return dateToStr("yyyy-MM-dd", date);
	}	
	
	/**
	 * 날짜더하기
	 * @param date 기준날짜
	 * @param div 구분 (YEAR:1, MONTH:2, DATE:5, DAY_OF_WEEK:7)
	 * @param days 더하는값
	 * @return
	 */
	public static Date dateAdd(Date date, Integer div, Integer days) {
	    Calendar cal = Calendar.getInstance();
	    cal.setTime(date);
	    cal.add(div, days);
	    return cal.getTime();
	}
	
	public static LocalDate toLocalDate(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	public static int dateDiff(String sdate, String edate, String div) throws Exception {
		return dateDiff(sdate, "yyyy-MM-dd", edate, "yyyy-MM-dd", div);
	}
	
	public static int dateDiff(String sdate, String spattern, String edate, String epattern, String div) throws Exception {
		Date date1 = Converter.toDate(sdate, spattern);
		Date date2 = Converter.toDate(edate, epattern);
		return dateDiff(date1, date2, div);
	}
	
	public static int dateDiff(Date sdate, Date edate, String div) {
		if ("M".equals(div)) {
	    Calendar m_calendar = Calendar.getInstance();
	    m_calendar.setTime(sdate);
	    int nMonth1 = 12 * m_calendar.get(Calendar.YEAR) + m_calendar.get(Calendar.MONTH);
	    m_calendar.setTime(edate);
	    int nMonth2 = 12 * m_calendar.get(Calendar.YEAR) + m_calendar.get(Calendar.MONTH);
			return nMonth2 - nMonth1;
		} else if ("D".equals(div)) {
			long diffInMillies = edate.getTime() - sdate.getTime();
			return toInt(TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS));
		} else if ("Y".equals(div)) {
	    Calendar m_calendar = Calendar.getInstance();
	    m_calendar.setTime(sdate);
	    int year1 = m_calendar.get(Calendar.YEAR);
	    m_calendar.setTime(edate);
	    int year2 = m_calendar.get(Calendar.YEAR);
			return year2 - year1;
		} else if ("m".equals(div)) {
			long diffInMillies = edate.getTime() - sdate.getTime();
			return Converter.toInt(diffInMillies / (60*1000));
		}
		else {
			return 0;
		}
	}
	
	public static Date toDate(Object obj, String pattern) throws ParseException {
		String tmp = toStr(obj);
		Date res = FastDateFormat.getInstance(pattern, Locale.KOREA).parse(tmp);
		return res;
	}
	
	public static Date toDate(Object obj) throws ParseException {
		return toDate(obj, "yyyy-MM-dd");
	}
	
	public static double toDouble(Object obj) {
		return toDouble(obj, 0d);
	}
	
	public static double toDouble(Object obj, double defaultValue) {
		String tmp = toStr(obj);
		if ("".equals(tmp)) return defaultValue;
		else return Double.parseDouble(tmp);
	}
	
	public static double decimalScale(Object obj , int loc , int mode) {
		BigDecimal bd = new BigDecimal(toDouble(obj));
		BigDecimal result = null;
		
		if(mode == 1) {
			result = bd.setScale(loc, BigDecimal.ROUND_DOWN);       //내림
		} else if(mode == 2) {
			result = bd.setScale(loc, BigDecimal.ROUND_HALF_UP);   //반올림
		} else if(mode == 3) {
			result = bd.setScale(loc, BigDecimal.ROUND_UP);             //올림
		} else if (mode == 0) {
			result = bd;
		}
		
		return result.doubleValue();
	}

	public static double decimalScale(Object obj) {
		return decimalScale(obj, 0, 2);
	}	
	public static double decimalScale2(Object obj) {
		return decimalScale(obj, 2, 2);
	}	
	public static double decimalScale3(Object obj) {
		return decimalScale(obj, 0, 3);
	}	
	
	/**
	 * DB에 등록된 파일 이미지 미리보기에 사용할 확장자 (ex agencyAdd.lime 대행사 수정페이지) 
	 * @param FileName
	 * @return
	 */
	public static String getExtByFile(Object fileName) {
		String ret = "";
		
		if(StringUtils.equals("", Converter.toStr(fileName))) {
			return ret;
		}
		
		String[] fileExtArr = Converter.toStr(fileName).split("[.]");
		return fileExtArr[fileExtArr.length-1].toUpperCase();
	}
	
	public static int getWeekNum(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.DAY_OF_WEEK);
	}
	
	public static String getWeekName(Date date) {
		int weekNum = getWeekNum(date);
		if (weekNum == 1) return "일";
		else if (weekNum == 2) return "월";
		else if (weekNum == 3) return "화";
		else if (weekNum == 4) return "수";
		else if (weekNum == 5) return "목";
		else if (weekNum == 6) return "금";
		else return "토";
	}
	
	public static int getWeekNum(String dateStr, String pattern) throws ParseException {
		Date date = toDate(dateStr, pattern);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.DAY_OF_WEEK);
	}	
}
