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
        boolean finishedAtMidnight;
        // This flag makes sure that it runs at least once to allow moving date to business day if duration passed is Zero
        boolean firstRunAllowZero = true;
        while (firstRunAllowZero || !duration.isZero()) {
            finishedAtMidnight = false;
            // Check if the current day is a business day
            BusinessDay currentBusinessDay = businessDays.get(endDateTime.getDayOfWeek());
            if (currentBusinessDay != null && !isHoliday(endDateTime)) {
                firstRunAllowZero = false;

                Duration availableWorkHours = minimumDurationPerDay != null && minimumDurationPerDay.compareTo(currentBusinessDay.getBusinessDayLength()) >= 0
                        ? minimumDurationPerDay
                        : currentBusinessDay.getBusinessDayLength();

                // Excluding the first day, skip over entire day if possible
                if (!firstDay && duration.compareTo(availableWorkHours) > 0) {
                    duration = duration.minus(availableWorkHours);
                }
                else {
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

                        if (shift.getEndTime().equals(LocalTime.MIDNIGHT)) {
                            shiftEnd = shiftEnd.plusDays(1);
                            finishedAtMidnight = true;
                        }

                        if(endDateTime.isAfter(shiftEnd)) {
                            continue;
                        }

                        if (endDateTime.isBefore(shiftStart)) {
                            endDateTime = shiftStart;
                        }

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

            if (finishedAtMidnight) {
                // Don't go back to midnight, start at the end time of the shift that crossed midnight
                endDateTime = endDateTime.with(currentBusinessDay.getFinalShiftEndTime());
                finishedAtMidnight = false;  // Reset the flag
            } else {
                // If no shifts crossed midnight, then reset the time to midnight
                endDateTime = endDateTime.plusDays(1).with(LocalTime.of(0, 0));
            }
        }

        return endDateTime;
    }

    public Duration calculateWorkingDurationBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return calculateWorkingDurationBetween(startDateTime, endDateTime, null);
    }

    public Duration calculateWorkingDurationBetween(LocalDateTime startDateTime, LocalDateTime endDateTime, Duration minimumDurationPerDay) {
        if(startDateTime.isAfter(endDateTime)) {
            throw new IllegalArgumentException("startDateTime must be before endDateTime.");
        }

        if (minimumDurationPerDay != null && minimumDurationPerDay.isNegative()) {
            throw new IllegalArgumentException("minimumDurationPerDay must be a positive number.");
        }

        Duration totalWorkingDuration = Duration.ZERO;
        LocalDateTime currentDateTime = startDateTime;
        boolean finishedAtMidnight;
        while (currentDateTime.isBefore(endDateTime)) {
            finishedAtMidnight = false;
            BusinessDay currentBusinessDay = businessDays.get(currentDateTime.getDayOfWeek());
            if (currentBusinessDay != null && !isHoliday(currentDateTime)) {
                Duration availableWorkHours = currentBusinessDay.getBusinessDayLength();

                for (BusinessShift shift : currentBusinessDay.getShifts()) {
                    LocalDateTime shiftStart = LocalDateTime.of(currentDateTime.toLocalDate(), shift.getStartTime());
                    LocalDateTime shiftEnd = LocalDateTime.of(currentDateTime.toLocalDate(), shift.getEndTime());

                    if (shift.getEndTime().equals(LocalTime.MIDNIGHT)) {
                        shiftEnd = shiftEnd.plusDays(1);
                        finishedAtMidnight = true;
                    }
                    if(currentDateTime.isAfter(shiftEnd)) {
                        continue;
                    }

                    if (minimumDurationPerDay != null && currentBusinessDay.isLastShift(shift)) {
                        if (currentBusinessDay.getBusinessDayLength().compareTo(minimumDurationPerDay) < 0) {
                            shiftEnd = shiftEnd.plus(minimumDurationPerDay.minus(currentBusinessDay.getBusinessDayLength()));
                        }
                    }

                    // Calculate duration in the overlapping time within this shift
                    LocalDateTime effectiveStart = currentDateTime.isBefore(shiftStart) ? shiftStart : currentDateTime;
                    LocalDateTime effectiveEnd = endDateTime.isAfter(shiftEnd) ? shiftEnd : endDateTime;
                    Duration overlap = Duration.between(effectiveStart, effectiveEnd);

                    // Add the overlapping time to the total working duration
                    totalWorkingDuration = totalWorkingDuration.plus(overlap);

                    // Move the current time to the end of this shift
                    currentDateTime = shiftEnd;
                    if(currentDateTime.equals(endDateTime) || currentDateTime.isAfter(endDateTime)) {
                        return totalWorkingDuration;
                    }
                }

            }
            if (finishedAtMidnight) {
                // Don't go back to midnight, start at the end time of the shift that crossed midnight
                currentDateTime = currentDateTime.with(currentBusinessDay.getFinalShiftEndTime());
                finishedAtMidnight = false;  // Reset the flag
            } else {
                // If no shifts crossed midnight, then reset the time to midnight
                currentDateTime = currentDateTime.plusDays(1).with(LocalTime.of(0, 0));
            }
        }

        return totalWorkingDuration;
    }


    private boolean isHoliday(LocalDateTime dateTime) {
        return holidays.contains(dateTime.toLocalDate());
    }

}

