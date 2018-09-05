package info.tinyapps.huges.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * different utility methods
 */
public class Utils {
    static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

    //formats date of birth as string to be send to server
    public static final String getDOB(int year, int monthOfYear,int dayOfMonth){
        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, monthOfYear);
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        return SDF.format(cal.getTime());
    }

    //parses date
    public static final Calendar getCalendar(String dobStr){
        Calendar cal = Calendar.getInstance();
        if(dobStr == null || dobStr.length() == 0)
            return cal;

        try {
            Date date = SDF.parse(dobStr);
            cal.setTime(date);
        }
        catch (Exception e){
        }

        return cal;
    }
}

