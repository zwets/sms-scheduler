package it.zwets.sms.scheduler.security;

import java.util.Arrays;

import org.flowable.idm.api.Group;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * IAM - identity and access management service.
 * 
 * This service manages accounts, groups, roles and authorities.  It is
 * built on top of the Flowable IDM component, which ties in with Spring
 * Security.
 * 
 * <h1>Terminology</h1>
 * 
 * <ul>
 * <li>Flowable IDM has accounts, groups, and privileges.  An account can
 *   be in any number of groups.  Priviliges can be attached to accounts
 *   and to groups.  Privileges translate into granted authorities.</li>
 * <li>Authorities bridge to Spring uses; they sit on the Authentication
 *   context when there is a logged-in user, and can be used in the filter
 *   chain for web requests.  Spring roles are just authorities with prefix
 *   "ROLE_", and can be conveniently tested with <code>hasRole()</code>.
 *   I.e. <code>hasRole("Q")</code> == <code>hasAuthority("ROLE_Q")</code>.
 * <li>In our SMS Service we similarly have two aspects for authorisation:
 *   user accounts have roles (USER, ADMIN) and "authorities" to act on
 *   behalf of specific tenants/clients (TEST, CONSORT, MINT).</li>
 * <li>We could go deeper (e.g. account A is authorized to schedule SMS
 *   for client C whereas B is authorized to see delivery status only),
 *   but in the interest of simplicity we stop at "unary predicates".</li>
 * </ul>
 * 
 * <h1>Design Decisions</h1>
 * 
 * <ul>
 * <li>We use no nested groups or role hierarchy, just a flat model with
 *   accounts that can be in zero or more groups.</li>
 * <li>Each group corresponds to exactly one authority (through the single
 *   unique privilege it confers).</li>
 * <li>We thus have a <code>1:N</code> relation of <code>account</code> to
 *   <code>group = privilege = authority</code>, and each account must be
 *   explicitly added to each group whose authorities it needs.</li>
 * <li>Authorities come in two flavours: ROLE and CLIENT.  All this means
 *   is that (in the absence of role hierarchy) <code>hasRole("X")</code>
 *   can be used as shorthand for <code>hasAuthority("ROLE_X")</code>.</li>
 * </ul>
 * 
 * <h1>Out of the box setup</h1>
 * 
 * <ul>
 * <li>Two ROLE authorities: ROLE_USER and ROLE_ADMIN, conferred by groups
 *   "users" and "admins" respectively.</li>
 * <li>One CLIENT authority: CLIENT_TEST, conferred by group "test".</li>
 * <li>One account "admin" with password "test" which is a member of both
 *   roles and the test client.</li>
 * </ul>
 * 
 * The out-of-box admin account is there so you can create an admin account
 * for yourself.  Remember to <b>remove it</b> right after that.
 * 
 * <h1>Implementation Details</h1>
 * 
 * We use the <code>Group.groupType</code> field to store the authority
 * flavour ("ROLE", "CLIENT"), which also doubles a the authority prefix.
 * So, for a new group/authority, we collect the group ID (e.g. "admins"),
 * flavour ("ROLE"), and authority name ("ADMIN").
 * 
 * We then create a <code>Group</code> with <code>id = "admins"</code> and
 * <code>type = "ROLE"</code>, and map it to a privilege with 
 * <code>name = group.type + "_" + "ADMIN"</code>.
 */

@Component
public class IamService {
    
    private static final Logger LOG = LoggerFactory.getLogger(IamService.class);
    
    public static final String USERS_GROUP = "users";
    public static final String ADMINS_GROUP = "admins";
    public static final String TEST_GROUP = "test";
    
    public static final String INITIAL_ADMIN = "admin";
    public static final String INITIAL_PASSWORD = "test";

    public enum Flavour { ROLE, CLIENT };
    
    @Autowired
    private IdmIdentityService identityService;
   
    public IamService(IdmIdentityService identityService) {
        this.identityService = identityService;
        
        if (identityService.createUserQuery().count() == 0) {
            LOG.debug("no accounts found in database, doing out-of-box setup");
            
            LOG.info("create the out-of-box accounts and groups");
            
            createRole(USERS_GROUP, "USER");
            createRole(ADMINS_GROUP, "ADMIN");
            createClient(TEST_GROUP, "TEST");

            LOG.info("create the out-of-box admin account {} with password {}", INITIAL_ADMIN, INITIAL_PASSWORD);
            
            createAccount(new AccountDetail(
                    INITIAL_ADMIN,
                    "Default Admin User",
                    "nobody@example.com",
                    new String[]{ USERS_GROUP, ADMINS_GROUP, TEST_GROUP }),
                    INITIAL_PASSWORD);
            
            LOG.warn("**IMPORTANT** remove the out-of-box {} account once you have"
                    + " used it set up an admin account for yourself", INITIAL_ADMIN);
        }
        else {
            LOG.debug("user accounts already present, skipping out-of-box account setup");
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
       
            LOG.debug("retrieve groups for account: {}", id);
            String groups[] = identityService.createGroupQuery()
                .groupMember(id).list().stream()
                .map(g -> g.getId())
                .toArray(String[]::new);
            
            account = new AccountDetail(user.getId(), user.getDisplayName(), user.getEmail(), groups);
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
        
        return identityService.createUserQuery()
                .orderByUserId().asc().list().stream()
                .map(u -> new AccountDetail(u.getId(), u.getDisplayName(), u.getEmail(), null))
                .toArray(AccountDetail[]::new);
    }

    /**
     * Retrieve all Accounts in Group
     * Note the account.groups fields will be set to null.
     * @return array of accounts, with the groups null-ed out
     */
    public AccountDetail[] getAccountsInGroup(String groupId) {
        LOG.debug("retrieve accounts in group: {}", groupId);
        
        return identityService.createUserQuery()
                .memberOfGroup(groupId)
                .orderByUserId().asc().list().stream()
                .map(u -> new AccountDetail(u.getId(), u.getDisplayName(), u.getEmail(), null))
                .toArray(AccountDetail[]::new);
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
     * Check password for acount id
     * @param id
     * @param password
     * @return true iff the password is correct
     */
    public boolean checkPassword(String id, String password) {
        LOG.debug("check password for account: {}", id);
        return identityService.checkPassword(id, password);
    }
    
    /**
     * Change password for id
     * @param accountId
     * @param password
     */
    public void updatePassword(String id, String password) {
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
     * @param roleName
     * @return the group details with the account list in it
     */
    public GroupDetail createRole(String groupId, String roleName) {
        LOG.info("creating role group: {} for role {}", groupId, roleName);
        return createGroup(groupId, Flavour.ROLE, roleName);
    }
    
    /**
     * Create a client group. No effect if already exists.
     * @param groupId
     * @param clientName
     * @return the group details with the account list in it
     */
    public GroupDetail createClient(String groupId, String clientName) {
        LOG.info("creating client group: {} for client {}", groupId, clientName);
        return createGroup(groupId, Flavour.CLIENT, clientName);        
    }
    
    /**
     * Create a new group or return existing group.
     * @param groupId name of the group
     * @param flavour the authority flavour (role, client)
     * @param authName the authority name (e.g. for group "users" use "USER")
     * @return the GroupDetail, with group list
     */
    protected GroupDetail createGroup(String groupId, Flavour flavour, String authName) {
        LOG.debug("creating group {} with authority {}_{}", groupId, flavour, authName);        

        GroupDetail group = getGroup(groupId, null);
        
        if (group == null) {
            
            Group flwGroup = identityService.newGroup(groupId);
            flwGroup.setId(groupId);
            flwGroup.setName(authName);
            flwGroup.setType(flavour.toString());
            identityService.saveGroup(flwGroup);
            
            group = new GroupDetail(groupId, flavour.toString(), authName, new String[0]);
            
	        String privName = "%s_%s".formatted(flavour, authName);
	        Privilege priv = identityService.createPrivilege(privName);
	        
	        LOG.debug("assigning privilege {} to group {}", privName);        
	        identityService.addGroupPrivilegeMapping(priv.getId(), groupId);
        }
        else if (!(group.type.equals(flavour.toString()))) {
            throw new RuntimeException("cannot create %s group %s: %s group with that ID exists"
                    .formatted(flavour, groupId, group.type));
        }
        
        return group;
    }

    /**
     * Delete the group with groupId.  No-op if group does not exist.
     * @param groupId
     */
    public void deleteGroup(String groupId) {
        LOG.debug("delete group {}", groupId);
        identityService.deleteGroup(groupId);
    }
    
    /**
     * Retrieve details of role as well as its account id list, or null if not found
     * @param id
     * @return the group details
     */
    public GroupDetail getRole(String id) {
        LOG.debug("retrieve role details for id: {}", id);
        return getGroup(id, Flavour.ROLE);
    }

    /**
     * Retrieve details of client group, with list of member accounts, or null if not found.
     * @param id
     * @return the group details
     */
    public GroupDetail getClient(String id) {
        LOG.debug("retrieve client details for id: {}", id);
        return getGroup(id, Flavour.CLIENT);
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
     * @param flavour the group type to filter on or null to return any type
     * @return the group details
     */
    protected GroupDetail getGroup(String id, Flavour flavour) {
        LOG.debug("retrieve {} group: {}", flavour == null ? "any" : flavour, id);
       
        GroupDetail group = null;
        
        Group flwGroup = identityService.createGroupQuery().groupId(id).singleResult();
        if (flwGroup != null && (flavour == null || flavour.toString().equals(flwGroup.getType()))) {
            
            LOG.debug("retrieve accounts for group: {}", id);
            String accounts[] = identityService.createUserQuery()
                .memberOfGroup(id).list().stream()
                .map(u -> u.getId())
                .toArray(String[]::new);
            
            group = new GroupDetail(flwGroup.getId(), flwGroup.getType(), flwGroup.getName(), accounts);
        }
        
        return group;    
    }

    /**
     * Retrieve all role groups (without their members)
     * @return the list of all role groups, with null-ed out accounts list
     */
    public GroupDetail[] getRoles() {
        LOG.debug("retrieve all roles");
        return identityService.createGroupQuery()
                .groupType(Flavour.ROLE.toString())
                .orderByGroupId().asc().list().stream()
                .map(g -> new GroupDetail(g.getId(), g.getType(), g.getName(), null))
                .toArray(GroupDetail[]::new);
    }

    /**
     * Retrieve all client groups (without their members)
     * @return the list of all client groups, with null-ed out accounts list
     */
    public GroupDetail[] getClients() {
        LOG.debug("retrieve all clients");
        return identityService.createGroupQuery()
                .groupType(Flavour.CLIENT.toString())
                .orderByGroupId().asc().list().stream()
                .map(g -> new GroupDetail(g.getId(), g.getType(), g.getName(), null))
                .toArray(GroupDetail[]::new);
    }

    /**
     * Retrieve all groups (role groups and client groups)
     * @return the list of all groups, but with null-ed out accounts list
     */
    public GroupDetail[] getGroups() {
        LOG.debug("retrieve all groups");
        return identityService.createGroupQuery()
                .orderByGroupId().asc().list().stream()
                .map(g -> new GroupDetail(g.getId(), g.getType(), g.getName(), null))
                .toArray(GroupDetail[]::new);
    }

    /** DTO to carry account information to and from service. */
    public final record AccountDetail(
        String id,
        String name,
        String email,
        String[] groups) {
    }

    /** DTO to carry group information to and from service. */
    public final record GroupDetail(
        String id,
        String type,
        String name,
        String[] acounts) {
    }
}
