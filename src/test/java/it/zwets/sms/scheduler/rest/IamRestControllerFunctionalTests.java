package it.zwets.sms.scheduler.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.zwets.sms.scheduler.iam.IamService;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class IamRestControllerFunctionalTests {

    private static final Logger LOG = LoggerFactory.getLogger(IamRestControllerFunctionalTests.class);
    
    @Autowired
    private IamService iamService;
    
    @LocalServerPort
    private int port;

    @BeforeEach
    public void setup() {
        createAccount("root", new String[] { IamService.ADMINS_GROUP });
        createAccount("user", new String[] { IamService.USERS_GROUP, IamService.TEST_GROUP });
    }
    
    @AfterEach
    public void teardown() {
        deleteAccount("user");
        deleteAccount("root");
    }

    @Test
    public void getAccountList() {
        
        ResponseEntity<String> response = request("root", "/iam/accounts");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        JsonNode root = asJson(response);
        boolean foundUser = false;
        boolean foundRoot = false;
        for (JsonNode node : root) {
            foundUser |= node.get("id").textValue().equals("user");
            foundRoot |= node.get("id").textValue().equals("root");
        }
        assertTrue(foundUser && foundRoot);
    }

    @Test
    public void testUserJson() {
        String account = userJson("bork0", "users", "admins");
        LOG.debug("This is not OK: {}", account);
        JsonNode root = parseJson(account);
        assertEquals("bork0", root.get("id").textValue());
        assertTrue(root.get("groups").isArray());
    }
    
    @Test
    public void createAccountWithPost() {
        ResponseEntity<String> response = postRequest("root", "/iam/accounts", userJson("bork1", "users", "admins"));
        assertEquals(HttpStatus.OK, response.getStatusCode());        
        
        JsonNode root = asJson(response);
        assertEquals("bork1", root.get("id").textValue());
        assertEquals(2, root.get("groups").size());
    }
    
    @Test
    public void createAccountWithPut() {
        ResponseEntity<String> response = putRequest("root", "/iam/accounts/bork2", userJson("bork2", "users"));
        LOG.info("PUT response: {}", response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        JsonNode root = asJson(response);
        assertEquals("bork2", root.get("id").textValue());
        assertEquals(1, root.get("groups").size());
    }
    
        // Helpers
    
    private JsonNode asJson(ResponseEntity<String> response) {
        return parseJson(response.getBody());
    }
    
    private JsonNode parseJson(String s) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(s);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    
    private String userJson(String name, String... groups) {
        return makeUserJson(name, name, name, name, groups);
    }
    
    private String makeUserJson(String id, String name, String email, String password, String[] groups) {
        return "{ 'id': '%s', 'name': '%s', 'email': '%s', 'password': '%s', 'groups': [ %s ] }"
                .replace('\'', '"')
                .formatted(id, name, email, password, 
                        Arrays.stream(groups).map(s -> "\"%s\"".formatted(s)).collect(Collectors.joining(",")));
    }
    
    private IamService.AccountDetail createAccount(String id, String[] groups) {
        return iamService.createAccount(new IamService.AccountDetail(id, id, id, id, groups));
    }

    private void deleteAccount(String id) {
        iamService.deleteAccount(id);
    }
    
    private ResponseEntity<String> exchange(String uid, String url, HttpMethod verb, HttpEntity<String> entity) {
        return new TestRestTemplate(uid,uid).exchange("http://localhost:" + port + url, verb, entity, String.class);
    }
    
    private ResponseEntity<String> request(String id, String url) {
        return exchange(id, url, HttpMethod.GET, null);
    }
    
    private ResponseEntity<String> deleteRequest(String id, String url) {
        return exchange(id, url, HttpMethod.DELETE, null);
    }
    
    private ResponseEntity<String> postRequest(String id, String url, String json) {
        return exchange(id, url, HttpMethod.POST, makeEntity(json));
    }
    
    private ResponseEntity<String> putRequest(String id, String url, String json) {
        return exchange(id, url, HttpMethod.PUT, makeEntity(json));
    }
    
    private HttpEntity<String> makeEntity(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<String>(json, headers);        
    }
}
