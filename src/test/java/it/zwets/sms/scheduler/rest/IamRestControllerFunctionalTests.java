package it.zwets.sms.scheduler.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
        
        ResponseEntity<String> response = getRequest("root", "/iam/accounts");
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
    public void createAccount() {
        String jsonAcct = makeJsonAccount("new-user");

        TestRestTemplate tmp = new TestRestTemplate("root", "root");
        
        List<MediaType> mt = new ArrayList<MediaType>();
        mt.add(MediaType.APPLICATION_JSON);
        HttpEntity<String> reqEnt = new HttpEntity<>(jsonAcct);
        reqEnt.getHeaders().setAccept(mt);
        
        ResponseEntity<String> rsp = tmp.postForEntity("http://localhost:" + port + "/iam/accounts", reqEnt, String.class);
        
//        ResponseEntity<String> rsp = RequestEntity<String>.post("http://localhost:" + port + "/iam/accounts").accept(MediaType.APPLICATION_JSON_VALUE);
        assertEquals(HttpStatus.OK, rsp.getStatusCode());
    }
        // Helpers
    
    private IamService.AccountDetail createAccount(String id, String[] groups) {
        return iamService.createAccount(new IamService.AccountDetail(id, id, id, groups), id);
    }

    private void deleteAccount(String id) {
        iamService.deleteAccount(id);
    }

    private ResponseEntity<String> getRequest(String id, String url) {
        return new TestRestTemplate(id, id).getForEntity("http://localhost:" + port + url, String.class);
    }
    
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
    
    private String makeJsonAccount(String id) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("id", id).put("name", id).put("email", id).put("password", id).put("groups", mapper.createArrayNode());
        String jsonStr = null;
        try {
            jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        LOG.debug("JsonNode: " + jsonStr);
        return jsonStr;
    }
    
//    private ResponseEntity<String> putRequest(String id, String url, String body) {
//        return new TestRestTemplate(id, id).exchange("http://localhost:" + port + url, HttpMethod.PUT, new RequestEntity<String>(), String.class, body);
//    }
    
}
