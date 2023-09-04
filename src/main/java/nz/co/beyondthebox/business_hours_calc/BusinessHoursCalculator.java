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

}

