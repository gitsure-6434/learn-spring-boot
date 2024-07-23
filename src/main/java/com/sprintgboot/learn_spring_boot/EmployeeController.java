package com.sprintgboot.learn_spring_boot;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
public class EmployeeController {

    @RequestMapping("/employee")
    public List<Employee> getAllEmployees() {
        return Arrays.asList(
           new Employee(1, "Reyansh", "MOMO"),
           new Employee(2, "Daksh", "MOMO"),
           new Employee(3, "Suresh", "MOMO")
        );
    }
}
