package com.tuusuario.employee_time_tracker.Controller;

import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Service.TimeEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/time-entries")
@RequiredArgsConstructor
public class TimeEntryController {

    private final TimeEntryService timeEntryService;

    @PostMapping("/clock-in/{employeeId}")
    public ResponseEntity<TimeEntry> clockIn(@PathVariable Long employeeId) {
        return ResponseEntity.ok(timeEntryService.clockIn(employeeId));
    }

    @PostMapping("/clock-out/{timeEntryId}")
    public ResponseEntity<TimeEntry> clockOut(@PathVariable Long timeEntryId) {
        return ResponseEntity.ok(timeEntryService.clockOut(timeEntryId));
    }
}
