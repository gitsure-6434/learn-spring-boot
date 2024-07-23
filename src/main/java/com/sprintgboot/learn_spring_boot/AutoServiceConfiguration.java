package com.sprintgboot.learn_spring_boot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "auto.service")
@Component
public class AutoServiceConfiguration {

    private int address;
    private String name;
    private String pin;

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    @Override
    public String toString() {
        return "AutoServiceConfiguration{" +
                "address=" + address +
                ", name='" + name + '\'' +
                ", pin='" + pin + '\'' +
                '}';
    }
}
