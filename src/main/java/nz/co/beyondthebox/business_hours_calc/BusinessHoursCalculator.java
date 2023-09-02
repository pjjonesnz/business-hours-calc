package nz.co.beyondthebox.business_hours_calc;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BusinessHoursCalculator {
    private final List<BusinessDay> businessDays;

    public BusinessHoursCalculator() {
        businessDays = new ArrayList<>();
        // Define your business hours for each day of the week
        // Example: Monday to Friday with two shifts
        businessDays.add(new BusinessDay(
                DayOfWeek.MONDAY,
                new BusinessShift(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        businessDays.add(new BusinessDay(
                DayOfWeek.TUESDAY,
                new BusinessShift(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        businessDays.add(new BusinessDay(
                DayOfWeek.WEDNESDAY,
                new BusinessShift(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        businessDays.add(new BusinessDay(
                DayOfWeek.THURSDAY,
                new BusinessShift(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        businessDays.add(new BusinessDay(
                DayOfWeek.FRIDAY,
                new BusinessShift(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
    }

    public LocalDateTime addBusinessHours(LocalDateTime startDateTime, Duration duration) {
        return addBusinessHours(startDateTime, duration, null);
    }


    public LocalDateTime addBusinessHours(LocalDateTime startDateTime, Duration duration, Duration minimumDurationPerDay) {
        LocalDateTime endDateTime = startDateTime;

        while (duration.toMinutes() > 0) {
            // Check if the current day is a business day
            BusinessDay currentBusinessDay = getBusinessDay(endDateTime.getDayOfWeek());
            if (currentBusinessDay != null) {
                // Calculate the duration within business hours for the current day

                Duration dayWorkingHours = Duration.ZERO;
                for (BusinessShift shift : currentBusinessDay.getShifts()) {
                    LocalDateTime shiftStart = LocalDateTime.of(
                            endDateTime.toLocalDate(),
                            shift.getStartTime()
                    );
                    LocalDateTime shiftEnd = LocalDateTime.of(
                            endDateTime.toLocalDate(),
                            shift.getEndTime()
                    );

                    // Add the length of the shift to the working hours for the day
                    Duration shiftDuration = Duration.between(shiftStart, shiftEnd);
                    dayWorkingHours = dayWorkingHours.plus(shiftDuration);

                    if (endDateTime.isBefore(shiftStart)) {
                        endDateTime = shiftStart;
                    }

                    if(minimumDurationPerDay != null && currentBusinessDay.isLastShift(shift)) {
                        if (dayWorkingHours.compareTo(minimumDurationPerDay) < 0) {
                            shiftEnd = shiftEnd.plus(minimumDurationPerDay.minus(dayWorkingHours));
                        }
                    }

                    Duration remainingDuration = Duration.between(endDateTime, shiftEnd);
                    if (duration.compareTo(remainingDuration) >= 0) {
                        endDateTime = shiftEnd;
                        duration = duration.minus(remainingDuration);
                    } else {
                        endDateTime = endDateTime.plus(duration);
                        duration = Duration.ZERO;
                        break;
                    }
                }
            }

            if (duration.toMinutes() > 0) {
                // Move to the next day
                endDateTime = endDateTime.plusDays(1);
                endDateTime = LocalDateTime.of(endDateTime.toLocalDate(), LocalTime.of(0, 0));
            }
        }

        return endDateTime;
    }

    private BusinessDay getBusinessDay(DayOfWeek dayOfWeek) {
        for (BusinessDay businessDay : businessDays) {
            if (businessDay.getDayOfWeek() == dayOfWeek) {
                return businessDay;
            }
        }
        return null; // Not a business day
    }

    public static void main(String[] args) {

        long startTime = System.nanoTime(); // Get the current time in nanoseconds
        long iterations = 10000;

        LocalDateTime endDateTime = null;
        // Code to be benchmarked goes here
        for (int i = 0; i < iterations; i++) {
            BusinessHoursCalculator calculator = new BusinessHoursCalculator();
            LocalDateTime startDateTime = LocalDateTime.of(2023, 9, 1, 10, 0); // Example start date and time
            Duration duration = Duration.ofHours(320); // Example duration of 6 hours
//            Duration minimumDurationPerDay = Duration.ofHours(8); // Example minimum duration per day
            endDateTime = calculator.addBusinessHours(startDateTime, duration);
        }
        System.out.println("End time after adding business hours: " + endDateTime);

        long endTime = System.nanoTime(); // Get the current time again
        long runTime = (endTime - startTime);

        // Convert nanoseconds to milliseconds for readability
        double milliseconds = runTime / 1e6;
        double timePerIteration = milliseconds / iterations;

        System.out.println("Execution time: " + milliseconds + " milliseconds");
        System.out.println("Execution time per iteration: " + timePerIteration + " milliseconds");
    }
}

class BusinessDay {
    private DayOfWeek dayOfWeek;
    private List<BusinessShift> shifts;

    public BusinessDay(DayOfWeek dayOfWeek, BusinessShift... shifts) {
        this.dayOfWeek = dayOfWeek;
        this.shifts = List.of(shifts);
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public List<BusinessShift> getShifts() {
        return shifts;
    }

    public Boolean isLastShift(BusinessShift shift) {
        return shifts.indexOf(shift) == shifts.size() - 1;
    }
}

class BusinessShift {
    private LocalTime startTime;
    private LocalTime endTime;

    public BusinessShift(LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }
}


