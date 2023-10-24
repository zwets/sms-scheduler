package it.zwets.sms.scheduler.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.zwets.sms.scheduler.iam.IamService;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
class IamRestControllerFunctionalTests {

    private static final Logger LOG = LoggerFactory.getLogger(IamRestControllerFunctionalTests.class);

    @LocalServerPort
    private int port;

    @Autowired
    private IamService iamService;
    
    private RestTestHelper rest;

    @BeforeAll
    public void setupTests() {
        rest = new RestTestHelper("http://localhost", port);
    }

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
        
        ResponseEntity<String> response = rest.GET("root", "/iam/accounts");
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
        ResponseEntity<String> response = rest.POST("root", "/iam/accounts", userJson("bork1", "users", "admins"));
        assertEquals(HttpStatus.OK, response.getStatusCode());        
        
        JsonNode root = asJson(response);
        assertEquals("bork1", root.get("id").textValue());
        assertEquals(2, root.get("groups").size());
    }
    
    @Test
    public void createAccountWithPut() {
        ResponseEntity<String> response = rest.PUT("root", "/iam/accounts/bork2", userJson("bork2", "users"));
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
    
}
