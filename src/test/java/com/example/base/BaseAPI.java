package com.example.base;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaseAPI {
    protected Properties properties;
    private WireMockServer wireMockServer;
    private static final Logger logger = Logger.getLogger(BaseAPI.class.getName());

    public BaseAPI() throws IOException {
        properties = new Properties();
        FileInputStream fis = new FileInputStream("src/main/resources/config.properties");
        properties.load(fis);
        RestAssured.baseURI = properties.getProperty("apiBaseUrl");
        logger.log(Level.INFO, "Base URI set to: {0}", RestAssured.baseURI);
    }

    public Response getRequest(String endpoint) {
        logger.log(Level.INFO, "Sending GET request to endpoint: {0}", endpoint);
        return RestAssured.get(endpoint);
    }

    @BeforeClass
    public void setup() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8080));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8080);
        logger.info("WireMock server started on port 8080");

        // Setup a dummy endpoint
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/dummy-endpoint"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("Hello, World!")));
        logger.info("Stubbed /dummy-endpoint with response 'Hello, World!'");
    }

    @AfterClass
    public void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            logger.info("WireMock server stopped");
        }
    }
}