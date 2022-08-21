package com.macbackpackers.services;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NgrokLookupService {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final DatabaseService dbService;
    private final GenericObjectPool<WebDriver> driverFactory;

    @Value("${ngrok.status.url}")
    private String statusUrl;

    @Value("${ngrok.status.url.xpath}")
    private String statusUrlXPath;

    @Autowired
    public NgrokLookupService(DatabaseService dbService, GenericObjectPool<WebDriver> driverFactory) {
        this.dbService = dbService;
        this.driverFactory = driverFactory;
    }

    public void updatePublicEndpoint() throws Exception {
        WebDriver driver = driverFactory.borrowObject();
        try {
            LOGGER.info("Looking up endpoint on " + statusUrl);
            driver.get(statusUrl);
            WebElement url = driver.findElement(By.xpath(statusUrlXPath));
            if (url == null) {
                LOGGER.error("Unable to find ngrok endpoint on " + statusUrl);
                throw new IllegalStateException("Unable to find ngrok endpoint on " + statusUrl);
            }
            dbService.updateOption("sms_lookup_url", url.getText());
        }
        finally {
            driverFactory.returnObject(driver);
        }
    }
}
