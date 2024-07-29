package com.sprintgboot.learn_spring_boot;

import jakarta.persistence.Id;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
public class EmployeeController {

    private EmployeeRepository employeeRepository;

    public  EmployeeController(EmployeeRepository employeeRepository) {
       this.employeeRepository =  employeeRepository;
    }

    @RequestMapping("/employee")
    public List<Employee> getAllEmployees() {
        return Arrays.asList(
           new Employee(1, "Reyansh", "MOMO"),
           new Employee(2, "Daksh", "MOMO"),
           new Employee(4, "Suresh", "MOMO")
        );
    }

    @PostMapping("/jpa-employee")
    public void saveData(@RequestBody Employee employee) {
        employeeRepository.save(employee);
    }

    @GetMapping("/jpa-employee/{id}")
    public Employee getEmployeeById(@PathVariable int id) {
        Optional<Employee> employee = employeeRepository.findById(id);
        return employee.get();
    }

    @GetMapping("/jpa-employee")
    public List<Employee> findAllEmployees() {
        return employeeRepository.findAll();
    }

    @DeleteMapping("/jpa-employee/{id}")
    public void deleteById(@PathVariable int id) {
        employeeRepository.deleteById(id);
    }
}
