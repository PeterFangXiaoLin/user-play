package com.yupi.usercenter.model.enums;

public enum TeamStatus {
    PUBLIC(0, "公开"),
    PRIVATE(1, "私有"),
    SECRET(2, "加密");

    private int value;
    private String text;

    public static TeamStatus getTeamStatus(Integer value) {
        if (value == null) {
            return null;
        }
        TeamStatus[] teamStatuses = TeamStatus.values();
        for (TeamStatus status : teamStatuses) {
            if (value == status.getValue()) {
                return status;
            }
        }
        return null;
    }

    TeamStatus(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
