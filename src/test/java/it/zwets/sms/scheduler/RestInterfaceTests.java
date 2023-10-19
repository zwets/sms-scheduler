package it.zwets.sms.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RestInterfaceTests {

	String baseUrl;
	@LocalServerPort int port;

	@BeforeEach
	public void setUp() throws MalformedURLException {
		baseUrl = "http://localhost:" + port;
	}

	@Test
	public void authorisedScheduledQuery() throws IllegalStateException, IOException {
		ResponseEntity<String> response = new TestRestTemplate("admin", "test")
		        .getForEntity(baseUrl + "/scheduled", String.class);
		
		assertEquals(HttpStatus.OK, response.getStatusCode());
		//assertTrue(response.getBody().contains("..."));
	}

	@Test
	public void wrongPasswordScheduledQueryFails() throws Exception {
        ResponseEntity<String> response = new TestRestTemplate("admin", "not-the-password")
                .getForEntity(baseUrl + "/scheduled", String.class);

		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

    @Test
    public void unauthorisedScheduledQueryFails() throws Exception {
        ResponseEntity<String> response = new TestRestTemplate()
                .getForEntity(baseUrl + "/scheduled", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
