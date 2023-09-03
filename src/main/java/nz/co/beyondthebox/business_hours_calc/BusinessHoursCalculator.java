package nz.co.beyondthebox.business_hours_calc;

import java.time.*;
import java.util.*;

public class BusinessHoursCalculator {
    private final Map<DayOfWeek, BusinessDay> businessDays;
    private final Set<LocalDate> holidays;

    public BusinessHoursCalculator(BusinessWeek businessWeek) {
        this(businessWeek.getBusinessDays());
    }

    public BusinessHoursCalculator(Map<DayOfWeek, BusinessDay> businessDays) {
        this.businessDays = businessDays;
        this.holidays = new TreeSet<>();
    }

    public BusinessHoursCalculator(BusinessWeek businessWeek, Set<LocalDate> holidays) {
        this(businessWeek.getBusinessDays(), holidays);
    }

    public BusinessHoursCalculator(Map<DayOfWeek, BusinessDay> businessDays, Set<LocalDate> holidays) {
        this.businessDays = businessDays;
        this.holidays = holidays;
    }

    public LocalDateTime addBusinessHours(LocalDateTime startDateTime, Duration duration) {
        return addBusinessHours(startDateTime, duration, null);
    }


    public LocalDateTime addBusinessHours(LocalDateTime startDateTime, Duration duration, Duration minimumDurationPerDay) {
        if(minimumDurationPerDay != null && minimumDurationPerDay.isNegative()) {
            throw new IllegalArgumentException("minimumDurationPerDay must be a positive number.");
        }
        if(duration.isNegative()) {
            throw new IllegalArgumentException("duration must be a positive number.");
        }
        LocalDateTime endDateTime = startDateTime;
        boolean firstDay = true;
        boolean crossedMidnight = false;
        while (!duration.isZero()) {
            crossedMidnight = false;
            // Check if the current day is a business day
            BusinessDay currentBusinessDay = businessDays.get(endDateTime.getDayOfWeek());
            if (currentBusinessDay != null && !isHoliday(endDateTime)) {

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
                    for (BusinessShift shift : currentBusinessDay.getShifts()) {

                        LocalDateTime shiftStart = LocalDateTime.of(
                                endDateTime.toLocalDate(),
                                shift.getStartTime()
                        );
                        LocalDateTime shiftEnd = LocalDateTime.of(
                                endDateTime.toLocalDate(),
                                shift.getEndTime()
                        );

                        if (shift.getEndTime().isBefore(shift.getStartTime())) {
                            shiftEnd = shiftEnd.plusDays(1);
                            crossedMidnight = true;
                        }

                        if(endDateTime.isAfter(shiftEnd)) {
                            continue;
                        }

                        if (endDateTime.isBefore(shiftStart)) {
                            endDateTime = shiftStart;
                        }

//                        If the total shift time in a day is less than the minimum required hours
//                        then extend the final shift into overtime
                        if (minimumDurationPerDay != null && currentBusinessDay.isLastShift(shift)) {
                            if (currentBusinessDay.getBusinessDayLength().compareTo(minimumDurationPerDay) < 0) {
                                shiftEnd = shiftEnd.plus(minimumDurationPerDay.minus(currentBusinessDay.getBusinessDayLength()));
                            }
                        }

                        Duration remainingDuration = Duration.between(endDateTime, shiftEnd);
                        if (duration.compareTo(remainingDuration) > 0) {
                            endDateTime = shiftEnd;
                            duration = duration.minus(remainingDuration);
                        } else {
                            return endDateTime.plus(duration);
                        }
                    }
                }
            }

            if (crossedMidnight) {
                // Don't go back to midnight, start at the end time of the shift that crossed midnight
                endDateTime = endDateTime.with(currentBusinessDay.getFinalShiftEndTime());
                crossedMidnight = false;  // Reset the flag
            } else {
                // If no shifts crossed midnight, then reset the time to midnight
                endDateTime = endDateTime.plusDays(1).with(LocalTime.of(0, 0));
            }
        }

        return endDateTime;
    }

    private boolean isHoliday(LocalDateTime dateTime) {
        return holidays.contains(dateTime.toLocalDate());
    }

    public static void main(String[] args) {


        long startTime = System.nanoTime(); // Get the current time in nanoseconds
        long iterations = 1000000;

        LocalDateTime endDateTime = null;
        // Code to be benchmarked goes here
        BusinessHoursCalculator calculator = new BusinessHoursCalculator(BusinessWeekTemplate.DEFAULT(), BusinessHolidays.DEFAULT());
        for (int i = 0; i < iterations; i++) {
            LocalDateTime startDateTime = LocalDateTime.of(2023, 9, 1, 8, 5); // Example start date and time
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
    private final List<BusinessShift> shifts = new ArrayList<>();

    private Duration businessDayLength = Duration.ZERO;

    public BusinessDay(BusinessShift... shifts) {
        this.shifts.addAll(List.of(shifts));
        for (BusinessShift shift : shifts) {
            addShiftToBusinessDayLength(shift);
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

    private void addShiftToBusinessDayLength(BusinessShift shift) {
        if (shift.getEndTime().isBefore(shift.getStartTime())) {
            // For shifts that cross midnight, calculate the time duration across the day boundary
            Duration beforeMidnight = Duration.between(shift.getStartTime(), LocalTime.of(23, 59, 59)).plusSeconds(1);
            Duration afterMidnight = Duration.between(LocalTime.of(0, 0), shift.getEndTime());
            businessDayLength = businessDayLength.plus(beforeMidnight).plus(afterMidnight);
        } else {
            // For regular shifts that don't cross midnight
            businessDayLength = businessDayLength.plus(Duration.between(shift.getStartTime(), shift.getEndTime()));
        }
    }

    public LocalTime getFinalShiftEndTime() {
        return shifts.get(shifts.size() - 1).getEndTime();
    }

    public void addShift(BusinessShift shift) {
        shifts.add(shift);
        addShiftToBusinessDayLength(shift);
    }
}

class BusinessWeekTemplate {
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

class BusinessHolidays {
    public static TreeSet<LocalDate> DEFAULT() {
        TreeSet<LocalDate> holidays = new TreeSet<>();
        holidays.add(LocalDate.of(2023,10,23));
        return holidays;
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

class BusinessWeek {
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

