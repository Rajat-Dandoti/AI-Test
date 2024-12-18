package com.example.tests;

import com.example.base.BaseAPI;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class APITest extends BaseAPI {

    private static final Logger logger = Logger.getLogger(APITest.class.getName());

    public APITest() throws IOException {
        super();
    }

    @Test
    public void testGetEndpoint() {
        String baseUrl = "http://localhost:8080";
        logger.log(Level.INFO, "Testing GET request to {0}", baseUrl + "/dummy-endpoint");

        // Make a GET request to the dummy endpoint
        Response response = getRequest("/dummy-endpoint");

        // Assert that the response status code is 200 (OK)
        Assert.assertEquals(response.getStatusCode(), 200);
        logger.log(Level.INFO, "Received response with status code: {0}", response.getStatusCode());
    }
}