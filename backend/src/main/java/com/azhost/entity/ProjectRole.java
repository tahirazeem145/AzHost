package com.azhost.entity;

public enum ProjectRole {
    VIEWER(1),
    MEMBER(2),
    OWNER(3);

    private final int level;

    ProjectRole(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public boolean satisfies(ProjectRole minimumRequiredRole) {
        return this.level >= minimumRequiredRole.getLevel();
    }
}
