package it.zwets.sms.scheduler.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import it.zwets.sms.scheduler.security.IamService;

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
//        createAccount("dummy", new String[] { });
    }
    
    @AfterEach
    public void teardown() {
        iamService.deleteAccount("nnn");
        iamService.deleteAccount("nnt");
        iamService.deleteAccount("nan");
        iamService.deleteAccount("nat");
        iamService.deleteAccount("unn");
        iamService.deleteAccount("unt");
        iamService.deleteAccount("uan");
        iamService.deleteAccount("uat");
        iamService.deleteAccount("uat");
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

//    @Test
//    public void postAccountsForbiddenForNonAdmins() {
//        for (String u : noadmin_accounts) {
//            ResponseEntity<String> response = postRequest(u, "/iam/accounts");
//            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should be forbidden for '%s'".formatted(u));
//        }
//    }

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

    @Test
    public void deleteAccountForbiddenForNonAdmins() {
        throw new NotImplementedException();
//        for (String u : noadmin_accounts) {
//            ResponseEntity<String> response = request(u, "/iam/accounts/borkbork");
//            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should give not found for '%s'".formatted(u));
//        }
    }

    @Test
    public void createAccountForbiddenForNonAdmins() {
        throw new NotImplementedException();
//        for (String u : noadmin_accounts) {
//            ResponseEntity<String> response = request(u, "/iam/accounts/borkbork");
//            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should give not found for '%s'".formatted(u));
//        }
    }

    @Test
    public void deleteAccountForbiddenForLoggedOnAdmins() {
        throw new NotImplementedException();
//        for (String u : admin_accounts) {
//            ResponseEntity<String> response = request(u, "/iam/accounts/" + u);
//            assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode(), "Should give not acceptable for '%s'".formatted(u));
//        }
    }
    
    @Test
    public void getClientUsersAllowedForClientUsers() {
        for (String u : client_users) {
            ResponseEntity<String> response = request(u, "/iam/clients/test");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(u));
        }
    }
    

        // Helpers
    
    private ResponseEntity<String> request(String id, String url) {
        return new TestRestTemplate(id, id).getForEntity("http://localhost:" + port + url, String.class);
    }
    
//    private ResponseEntity<String> putRequest(String id, String url, String body) {
//        return new TestRestTemplate(id, id).exchange("http://localhost:" + port + url, HttpMethod.PUT, new RequestEntity<String>(), String.class, body);
//    }
    
    private IamService.AccountDetail createAccount(String id, String[] groups) {
        return iamService.createAccount(new IamService.AccountDetail(id, id, id, groups), id);
    }

    private void deleteAccount(String id) {
        iamService.deleteAccount(id);
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
