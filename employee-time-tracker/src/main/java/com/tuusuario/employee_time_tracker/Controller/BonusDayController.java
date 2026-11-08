package com.tuusuario.employee_time_tracker.Controller;

import com.tuusuario.employee_time_tracker.Model.Dto.BonusDayDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.BonusDayRequestDTO;
import com.tuusuario.employee_time_tracker.Service.BonusDayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Dias con bono por hora (solo ADMIN). */
@RestController
@RequestMapping("/api/bonus-days")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BonusDayController {

    private final BonusDayService bonusDayService;

    @GetMapping
    public List<BonusDayDTO> getRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return bonusDayService.getRange(from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BonusDayDTO create(@Valid @RequestBody BonusDayRequestDTO dto) {
        return bonusDayService.create(dto);
    }

    @PutMapping("/{id}")
    public BonusDayDTO update(@PathVariable Long id, @Valid @RequestBody BonusDayRequestDTO dto) {
        return bonusDayService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bonusDayService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
