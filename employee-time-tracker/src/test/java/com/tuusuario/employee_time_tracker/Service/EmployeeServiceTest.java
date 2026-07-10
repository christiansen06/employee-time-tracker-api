package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Exception.DuplicateResourceException;
import com.tuusuario.employee_time_tracker.Model.Dto.EmployeeRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.EmployeeResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import com.tuusuario.employee_time_tracker.Repository.TimeEntryRepository;
import com.tuusuario.employee_time_tracker.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private CurrentEmployeeService currentEmployeeService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserRepository userRepository;
    @Mock private TimeEntryRepository timeEntryRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private EmployeeService service;

    private EmployeeRequestDTO request(String email) {
        EmployeeRequestDTO dto = new EmployeeRequestDTO();
        dto.setName("Juan");
        dto.setLastName("Perez");
        dto.setEmail(email);
        dto.setPosition("Cocina");
        return dto;
    }

    @Test
    void createEmployeeRejectsDuplicateEmail() {
        when(employeeRepository.existsByEmail("juan@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createEmployee(request("juan@test.com")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createEmployeeStartsActive() {
        when(employeeRepository.existsByEmail("juan@test.com")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        EmployeeResponseDTO dto = service.createEmployee(request("juan@test.com"));

        assertThat(dto.getActive()).isTrue();
        assertThat(dto.isHasPin()).isFalse();
    }

    @Test
    void updateEmployeeRejectsEmailOfAnotherEmployee() {
        Employee existing = Employee.builder().id(1L).name("Juan").lastName("Perez")
                .email("juan@test.com").position("Cocina").active(true).build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.existsByEmailAndIdNot("otro@test.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateEmployee(1L, request("otro@test.com")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deactivateEmployeeSetsInactive() {
        Employee existing = Employee.builder().id(1L).name("Juan").lastName("Perez")
                .email("juan@test.com").position("Cocina").active(true).build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.deactivateEmployee(1L);

        assertThat(existing.getActive()).isFalse();
    }

    @Test
    void deleteEmployeeWithoutHistoryDeletesAndAudits() {
        Employee existing = Employee.builder().id(1L).name("Juan").lastName("Perez")
                .email("juan@test.com").position("Cocina").active(true).build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(timeEntryRepository.existsByEmployeeId(1L)).thenReturn(false);
        when(userRepository.existsByEmployee_Id(1L)).thenReturn(false);

        service.deleteEmployee(1L);

        verify(employeeRepository).delete(existing);
        verify(auditLogService).record(org.mockito.ArgumentMatchers.eq("EMPLOYEE"),
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("DELETE"),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void deleteEmployeeWithTimeEntriesIsRejected() {
        Employee existing = Employee.builder().id(1L).name("Juan").lastName("Perez")
                .email("juan@test.com").position("Cocina").active(true).build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(timeEntryRepository.existsByEmployeeId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteEmployee(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Deactivate");
        verify(employeeRepository, never()).delete(any(Employee.class));
    }

    @Test
    void deleteEmployeeWithLinkedAccountIsRejected() {
        Employee existing = Employee.builder().id(1L).name("Juan").lastName("Perez")
                .email("juan@test.com").position("Cocina").active(true).build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(timeEntryRepository.existsByEmployeeId(1L)).thenReturn(false);
        when(userRepository.existsByEmployee_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteEmployee(1L))
                .isInstanceOf(IllegalStateException.class);
        verify(employeeRepository, never()).delete(any(Employee.class));
    }

    @Test
    void setPinStoresHashedPin() {
        Employee existing = Employee.builder().id(1L).name("Juan").lastName("Perez")
                .email("juan@test.com").position("Cocina").active(true).build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("1234")).thenReturn("$pinhash$");
        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.setPin(1L, "1234");

        assertThat(existing.getPinHash()).isEqualTo("$pinhash$");
    }
}
