package it.zwets.sms.scheduler.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import it.zwets.sms.scheduler.iam.IamService;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
class SchedulerRestControllerTests {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerRestControllerTests.class);

    @LocalServerPort
    int port;

    @Autowired
    private IamService iamService;
    
    private RestTestHelper rest;

    @BeforeAll
    public void beforeAll() {
        rest = new RestTestHelper(iamService, "http://localhost", port, "tester", "test");
        rest.createDefaultAccount(IamService.USERS_GROUP, IamService.TEST_GROUP);
    }

    @AfterAll
    public void afterAll() {
        rest.deleteDefaultAccount();
    }
    
    @BeforeEach
    public void setup() {
    }

    @AfterEach
    public void teardown() {
    }

        // Security tests

    @Test
    public void anonymousRequestOnRootUnauthorised() {
        ResponseEntity<String> response = new TestRestTemplate()
                .getForEntity("http://localhost:" + port + "/schedule", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
    
    @Test
    public void perpetratorRequestOnRootUnauthorised() {
        ResponseEntity<String> response = new TestRestTemplate("test", "hacker")
                .getForEntity("http://localhost:" + port + "/schedule", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
    
    @Test
    public void adminRequestOnRoot() {
        ResponseEntity<String> response = rest.GET("admin", "test", "/schedule");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
    
    @Test
    public void testRoot() {
        ResponseEntity<String> response = rest.GET("/schedule");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
