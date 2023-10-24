package it.zwets.sms.scheduler.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import it.zwets.sms.scheduler.iam.IamService.AccountDetail;
import it.zwets.sms.scheduler.iam.IamService.Flavour;

@SpringBootTest
class IamServiceTests {

    @Autowired
    private IamService svc;
    
    @Test
    void adminUserPresent() {
        assertTrue(svc.isValidAccount(IamService.INITIAL_ADMIN));
    }

    @Test
    void usersRoleGroupPresent() {
        assertNotNull(svc.getGroup(Flavour.ROLE, IamService.USERS_GROUP));
    }

    @Test
    void usersRoleGroupNotFoundAsClientGroup() {
        assertNull(svc.getGroup(Flavour.CLIENT, IamService.USERS_GROUP));
    }
    
    @Test
    void adminsRolePresent() {
        assertNotNull(svc.getGroup(Flavour.ROLE, IamService.ADMINS_GROUP));
    }

    @Test
    void testClientPresent() {
        assertNotNull(svc.getGroup(Flavour.CLIENT, IamService.TEST_GROUP));
    }
    
    @Test
    void testRoleCount() {
        assertEquals(2, svc.getGroups(Flavour.ROLE).length);
    }
    
    @Test
    void testClientCount() {
        assertEquals(1, svc.getGroups(Flavour.CLIENT).length);
    }

    @Test
    void testGroupCount() {
        assertEquals(3, svc.getGroups().length);
    }
    
    @Test
    void noTestRolePresent() {
        assertNull(svc.getGroup(Flavour.ROLE, IamService.TEST_GROUP));
    }
    
    @Test
    void testCreateAccountNoGroups() {
        final String id = "newid1";
        final String password = "my-new-password";
        
        AccountDetail account = new AccountDetail(id, "New Account", "email@example.com", password, null);
        svc.createAccount(account);
        
        AccountDetail found = svc.getAccount(id);
        assertEquals(account.id(), found.id());
        assertEquals(account.name(), found.name());
        assertEquals(account.email(), found.email());
        assertEquals(0, found.groups().length);
        assertTrue(svc.checkPassword(id, password));
        
        svc.deleteAccount(id);
        assertNull(svc.getAccount(id));
    }

    @Test
    void testCreateAccountInAdmins() {
        final String id = "newid2";
        final String password = "my-new-password";
        final String groups[] = { IamService.ADMINS_GROUP };
        
        AccountDetail account = new AccountDetail(id, "New Account", "email@example.com", password, groups);
        svc.createAccount(account);
        
        AccountDetail found = svc.getAccount(id);
        assertEquals(account.id(), found.id());
        assertEquals(account.name(), found.name());
        assertEquals(account.email(), found.email());
        assertEquals("admins", found.groups()[0]);
        assertTrue(svc.checkPassword(id, password));
        
        svc.deleteAccount(id);
    }
  
    @Test
    void testCreateAccountWithEmptyPassword() {
        final String id = "newid4";
        
        AccountDetail account = new AccountDetail(id, "New Account", "email@example.com", "", null);
        svc.createAccount(account);
        
        AccountDetail found = svc.getAccount(id);
        assertEquals(account.id(), found.id());
        assertEquals(account.name(), found.name());
        assertEquals(account.email(), found.email());
        assertEquals(0, found.groups().length);
        assertTrue(svc.checkPassword(id, ""));
        
        svc.deleteAccount(id);
        assertNull(svc.getAccount(id));
    }
    
 
    @Test
    void adminUserInAdmins() {
        assertTrue(svc.isAccountInGroup(IamService.INITIAL_ADMIN, IamService.ADMINS_GROUP));
    }

    @Test
    void adminUserInUsers() {
        assertTrue(svc.isAccountInGroup(IamService.INITIAL_ADMIN, IamService.USERS_GROUP));
    }

    @Test
    void adminUserInTest() {
        assertTrue(svc.isAccountInGroup(IamService.INITIAL_ADMIN, IamService.TEST_GROUP));
    }
    
    @Test
    void testPassword() {
        assertTrue(svc.checkPassword(IamService.INITIAL_ADMIN, IamService.INITIAL_PASSWORD));
    }

    @Test
    void testUpdatePassword() {
        final String NEW_PASSWORD = "NEW_PASSWORD";
        
        svc.updatePassword(IamService.INITIAL_ADMIN, NEW_PASSWORD);
        assertFalse(svc.checkPassword(IamService.INITIAL_ADMIN, IamService.INITIAL_PASSWORD));
        assertTrue(svc.checkPassword(IamService.INITIAL_ADMIN, NEW_PASSWORD));
        
        svc.updatePassword(IamService.INITIAL_ADMIN, IamService.INITIAL_PASSWORD);
        assertFalse(svc.checkPassword(IamService.INITIAL_ADMIN, NEW_PASSWORD));
        assertTrue(svc.checkPassword(IamService.INITIAL_ADMIN, IamService.INITIAL_PASSWORD));
    }

    @Test
    void testRemoveAdminUserFromGroup() {
        svc.removeAccountFromGroup(IamService.INITIAL_ADMIN, IamService.TEST_GROUP);
        assertFalse(svc.isAccountInGroup(IamService.INITIAL_ADMIN, IamService.TEST_GROUP));
        
        svc.addAccountToGroup(IamService.INITIAL_ADMIN, IamService.TEST_GROUP);
        assertTrue(svc.isAccountInGroup(IamService.INITIAL_ADMIN, IamService.TEST_GROUP));
    }

    @Test
    void testCreateAndDeleteRole() {
        final String NEW_ROLE_ID = "new-role";
        
        svc.createGroup(Flavour.ROLE, NEW_ROLE_ID);
        assertNotNull(svc.getGroup(Flavour.ROLE, NEW_ROLE_ID));
        
        svc.deleteGroup(NEW_ROLE_ID);
        assertNull(svc.getGroup(Flavour.ROLE, NEW_ROLE_ID));
    }

    @Test
    void testCreateAndDeleteClient() {
        final String NEW_CLIENT_ID = "new-client";
        
        svc.createGroup(Flavour.CLIENT, NEW_CLIENT_ID);
        assertNotNull(svc.getGroup(Flavour.CLIENT, NEW_CLIENT_ID));
        
        svc.deleteGroup(NEW_CLIENT_ID);
        assertNull(svc.getGroup(Flavour.ROLE, NEW_CLIENT_ID));
    }
}
