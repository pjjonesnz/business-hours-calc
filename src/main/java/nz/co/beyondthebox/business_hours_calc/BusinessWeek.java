package nz.co.beyondthebox.business_hours_calc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class BusinessWeek {
    private final Map<DayOfWeek, BusinessDay> businessDays;

    public BusinessWeek() {
        this.businessDays = new HashMap<>();
    }

    public BusinessWeek addDay(DayOfWeek dayOfWeek, BusinessDay businessDay) {
        // TODO: days must be added in order for this to work - modify so they can be added in any order
        // Here we can handle the logic to split shifts that span multiple days.
        for (BusinessShift shift : businessDay.getShifts()) {
            if (shift.getEndTime().isBefore(shift.getStartTime())) {
                // This shift spans multiple days
                // Split the shift into two: one ending at midnight and one starting at midnight.
                BusinessShift firstShift = new BusinessShift(shift.getStartTime(), LocalTime.MIDNIGHT);
                BusinessShift secondShift = new BusinessShift(LocalTime.MIDNIGHT, shift.getEndTime());

                // Add the first part of the shift to the current day
                addShiftToDay(dayOfWeek, firstShift);

                // Add the second part of the shift to the next day
                DayOfWeek nextDay = DayOfWeek.of((dayOfWeek.getValue() % 7) + 1);
                addShiftToDay(nextDay, secondShift);
            } else {
                // This shift is within a single day
                addShiftToDay(dayOfWeek, shift);
            }
        }
        return this;
    }

    public BusinessWeek addShiftToDay(DayOfWeek dayOfWeek, BusinessShift shift) {
        businessDays
                .computeIfAbsent(dayOfWeek, k -> new BusinessDay())
                .addShift(shift);
        return this;
    }

    public Map<DayOfWeek, BusinessDay> getBusinessDays() {
        return businessDays;
    }

    public BusinessWeek initialiseDefault() {
        addDay(DayOfWeek.MONDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        addDay(DayOfWeek.TUESDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        addDay(DayOfWeek.WEDNESDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        addDay(DayOfWeek.THURSDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        addDay(DayOfWeek.FRIDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        return this;
    }
}
