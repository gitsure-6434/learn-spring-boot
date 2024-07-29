package com.sprintgboot.learn_spring_boot;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Employee {

    public Employee() { }

    @Id
    @GeneratedValue
    private int empId;
    private String empNameOne;
    private String owner;

    public Employee(int empId, String empNameOne, String owner) {
        this.empId = empId;
        this.empNameOne = empNameOne;
        this.owner = owner;
    }

    public int getEmpId() {
        return empId;
    }

    public void setEmpId(int empId) {
        this.empId = empId;
    }

    public String getEmpNameOne() {
        return empNameOne;
    }

    public void setEmpNameOne(String empName) {
        this.empNameOne = empName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "empId=" + empId +
                ", empNameOne='" + empNameOne + '\'' +
                ", owner='" + owner + '\'' +
                '}';
    }
}
