package it.zwets.sms.scheduler.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Disabled
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SchedulerRestControllerTests {

    @LocalServerPort
    int port;

    private ResponseEntity<String> anonRequest(String url) {
        return new TestRestTemplate().getForEntity("http://localhost:" + port + url, String.class);
    }
    
    private ResponseEntity<String> request(String user, String pass, String url) {
        return new TestRestTemplate(user, pass).getForEntity("http://localhost:" + port + url, String.class);
    }
    
    private ResponseEntity<String> perpetratorRequest(String url) {
        return request("user", "not-the-password", url);
    }
    
    private ResponseEntity<String> adminRequest(String url) {
        return request("admin", "test", url);
    }
    
    private ResponseEntity<String> userRequest(String url) {
        return request("user", "test", url);
    }
    
    @Test
    public void getScheduledRootRequiresAuth() {
        ResponseEntity<String> response = anonRequest("/schedule");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
    
    @Test
    public void getScheduledRootBlocksPerpetrator() {
        ResponseEntity<String> response = perpetratorRequest("/schedule");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void getScheduledRootRequiresAdmin() {
        ResponseEntity<String> response = userRequest("/schedule");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void getScheduledRootWorksForAdmin() {
        ResponseEntity<String> response = adminRequest("/schedule");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void getScheduledWithClientRequiresAuth() {
        ResponseEntity<String> response = anonRequest("/schedule/CLIENT");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void getScheduledWithClientWorksForUser() {
        ResponseEntity<String> response = userRequest("/schedule/CLIENT");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void getScheduledWithClientWorksForAdmin() {
        ResponseEntity<String> response = adminRequest("/schedule/CLIENT");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void getScheduledWithTargetRequiresAuth() {
        ResponseEntity<String> response = anonRequest("/schedule/CLIENT/TARGET");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void getScheduledWithTargetWorksForUser() {
        ResponseEntity<String> response = userRequest("/schedule/CLIENT/TARGET");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void getScheduledWithTargetWorksForAdmin() {
        ResponseEntity<String> response = adminRequest("/schedule/CLIENT/TARGET");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
