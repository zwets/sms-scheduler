package it.zwets.sms.scheduler.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.zwets.sms.scheduler.iam.IamService;
import it.zwets.sms.scheduler.iam.IamService.AccountDetail;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
class IamRestControllerSecurityTests {

    private static final Logger LOG = LoggerFactory.getLogger(IamRestControllerSecurityTests.class);

    @LocalServerPort
    private int port;

    @Autowired
    private IamService iamService;
    
    private RestTestHelper rest;
    
    @BeforeAll
    public void beforeAll() {
        rest = new RestTestHelper("http://localhost", port);
    }
    
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
        deleteAccount("dummy");
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
    
        // Tests on /accounts
    
    @Test
    public void getAccountsForbiddenForNonAdmin() {
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/accounts");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should be forbidden for '%s'".formatted(u));
        }
    }

    @Test
    public void getAccountsAllowedForAdmins() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/accounts");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be OK for '%s'".formatted(u));
        }
    }

        // Tests on /accounts/{id} i.e. "my own account"
    
    @Test
    public void getMyAccountWorksForAllAccounts() {
        for (String u : all_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/accounts/" + u);
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should succeed for '%s'".formatted(u));
        }
    }

    @Test
    public void getAnotherAccountWorksOnlyForAdmins() {
        createAccount("target", null);
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/accounts/target");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should work for '%s'".formatted(u));
        }
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/accounts/target");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should be forbidden for '%s'".formatted(u));
        }
        deleteAccount("target");
    }
    
    @Test
    public void getNonExistentAccountForbiddenForNonAdmins() {
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/accounts/borkbork");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should be forbidden for '%s'".formatted(u));
        }
    }
    
    @Test
    public void getNonExistentAccountNotFoundForAdmins() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/accounts/borkbork");
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should give not found for '%s'".formatted(u));
        }
    }

        // Create Account
    
    @Test
    public void createAccountWorksForAdmins() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.POST(u, "/iam/accounts", userJson("newuser", "users"));
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should work for '%s'".formatted(u));
            deleteAccount("newuser");
        }
    }

    @Test
    public void createDuplicateAccountConflict() {
        ResponseEntity<String> response = rest.POST("nan", "/iam/accounts", userJson("nnn", null));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        deleteAccount("newuser");
    }

    @Test
    public void createAccountForbiddenForNonAdmins() {
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = rest.POST(u, "/iam/accounts", userJson("newuser", "users"));
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should give FORBIDDEN found for '%s'".formatted(u));
        }
    }

        // Update Account
    
    @Test
    public void updateAccountWorksForAdmins() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.PUT(u, "/iam/accounts/dummy", userJson("dummy", "users"));
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should work for '%s'".formatted(u));
        }
    }

    @Test
    public void updateAccountOnEmptyEndpoint() {
        ResponseEntity<String> response = rest.PUT("nan", "/iam/accounts/", userJson("dummy", null));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void updateAccountOnMismatchingEndpoint1() {
        ResponseEntity<String> response = rest.PUT("nan", "/iam/accounts/dummy", userJson("not-dummy", null));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void updateAccountOnMismatchingEndpoint2() {
        ResponseEntity<String> response = rest.PUT("nan", "/iam/accounts/not-dummy", userJson("dummy", null));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void updateAccountWorksForSelf() {
        String u = "nnn";
        ResponseEntity<String> response = rest.PUT(u, "/iam/accounts/%s".formatted(u), makeUserJson(u, "New Name", "new@example.com", null, null));
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should work for '%s'".formatted(u));
        
        AccountDetail account = deserializeAccount(response);
        assertEquals(u, account.id());
        assertEquals("New Name", account.name());
        assertEquals("new@example.com", account.email());
        assertNull(account.password());
        assertEquals(0, account.groups().length);
    }
    
    @Test
    public void updateAccountForSelfCannotChangeGroups() {
        String u = "unt";
        ResponseEntity<String> response = rest.PUT(u, "/iam/accounts/%s".formatted(u), makeUserJson(u, "New Name", "new@example.com", null, 
                new String[] { "admins", "users", "test" }));
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should work for '%s'".formatted(u));
        
        AccountDetail account = deserializeAccount(response);
        assertEquals(u, account.id());
        assertEquals("New Name", account.name());
        assertEquals("new@example.com", account.email());
        assertNull(account.password());
        assertEquals(2, account.groups().length);
    }

        // Delete Account
    
    @Test
    public void deleteAccountForbiddenForNonAdmins() {
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = rest.DELETE(u, "/iam/accounts/dummy");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should give FORBIDDEN for '%s'".formatted(u));
        }
    }

    @Test
    public void deleteAccountAllowedForAdmin() {
        for (String u : admin_accounts) {
            createAccount("deleteme", null);
            ResponseEntity<String> response = rest.DELETE(u, "/iam/accounts/deleteme");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should give OK for '%s'".formatted(u));
        }
    }
    
    @Test
    public void deleteAccountForbiddenForLoggedOnAdmin() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.DELETE(u, "/iam/accounts/" + u);
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should give FORBIDDEN for '%s'".formatted(u));
        }
    }

    @Test
    public void deleteNonExistentAccountIsOk() {
        ResponseEntity<String> response = rest.DELETE("nan", "/iam/accounts/nonexistent");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

        // Password

    @Test
    public void updatePasswordAllowedForAdmins() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.POST(u, "/iam/accounts/dummy/password", u);
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should give OK for '%s'".formatted(u));
            iamService.checkPassword("dummy", u);
        }
    }

    @Test
    public void updatePasswordAllowedForSelf() {
        for (String u : all_accounts) {
            ResponseEntity<String> response = rest.POST(u, "/iam/accounts/%s/password".formatted(u), "new-password");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should give OK for '%s'".formatted(u));
            iamService.checkPassword("dummy", "new-password");
        }
    }

    @Test
    public void updatePasswordNotAllowedForOther() {
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = rest.POST(u, "/iam/accounts/dummy/password", "anything");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should give FORBIDDEN for '%s'".formatted(u));
        }
    }

        // General Groups Methods

    @Test
    public void getRolesAllowedForAdmins() {
        for (String u : admin_accounts) {
            for (String r : new String[]{ "users", "admins" }) {
                ResponseEntity<String> response = rest.GET(u, "/iam/groups/" + r);
                assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(u));
            }
        }
    }

    @Test
    public void getRolesForbiddenForNonAdmins() {
        for (String u : noadmin_accounts) {
            for (String r : new String[]{ "users", "admins" }) {
                ResponseEntity<String> response = rest.GET(u, "/iam/groups/" + r);
                assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should give FORBIDDEN for '%s'".formatted(u));
            }
        }
    }

        // Group Users Members

    @Test
    public void getUsersAccountAllowedForAdmins() {
        for (String a : admin_accounts) {
            for (String u : user_accounts) {
                ResponseEntity<String> response = rest.GET(a, "/iam/groups/users/" + u);
                assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(a));
            }
            for (String u : nouser_accounts) {
                ResponseEntity<String> response = rest.GET(a, "/iam/groups/users/" + u);
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should be not found for '%s'".formatted(a));
            }
        }
    }

    @Test
    public void getUsersAccountAllowedForSelf() {
        for (String u : user_noadmin_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/groups/users/" + u);
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be OK for '%s'".formatted(u));
        }
    }
    
    @Test
    public void getUsersAccountForbiddenForNonSelf() {
        for (String u : user_noadmin_accounts) {
            for (String o : all_accounts) {
                if (!u.equals(o)) {
                    ResponseEntity<String> response = rest.GET(u, "/iam/groups/users/" + o);
                    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should be OK for '%s'".formatted(u));
                }
            }
        }
    }
   
    @Test void postAndDeleteAccountInUsersAllowedForAdmins() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.POST(u, "/iam/groups/users", "nnn");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(u));
            response = rest.GET(u, "/iam/groups/users/nnn");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(u));
            response = rest.DELETE(u, "/iam/groups/users/nnn");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(u));
        }
    }

    @Test
    public void postAccountInUsersForbiddenForNonAdmins() {
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = rest.POST(u, "/iam/groups/users", "nnn");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should be FORBIDDEN for '%s'".formatted(u));
        }
    }

    @Test
    public void postNonExistentAccountInUsers() {
        ResponseEntity<String> response = rest.POST("nan", "/iam/groups/users", "nonexistent");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void deleteNonExistentAccountInUsersIsOk() {
        ResponseEntity<String> response = rest.DELETE("nan", "/iam/groups/users/nonexistent");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void duplicatePostAccountInUsersIsOK() {
        ResponseEntity<String> response = rest.POST("nan", "/iam/groups/users", "unn");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

        // Group Admins Members

    @Test
    public void getAdminsAccountAllowedForAdmins() {
        for (String u : admin_accounts) {
            for (String a : admin_accounts) {
                ResponseEntity<String> response = rest.GET(u, "/iam/groups/admins/" + a);
                assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(u));
            }
            for (String a : noadmin_accounts) {
                ResponseEntity<String> response = rest.GET(u, "/iam/groups/admins/" + a);
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should be not found for '%s'".formatted(u));
            }
        }
    }

    @Test
    public void getAdminsAccountAllowedForSelf() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/groups/admins/" + u);
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be OK for '%s'".formatted(u));
        }
    }
    
        // Clients Methods
    
    @Test
    public void getClientListAllowedForAdmins() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/clients");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(u));
        }
    }

    @Test
    public void getClientAccountsAllowedForMembers() {
        for (String u : client_users) {
            ResponseEntity<String> response = rest.GET(u, "/iam/clients/test");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(u));
        }
    }

    @Test
    public void getClientAccountsForbiddenForNonMembers() {
        for (String u : noclient_noadm_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/clients/test");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should give FORBIDDEN for '%s'".formatted(u));
        }
    }

    @Test
    public void createAndDeleteClientAllowedForAdmins() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.POST(u, "/iam/clients", "new-client");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(u));
            response = rest.DELETE(u, "/iam/clients/new-client");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(u));
        }
    }

    @Test
    public void createAndDeleteClientForbiddenForNonAdmins() {
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = rest.POST(u, "/iam/clients", "new-client");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should be forbidden for '%s'".formatted(u));
            response = rest.DELETE(u, "/iam/clients/test");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should be forbidden for '%s'".formatted(u));
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
    
    private AccountDetail deserializeAccount(ResponseEntity<String> entity) {
        
        JsonNode root = asJson(entity);
        String id = root.get("id").asText(null);
        String name = root.get("name").asText(null);
        String email = root.get("email").asText(null);
        String password = root.get("password").asText(null);
        
        JsonNode gnode = root.get("groups");
        String[] groups = gnode.isNull() ? null : new String[gnode.size()];
        for (int i = 0; i < gnode.size(); ++i) {
            groups[i] = gnode.get(i).asText(null);
        }
        
        return new AccountDetail(id, name, email, password, groups);
    }
    
    private String userJson(String name, String... groups) {
        return makeUserJson(name, name, name, name, groups);
    }
    
    private String makeUserJson(String id, String name, String email, String password, String[] groups) {

        String groupValue = groups == null ? "null" : 
            "[ " + Arrays.stream(groups).map(s -> "\"%s\"".formatted(s)).collect(Collectors.joining(",")) + " ]";

        return "{ 'id': '%s', 'name': '%s', 'email': '%s', 'password': '%s', 'groups': %s }"
                .replace('\'', '"')
                .formatted(id, name, email, password, groupValue);
    }
    
    private IamService.AccountDetail createAccount(String id, String[] groups) {
        return iamService.createAccount(new IamService.AccountDetail(id, id, id, id, groups));
    }

    private void deleteAccount(String id) {
        iamService.deleteAccount(id);
    }

    private String[] all_accounts =            { "nnn", "nnt", "nan", "nat", "unn", "unt", "uan", "uat" };
    private String[] nouser_noadmin_accounts = { "nnn", "nnt" };
    private String[] noclient_noadm_accounts = { "nnn",                      "unn",                     };
    private String[] user_noadmin_accounts   = {                             "unn", "unt"               };
    private String[] user_or_admin_accounts =  {               "nan", "nat", "unn", "unt", "uan", "uat" };
    private String[] user_accounts =    { "unn", "unt", "uan", "uat" };
    private String[] nouser_accounts =  { "nnn", "nnt", "nan", "nat" };
    private String[] admin_accounts =   { "nan", "nat", "uan", "uat" };
    private String[] noadmin_accounts = { "nnn", "nnt", "unn", "unt" };
    private String[] client_users =     { "unt", "uat" };
    private String[] noclient_acounts = { "nnn", "nan", "unn", "uan" };
}

// vim: sts=4:sw=4:et:ai:si
