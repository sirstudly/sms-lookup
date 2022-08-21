package com.macbackpackers.services;

import com.macbackpackers.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes= {Application.class})
public class NgrokLookupServiceTest {

    @Autowired
    private NgrokLookupService ngrokService;

    @Test
    public void testUpdatePublicEndpoint() throws Exception {
        ngrokService.updatePublicEndpoint();
    }
}
