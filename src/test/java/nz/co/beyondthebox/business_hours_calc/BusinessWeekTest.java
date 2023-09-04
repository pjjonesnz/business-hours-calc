package nz.co.beyondthebox.business_hours_calc;

import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BusinessWeekTest {

    @Test
    public void testShiftsAreSorted() {
        BusinessWeek businessWeek = new BusinessWeek();

        // Create a BusinessDay with shifts in random order
        BusinessDay day = new BusinessDay();
        day.addShift(new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0)));
        day.addShift(new BusinessShift(LocalTime.of(8, 0), LocalTime.of(12, 0)));
        day.addShift(new BusinessShift(LocalTime.of(18, 0), LocalTime.of(22, 0)));

        // Add the BusinessDay to the BusinessWeek
        businessWeek.addDay(DayOfWeek.MONDAY, day);

        // Get the sorted shifts for Monday
        BusinessDay sortedDay = businessWeek.getBusinessDays().get(DayOfWeek.MONDAY);
        List<BusinessShift> sortedShifts = sortedDay.getSortedShifts();

        // Check that shifts are sorted by their start time
        assertEquals(LocalTime.of(8, 0), sortedShifts.get(0).getStartTime());
        assertEquals(LocalTime.of(13, 0), sortedShifts.get(1).getStartTime());
        assertEquals(LocalTime.of(18, 0), sortedShifts.get(2).getStartTime());
    }
}
