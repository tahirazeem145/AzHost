package com.azhost.service;

import com.azhost.dto.HealthResponseDto;
import com.azhost.dto.InfoResponseDto;
import org.springframework.stereotype.Service;

@Service
public class SystemInfoService {

    public HealthResponseDto getHealthStatus() {
        return new HealthResponseDto("UP", "AZHost");
    }

    public InfoResponseDto getApplicationInfo() {
        return new InfoResponseDto("AZHost", "0.1.0", "Phase 1", "development");
    }
}

