package com.azhost.dto;

public class InfoResponseDto {
    private String name;
    private String version;
    private String phase;
    private String status;

    public InfoResponseDto() {}

    public InfoResponseDto(String name, String version, String phase, String status) {
        this.name = name;
        this.version = version;
        this.phase = phase;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

