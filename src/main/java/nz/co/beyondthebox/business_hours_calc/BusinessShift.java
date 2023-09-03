package nz.co.beyondthebox.business_hours_calc;

import java.time.LocalTime;

public class BusinessShift {
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
