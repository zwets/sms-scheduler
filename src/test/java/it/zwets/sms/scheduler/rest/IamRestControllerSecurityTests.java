package it.zwets.sms.scheduler.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class IamRestControllerSecurityTests {

    @Autowired(required = true)
    private IamService iamService;
    
    @LocalServerPort
    private int port;

    @BeforeEach
    public void setup() {
        createAccount("nnn", new String[] { });
        createAccount("nnt", new String[] { IamService.TEST_GROUP });
        createAccount("nan", new String[] { IamService.ADMINS_GROUP });
        createAccount("nat", new String[] { IamService.ADMINS_GROUP, IamService.TEST_GROUP });
        createAccount("unn", new String[] { IamService.USERS_GROUP });
        createAccount("unt", new String[] { IamService.USERS_GROUP, IamService.TEST_GROUP });
        createAccount("uan", new String[] { IamService.USERS_GROUP, IamService.ADMINS_GROUP });
        createAccount("uat", new String[] { IamService.USERS_GROUP, IamService.ADMINS_GROUP, IamService.TEST_GROUP });
        createAccount("dummy", new String[] { });
    }
    
    @AfterEach
    public void teardown() {
        deleteAccount("nnn");
        deleteAccount("nnt");
        deleteAccount("nan");
        deleteAccount("nat");
        deleteAccount("unn");
        deleteAccount("unt");
        deleteAccount("uan");
        deleteAccount("uat");
        deleteAccount("uat");
    }

        // General /iam tests
    
    @Test
    public void anonymousRequestOnRootUnauthorised() {
        ResponseEntity<String> response = new TestRestTemplate()
                .getForEntity("http://localhost:" + port + "/iam", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
    
    @Test
    public void perpetratorRequestOnRootUnauthorised() {
        ResponseEntity<String> response = new TestRestTemplate("uat", "hacker")
                .getForEntity("http://localhost:" + port + "/iam", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
    
    @Test
    public void authenticatedRequestOnRoot() {
        ResponseEntity<String> response = new TestRestTemplate("nnn", "nnn")
                .getForEntity("http://localhost:" + port + "/iam", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
    
        // Tests on /iam/accounts
    
    @Test
    public void getAccountsForbiddenForNonAdmin() {
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = request(u, "/iam/accounts");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should be forbidden for '%s'".formatted(u));
        }
    }

    @Test
    public void getAccountsAllowedForAdmins() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = request(u, "/iam/accounts");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be OK for '%s'".formatted(u));
        }
    }

        // Tests on /accounts/{id} i.e. "my own account"
    
    @Test
    public void getMyAccountWorksForAllAccounts() {
        for (String u : all_accounts) {
            ResponseEntity<String> response = request(u, "/iam/accounts/" + u);
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should succeed for '%s'".formatted(u));
        }
    }

    @Test
    public void getAnotherAccountWorksOnlyForAdmins() {
        createAccount("target", null);
        for (String u : admin_accounts) {
            ResponseEntity<String> response = request(u, "/iam/accounts/target");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should work for '%s'".formatted(u));
        }
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = request(u, "/iam/accounts/target");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should be forbidden for '%s'".formatted(u));
        }
        deleteAccount("target");
    }
    
    @Test
    public void getNonExistentAccountForbiddenForNonAdmins() {
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = request(u, "/iam/accounts/borkbork");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should be forbidden for '%s'".formatted(u));
        }
    }
    
    @Test
    public void getNonExistentAccountNotFoundForAdmins() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = request(u, "/iam/accounts/borkbork");
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should give not found for '%s'".formatted(u));
        }
    }

        // Create Account
    
    @Test
    public void createAccountWorksForAdmins() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = postRequest(u, "/iam/accounts", userJson("newuser", "users"));
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should work for '%s'".formatted(u));
            deleteAccount("newuser");
        }
    }

    @Test
    public void createAccountForbiddenForNonAdmins() {
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = postRequest(u, "/iam/accounts", userJson("newuser", "users"));
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should give FORBIDDEN found for '%s'".formatted(u));
        }
    }

    @Test
    public void deleteAccountForbiddenForNonAdmins() {
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = deleteRequest(u, "/iam/accounts/dummy");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should give FORBIDDEN for '%s'".formatted(u));
        }
    }

    @Test
    public void deleteAccountAllowedForAdmin() {
        for (String u : admin_accounts) {
            createAccount("deleteme", null);
            ResponseEntity<String> response = deleteRequest(u, "/iam/accounts/deleteme");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should give OK for '%s'".formatted(u));
        }
    }
    
    @Test
    public void deleteAccountForbiddenForLoggedOnAdmin() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = deleteRequest(u, "/iam/accounts/" + u);
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should give FORBIDDEN for '%s'".formatted(u));
        }
    }
    
    @Test
    public void getClientAccountsAllowedForMembers() {
        for (String u : client_users) {
            ResponseEntity<String> response = request(u, "/iam/clients/test");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(u));
        }
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

    private String[] all_accounts =            { "nnn", "nnt", "nan", "nat", "unn", "unt", "uan", "uat" };
    private String[] nouser_noadmin_accounts = { "nnn", "nnt" };
    private String[] user_or_admin_accounts =  {               "nan", "nat", "unn", "unt", "uan", "uat" };
    private String[] user_accounts =    { "unn", "unt", "uan", "uat" };
    private String[] nouser_accounts =  { "nnn", "nnt", "nan", "nat" };
    private String[] admin_accounts =   { "nan", "nat", "uan", "uat" };
    private String[] noadmin_accounts = { "nnn", "nnt", "unn", "unt" };
    private String[] client_users =     { "unt", "uat" };
    private String[] noclient_acounts = { "nnn", "nan", "unn", "uan" };
}
