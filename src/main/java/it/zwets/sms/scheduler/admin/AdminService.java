package it.zwets.sms.scheduler.admin;

import java.io.Serializable;
import java.util.Arrays;

import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Manages accounts and groups.
 *
 * The way this is built on top of the Flowable IDM component is:
 * <ul>
 * <li>an Account is an id with a name, email and password</li>
 * <li>an Account can be in zero or more Roles (groups) and in zero
 *   or more Clients (groups).</li>
 * <li>Roles and Clients are just two different types of groups.
 *   Roles (users, admins) determine what an account can do; clients
 *   determine for which client (e.g. consort, mint) an account can 
 *   operate.</li>
 * </ul>
 * Out of the box we have:
 * <ul>
 * <li>Two role groups: users and admins.  Their groupIds are given by
 *   <code>{@link USERS_ROLE}</code> and <code>{@link ADMINS_ROLE}</code></li>
 * <li>One client group: test, whose groupId is given by 
 *   <code>{@link TEST_CLIENT}</code></li>
 * <li>One admin account: admin, whose accountId and password are specified
 *   by the properties given by <code>{@link INITIAL_USER_PROPERTY}</code>
 *   and <code>{@link INITIAL_PASSWORD_PROPERTY}</code>.</li>
 * </ul>
 * The out-of-box admin account is a member of all groups (roles and clients).
 * You should <b>remove it once you have used it to create an admin account
 * for yourself.</b> It will not be recreated unless you remove all accounts.
 * If you do not want it to be created at all, comment out the 
 * <code>sms.scheduler.admin.*</code> properties.
 * 
 * @param id
 * @param clientId
 */
@Component
public class AdminService {
    
    private static final Logger LOG = LoggerFactory.getLogger(AdminService.class);
    
    /** Name of the built-in <code>USERS</code> group. */
    public static final String USERS_ROLE = "users";
    
    /** Name of the built-in <code>ADMINS</code> group. */
    public static final String ADMINS_ROLE = "admins";

    /** Name of the built-in <code>TEST</code> client (group). */
    public static final String TEST_CLIENT = "test";

    private static final String ROLE_TYPE = "role"; 
    private static final String CLIENT_TYPE = "client";

    @Value("${sms.scheduler.admin.user")
    private String initialUser;
    
    @Value("${sms.scheduler.admin.password")
    private String initialPassword;
    
    private IdentityService identityService;
   
    public AdminService(IdentityService identityService) {
        this.identityService = identityService;
        
        if (identityService.createUserQuery().count() == 0) {
            LOG.debug("no accounts found in database, doing out-of-box setup");
          
            if (initialUser != null && initialPassword != null && !initialPassword.isBlank()) {
                LOG.info("create the out-of-box accounts and groups", initialUser);
                
                createRole(USERS_ROLE, "Users");
                createRole(ADMINS_ROLE, "Administrators");
                createClient(TEST_CLIENT, "Test Client");

                LOG.info("create the out-of-box admin account {} with password {}", initialUser, initialPassword);
                createAccount(new AccountDetail(
                        initialUser,
                        "Default Admin User",
                        "nobody@example.com",
                        new String[]{ USERS_ROLE, ADMINS_ROLE, TEST_CLIENT }),
                        initialPassword);
                
                LOG.warn("**IMPORTANT** remove the out-of-box {} account once you have"
                        + " used it set up an admin account for yourself", initialUser);
            }
            else {
                LOG.warn("no out-of-box accounts set up, no sms.scheduler.admin.* properties");
            }
        }
        else {
            LOG.debug("user accounts already present, skipping the out-of-box account setup");
        }
    }
 
    /**
     * Create an account, no action if an account by this ID already exists.
     * @param details the id, name, email and optional groups to add the account to
     * @param password the password to set on the account
     * @return the Account found or created
     * @throws RuntimeException from back-end if name (or email?) is not unique
     */
    public AccountDetail createAccount(final AccountDetail detail, String password) {
        LOG.debug("create account: {}", detail.id); 

        AccountDetail account = getAccount(detail.id);
        
        if (account == null) {

            account = new AccountDetail(
                    detail.id,
                    detail.name,
                    detail.email,
                    detail.groups == null ? new String[0] : Arrays.copyOf(detail.groups, detail.groups.length));
            
            LOG.info("creating account: {}", account.id);
            User user = identityService.newUser(account.id);
            user.setDisplayName(account.name);
            user.setEmail(account.email);
            user.setPassword(password);
            identityService.saveUser(user);

            for (String groupId : account.groups) {
                addAccountToGroup(account.id, groupId);
            }
        }

        return account;
    }

    /**
     * Delete account, ignored if it does not exist.
     * @param id
     */
    public void deleteAccount(String id) {
        LOG.info("delete account: {}", id);
        identityService.deleteUser(id);
    }

    /**
     * Return true iff account id exists.
     * @param id
     * @return
     */
    public boolean isValidAccount(String id) {
        boolean result = identityService.createUserQuery().userId(id).count() != 0;
        LOG.debug("isValidAccount({}) -> {}", id, result);
        return result;
    }
    
    /**
     * Return account details for id, or null if not found.
     * @param id the account Id to look up
     * @return
     */
    public AccountDetail getAccount(String id) {
        LOG.debug("get account: {}", id);
        
        AccountDetail account = null;
        
        User user = identityService.createUserQuery().userId(id).singleResult();
        if (user != null) {
       
            account = new AccountDetail(user.getId(), user.getDisplayName(), user.getEmail(), null);
            
            LOG.debug("retrieve groups for account: {}", id);
            account.groups = (String[]) identityService.createGroupQuery()
                .groupMember(id).list().stream()
                .map(g -> g.getId())
                .toArray();
        }
        
        return account;
    }

    /**
     * Retrieve all Accounts.
     * Note the account.groups fields will be set to null.
     * @return array of accounts, with the groups null-ed out.
     */
    public AccountDetail[] getAccounts() {
        LOG.debug("retrieve all accounts");
        
        return (AccountDetail[]) identityService.createUserQuery()
                .orderByUserId().asc().list().stream()
                .map(u -> new AccountDetail(u.getId(), u.getDisplayName(), u.getEmail(), null))
                .toArray();
    }

    /**
     * Retrieve all Accounts in Group
     * Note the account.groups fields will be set to null.
     * @return array of accounts, with the groups null-ed out
     */
    public AccountDetail[] getAccountsInGroup(String groupId) {
        LOG.debug("retrieve accounts in group: {}", groupId);
        
        return (AccountDetail[]) identityService.createUserQuery()
                .memberOfGroup(groupId)
                .orderByUserId().asc().list().stream()
                .map(u -> new AccountDetail(u.getId(), u.getDisplayName(), u.getEmail(), null))
                .toArray();
    }

    /**
     * Check if account is member of group.
     * @p6aram accountId
     * @param groupId
     * @return true iff accountId is a member of groupId.
     */
    public boolean isAccountInGroup(String accountId, String groupId) {
        boolean result = identityService.createGroupQuery()
                .groupId(groupId)
                .groupMember(accountId)
                .count() != 0;
        LOG.debug("isAccountInGroup({},{}) -> {}", accountId, groupId, result);
        return result;
    }

    /**
     * Add account to group.
     * @param accountId
     * @param groupId
     * @throws RuntimeException if account or group don't exist
     */
    public void addAccountToGroup(String accountId, String groupId) {
        LOG.info("add account {} to group {}", accountId, groupId);
        if (!isAccountInGroup(accountId, groupId)) {
            identityService.createMembership(accountId, groupId);
        }
    }
    
    /**
     * Remove account from group.
     * @param accountId
     * @param groupId
     */
    public void removeAccountFromGroup(String accountId, String groupId) {
        LOG.info("remove account {} from group {}", accountId, groupId);
        identityService.deleteMembership(accountId, groupId);
    }
    
    /**
     * Change password for id
     * @param accountId
     * @param password
     */
    public void updateAccountPassword(String id, String password) {
        LOG.debug("update password for account: {}", id);
        User user = identityService.createUserQuery().userId(id).singleResult();
        if (user != null) {
            user.setPassword(password);
            identityService.updateUserPassword(user);
            LOG.debug("password updated for account: {}", id);
        }
        else {
            LOG.warn("account does not exist: {}", id);
        }
    }

    /**
     * Create a role group. No effect if already exists.
     * @param groupId
     * @param name
     * @return the role details with the account list in it
     */
    public GroupDetail createRole(String groupId, String name) {
        LOG.info("creating role group: {}", groupId);
        return createGroup(groupId, name, ROLE_TYPE);
    }
    
    /**
     * Create a client group. No effect if already exists.
     * @param groupId
     * @param name
     * @return the client details with the account list in it
     */
    public GroupDetail createClient(String groupId, String name) {
        LOG.info("creating client group: {}", groupId);
        return createGroup(groupId, name, CLIENT_TYPE);        
    }
    
    /**
     * Create a new group or return existing group.
     * @param clientId
     * @param name
     * @param type
     * @return the GroupDetail, with group list
     */
    protected GroupDetail createGroup(String groupId, String name, String type) {
        LOG.debug("creating group: {}", groupId);        

        GroupDetail group = getGroup(groupId, null);
        
        if (group == null) {
            Group flwGroup = identityService.newGroup(groupId);
            flwGroup.setId(groupId);
            flwGroup.setName(name);
            flwGroup.setType(type);
            identityService.saveGroup(flwGroup);
            
            group = new GroupDetail(groupId, name, type, new String[0]);
        }
        else if (group.getType() != type) {
            throw new RuntimeException("cannot create %s group %s: %s group with that ID exists"
                    .formatted(type, groupId, group.getType()));
        }
        
        return group;
    }

    /**
     * Retrieve details of role as well as its account id list, or null if not found
     * @param id
     * @return the group details
     */
    public GroupDetail getRole(String id) {
        LOG.debug("retrieve role details for id: {}", id);
        return getGroup(id, ROLE_TYPE);
    }

    /**
     * Retrieve details of client group, with list of member accounts, or null if not found.
     * @param id
     * @return the group details
     */
    public GroupDetail getClient(String id) {
        LOG.debug("retrieve client details for id: {}", id);
        return getGroup(id, CLIENT_TYPE);
    }

    /**
     * Retrieve details of group (of any type), with list of member accounts, or null if not found.
     * @param id
     * @return the group details
     */
    public GroupDetail getGroup(String id) {
        LOG.debug("retrieve group: {}", id);
        return getGroup(id, null);
    }

    /**
     * Retrieve group with id and optional type, including member account ids, or null if not found.
     * @param id the role or client ID
     * @param type the group type to filter on or null to return any type
     * @return the group details
     */
    protected GroupDetail getGroup(String id, String type) {
        LOG.debug("retrieve group: {}", id);
       
        GroupDetail group = null;
        
        Group flwGroup = identityService.createGroupQuery().groupId(id).singleResult();
        if (flwGroup != null && (type == null || type == flwGroup.getType())) {
            
            group = new GroupDetail(flwGroup.getId(), flwGroup.getName(), flwGroup.getType(), null);

            LOG.debug("retrieve accounts for group: {}", id);
            group.accounts = (String[]) identityService.createUserQuery()
                .memberOfGroup(id).list().stream()
                .map(u -> u.getId())
                .toArray();
        }
        
        return group;    
    }

    /**
     * Retrieve all role groups (without their members)
     * @return the list of all role groups, with null-ed out accounts list
     */
    public GroupDetail[] getRoles() {
        LOG.debug("retrieve all roles");
        return (GroupDetail[]) identityService.createGroupQuery()
                .groupType(ROLE_TYPE)
                .orderByGroupId().asc().list().stream()
                .map(g -> new GroupDetail(g.getId(), g.getName(), g.getType(), null))
                .toArray();
    }

    /**
     * Retrieve all client groups (without their members)
     * @return the list of all client groups, with null-ed out accounts list
     */
    public GroupDetail[] getClients() {
        LOG.debug("retrieve all clients");
        return (GroupDetail[]) identityService.createGroupQuery()
                .groupType(CLIENT_TYPE)
                .orderByGroupId().asc().list().stream()
                .map(g -> new GroupDetail(g.getId(), g.getName(), g.getType(), null))
                .toArray();
    }

    /**
     * Retrieve all groups (role groups and client groups)
     * @return the list of all groups, but with null-ed out accounts list
     */
    public GroupDetail[] getGroups() {
        LOG.debug("retrieve all groups");
        return (GroupDetail[]) identityService.createGroupQuery()
                .orderByGroupId().asc().list().stream()
                .map(g -> new GroupDetail(g.getId(), g.getName(), g.getType(), null))
                .toArray();
    }

    /**
     * DTO to carry account information to and from service.
     * @author zwets
     */
    public static class AccountDetail implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String name;
        private String email;
        private String[] groups;
        public AccountDetail(String id, String name, String email, String[] groups) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.groups = groups;
        }
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getEmail() {
            return email;
        }
        public void setEmail(String email) {
            this.email = email;
        }
        public String[] getGroups() {
            return groups;
        }
        public void setGroups(String[] groups) {
            this.groups = groups;
        }
    }

    /**
     * DTO to carry Group information to and from service.
     * @author zwets
     */
    public static class GroupDetail implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String name;
        private String type;
        private String[] accounts;
        public GroupDetail(String id, String name, String type, String[] accounts) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.accounts = null;
        }
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.name = type;
        }
        public String[] getAccounts() {
            return accounts;
        }
        public void setAccounts(String[] accounts) {
            this.accounts = accounts;
        }
    }
}
