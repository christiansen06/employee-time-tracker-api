package com.tuusuario.employee_time_tracker.Repository;

import com.tuusuario.employee_time_tracker.Model.Entity.User;
import com.tuusuario.employee_time_tracker.Model.Enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRole(Role role);

    boolean existsByEmployee_Id(Long employeeId);
}
