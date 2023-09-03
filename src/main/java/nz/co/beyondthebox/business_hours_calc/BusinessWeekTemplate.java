package nz.co.beyondthebox.business_hours_calc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class BusinessWeekTemplate {
    public static Map<DayOfWeek, BusinessDay> DEFAULT() {
        Map<DayOfWeek, BusinessDay> businessDays = new HashMap<>();
        // Define your business hours for each day of the week
        // Example: Monday to Friday with two shifts
        businessDays.put(DayOfWeek.MONDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        businessDays.put(DayOfWeek.TUESDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        businessDays.put(DayOfWeek.WEDNESDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        businessDays.put(DayOfWeek.THURSDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        businessDays.put(DayOfWeek.FRIDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        return businessDays;
    }
}
