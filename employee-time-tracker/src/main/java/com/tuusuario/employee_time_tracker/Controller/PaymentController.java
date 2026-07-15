package com.tuusuario.employee_time_tracker.Controller;

import com.tuusuario.employee_time_tracker.Model.Dto.CreatePaymentRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.PaymentDTO;
import com.tuusuario.employee_time_tracker.Service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Pagos de liquidacion (solo ADMIN, garantizado por SecurityConfig). */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** Marca el periodo como pagado y congela sus jornadas. */
    @PostMapping
    public ResponseEntity<PaymentDTO> create(
            @Valid @RequestBody CreatePaymentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.create(dto.getEmployeeId(), dto.getFrom(), dto.getTo()));
    }

    /** Historial de pagos realizados, del mas reciente al mas viejo. */
    @GetMapping
    public Page<PaymentDTO> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return paymentService.getPage(page, size);
    }

    /** Reabre un pago: el periodo vuelve a ser editable. Queda auditado. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
