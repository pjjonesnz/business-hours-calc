package nz.co.beyondthebox.business_hours_calc;

import java.time.LocalTime;

public class BusinessShift implements Comparable<BusinessShift>{
    private final LocalTime startTime;
    private final LocalTime endTime;

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

    @Override
    public int compareTo(BusinessShift other) {
        return this.startTime.compareTo(other.startTime);
    }
}
