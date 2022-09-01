package com.macbackpackers.beans;

public abstract class Last2faCode {
    private String message;
    private String date;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public abstract String getOtp();
}
