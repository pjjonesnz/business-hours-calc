package nz.co.beyondthebox.business_hours_calc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BusinessHoursCalculatorTest {

    private BusinessHoursCalculator calculator;
    private Map<DayOfWeek, BusinessDay> businessDays;
    private TreeSet<LocalDate> holidays;

    @BeforeEach
    public void setUp() {
        businessDays = BusinessWeek.DEFAULT();
        holidays = BusinessHolidays.DEFAULT();
        calculator = new BusinessHoursCalculator(businessDays, holidays);
    }

    @Test
    public void testAddBusinessHoursWithinOneDay() {
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
        holidays.add(LocalDate.of(2023, 10, 24));
        calculator = new BusinessHoursCalculator(businessDays, holidays);

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


}
