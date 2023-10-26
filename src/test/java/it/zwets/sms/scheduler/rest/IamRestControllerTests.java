package it.zwets.sms.scheduler.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
class IamRestControllerTests {

    private static final Logger LOG = LoggerFactory.getLogger(IamRestControllerTests.class);
    private static final String PASSWORD_PREFIX = "password_";

    @LocalServerPort
    private int port;

    @Autowired
    private IamService iamService;
    
    private RestTestHelper rest;
    
    @BeforeAll
    public void beforeAll() {
        rest = new RestTestHelper("http://localhost", port, PASSWORD_PREFIX);
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
        ResponseEntity<String> response = new TestRestTemplate("nnn", rest.getPassword("nnn"))
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

        // Tests on /accounts/{id}
    
    @Test
    public void getAccountWorksForSelf() {
        for (String u : all_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/accounts/" + u);
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should succeed for '%s'".formatted(u));
        }
    }

    @Test
    public void getAccountWorksForAdmins() {
        createAccount("getAccountWorksForAdmins", null);
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/accounts/getAccountWorksForAdmins");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should work for '%s'".formatted(u));
        }
        deleteAccount("getAccountWorksForAdmins");
    }

    @Test
    public void getAccountForbiddenForNonSelfNonAdmin() {
        createAccount("getAccountForbiddenForNonSelfNonAdmin", null);
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/accounts/target");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should be forbidden for '%s'".formatted(u));
        }
        deleteAccount("getAccountForbiddenForNonSelfNonAdmin");
    }
    
    @Test
    public void getNonExistentAccountForbiddenForNonAdmins() {
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/accounts/getNonExistentAccountForbiddenForNonAdmins");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should be forbidden for '%s'".formatted(u));
        }
    }
    
    @Test
    public void getNonExistentAccountNotFoundForAdmins() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/accounts/getNonExistentAccountNotFoundForAdmins");
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should give not found for '%s'".formatted(u));
        }
    }

        // Create Account
    
    @Test
    public void createAccountWorksForAdmins() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.POST(u, "/iam/accounts", simpleUserJson("createAccountWorksForAdmins", "users"));
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should work for '%s'".formatted(u));
            deleteAccount("createAccountWorksForAdmins");
        }
    }

    @Test
    public void createAccountForbiddenForNonAdmins() {
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = rest.POST(u, "/iam/accounts", simpleUserJson("createAccountForbiddenForNonAdmins", "users"));
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should give FORBIDDEN for '%s'".formatted(u));
        }
    }

    @Test
    public void createAccountChecksEmptyId() {
        String u = "nan";
        ResponseEntity<String> response = rest.POST(u, "/iam/accounts", simpleUserJson("", "users"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void createAccountChecksGroups() {
        ResponseEntity<String> response = rest.POST("nan", "/iam/accounts", simpleUserJson("createAccountChecksGroups", "non-existent-group"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void createAccountChecksExists() {
        ResponseEntity<String> response = rest.POST("nan", "/iam/accounts", simpleUserJson("nnn"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

        // Update Account
    
    @Test
    public void updateAccountWorksForAdmins() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.PUT(u, "/iam/accounts/dummy", simpleUserJson("dummy", "users"));
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should work for '%s'".formatted(u));
        }
    }

    @Test
    public void updateAccountForbiddenForNonAdmins() {
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = rest.PUT(u, "/iam/accounts/dummy", simpleUserJson("dummy", "users"));
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should give FORBIDDEN for '%s'".formatted(u));
        }
    }

    @Test
    public void updateAccountWorksForSelf() {
        String u = "nnn";
        ResponseEntity<String> response = rest.PUT(u, "/iam/accounts/" + u, userJson(null, "New Name", "new@example.com", null, null));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        AccountDetail account = deserializeAccount(response);
        assertEquals(u, account.id());
        assertEquals("New Name", account.name());
        assertEquals("new@example.com", account.email());
        assertNull(account.password());
        assertEquals(0, account.groups().length);
    }
    
    @Test void updateAccountCanOmitFields() {
        String u = "nnt";
        ResponseEntity<String> response = rest.PUT(u, "/iam/accounts/" + u, userJson(u, null, null, null, null));
        assertEquals(HttpStatus.OK, response.getStatusCode());

        AccountDetail account = deserializeAccount(response);
        assertEquals(u, account.id());
        assertEquals("Mr. " + u, account.name());
        assertEquals(u + "@example.com", account.email());
        assertNull(account.password());
        assertEquals(1, account.groups().length); // nnt has one group
        assertTrue(iamService.checkPassword(u, "password_" + u));
    }

    @Test
    public void updateAccountLeavesNullFieldsUntouched() {
        String u = "nnt";
        ResponseEntity<String> response = rest.PUT(u, "/iam/accounts/" + u, userJson(u, null, null, null, null));
        assertEquals(HttpStatus.OK, response.getStatusCode());

        AccountDetail account = deserializeAccount(response);
        assertEquals(u, account.id());
        assertEquals("Mr. " + u, account.name());
        assertEquals(u + "@example.com", account.email());
        assertNull(account.password());
        assertEquals(1, account.groups().length); // nnt has one group
        assertTrue(iamService.checkPassword(u, "password_" + u));
    }

    @Test
    public void updateAccountLeavesEmptyFieldsUntouched() {
        String u = "nat";
        ResponseEntity<String> response = rest.PUT(u, "/iam/accounts/" + u, userJson(u, "", "", "", null));
        assertEquals(HttpStatus.OK, response.getStatusCode());

        AccountDetail account = deserializeAccount(response);
        assertEquals(u, account.id());
        assertEquals("Mr. " + u, account.name());
        assertEquals(u + "@example.com", account.email());
        assertNull(account.password());
        assertEquals(2, account.groups().length); // nat has two
        assertTrue(iamService.checkPassword(u, "password_" + u));
    }

    @Test
    public void updateAccountCanChangePassword() {
        String u = "nan";
        String op = rest.getPassword(u);
        String np = "new-password";
        assertTrue(iamService.checkPassword(u, op));
        ResponseEntity<String> response = rest.PUT(u, "/iam/accounts/" + u, userJson(u, null, null, np, null));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(iamService.checkPassword(u, np));
        iamService.updatePassword(u, op);
        assertTrue(iamService.checkPassword(u, op));
    }

    @Test
    public void updateSelfAccountCannotChangeGroups() {
        String u = "unt";
        ResponseEntity<String> response = rest.PUT(u, "/iam/accounts/%s".formatted(u), userJson(u, "New Name", "new@example.com", null, 
                new String[] { IamService.ADMINS_GROUP, IamService.USERS_GROUP, IamService.TEST_GROUP }));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        AccountDetail account = deserializeAccount(response);
        assertEquals(u, account.id());
        assertEquals("New Name", account.name());
        assertEquals("new@example.com", account.email());
        assertNull(account.password());
        assertTrue(Arrays.asList(account.groups()).contains(IamService.USERS_GROUP));
        assertTrue(Arrays.asList(account.groups()).contains(IamService.TEST_GROUP));
        assertFalse(Arrays.asList(account.groups()).contains(IamService.ADMINS_GROUP));
        assertEquals(2, account.groups().length);
    }

    @Test
    public void updateAdminSelfAccountCanChangeGroups() {
        String u = "nan";
        ResponseEntity<String> response = rest.PUT(u, "/iam/accounts/%s".formatted(u), userJson(u, null, null, null, 
                new String[] { IamService.ADMINS_GROUP, IamService.TEST_GROUP }));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(iamService.isAccountInGroup(u, IamService.TEST_GROUP));
        iamService.removeAccountFromGroup(u, IamService.TEST_GROUP);
    }
    
    @Test
    public void updateAdminSelfAccountCannotRemoveAdminRole() {
        String u = "nan";
        ResponseEntity<String> response = rest.PUT(u, "/iam/accounts/%s".formatted(u), userJson(u, null, null, null, 
                new String[] { IamService.TEST_GROUP }));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
    
    @Test
    public void updateAccountFailsOnEmptyEndpoint() {
        ResponseEntity<String> response = rest.PUT("nan", "/iam/accounts/", simpleUserJson("dummy"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void updateAccountFailsOnMismatchingEndpoint() {
        ResponseEntity<String> response = rest.PUT("nan", "/iam/accounts/dummy", simpleUserJson("not-dummy"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void updateAccountFailsOnNonExistentUser() {
        ResponseEntity<String> response = rest.PUT("nan", "/iam/accounts/non-existent", simpleUserJson("non-existent"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
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
            String t = "dummy";
            String op = rest.getPassword(t);
            String np = "new-password";
            assertTrue(iamService.checkPassword(t, op));
            ResponseEntity<String> response = rest.POST(u, "/iam/accounts/%s/password".formatted(t), np);
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should give OK for '%s'".formatted(u));
            assertTrue(iamService.checkPassword(t, np));
            iamService.updatePassword(t, op);            
            assertTrue(iamService.checkPassword(t, op));
        }
    }

    @Test
    public void updatePasswordAllowedForSelf() {
        for (String u : all_accounts) {
            String op = rest.getPassword(u);
            String np = "new-password";
            assertTrue(iamService.checkPassword(u, op));
            ResponseEntity<String> response = rest.POST(u, "/iam/accounts/%s/password".formatted(u), np);
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should give OK for '%s'".formatted(u));
            assertTrue(iamService.checkPassword(u, np));
            iamService.updatePassword(u, op);            
            assertTrue(iamService.checkPassword(u, op));
        }
    }

    @Test
    public void updatePasswordForbiddenForOther() {
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = rest.POST(u, "/iam/accounts/dummy/password", "anything");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should give FORBIDDEN for '%s'".formatted(u));
        }
    }

        // General Roles Methods

    @Test
    public void getRolesAllowedForAdmins() {
        for (String u : admin_accounts) {
            for (String r : new String[]{ "users", "admins" }) {
                ResponseEntity<String> response = rest.GET(u, "/iam/roles/" + r);
                assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(u));
            }
        }
    }

    @Test
    public void getRolesForbiddenForNonAdmins() {
        for (String u : noadmin_accounts) {
            for (String r : new String[]{ "users", "admins" }) {
                ResponseEntity<String> response = rest.GET(u, "/iam/roles/" + r);
                assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should give FORBIDDEN for '%s'".formatted(u));
            }
        }
    }

        // Group Users Members

    @Test
    public void getUsersAccountAllowedForAdmins() {
        for (String a : admin_accounts) {
            for (String u : user_accounts) {
                ResponseEntity<String> response = rest.GET(a, "/iam/roles/users/" + u);
                assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(a));
            }
            for (String u : nouser_accounts) {
                ResponseEntity<String> response = rest.GET(a, "/iam/roles/users/" + u);
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should be not found for '%s'".formatted(a));
            }
        }
    }

    @Test
    public void getUsersAccountAllowedForSelf() {
        for (String u : user_noadmin_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/roles/users/" + u);
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be OK for '%s'".formatted(u));
        }
    }
    
    @Test
    public void getUsersAccountForbiddenForNonSelf() {
        for (String u : user_noadmin_accounts) {
            for (String o : all_accounts) {
                if (!u.equals(o)) {
                    ResponseEntity<String> response = rest.GET(u, "/iam/roles/users/" + o);
                    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should be OK for '%s'".formatted(u));
                }
            }
        }
    }
   
    @Test void postAndDeleteAccountInUsersAllowedForAdmins() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.POST(u, "/iam/roles/users", "nnn");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(u));
            response = rest.GET(u, "/iam/roles/users/nnn");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(u));
            response = rest.DELETE(u, "/iam/roles/users/nnn");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(u));
        }
    }

    @Test
    public void postAccountInUsersForbiddenForNonAdmins() {
        for (String u : noadmin_accounts) {
            ResponseEntity<String> response = rest.POST(u, "/iam/roles/users", "nnn");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should be FORBIDDEN for '%s'".formatted(u));
        }
    }

    @Test
    public void postNonExistentAccountInUsers() {
        ResponseEntity<String> response = rest.POST("nan", "/iam/roles/users", "nonexistent");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void deleteNonExistentAccountInUsersIsOk() {
        ResponseEntity<String> response = rest.DELETE("nan", "/iam/roles/users/nonexistent");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void duplicatePostAccountInUsersIsOK() {
        ResponseEntity<String> response = rest.POST("nan", "/iam/roles/users", "unn");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

        // Group Admins Members

    @Test
    public void getAdminsAccountAllowedForAdmins() {
        for (String u : admin_accounts) {
            for (String a : admin_accounts) {
                ResponseEntity<String> response = rest.GET(u, "/iam/roles/admins/" + a);
                assertEquals(HttpStatus.OK, response.getStatusCode(), "Should be allowed for '%s'".formatted(u));
            }
            for (String a : noadmin_accounts) {
                ResponseEntity<String> response = rest.GET(u, "/iam/roles/admins/" + a);
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should be not found for '%s'".formatted(u));
            }
        }
    }

    @Test
    public void getAdminsAccountAllowedForSelf() {
        for (String u : admin_accounts) {
            ResponseEntity<String> response = rest.GET(u, "/iam/roles/admins/" + u);
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
    
    private String simpleUserJson(String name, String... groups) {
        return userJson(name, name, name, name, groups);
    }
    
    private String userJson(String id, String name, String email, String password, String[] groups) {

        String sep = "";
        StringBuilder b = new StringBuilder();
        b.append("{ ");
        if (id != null) { b.append(sep).append(quotedField("id", id)); sep = ", "; }
        if (name != null) { b.append(sep).append(quotedField("name", name)); sep = ", "; }
        if (email != null) { b.append(sep).append(quotedField("email", email)); sep = ", "; }
        if (password != null) { b.append(sep).append(quotedField("password", password)); sep = ", "; }
        if (groups != null) { b.append(sep).append(quotedField("groups", groups)); sep = ", "; }
        b.append(" }");
        
        LOG.debug("JSON: {}", b.toString());
        return b.toString();
    }
    
    private String quotedField(String name, String value) {
        return "\"%s\": %s".formatted(name, quote(value));
    }
    
    private String quotedField(String name, String[] value) {
        return "\"%s\": %s".formatted(name, quote(value));
    }
    
    private String quote(String s) {
        return s == null ? "null" : "\"%s\"".formatted(s);
    }
    
    private String quote(String[] ss) {
        return ss == null ? "null" : 
            "[ " + Arrays.stream(ss).map(s -> "\"%s\"".formatted(s)).collect(Collectors.joining(",")) + " ]";
    }
    
    private IamService.AccountDetail createAccount(String id, String[] groups) {
        return iamService.createAccount(new IamService.AccountDetail(
                id, "Mr. " + id, id + "@example.com", PASSWORD_PREFIX + id, groups));
    }

    private void deleteAccount(String id) {
        iamService.deleteAccount(id);
    }

    private String[] all_accounts =            { "nnn", "nnt", "nan", "nat", "unn", "unt", "uan", "uat" };
//    private String[] nouser_noadmin_accounts = { "nnn", "nnt" };
    private String[] noclient_noadm_accounts = { "nnn",                      "unn",                     };
    private String[] user_noadmin_accounts   = {                             "unn", "unt"               };
//    private String[] user_or_admin_accounts =  {               "nan", "nat", "unn", "unt", "uan", "uat" };
    private String[] user_accounts =    { "unn", "unt", "uan", "uat" };
    private String[] nouser_accounts =  { "nnn", "nnt", "nan", "nat" };
    private String[] admin_accounts =   { "nan", "nat", "uan", "uat" };
    private String[] noadmin_accounts = { "nnn", "nnt", "unn", "unt" };
    private String[] client_users =     { "unt", "uat" };
}

// vim: sts=4:sw=4:et:ai:si
