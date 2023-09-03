package nz.co.beyondthebox.business_hours_calc;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BusinessDay {
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
