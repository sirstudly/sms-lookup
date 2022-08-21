package com.macbackpackers.services;

import com.gargoylesoftware.htmlunit.WebClient;
import com.macbackpackers.beans.Cloudbeds2faCode;
import com.macbackpackers.beans.Last2faCode;
import com.macbackpackers.beans.ModemConfigProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
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
 * Singleton service class for looking up SMS content. Has a WebClient so once a method completes,
 * there should be no state left in the browser.
 */
@Service
public class SmsLookupService {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final ModemConfigProperties props;
    private final WebClient webClient;
    private final GenericObjectPool<WebDriver> driverFactory;

    /** maximum time to wait when navigating web requests */
    private static final int MAX_WAIT_SECONDS = 60;

    @Autowired
    public SmsLookupService(ModemConfigProperties props, WebClient webClient, GenericObjectPool<WebDriver> driverFactory) {
        this.props = props;
        this.webClient = webClient;
        this.driverFactory = driverFactory;
    }

    public synchronized Last2faCode getLast2faCode(String application) throws Exception {
        WebDriver driver = driverFactory.borrowObject();
        try {
            WebDriverWait wait = new WebDriverWait(driver, MAX_WAIT_SECONDS);
            LOGGER.info(ToStringBuilder.reflectionToString(props));

            driver.get(props.getUrl());

            WebElement passwordField = findElement(wait, By.id("login_password"));
            passwordField.sendKeys(props.getPassword());

            WebElement loginButton = findElement(wait, By.id("login_btn"));
            loginButton.click();
            wait.until( d -> stalenessOf( loginButton ).apply(d) );

            LOGGER.info("CURRENTLY ON " + driver.getCurrentUrl());
            driver.get(StringUtils.stripEnd(props.getUrl(), "/") + "/html/content.html#sms");
            findElement(wait, By.id("sms_display"));

            if("cloudbeds".equalsIgnoreCase(application)) {
                By byCloudbeds = By.xpath( "//div[text()='62884']" );
                findElement(wait, byCloudbeds);
                List<WebElement> cloudbedsMessages = driver.findElements( byCloudbeds );
                if (cloudbedsMessages.size() > 0) {
                    cloudbedsMessages.get(0).click();
                    wait.until(d -> invisibilityOf(cloudbedsMessages.get(0)));
                    sleep(5); // wait until scroll completes

                    String lastDate = null;
                    String lastData = null;
                    for(WebElement elem : driver.findElements(By.xpath("//div[contains(@class,'color_descroption_gray')]"))) {
                        List<WebElement> tables = elem.findElements(By.xpath("./following-sibling::table"));
                        if(tables.size() > 0) {
                            lastDate = StringUtils.trim(elem.getText());
                            lastData = StringUtils.trim(tables.get(0).getText());
                            LOGGER.info("Found record {} on {}.", lastData, lastDate);
                        }
                    }
                    return new Cloudbeds2faCode(lastData, lastDate);
                }
                else {
                    LOGGER.info("No messages from Cloudbeds....");
                }
            }
        }
        finally {
            driverFactory.returnObject(driver);
        }
        return null;
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
