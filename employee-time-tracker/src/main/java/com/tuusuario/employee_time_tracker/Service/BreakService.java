package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Exception.ResourceNotFoundException;
import com.tuusuario.employee_time_tracker.Model.Dto.BreakEndRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.BreakResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.BreakStartRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.BreakEntry;
import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Model.Enums.BreakStatus;
import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;
import com.tuusuario.employee_time_tracker.Repository.BreakEntryRepository;
import com.tuusuario.employee_time_tracker.Repository.TimeEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class BreakService {
    private final BreakEntryRepository breakEntryRepository;
    private final TimeEntryRepository timeEntryRepository;

    public BreakService(BreakEntryRepository breakEntryRepository, TimeEntryRepository timeEntryRepository) {
        this.breakEntryRepository = breakEntryRepository;
        this.timeEntryRepository = timeEntryRepository;
    }

    //STAR BREAK
    public BreakResponseDTO startBreak(BreakStartRequestDTO dto) {
        TimeEntry timeEntry = timeEntryRepository.findById(dto.getTimeEntryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Time entry not found with id: " + dto.getTimeEntryId()));

        //Validar que no exista un Break Activo
        boolean hasActiveBreak = timeEntry.getBreaks() != null
                && timeEntry.getBreaks().stream()
                .anyMatch(b -> b.getBreakStatus() == BreakStatus.ON_BREAK);

        if(hasActiveBreak) {
            throw new IllegalStateException(
                    "There is already active break for this time entry"
            );
        }
        //Validar que el time entry este activo
        if(timeEntry.getStatus() != TimeEntryStatus.CLOCKED_IN) {
            throw new IllegalStateException(
                    "Cannot start break because employee is not clocked in");
        }

        BreakEntry breakEntry = BreakEntry.builder()
                .breakStart(LocalDateTime.now())
                .breakStatus(BreakStatus.ON_BREAK)
                .timeEntry(timeEntry)
                .build();

        timeEntry.setStatus(TimeEntryStatus.ON_BREAK);
        timeEntryRepository.save(timeEntry);

        BreakEntry savedBreak = breakEntryRepository.save(breakEntry);
        return mapToDTO(savedBreak);
    }

    //END BREAK
    public BreakResponseDTO endBreak(BreakEndRequestDTO dto) {

        BreakEntry breakEntry = breakEntryRepository.findById(dto.getBreakEntryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Break entry not found with id: " + dto.getBreakEntryId()));

        //Validar que el Break este activo
        if(breakEntry.getBreakStatus() != BreakStatus.ON_BREAK) {
            throw new IllegalStateException(
                    "This Break has already been finished.");
        }

        LocalDateTime endTime = LocalDateTime.now();
        long duration = Duration.between(breakEntry.getBreakStart(), endTime).toMinutes();

        breakEntry.setBreakEnd(endTime);
        breakEntry.setDurationMinutes(duration);
        breakEntry.setBreakStatus(BreakStatus.FINISHED);

        TimeEntry timeEntry = breakEntry.getTimeEntry();
        if (timeEntry != null) {
            timeEntry.setStatus(TimeEntryStatus.CLOCKED_IN);
            timeEntryRepository.save(timeEntry);
        }

        BreakEntry updateBreak = breakEntryRepository.save(breakEntry);
        return mapToDTO(updateBreak);
    }

    public List<BreakResponseDTO> getActiveBreaks() {
        return breakEntryRepository.findByBreakStatus(BreakStatus.ON_BREAK)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    //MAPPER
    private BreakResponseDTO mapToDTO(BreakEntry breakEntry) {
        return BreakResponseDTO.builder()
                .id(breakEntry.getId())
                .breakStart(breakEntry.getBreakStart())
                .breakEnd(breakEntry.getBreakEnd())
                .durationMinutes(breakEntry.getDurationMinutes())
                .breakStatus(breakEntry.getBreakStatus())
                .build();
    }
}
