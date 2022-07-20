package com.macbackpackers.beans;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "modem")
public class ModemConfigProperties {

    @NotBlank private String url;
    private String username;
    private String password;
    @NotBlank private String className;

    private List<String> supportedHandlers;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<String> getSupportedHandlers() {
        return supportedHandlers;
    }

    public void setSupportedHandlers(List<String> supportedHandlers) {
        this.supportedHandlers = supportedHandlers;
    }
}
