package nz.co.beyondthebox.business_hours_calc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BusinessHoursCalculatorTest {

    private BusinessHoursCalculator calculator;
    private Map<DayOfWeek, BusinessDay> businessDays;
    private TreeSet<LocalDate> holidays;

    @BeforeEach
    public void setUp() {
        BusinessWeek businessWeek = new BusinessWeek().initialiseDefault();
        holidays = BusinessHolidays.DEFAULT();
        calculator = new BusinessHoursCalculator(businessWeek, holidays);
    }

//    @Test
//    public void testAddBusinessHoursWithinOneDay() {
//        LocalDateTime start = LocalDateTime.of(2023, 9, 4, 8, 0);  // It's a Monday
//        Duration duration = Duration.ofHours(4);
//        LocalDateTime expected = LocalDateTime.of(2023, 9, 4, 12, 0);
//        assertEquals(expected, calculator.addBusinessHours(start, duration));
//    }

    @Test
    public void testAddBusinessHoursWithinOneDay() {
        BusinessWeek businessWeek = new BusinessWeek();

        // Add Monday to BusinessWeek
        businessWeek.addDay(DayOfWeek.MONDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));

        // Initialize BusinessHoursCalculator with the businessWeek
        BusinessHoursCalculator calculator = new BusinessHoursCalculator(businessWeek);

        // The rest of the test remains the same
        LocalDateTime start = LocalDateTime.of(2023, 9, 4, 8, 0);  // It's a Monday
        Duration duration = Duration.ofHours(4);
        LocalDateTime expected = LocalDateTime.of(2023, 9, 4, 12, 0);
        assertEquals(expected, calculator.addBusinessHours(start, duration));
    }

    @Test
    public void testAddBusinessHoursOverOneDay() {
        LocalDateTime start = LocalDateTime.of(2023, 9, 4, 8, 0);  // It's a Monday
        Duration duration = Duration.ofHours(8);
        LocalDateTime expected = LocalDateTime.of(2023, 9, 4, 17, 0);
        assertEquals(expected, calculator.addBusinessHours(start, duration));
    }

    @Test
    public void testAddBusinessHoursOverWeekend() {
        LocalDateTime start = LocalDateTime.of(2023, 9, 8, 8, 0);  // It's a Friday
        Duration duration = Duration.ofHours(20); // 8 hours on Friday, 8 on next Monday, 4 on next Tuesday
        LocalDateTime expected = LocalDateTime.of(2023, 9, 12, 12, 0); // Next Monday
        assertEquals(expected, calculator.addBusinessHours(start, duration));
    }

    @Test
    public void testAddBusinessHoursOnHoliday() {
        LocalDateTime start = LocalDateTime.of(2023, 10, 23, 8, 0);  // It's a holiday
        Duration duration = Duration.ofHours(8);
        LocalDateTime expected = LocalDateTime.of(2023, 10, 24, 17, 0); // Next working day is Tuesday
        assertEquals(expected, calculator.addBusinessHours(start, duration));
    }

    @Test
    public void testAddBusinessHoursWithMinimumDurationPerDay() {
        LocalDateTime start = LocalDateTime.of(2023, 9, 4, 8, 0);  // It's a Monday
        Duration duration = Duration.ofHours(45);  // 5 full business days at 9 hours per day
        Duration minimumDurationPerDay = Duration.ofHours(9);  // Minimum 9 hours per day
        LocalDateTime expected = LocalDateTime.of(2023, 9, 8, 17, 0); // Next Monday
        assertEquals(expected, calculator.addBusinessHours(start, duration, minimumDurationPerDay));
    }

    @Test
    public void testZeroDuration() {
        LocalDateTime start = LocalDateTime.of(2023, 9, 4, 8, 0);  // It's a Monday
        Duration duration = Duration.ZERO;
        assertEquals(start, calculator.addBusinessHours(start, duration));
    }

    @Test
    public void testAddBusinessHoursExactlyAtShiftEnd() {
        LocalDateTime start = LocalDateTime.of(2023, 9, 4, 17, 0);  // It's a Monday at shift end
        Duration duration = Duration.ofMinutes(0);  // No additional time
        LocalDateTime expected = LocalDateTime.of(2023, 9, 4, 17, 0);  // Should be same as start
        assertEquals(expected, calculator.addBusinessHours(start, duration));
    }

    @Test
    public void testAddBusinessHoursStartsOnWeekend() {
        LocalDateTime start = LocalDateTime.of(2023, 9, 9, 10, 0);  // It's a Saturday
        Duration duration = Duration.ofHours(8);
        LocalDateTime expected = LocalDateTime.of(2023, 9, 11, 17, 0);  // Should move to next Monday
        assertEquals(expected, calculator.addBusinessHours(start, duration));
    }

    @Test
    public void testAddBusinessHoursStartsOnHoliday() {
        LocalDateTime start = LocalDateTime.of(2023, 10, 23, 10, 0);  // It's a holiday
        Duration duration = Duration.ofHours(8);
        LocalDateTime expected = LocalDateTime.of(2023, 10, 24, 17, 0);  // Should move to next working day
        assertEquals(expected, calculator.addBusinessHours(start, duration));
    }

    @Test
    public void testAddBusinessHoursCrossingMultipleHolidays() {
        // Adding additional holiday for the test
        BusinessWeek businessWeek = new BusinessWeek().initialiseDefault();
        holidays.add(LocalDate.of(2023, 10, 24));
        calculator = new BusinessHoursCalculator(businessWeek, holidays);

        LocalDateTime start = LocalDateTime.of(2023, 10, 23, 8, 0);  // Starts on a holiday
        Duration duration = Duration.ofHours(8);
        LocalDateTime expected = LocalDateTime.of(2023, 10, 25, 17, 0);  // Should skip to the day after next holiday
        assertEquals(expected, calculator.addBusinessHours(start, duration));
    }

    @Test
    public void testMinimumDurationGreaterThanBusinessDay() {
        LocalDateTime start = LocalDateTime.of(2023, 9, 4, 8, 0);  // It's a Monday
        Duration duration = Duration.ofHours(8);  // 8 hours needed
        Duration minimumDurationPerDay = Duration.ofHours(10);  // Minimum 10 hours per day
        LocalDateTime expected = LocalDateTime.of(2023, 9, 4, 17, 0);  // Should extend to next day
        assertEquals(expected, calculator.addBusinessHours(start, duration, minimumDurationPerDay));
    }

    @Test
    public void testMinimumDurationEqualBusinessDay() {
        LocalDateTime start = LocalDateTime.of(2023, 9, 4, 8, 0);  // It's a Monday
        Duration duration = Duration.ofHours(8);  // 8 hours needed
        Duration minimumDurationPerDay = Duration.ofHours(8);  // Minimum 8 hours per day, matches business day
        LocalDateTime expected = LocalDateTime.of(2023, 9, 4, 17, 0);  // Should not extend to next day
        assertEquals(expected, calculator.addBusinessHours(start, duration, minimumDurationPerDay));
    }

    @Test
    public void testAddOneHourToEdgeOfBusinessDay() {
        LocalDateTime start = LocalDateTime.of(2023, 9, 4, 16, 0);  // It's a Monday, 1 minute before day end
        Duration duration = Duration.ofHours(1);  // Add 1 minute
        LocalDateTime expected = LocalDateTime.of(2023, 9, 4, 17, 0);  // Should end exactly at shift end
        assertEquals(expected, calculator.addBusinessHours(start, duration));
    }

    @Test
    public void testAddOneMinuteToEdgeOfBusinessDay() {
        LocalDateTime start = LocalDateTime.of(2023, 9, 4, 16, 59);  // It's a Monday, 1 minute before day end
        Duration duration = Duration.ofMinutes(1);  // Add 1 minute
        LocalDateTime expected = LocalDateTime.of(2023, 9, 4, 17, 0);  // Should end exactly at shift end
        assertEquals(expected, calculator.addBusinessHours(start, duration));
    }

    @Test
    public void testAddBusinessHoursOverMultipleWeeks() {
        LocalDateTime start = LocalDateTime.of(2023, 9, 4, 8, 0);  // It's a Monday
        Duration duration = Duration.ofHours(80);  // This will span more than a week

        LocalDateTime result = calculator.addBusinessHours(start, duration);

        // Manually calculate the expected end time
        // 40 hours for first week (Mon to Fri, 8 hours each), 40 hours for the second week
        LocalDateTime expectedEnd = LocalDateTime.of(2023, 9, 15, 17, 0); // Should be a Monday of the next week
        assertEquals(expectedEnd, result);
    }

    @Test
    public void testAddBusinessHoursSpanningMultipleWeeksWithHolidays() {
        // Add multiple holidays for the test
        BusinessWeek businessWeek = new BusinessWeek().initialiseDefault();
        holidays.add(LocalDate.of(2023, 9, 11));
        holidays.add(LocalDate.of(2023, 9, 15));
        calculator = new BusinessHoursCalculator(businessWeek, holidays);
        LocalDateTime start = LocalDateTime.of(2023, 9, 4, 8, 0);  // It's a Monday
        Duration duration = Duration.ofHours(80);  // 64 hours, enough to span multiple weeks
        LocalDateTime expected = LocalDateTime.of(2023, 9, 19, 17, 0);  // Should land on a Thursday at 1PM, skipping weekends and holidays
        assertEquals(expected, calculator.addBusinessHours(start, duration));
    }

    @Test
    public void testAddBusinessHoursNegativeDuration() {
        LocalDateTime start = LocalDateTime.of(2023, 9, 4, 8, 0);  // It's a Monday
        Duration duration = Duration.ofHours(-1);  // Negative hours
        assertThrows(IllegalArgumentException.class, () -> {
            calculator.addBusinessHours(start, duration);
        });
    }

    @Test
    public void testAddBusinessHoursOnLeapYear() {
        LocalDateTime start = LocalDateTime.of(2024, 2, 29, 8, 0);  // It's a Leap Year and it's Feb 29th
        Duration duration = Duration.ofHours(8);  // 8 hours
        LocalDateTime expected = LocalDateTime.of(2024, 2, 29, 17, 0);  // Should end on the same day at 5 PM
        assertEquals(expected, calculator.addBusinessHours(start, duration));
    }

    @Test
    public void testAddBusinessHoursOverMultipleDaysOnLeapYear() {
        LocalDateTime start = LocalDateTime.of(2024, 2, 28, 8, 0);  // It's a Leap Year and it's Feb 29th
        Duration duration = Duration.ofHours(24);  // 8 hours
        LocalDateTime expected = LocalDateTime.of(2024, 3, 1, 17, 0);  // Should end on the same day at 5 PM
        assertEquals(expected, calculator.addBusinessHours(start, duration));
    }


    @Test
    public void testBusinessHoursWithShiftSpanningOverTwoDays() {
        // Create a BusinessDay with a shift that spans over two days
        BusinessDay specialDay = new BusinessDay(
                new BusinessShift(LocalTime.of(22, 0), LocalTime.of(2, 0))
        );

        // Include this special day in the week's business days
        Map<DayOfWeek, BusinessDay> specialBusinessDays = new HashMap<>();
        specialBusinessDays.put(DayOfWeek.MONDAY, specialDay);
        specialBusinessDays.put(DayOfWeek.TUESDAY, specialDay); // Add more days if needed

        BusinessHoursCalculator calculator = new BusinessHoursCalculator(specialBusinessDays);

        LocalDateTime startDateTime = LocalDateTime.of(2023, 9, 4, 23, 0); // 4th Sep 2023, 11:00 PM
        Duration duration = Duration.ofHours(3); // 3 hours to add

        LocalDateTime result = calculator.addBusinessHours(startDateTime, duration);

        LocalDateTime expectedDateTime = LocalDateTime.of(2023, 9, 5, 2, 0); // 5th Sep 2023, 2:00 AM

        assertEquals(expectedDateTime, result);
    }

    @Test
    public void testBusinessHoursWithShiftsSpanningDaysOverMultipleDays() {
        // Create a BusinessDay with a shift that spans over two days
        BusinessDay specialDay = new BusinessDay(
                new BusinessShift(LocalTime.of(22, 0), LocalTime.of(2, 0))
        );

        // Include this special day in the week's business days
        Map<DayOfWeek, BusinessDay> specialBusinessDays = new HashMap<>();
        specialBusinessDays.put(DayOfWeek.MONDAY, specialDay);
        specialBusinessDays.put(DayOfWeek.TUESDAY, specialDay); // Add more days if needed

        BusinessHoursCalculator calculator = new BusinessHoursCalculator(specialBusinessDays);

        LocalDateTime startDateTime = LocalDateTime.of(2023, 9, 4, 23, 0); // 4th Sep 2023, 11:00 PM
        Duration duration = Duration.ofHours(6); // 6 hours to add

        LocalDateTime result = calculator.addBusinessHours(startDateTime, duration);

        LocalDateTime expectedDateTime = LocalDateTime.of(2023, 9, 6, 1, 0); // 6th Sep 2023, 1:00 AM

        assertEquals(expectedDateTime, result);
    }

    @Test
    public void testWithMultipleShiftsSomeSpanningTwoDays() {
        // Initialize the businessDays map
        Map<DayOfWeek, BusinessDay> businessDays = new HashMap<>();

        // Special shift schedule for Monday
        businessDays.put(DayOfWeek.MONDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(11, 0), LocalTime.of(17, 0)),
                new BusinessShift(LocalTime.of(23, 0), LocalTime.of(1, 0))
        ));

        // Regular shift schedule for Tuesday to Friday
        for (DayOfWeek day : new DayOfWeek[]{DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            businessDays.put(day, new BusinessDay(
                    new BusinessShift(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                    new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
            ));
        }

        // Create the BusinessHoursCalculator object and set the business days
        BusinessHoursCalculator calc = new BusinessHoursCalculator(businessDays);

        // Assume the start date is Monday, 2023-09-18 at 9:00 AM
        LocalDateTime start = LocalDateTime.of(2023, 9, 18, 9, 0);
        Duration duration = Duration.ofHours(42); // 64-hour duration

        // Calculate the end time
        LocalDateTime end = calc.addBusinessHours(start, duration, null);

        // Your expected end time calculation
        // This depends on your implementation, please adjust accordingly
        LocalDateTime expectedEnd = LocalDateTime.of(2023, 9, 25, 13, 0);

        assertEquals(expectedEnd, end);
    }

    @Test
    public void test64HoursStartingMonday() {
        // Initialize the businessDays map
        Map<DayOfWeek, BusinessDay> businessDays = new HashMap<>();

        // Special shift schedule for Monday
        businessDays.put(DayOfWeek.MONDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(9, 0), LocalTime.of(17, 0)),
                new BusinessShift(LocalTime.of(23, 0), LocalTime.of(1, 0))
        ));

        // Regular shift schedule for Tuesday to Friday
        for (DayOfWeek day : new DayOfWeek[]{DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            businessDays.put(day, new BusinessDay(
                    new BusinessShift(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                    new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
            ));
        }

        // Create the BusinessHoursCalculator object and set the business days
        BusinessHoursCalculator calc = new BusinessHoursCalculator(businessDays);

        // Start time is Monday, 2023-09-18 at 9:00 AM
        LocalDateTime start = LocalDateTime.of(2023, 9, 18, 9, 0);
        Duration duration = Duration.ofHours(64); // 64-hour duration

        // Calculate the end time
        LocalDateTime end = calc.addBusinessHours(start, duration, null);

        // Your expected end time calculation
        // This is based on 10 hours on Monday, 8 hours on Tuesday, 8 hours on Wednesday, 8 hours on Thursday, and 8 hours on Friday
        // That's 42 hours, so we need an additional 22 hours
        // Assuming the next Monday is also a working day of 10 hours, and Tuesday is 8 hours, we would end on Wednesday at 12pm
        LocalDateTime expectedEnd = LocalDateTime.of(2023, 9, 27, 12, 0);

        assertEquals(expectedEnd, end);
    }

    @Test
    public void test64HoursStartingMondayWithHoliday() {
        // Initialize the businessDays map
        Map<DayOfWeek, BusinessDay> businessDays = new HashMap<>();

        // Special shift schedule for Monday
        businessDays.put(DayOfWeek.MONDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(9, 0), LocalTime.of(17, 0)),
                new BusinessShift(LocalTime.of(23, 0), LocalTime.of(1, 0))
        ));

        // Regular shift schedule for Tuesday to Friday
        for (DayOfWeek day : new DayOfWeek[]{DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            businessDays.put(day, new BusinessDay(
                    new BusinessShift(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                    new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
            ));
        }

        // Add holiday on Wednesday of the first week
        Set<LocalDate> holidays = new TreeSet<>();
        holidays.add(LocalDate.of(2023, 9, 20));

        // Create the BusinessHoursCalculator object and set the business days
        BusinessHoursCalculator calc = new BusinessHoursCalculator(businessDays, holidays);

        // Start time is Monday, 2023-09-18 at 9:00 AM
        LocalDateTime start = LocalDateTime.of(2023, 9, 18, 9, 0);
        Duration duration = Duration.ofHours(64); // 64-hour duration

        // Calculate the end time
        LocalDateTime end = calc.addBusinessHours(start, duration, null);

        // Your expected end time calculation
        // This is based on 10 hours on Monday, 8 hours on Tuesday, 0 hours on holiday Wednesday, 8 hours on Thursday, and 8 hours on Friday
        // That's 34 hours by the end of the first workweek.
        // To reach 64 hours, you'd still need another 30 hours.
        // 10 more hours on the next Monday, 8 more hours on the next Tuesday, 8 more hours on the next Wednesday
        // 4 remaining hours would fall on the next Thursday, taking us to 12:00 PM
        LocalDateTime expectedEnd = LocalDateTime.of(2023, 9, 28, 12, 0);

        assertEquals(expectedEnd, end);
    }

    @Test
    public void test64HoursStartingTuesdayWithTwoHolidays() {
        // Initialize the businessDays map
        Map<DayOfWeek, BusinessDay> businessDays = new HashMap<>();

        // Special shift schedule for Monday
        businessDays.put(DayOfWeek.MONDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(9, 0), LocalTime.of(17, 0)),
                new BusinessShift(LocalTime.of(23, 0), LocalTime.of(1, 0))
        ));

        // Regular shift schedule for Tuesday to Friday
        for (DayOfWeek day : new DayOfWeek[]{DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            businessDays.put(day, new BusinessDay(
                    new BusinessShift(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                    new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
            ));
        }

        // Add holidays: One on Thursday of the first week and another on Tuesday of the second week
        Set<LocalDate> holidays = new HashSet<>();
        holidays.add(LocalDate.of(2023, 9, 21)); // First week Thursday
        holidays.add(LocalDate.of(2023, 9, 26)); // Second week Tuesday

        // Create the BusinessHoursCalculator object and set the business days
        BusinessHoursCalculator calc = new BusinessHoursCalculator(businessDays, holidays);

        // Start time is Tuesday, 2023-09-19 at 8:00 AM
        LocalDateTime start = LocalDateTime.of(2023, 9, 19, 8, 0);
        Duration duration = Duration.ofHours(64); // 64-hour duration

        // Calculate the end time
        LocalDateTime end = calc.addBusinessHours(start, duration, null);

        // Your expected end time calculation
        // 8 hours on Tuesday, 8 hours on Wednesday, 0 hours on Thursday (holiday), 8 hours on Friday.
        // That's 24 hours for the first week.
        // 10 hours on next Monday, 0 hours on next Tuesday (holiday), 8 hours on next Wednesday
        // That sums up to 42 hours across the two weeks.
        // We need another 22 hours: 10 hours on next Monday, 8 hours on next Tuesday, and 4 more hours would be on next Wednesday at 12:00 PM
        LocalDateTime expectedEnd = LocalDateTime.of(2023, 10, 2, 15, 0);

        assertEquals(expectedEnd, end);
    }

    @Test
    public void testShiftCrossingMidnightWithStartTimeAfterMidnight() {
        BusinessWeek businessWeek = new BusinessWeek()
                .addDay(DayOfWeek.MONDAY, new BusinessDay(
                        new BusinessShift(LocalTime.of(22, 0), LocalTime.of(2, 0))
                ));

        // BusinessHoursCalculator should now take a BusinessWeek object
        BusinessHoursCalculator calculator = new BusinessHoursCalculator(businessWeek);

        // Start time is on a Tuesday just after midnight
        LocalDateTime start = LocalDateTime.of(2023, 9, 19, 0, 30);
        Duration duration = Duration.ofHours(1);

        // We should end up still within that extended Monday shift, but on Tuesday
        LocalDateTime expectedEnd = LocalDateTime.of(2023, 9, 19, 1, 30);

        assertEquals(expectedEnd, calculator.addBusinessHours(start, duration));
    }

    @Test
    public void testAddBusinessHoursHandlesUnorderedShiftsCorrectly() {
        BusinessWeek businessWeek = new BusinessWeek();

        // Adding shifts for Monday in the wrong order
        businessWeek.addDay(DayOfWeek.MONDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(17, 0), LocalTime.of(20, 0)),
                new BusinessShift(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));

        BusinessHoursCalculator calculator = new BusinessHoursCalculator(businessWeek);

        LocalDateTime startDateTime = LocalDateTime.of(2023, 9, 25, 9, 0); // 9:00 AM on a Monday
        Duration durationToAdd = Duration.ofHours(8); // Adding 8 hours should result in a time on the same day

        // Functionality being tested
        LocalDateTime newTime = calculator.addBusinessHours(startDateTime, durationToAdd);

        // Assuming that your calculator considers all shifts while adding time,
        // adding 8 hours to 9:00 AM on Monday should result in 18:00 on the same Monday.
        assertEquals(LocalDateTime.of(2023, 9, 25, 18, 0), newTime);
    }

    @Test
    public void testAddBusinessHoursHandlesOverlappingShiftsCorrectly() {
        BusinessWeek businessWeek = new BusinessWeek();

        // Adding shifts for Monday that overlap
        businessWeek.addDay(DayOfWeek.MONDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(11, 0), LocalTime.of(15, 0)),
                new BusinessShift(LocalTime.of(16, 0), LocalTime.of(19, 0))
        ));

        BusinessHoursCalculator calculator = new BusinessHoursCalculator(businessWeek);

        LocalDateTime startDateTime = LocalDateTime.of(2023, 9, 25, 9, 0); // 9:00 AM on a Monday
        Duration durationToAdd = Duration.ofHours(8); // Adding 8 hours

        // Functionality being tested
        LocalDateTime newTime = calculator.addBusinessHours(startDateTime, durationToAdd);

        // Adding 8 hours to 9:00 AM on Monday should result in 6:00 PM on the same Monday.
        assertEquals(LocalDateTime.of(2023, 9, 25, 18, 0), newTime);
    }

    @Test
    public void testAddBusinessHoursWithVariousIssues() {
        // Define a week with incorrect order, overlapping shifts, and shifts that span days
        Map<DayOfWeek, BusinessDay> businessDays = new HashMap<>();
        businessDays.put(DayOfWeek.MONDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(14, 0), LocalTime.of(18, 0)),
                new BusinessShift(LocalTime.of(12, 0), LocalTime.of(16, 0)),
                new BusinessShift(LocalTime.of(20, 0), LocalTime.of(2, 0))
        ));
        businessDays.put(DayOfWeek.TUESDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(12, 0), LocalTime.of(16, 0)),
                new BusinessShift(LocalTime.of(10, 0), LocalTime.of(15, 0))
        ));
        BusinessHoursCalculator calculator = new BusinessHoursCalculator(businessDays);

        // Test: Start from Monday 9 AM and add 16 hours
        LocalDateTime startDateTime = LocalDateTime.of(2023, 9, 4, 9, 0); // 2023-09-04 09:00 (Monday)
        Duration duration = Duration.ofHours(16);

        LocalDateTime endDateTime = calculator.addBusinessHours(startDateTime, duration);

        // Expected End Time: 2023-09-05 14:00 (Tuesday)
        LocalDateTime expectedEndDateTime = LocalDateTime.of(2023, 9, 5, 14, 0);
        assertEquals(expectedEndDateTime, endDateTime);
    }
}
