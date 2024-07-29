package com.sprintgboot.learn_spring_boot;

import jakarta.persistence.Id;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
}
