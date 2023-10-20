package it.zwets.sms.scheduler.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RestInterfaceSecurityTests {

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
    public void getAdminUsersWorksForAdmin() {
        ResponseEntity<String> response = adminRequest("/admin/users");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
