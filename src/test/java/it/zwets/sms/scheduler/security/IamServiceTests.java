package it.zwets.sms.scheduler.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class IamServiceTests {

    @Autowired
    private IamService svc;
    
    @Test
    void adminUserPresent() {
        assertTrue(svc.isValidAccount(AdminService.DEFAULT_USER));
    }

    @Test
    void usersRolePresent() {
        assertNotNull(svc.getRole(AdminService.USERS_ROLE));
    }

    @Test
    void noUsersClientPresent() {
        assertNull(svc.getClient(AdminService.USERS_ROLE));
    }
    
    @Test
    void adminsRolePresent() {
        assertNotNull(svc.getRole(AdminService.USERS_ROLE));
    }

    @Test
    void testClientPresent() {
        assertNotNull(svc.getClient(AdminService.TEST_CLIENT));
    }
    
    @Test
    void testRoleCount() {
        assertEquals(2, svc.getRoles().length);
    }
    
    @Test
    void testClientCount() {
        assertEquals(1, svc.getClients().length);
    }

    @Test
    void testGroupCount() {
        assertEquals(3, svc.getGroups().length);
    }
    
    @Test
    void noTestRolePresent() {
        assertNull(svc.getRole(AdminService.TEST_CLIENT));
    }
    
    @Test
    void adminUserInAdmins() {
        assertTrue(svc.isAccountInGroup(AdminService.DEFAULT_USER, AdminService.ADMINS_ROLE));
    }

    @Test
    void adminUserInUsers() {
        assertTrue(svc.isAccountInGroup(AdminService.DEFAULT_USER, AdminService.USERS_ROLE));
    }

    @Test
    void adminUserInTest() {
        assertTrue(svc.isAccountInGroup(AdminService.DEFAULT_USER, AdminService.TEST_CLIENT));
    }
    
    @Test
    void testPassword() {
        assertTrue(svc.checkPassword(AdminService.DEFAULT_USER, AdminService.DEFAULT_PASSWORD));
    }

    @Test
    void testUpdatePassword() {
        final String NEW_PASSWORD = "NEW_PASSWORD";
        svc.updatePassword(AdminService.DEFAULT_USER, NEW_PASSWORD);
        assertFalse(svc.checkPassword(AdminService.DEFAULT_USER, AdminService.DEFAULT_PASSWORD));
        assertTrue(svc.checkPassword(AdminService.DEFAULT_USER, NEW_PASSWORD));
        svc.updatePassword(AdminService.DEFAULT_USER, AdminService.DEFAULT_PASSWORD);
        assertFalse(svc.checkPassword(AdminService.DEFAULT_USER, NEW_PASSWORD));
    }

    @Test
    void removeAdminUserFromTest() {
        svc.removeAccountFromGroup(AdminService.DEFAULT_USER, AdminService.TEST_CLIENT);
        assertFalse(svc.isAccountInGroup(AdminService.DEFAULT_USER, AdminService.TEST_CLIENT));
        svc.addAccountToGroup(AdminService.DEFAULT_USER, AdminService.TEST_CLIENT);
        assertTrue(svc.isAccountInGroup(AdminService.DEFAULT_USER, AdminService.TEST_CLIENT));
    }

    @Test
    void createRole() {
        final String NEW_ROLE_ID = "new-role";
        svc.createRole(NEW_ROLE_ID, "My New Role");
        assertNotNull(svc.getRole(NEW_ROLE_ID));
    }

    @Test
    void createClient() {
        final String NEW_CLIENT_ID = "new-client";
        svc.createRole(NEW_CLIENT_ID, "My New Client");
        assertNotNull(svc.getRole(NEW_CLIENT_ID));
    }
}
