package com.macbackpackers;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes= {Application.class})
public class SmsLookupControllerTest {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TestRestTemplate template;

    @Test
    public void getLast2faCloudbeds() {
        ResponseEntity<String> response = template.getForEntity("/last2fa/cloudbeds", String.class);
        LOGGER.info(response.getBody());
    }

    @Test
    public void getLast2faBookingDotCom() {
        ResponseEntity<String> response = template.getForEntity("/last2fa/bdc", String.class);
        LOGGER.info(response.getBody());
    }

    @Test
    public void restartModem() {
        ResponseEntity<String> response = template.getForEntity("/restartModem", String.class);
        LOGGER.info(response.getBody());
    }
}
