package com.macbackpackers.services;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.macbackpackers.beans.ModemConfigProperties;
import com.macbackpackers.exceptions.ApplicationNotSupportedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOf;
import static org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlContains;
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

    public synchronized String getLast2faCode(String application) throws Exception {
        WebDriver driver = driverFactory.borrowObject();
        try {
            WebDriverWait wait = new WebDriverWait(driver, MAX_WAIT_SECONDS);
            LOGGER.info(ToStringBuilder.reflectionToString(props));

            driver.get(props.getUrl());

            WebElement passwordField = findElement(driver, wait, By.id("login_password"));
            passwordField.sendKeys(props.getPassword());

            WebElement loginButton = findElement(driver, wait, By.id("login_btn"));
            loginButton.click();
            wait.until( d -> stalenessOf( loginButton ).apply(d) );

            LOGGER.info("CURRENTLY ON " + driver.getCurrentUrl());
            driver.get(StringUtils.stripEnd(props.getUrl(), "/") + "/html/content.html#sms");
            wait.until( d -> visibilityOfElementLocated(By.id("sms_display")).apply(d));

            if("cloudbeds".equalsIgnoreCase(application)) {
                List<WebElement> cloudbedsMessages = driver.findElements( By.xpath( "//div[text()='62884']" ) );
                if(cloudbedsMessages.size() > 0) {
                    cloudbedsMessages.get(0).click();
                    wait.until(d -> invisibilityOf(cloudbedsMessages.get(0)));
                    sleep(5); // wait until scroll completes

                    driver.findElements(By.xpath("//div[contains(@class,'color_descroption_gray')]")).stream()
                            .forEach(elem ->  {
                                List<WebElement> tables = elem.findElements(By.xpath("./following-sibling::table"));
                                if(tables.size() > 0) {
                                    LOGGER.info("DATE: " + elem.getText());
                                    LOGGER.info("DATA: " + tables.get(0).getText() );
                                }
                            });

//                    logRequests(driver);

                    // https://stackoverflow.com/questions/69526101/sending-a-post-request-with-payload-in-java
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    String script = "var xhr = new XMLHttpRequest();\r\n" +
                            "xhr.open('POST', '/api/sms/sms-list-phone', false);\r\n" +
                            headersToString(getRequestHeaders(driver)) +
                            "\r\n" +
                            "xhr.send('<?xml version=\"1.0\" encoding=\"UTF-8\"?><request><phone>62884</phone><pageindex>1</pageindex><readcount>20</readcount></request>');\r\n" +
                            "return xhr.response;";
                    LOGGER.info("EXECUTING SCRIPT: " + script);
                    Object response = js.executeScript(script, (Object) null);

                    LOGGER.info("RESPONSE: " + response);
                }
                else {
                    LOGGER.info("No messages from Cloudbeds....");
                }
            }
        }
        finally {
            driverFactory.returnObject(driver);
        }
        return "";
    }

    private String headersToString(Map<String, String> headers) {
        return headers.keySet().stream()
                .map(k -> "xhr.setRequestHeader('" + k + "', '" + headers.get(k) + "');")
                .collect(Collectors.joining("\r\n"));
    }

    private Map<String, String> getRequestHeaders(WebDriver driver) {
        HashMap<String, String> requestHeaders = new HashMap<>();
        LogEntries logs = driver.manage().logs().get("performance");
        for (Iterator<LogEntry> it = logs.iterator(); it.hasNext();) {
            LogEntry entry = it.next();
            try {
                JSONObject json = new JSONObject(entry.getMessage());

                JSONObject message = json.getJSONObject("message");
                String method = message.getString("method");

                // save our cookie
                if ("Network.requestWillBeSentExtraInfo".equals(method)
                //        && requestHeaders.get("Cookie") != null
                ) {
                    JSONObject params = message.getJSONObject("params");
                    JSONArray cookies = params.getJSONArray("associatedCookies");
                    for (int i = 0; i < cookies.length(); i++) {
                        JSONObject cookie = cookies.getJSONObject(i).getJSONObject("cookie");
                        if ("SessionID".equals(cookie.getString("name"))) {
                            LOGGER.info("Adding sessionID to req " + cookie.getString("value"));
                            requestHeaders.put("Cookie", "SessionID=" + cookie.getString("value"));
                        }
                    }
                }

                if ("Network.requestWillBeSent".equals(method)) {
                    JSONObject params = message.getJSONObject("params");
                    JSONObject request = params.getJSONObject("request");
                    if(request.getString("url").endsWith("/sms-list-phone")) {
                        JSONObject headers = request.getJSONObject("headers");

                        // save all the headers
                        for (Iterator<String> keys = headers.keys(); keys.hasNext(); ) {
                            String key = keys.next();
                            requestHeaders.put(key, headers.get(key).toString());
                        }
                        return requestHeaders;
                    }
                }
           }
            catch (JSONException e)  {
                LOGGER.error("JSON parse error ", e);
            }
        }
        throw new RuntimeException("Could not find requisite network call");
    }

    private void logRequests(WebDriver driver) {
        int status = -1;

        LOGGER.info("\nList of log entries:\n");

        LogEntries logs = driver.manage().logs().get("performance");
        for (Iterator<LogEntry> it = logs.iterator(); it.hasNext();)
        {
            LogEntry entry = it.next();

            try
            {
                JSONObject json = new JSONObject(entry.getMessage());

                LOGGER.info(json.toString());

                JSONObject message = json.getJSONObject("message");
                String method = message.getString("method");

                if (method != null
                        && "Network.responseReceived".equals(method))
                {
                    JSONObject params = message.getJSONObject("params");

                    JSONObject response = params.getJSONObject("response");
//                                String messageUrl = response.getString("url");

                    LOGGER.info("RESPONSE :" + response);

//                                if (currentURL.equals(messageUrl))
//                                {
//                                    status = response.getInt("status");
//
//                                    System.out.println(
//                                            "---------- bingo !!!!!!!!!!!!!! returned response for "
//                                                    + messageUrl + ": " + status);
//
//                                    System.out.println(
//                                            "---------- bingo !!!!!!!!!!!!!! headers: "
//                                                    + response.get("headers"));
//                                }
                }
            } catch (JSONException e)
            {
                LOGGER.error("JSON parse error ", e);
            }
        }
        LOGGER.info("status code: " + status);
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
    private WebElement findElement( WebDriver driver, WebDriverWait wait, By by ) {
        return wait.until(d -> visibilityOfElementLocated(by).apply(d));
    }

    public synchronized String getLast2faCode_notWorking(String application) throws IOException, ApplicationNotSupportedException {
        LOGGER.info(ToStringBuilder.reflectionToString(props));

        HtmlPage page = webClient.getPage(props.getUrl());

        HtmlPasswordInput passwordField = page.getHtmlElementById("login_password");
        passwordField.type(props.getPassword());

        HtmlDivision loginBtn = page.getHtmlElementById("login_btn");
        page = loginBtn.click();

        LOGGER.info("CURRENTLY ON " + page.getUrl());
        webClient.getPage(StringUtils.stripEnd(props.getUrl(), "/") + "/html/content.html#sms");
        LOGGER.info("GOING TO " + page.getUrl());

        if("cloudbeds".equalsIgnoreCase(application)) {
            HtmlDivision smsRowDiv = page.getFirstByXPath("//div[text()='62884']");
            if(smsRowDiv != null) {
                smsRowDiv.click();
            }
            else {
                LOGGER.info("Unable to find SMS entry for Cloudbeds");
            }
            LOGGER.info(page.asXml());
        }
        else {
            throw new ApplicationNotSupportedException(application + " not supported");
        }

        return "TBD";
    }
}
