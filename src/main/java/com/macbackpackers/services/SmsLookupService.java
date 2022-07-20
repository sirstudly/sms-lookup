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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

/**
 * Singleton service class for looking up SMS content. Has a WebClient so once a method completes,
 * there should be no state left in the browser.
 */
@Service
public class SmsLookupService {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final ModemConfigProperties props;
    private final WebClient webClient;

    @Autowired
    public SmsLookupService(ModemConfigProperties props, WebClient webClient) {
        this.props = props;
        this.webClient = webClient;
    }

    public synchronized String getLast2faCode(String application) throws IOException, ApplicationNotSupportedException {
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
