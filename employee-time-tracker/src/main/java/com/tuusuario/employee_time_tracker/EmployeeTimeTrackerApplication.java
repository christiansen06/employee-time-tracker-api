package com.tuusuario.employee_time_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EmployeeTimeTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmployeeTimeTrackerApplication.class, args);
	}

}
