package com.macbackpackers;

import com.macbackpackers.exceptions.ApplicationNotSupportedException;
import com.macbackpackers.services.SmsLookupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
public class SmsLookupController {

    private final SmsLookupService service;

    @Autowired
    public SmsLookupController(SmsLookupService service) {
        this.service = service;
    }

    @GetMapping(value = "/last2fa/{app}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String last2fa(@PathVariable("app") String application) {
        try {
            String answer = service.getLast2faCode(application);
            return "{\"message\":\"This is the 2fa code for " + application + " is " + answer + ".\"}";
        }
        catch (ApplicationNotSupportedException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
        catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        }
    }

}
