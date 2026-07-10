package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Exception.ResourceNotFoundException;
import com.tuusuario.employee_time_tracker.Model.Dto.CurrentStatusDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Enums.WorkState;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KioskServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TimeEntryService timeEntryService;
    @Mock private BreakService breakService;
    @Mock private EmployeeService employeeService;

    @InjectMocks private KioskService service;

    private Employee employeeWithPin() {
        return Employee.builder().id(1L).name("Juan").lastName("Perez")
                .email("juan@test.com").position("Cocina")
                .active(true).pinHash("$hash$").build();
    }

    @Test
    void verifyFailsForUnknownEmployee() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(99L, "1234"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void verifyFailsForInactiveEmployee() {
        Employee emp = employeeWithPin();
        emp.setActive(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));

        assertThatThrownBy(() -> service.verify(1L, "1234"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void verifyFailsWithoutConfiguredPin() {
        Employee emp = employeeWithPin();
        emp.setPinHash(null);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));

        assertThatThrownBy(() -> service.verify(1L, "1234"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PIN");
    }

    @Test
    void verifyFailsWithWrongPin() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employeeWithPin()));
        when(passwordEncoder.matches("0000", "$hash$")).thenReturn(false);

        assertThatThrownBy(() -> service.verify(1L, "0000"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void fiveWrongAttemptsLockTheEmployee() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employeeWithPin()));
        when(passwordEncoder.matches("0000", "$hash$")).thenReturn(false);
        lenient().when(passwordEncoder.matches("1234", "$hash$")).thenReturn(true);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.verify(1L, "0000"))
                    .isInstanceOf(BadCredentialsException.class);
        }

        // Aun con el PIN correcto, el empleado queda bloqueado un minuto.
        assertThatThrownBy(() -> service.verify(1L, "1234"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("intentos");
    }

    @Test
    void correctPinReturnsCurrentStatus() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employeeWithPin()));
        when(passwordEncoder.matches("1234", "$hash$")).thenReturn(true);
        when(timeEntryService.getStatusByEmployeeId(1L)).thenReturn(
                CurrentStatusDTO.builder().employeeId(1L).state(WorkState.NO_SHIFT).build());

        CurrentStatusDTO status = service.verify(1L, "1234");

        assertThat(status.getState()).isEqualTo(WorkState.NO_SHIFT);
    }

    @Test
    void wrongPinCounterResetsAfterSuccess() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employeeWithPin()));
        when(passwordEncoder.matches("0000", "$hash$")).thenReturn(false);
        when(passwordEncoder.matches("1234", "$hash$")).thenReturn(true);
        when(timeEntryService.getStatusByEmployeeId(1L)).thenReturn(
                CurrentStatusDTO.builder().employeeId(1L).state(WorkState.NO_SHIFT).build());

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.verify(1L, "0000"))
                    .isInstanceOf(BadCredentialsException.class);
        }
        // Acierta antes del 5to fallo: el contador se limpia.
        service.verify(1L, "1234");

        // Y puede volver a fallar sin quedar bloqueado.
        assertThatThrownBy(() -> service.verify(1L, "0000"))
                .isInstanceOf(BadCredentialsException.class);
    }
}
