package com.macbackpackers;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebWindowEvent;
import com.gargoylesoftware.htmlunit.WebWindowListener;
import com.macbackpackers.services.NgrokLookupService;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Arrays;

@SpringBootApplication
public class Application {

    private final Logger LOGGER = LoggerFactory.getLogger( getClass() );

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @Scope( "prototype" )
    public WebClient getWebClient() throws IOException {
        WebClient webClient = new WebClient( BrowserVersion.FIREFOX );
        webClient.getOptions().setTimeout( 120000 );
        webClient.getOptions().setRedirectEnabled( true );
        webClient.getOptions().setJavaScriptEnabled( true );
        webClient.getOptions().setThrowExceptionOnFailingStatusCode( false );
        webClient.getOptions().setThrowExceptionOnScriptError( false );
        webClient.getOptions().setCssEnabled( true );
        webClient.getOptions().setUseInsecureSSL( true );
        webClient.setAjaxController( new NicelyResynchronizingAjaxController() );
        webClient.addWebWindowListener( new WebWindowListener() {
            @Override
            public void webWindowOpened( WebWindowEvent event ) {
            }

            @Override
            public void webWindowContentChanged( WebWindowEvent event ) {
                LOGGER.info( "Content changed: " + event.getNewPage().getUrl() );
            }

            @Override
            public void webWindowClosed( WebWindowEvent event ) {
            }
        });
        return webClient;
    }

    @Bean
    public GenericObjectPoolConfig getWebDriverPoolConfig() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig<>();
        config.setJmxEnabled(false);
        return config;
    }

    @Bean
    public GenericObjectPool<WebDriver> getWebDriverPool(WebDriverFactory driverFactory, GenericObjectPoolConfig config) {
        GenericObjectPool<WebDriver> objectPool = new GenericObjectPool<>( driverFactory, config);
        objectPool.setBlockWhenExhausted( true );
        objectPool.setMaxTotal( 1 ); // only keep one around for now
        return objectPool;
    }

    @Bean
    public DataSource getDataSource(
            @Value("${db.url}") String url,
            @Value("${db.username}") String username,
            @Value("${db.password}") String password) {
        MysqlDataSource ds = new MysqlDataSource();
        ds.setURL(url);
        ds.setUser(username);
        ds.setPassword(password);
        return ds;
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {

            LOGGER.info("Let's inspect the beans provided by Spring Boot:");

            String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
            for (String beanName : beanNames) {
                LOGGER.info(beanName);
            }

            // Ngrok needs to be running; update the public endpoint on startup
            NgrokLookupService ngrokService = ctx.getBean(NgrokLookupService.class);
            ngrokService.updatePublicEndpoint();
//            ((ConfigurableApplicationContext) ctx).close();
        };
    }
}
