package nz.co.beyondthebox.business_hours_calc;

import java.time.LocalDate;
import java.util.TreeSet;

public class BusinessHolidays {
    public static TreeSet<LocalDate> DEFAULT() {
        TreeSet<LocalDate> holidays = new TreeSet<>();
        holidays.add(LocalDate.of(2023, 10, 23));
        return holidays;
    }
}
