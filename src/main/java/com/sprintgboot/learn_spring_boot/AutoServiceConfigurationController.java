package com.sprintgboot.learn_spring_boot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
public class AutoServiceConfigurationController {

    @Autowired
    private AutoServiceConfiguration autoServiceConfiguration;

    @RequestMapping("/autoservice")
    public AutoServiceConfiguration getAllEmployees() {
        return autoServiceConfiguration;
    }
}
