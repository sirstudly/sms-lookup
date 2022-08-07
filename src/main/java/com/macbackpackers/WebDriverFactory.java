
package com.macbackpackers;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * A factory for creating WebDriver instances.
 */
@Component
public class WebDriverFactory extends BasePooledObjectFactory<WebDriver> {

    @Value("${chromescraper.maxwait.seconds:60}")
    private int maxWaitSeconds;

    @Value("${chromescraper.driver.options}")
    private String chromeOptions;

    @Override
    public WebDriver create() throws Exception {
        System.setProperty("webdriver.chrome.driver", getClass().getClassLoader().getResource(
                SystemUtils.IS_OS_WINDOWS ? "chromedriver.exe" : "chromedriver").getPath());

        ChromeOptions options = new ChromeOptions();
        List<String> optionValues = new ArrayList<>(Arrays.asList(chromeOptions.split(" ")));
        options.addArguments(optionValues.toArray(new String[optionValues.size()]));
//        options.setExperimentalOption("debuggerAddress", "127.0.0.1:9222");

        // enable performance logging
        // https://stackoverflow.com/a/39979509
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.PERFORMANCE, Level.ALL);
        options.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);

        ChromeDriver driver = new ChromeDriver(options);

        // configure wait-time when finding elements on the page
        driver.manage().timeouts().pageLoadTimeout(maxWaitSeconds, TimeUnit.SECONDS);

        return driver;
    }

    /**
     * Use the default PooledObject implementation.
     */
    @Override
    public PooledObject<WebDriver> wrap(WebDriver driver) {
        return new DefaultPooledObject<>(driver);
    }

    @Override
    public void destroyObject(PooledObject<WebDriver> pooledObj) {
        pooledObj.getObject().quit();
    }

}
