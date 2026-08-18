package com.azhost.controller;

import com.azhost.dto.HealthResponseDto;
import com.azhost.service.SystemInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final SystemInfoService systemInfoService;

    public HealthController(SystemInfoService systemInfoService) {
        this.systemInfoService = systemInfoService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponseDto> getHealth() {
        return ResponseEntity.ok(systemInfoService.getHealthStatus());
    }
}
