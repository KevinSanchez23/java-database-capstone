package com.project.back_end.mvc;

import com.project.back_end.services.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Controller
public class DashboardController {

    private final Service service;

    @Autowired
    public DashboardController(Service service) {
        this.service = service;
    }

    @GetMapping("/adminDashboard/{token}")
    public String adminDashboard(@PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        Map<String, String> errors = validation.getBody();

        if (errors != null && errors.isEmpty()) {
            return "admin/adminDashboard";
        }

        return "redirect:http://localhost:8080";
    }

    @GetMapping("/doctorDashboard/{token}")
    public String doctorDashboard(@PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "doctor");
        Map<String, String> errors = validation.getBody();

        if (errors != null && errors.isEmpty()) {
            return "doctor/doctorDashboard";
        }

        return "redirect:http://localhost:8080";
    }
}
