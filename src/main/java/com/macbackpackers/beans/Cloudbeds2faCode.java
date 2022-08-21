package com.macbackpackers.beans;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cloudbeds2faCode extends Last2faCode {
    public Cloudbeds2faCode(String message, String date) {
        setMessage(message);
        setDate(date);

        Pattern p = Pattern.compile("Your Cloudbeds code is ([0-9]+)");
        Matcher m = p.matcher(message);
        if (m.find()) {
            setOtp(m.group(1));
        }
    }
}
