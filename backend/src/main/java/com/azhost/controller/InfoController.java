package com.azhost.controller;

import com.azhost.dto.InfoResponseDto;
import com.azhost.service.SystemInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InfoController {

    private final SystemInfoService systemInfoService;

    public InfoController(SystemInfoService systemInfoService) {
        this.systemInfoService = systemInfoService;
    }

    @GetMapping("/info")
    public ResponseEntity<InfoResponseDto> getInfo() {
        return ResponseEntity.ok(systemInfoService.getApplicationInfo());
    }
}
