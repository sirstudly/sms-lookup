package com.macbackpackers.services;

import com.macbackpackers.beans.Last2faCode;
import com.macbackpackers.beans.ModemConfigProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOf;
import static org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

/**
 * Singleton service class for accessing the web configuration tool for the Huawei CPE Pro.
 */
@Service
public class HuaweiCpeProService {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final ModemConfigProperties props;
    private final GenericObjectPool<WebDriver> driverFactory;

    /** maximum time to wait when navigating web requests */
    private static final int MAX_WAIT_SECONDS = 60;

    @Autowired
    public HuaweiCpeProService(ModemConfigProperties props, GenericObjectPool<WebDriver> driverFactory) {
        this.props = props;
        this.driverFactory = driverFactory;
    }

    public synchronized Last2faCode getLast2faCode(String application) throws Exception {
        WebDriver driver = driverFactory.borrowObject();
        try {
            WebDriverWait wait = new WebDriverWait(driver, MAX_WAIT_SECONDS);
            doLogin(driver, wait);

            driver.get(StringUtils.stripEnd(props.getUrl(), "/") + "/html/content.html#sms");
            findElement(wait, By.id("sms_display"));

            if ("cloudbeds".equalsIgnoreCase(application)) {
                return getLast2faCode(driver, wait, "62884");
            }
            else if ("bdc".equalsIgnoreCase(application)) {
                return getLast2faCode(driver, wait, "Booking.com");
            }
            throw new UnsupportedOperationException("Unsupported app " + application);
        }
        finally {
            driverFactory.returnObject(driver);
        }
    }

    private Last2faCode getLast2faCode(WebDriver driver, WebDriverWait wait, String smsFrom) {
        By by = By.xpath("//div[text()='" + smsFrom + "']");
        findElement(wait, by);
        List<WebElement> messages = driver.findElements(by);
        if (messages.size() > 0) {
            messages.get(0).click();
            wait.until(d -> invisibilityOf(messages.get(0)));
            sleep(5); // wait until scroll completes

            String lastDate = null;
            String lastData = null;
            for (WebElement elem : driver.findElements(By.xpath("//div[contains(@class,'color_descroption_gray')]"))) {
                List<WebElement> tables = elem.findElements(By.xpath("./following-sibling::table"));
                if (tables.size() > 0) {
                    lastDate = StringUtils.trim(elem.getText());
                    lastData = StringUtils.trim(tables.get(0).getText());
                    LOGGER.info("Found record {} on {}.", lastData, lastDate);
                }
            }
            return new Last2faCode(lastData, lastDate);
        }
        LOGGER.info("No messages from " + smsFrom + "....");
        return null;
    }

    public synchronized void restartModem() throws Exception {
        WebDriver driver = driverFactory.borrowObject();
        try {
            WebDriverWait wait = new WebDriverWait(driver, MAX_WAIT_SECONDS);
            doLogin(driver, wait);

            WebElement restartButton = findElement(wait, By.xpath("//div[@lang-id='public.reboot']"));
            restartButton.click();

            WebElement continueButton = findElement(wait, By.xpath("//div[@id='submit_light']/div/div/div[text()='Continue']"));
            continueButton.click();
        }
        finally {
            driverFactory.returnObject(driver);
        }
    }

    private void doLogin(WebDriver driver, WebDriverWait wait) {
        driver.get(props.getUrl());

        WebElement passwordField = findElement(wait, By.id("login_password"));
        passwordField.sendKeys(props.getPassword());

        WebElement loginButton = findElement(wait, By.id("login_btn"));
        loginButton.click();
        wait.until(d -> stalenessOf( loginButton ).apply(d) );
        LOGGER.info("Current URL: " + driver.getCurrentUrl());
    }

    private void sleep(int seconds) {
        try {
            this.wait(seconds * 1000);
        } catch (InterruptedException e) {
            // leave no trace
        }
    }

    /**
     * Waits until element is visible and returns it.
     *
     * @param wait
     * @param by
     * @return visible element
     */
    private WebElement findElement( WebDriverWait wait, By by ) {
        return wait.until(d -> visibilityOfElementLocated(by).apply(d));
    }
}
