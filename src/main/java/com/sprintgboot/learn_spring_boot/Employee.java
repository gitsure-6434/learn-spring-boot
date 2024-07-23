package com.sprintgboot.learn_spring_boot;

public class Employee {

    private int empId;
    private String empName;
    private String owner;

    public Employee(int empId, String empName, String owner) {
        this.empId = empId;
        this.empName = empName;
        this.owner = owner;
    }

    public int getEmpId() {
        return empId;
    }

    public void setEmpId(int empId) {
        this.empId = empId;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
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
                ", empName='" + empName + '\'' +
                ", owner='" + owner + '\'' +
                '}';
    }
}
