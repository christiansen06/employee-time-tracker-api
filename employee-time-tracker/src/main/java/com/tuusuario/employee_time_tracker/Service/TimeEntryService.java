package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Enums.TimeEntryStatus;
import com.tuusuario.employee_time_tracker.Model.Employee;
import com.tuusuario.employee_time_tracker.Model.TimeEntry;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import com.tuusuario.employee_time_tracker.Repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TimeEntryService {
    private final TimeEntryRepository timeEntryRepository;
    private final EmployeeRepository employeeRepository;

    public TimeEntry clockIn(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        TimeEntry timeEntry = TimeEntry.builder()
                .clockIn(LocalDateTime.now())
                .status(TimeEntryStatus.WORKING)
                .employee(employee)
                .build();

        return timeEntryRepository.save(timeEntry);
    }

    public TimeEntry clockOut(Long timeEntryId) {

        TimeEntry timeEntry = timeEntryRepository.findById(timeEntryId)
                .orElseThrow(() -> new RuntimeException("TimeEntry not found"));

        timeEntry.setClockOut(LocalDateTime.now());
        timeEntry.setStatus(TimeEntryStatus.FINISHED);

        return timeEntryRepository.save(timeEntry);
    }
}
