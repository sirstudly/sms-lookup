package com.macbackpackers.services;

import com.macbackpackers.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes= {Application.class})
public class DatabaseServiceTest {

    @Autowired
    private DatabaseService db;

    @Test
    public void testUpdateOption() {
        db.updateOption("hbo_sms_lookup_url", "http://localhost:8080/smslookup");
    }
}
