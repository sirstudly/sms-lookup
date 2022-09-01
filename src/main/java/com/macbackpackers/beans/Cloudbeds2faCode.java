package com.macbackpackers.beans;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cloudbeds2faCode extends Last2faCode {
    public Cloudbeds2faCode(String message, String date) {
        setMessage(message);
        setDate(date);
    }

    @Override
    public String getOtp() {
        Pattern p = Pattern.compile("Your Cloudbeds code is ([0-9]+)");
        Matcher m = p.matcher(getMessage());
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
