package nz.co.beyondthebox.business_hours_calc;

import java.time.*;
import java.util.*;

public class BusinessHoursCalculator {
    private Map<DayOfWeek, BusinessDay> businessDays;

    private List<LocalDate> holidays;

    public BusinessHoursCalculator() {
        businessDays = new TreeMap<>();
        holidays = new ArrayList<>();
        // Define your business hours for each day of the week
        // Example: Monday to Friday with two shifts
        businessDays.put(DayOfWeek.MONDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        businessDays.put(DayOfWeek.TUESDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        businessDays.put(DayOfWeek.WEDNESDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        businessDays.put(DayOfWeek.THURSDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));
        businessDays.put(DayOfWeek.FRIDAY, new BusinessDay(
                new BusinessShift(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new BusinessShift(LocalTime.of(13, 0), LocalTime.of(17, 0))
        ));

        holidays.add(LocalDate.of(2023,10,23));
    }

    public BusinessHoursCalculator(Map<DayOfWeek, BusinessDay> businessDays) {
        this.businessDays = businessDays;
    }

    public LocalDateTime addBusinessHours(LocalDateTime startDateTime, Duration duration) {
        return addBusinessHours(startDateTime, duration, null);
    }


    public LocalDateTime addBusinessHours(LocalDateTime startDateTime, Duration duration, Duration minimumDurationPerDay) {
        LocalDateTime endDateTime = startDateTime;
        boolean firstDay = true;
        while (duration.toMinutes() > 0) {

            // Check if the current day is a business day
            BusinessDay currentBusinessDay = businessDays.get(endDateTime.getDayOfWeek());
            if (currentBusinessDay != null && !isHoliday(endDateTime.toLocalDate())) {

                Duration availableWorkHours = minimumDurationPerDay != null && minimumDurationPerDay.compareTo(currentBusinessDay.getBusinessDayLength()) >= 0
                        ? minimumDurationPerDay
                        : currentBusinessDay.getBusinessDayLength();

                // Excluding the first day, skip over entire day if possible
                if (!firstDay && duration.compareTo(availableWorkHours) > 0) {
                    duration = duration.minus(availableWorkHours);
                } else if (!firstDay && duration.compareTo(availableWorkHours) == 0) {
                    endDateTime = LocalDateTime.of(
                            endDateTime.toLocalDate(), currentBusinessDay.getFinalShiftEndTime());
                    return endDateTime;
                } else {
                    firstDay = false;
                    // Calculate the duration within business hours for the final day
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

                        if (minimumDurationPerDay != null && currentBusinessDay.isLastShift(shift)) {
                            if (dayWorkingHours.compareTo(minimumDurationPerDay) < 0) {
                                shiftEnd = shiftEnd.plus(minimumDurationPerDay.minus(dayWorkingHours));
                            }
                        }

                        Duration remainingDuration = Duration.between(endDateTime, shiftEnd);
                        if (duration.compareTo(remainingDuration) > 0) {
                            endDateTime = shiftEnd;
                            duration = duration.minus(remainingDuration);
                        } else {
                            endDateTime = endDateTime.plus(duration);
                            return endDateTime;
                        }
                    }
                }
            }

            endDateTime = endDateTime.plusDays(1);
            endDateTime = LocalDateTime.of(endDateTime.toLocalDate(), LocalTime.of(0, 0));
        }

        return endDateTime;
    }

    private boolean isHoliday(LocalDate date) {
        return holidays.contains(date);
    }

    public static void main(String[] args) {


        long startTime = System.nanoTime(); // Get the current time in nanoseconds
        long iterations = 1000000;

        LocalDateTime endDateTime = null;
        // Code to be benchmarked goes here
        BusinessHoursCalculator calculator = new BusinessHoursCalculator();
        for (int i = 0; i < iterations; i++) {
            LocalDateTime startDateTime = LocalDateTime.of(2023, 9, 1, 7, 0); // Example start date and time
            Duration duration = Duration.ofHours(320); // Example duration of 6 hours
//            Duration minimumDurationPerDay = Duration.ofHours(11); // Example minimum duration per day
            endDateTime = calculator.addBusinessHours(startDateTime, duration);
        }
        System.out.println("End time after adding business hours: " + endDateTime);

        long endTime = System.nanoTime(); // Get the current time again
        long runTime = (endTime - startTime);

        // Convert nanoseconds to milliseconds for readability
        double milliseconds = runTime / 1e6;
        long timePerIteration = runTime / iterations;

        System.out.println("Execution time: " + milliseconds + " milliseconds");
        System.out.println("Execution time per iteration: " + timePerIteration + " nanoseconds");
    }
}

class BusinessDay {
    private final List<BusinessShift> shifts;

    private Duration businessDayLength = Duration.ZERO;

    public BusinessDay(BusinessShift... shifts) {
        this.shifts = List.of(shifts);
        for (BusinessShift shift : shifts) {
            businessDayLength = businessDayLength.plus(Duration.between(shift.getStartTime(), shift.getEndTime()));
        }
    }

    public List<BusinessShift> getShifts() {
        return shifts;
    }

    public Boolean isLastShift(BusinessShift shift) {
        return shifts.indexOf(shift) == shifts.size() - 1;
    }

    public Duration getBusinessDayLength() {
        return businessDayLength;
    }

    public LocalTime getFinalShiftEndTime() {
        return shifts.get(shifts.size() - 1).getEndTime();
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


