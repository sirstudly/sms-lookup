package com.macbackpackers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.macbackpackers.beans.Last2faCode;
import com.macbackpackers.exceptions.ApplicationNotSupportedException;
import com.macbackpackers.services.SmsLookupService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

@RestController
public class SmsLookupController {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final SmsLookupService service;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Value("${controller.allowedRemoteIpAddresses:}")
    private String allowedRemoteIpAddresses;

    @Autowired
    public SmsLookupController(SmsLookupService service) {
        this.service = service;
    }

    @GetMapping(value = "/last2fa/{app}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String last2fa(@PathVariable("app") String application, HttpServletRequest request) {
        try {
            authorize(StringUtils.defaultString(request.getHeader("x-forwarded-for"), request.getRemoteAddr()));
            Last2faCode otp = service.getLast2faCode(application);
            String response = gson.toJson(otp);
            LOGGER.info("Returning response :" + response);
            return response;
        }
        catch (ApplicationNotSupportedException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
        catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        }
    }

    private void authorize(String remoteAddr) {
        if (StringUtils.isBlank(allowedRemoteIpAddresses) ||
                Arrays.asList(allowedRemoteIpAddresses.split(",")).contains(remoteAddr)) {
            LOGGER.info("Allowing remote request from " + remoteAddr);
        }
        else {
            LOGGER.warn("Denying access from remote address {}", remoteAddr);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Request denied.");
        }
    }

}
