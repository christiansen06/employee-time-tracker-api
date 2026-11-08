package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Exception.DuplicateResourceException;
import com.tuusuario.employee_time_tracker.Exception.ResourceNotFoundException;
import com.tuusuario.employee_time_tracker.Model.Dto.BonusDayDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.BonusDayRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.BonusDay;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Repository.BonusDayRepository;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dias con bono: el admin elige el dia, el monto por hora y quienes lo
 * cobran. Como cambia la liquidacion, respeta el cierre por pago.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BonusDayService {

    private final BonusDayRepository bonusDayRepository;
    private final EmployeeRepository employeeRepository;
    private final PaidPeriodGuard paidPeriodGuard;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<BonusDayDTO> getRange(LocalDate from, LocalDate to) {
        return bonusDayRepository.findByBonusDateBetweenOrderByBonusDateAsc(from, to)
                .stream().map(this::toDTO).toList();
    }

    public BonusDayDTO create(BonusDayRequestDTO dto) {
        if (bonusDayRepository.existsByBonusDate(dto.getDate())) {
            throw new DuplicateResourceException(
                    "There is already a bonus for that day. Edit it instead.");
        }
        Set<Employee> employees = loadEmployees(dto.getEmployeeIds());
        assertNotPaid(employees, dto.getDate());

        BonusDay day = bonusDayRepository.save(BonusDay.builder()
                .bonusDate(dto.getDate())
                .amountPerHour(dto.getAmountPerHour())
                .note(clean(dto.getNote()))
                .employees(employees)
                .createdBy(currentUsername())
                .build());

        auditLogService.record("BONUS_DAY", day.getId(), "CREATE", describe(day));
        return toDTO(day);
    }

    public BonusDayDTO update(Long id, BonusDayRequestDTO dto) {
        BonusDay day = find(id);
        if (bonusDayRepository.existsByBonusDateAndIdNot(dto.getDate(), id)) {
            throw new DuplicateResourceException(
                    "There is already a bonus for that day. Edit it instead.");
        }
        Set<Employee> employees = loadEmployees(dto.getEmployeeIds());

        // Ni lo que habia ni lo que queda puede caer en un periodo pagado.
        assertNotPaid(day.getEmployees(), day.getBonusDate());
        assertNotPaid(employees, dto.getDate());

        String before = describe(day);
        day.setBonusDate(dto.getDate());
        day.setAmountPerHour(dto.getAmountPerHour());
        day.setNote(clean(dto.getNote()));
        day.getEmployees().clear();
        day.getEmployees().addAll(employees);
        BonusDay saved = bonusDayRepository.save(day);

        auditLogService.record("BONUS_DAY", id, "UPDATE", before + " -> " + describe(saved));
        return toDTO(saved);
    }

    public void delete(Long id) {
        BonusDay day = find(id);
        assertNotPaid(day.getEmployees(), day.getBonusDate());
        auditLogService.record("BONUS_DAY", id, "DELETE", describe(day));
        bonusDayRepository.delete(day);
    }

    private BonusDay find(Long id) {
        return bonusDayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bonus day not found with id: " + id));
    }

    private Set<Employee> loadEmployees(Set<Long> ids) {
        Set<Employee> employees = new HashSet<>(employeeRepository.findAllById(ids));
        if (employees.size() != ids.size()) {
            throw new ResourceNotFoundException("Some employees were not found.");
        }
        return employees;
    }

    private void assertNotPaid(Set<Employee> employees, LocalDate date) {
        employees.forEach(e -> paidPeriodGuard.assertNotPaid(e.getId(), date));
    }

    private BonusDayDTO toDTO(BonusDay day) {
        List<Employee> sorted = day.getEmployees().stream()
                .sorted(Comparator.comparing(Employee::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Employee::getLastName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return BonusDayDTO.builder()
                .id(day.getId())
                .date(day.getBonusDate())
                .amountPerHour(day.getAmountPerHour())
                .note(day.getNote())
                .employeeIds(sorted.stream().map(Employee::getId).toList())
                .employeeNames(sorted.stream()
                        .map(e -> e.getName() + " " + e.getLastName()).toList())
                .build();
    }

    private String describe(BonusDay day) {
        return day.getBonusDate() + " $" + day.getAmountPerHour() + "/h employees="
                + day.getEmployees().stream().map(Employee::getId).sorted().toList();
    }

    private String clean(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
