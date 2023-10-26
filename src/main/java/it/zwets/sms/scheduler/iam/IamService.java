package it.zwets.sms.scheduler.iam;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
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
 *   be in any number of groups.  Privileges can be attached to accounts
 *   and to groups.  Privileges translate into granted authorities.</li>
 * <li>Authorities bridge to Spring Boot; they sit on the Authentication
 *   context when there is a logged-in user, and can be used in the filter
 *   chain for web requests.  Spring roles are just authorities with prefix
 *   "ROLE_", and can be conveniently tested with <code>hasRole()</code>.
 *   So, <code>hasRole("Q") == hasAuthority("ROLE_Q")</code>.
 * <li>In our SMS Service we similarly have two aspects for authorisation:
 *   user accounts have roles (user, admin) and "authorities" to act on
 *   behalf of clients (test, consort, mint).</li>
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
 * <li>Each group corresponds to exactly one authority, through the single
 *   unique privilege it confers.</li>
 * <li>We thus have a <code>1:N</code> relation of <code>account</code> to
 *   <code>group = privilege = authority</code>, and each account must be
 *   explicitly added to each group whose authorities it needs.</li>
 * <li>For each group "gid" we create, we create a privilege "ROLE_gid" and
 *   map the group (uniquely) on this.</li>
 * <li>We distinguish (for now) two flavours of groups, which we administer
 *   in the <code>groupType</code> of the group:
 *   <ul><li>The "role" flavour has two fixed built-in groups: <b>users</b>
 *     and <b>admins</b>. (There is no point in allowing dynamic additions,
 *     as these roles are "hard-baked" in the permission checks in the app.)
 *     <li>The "client" flavour is dynamic: a new group is added (by a user
 *     with admin permission) for each new client (tenant).</li>
 *   </ul>
 *   Effectively, the "role" flavour is for function authorisation, and the
 *   "client" flavour is for data authorisation.
 * </ul>
 * 
 * <h1>Out of the box setup</h1>
 * 
 * <ul>
 * <li>One account "admin" with password "test". This account is a member of
 *   the "admins" group.</li>
 * <li>Two role groups: "users" and "admins". Any account that is not in the
 *   "users" group has no access to the application. Only accounts in the
 *   "admins" group can create new accounts and clients.</li>
 * <li>One client group: <b>test</b>, whose members have authority to act
 *   op behalf of the "test" client..</li>
 * </ul>
 * 
 * The out-of-box admin account is there so you can create an admin account
 * for yourself.  See the <code>account-create</code> script in this repo.
 * 
 * <h1>Implementation Details</h1>
 * 
 * When a group "gid" is created, we create a privilege "ROLE_gid" and map
 * the group on the privilege. We set the group flavour on its groupType.
 * Groups of both flavours live in a single "namespace", i.e. you cannot
 * have a role group by the same name as a client group.
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
            LOG.debug("no accounts found in database, create out-of-box accounts and groups");

            createGroup(Flavour.ROLE, USERS_GROUP);
            createGroup(Flavour.ROLE, ADMINS_GROUP);
            createGroup(Flavour.CLIENT, TEST_GROUP);

            LOG.info("create out-of-box admin account {} with password {}", INITIAL_ADMIN, INITIAL_PASSWORD);

            createAccount(new AccountDetail(
                    INITIAL_ADMIN,
                    "Default Admin User",
                    "nobody@example.com",
                    INITIAL_PASSWORD,
                    new String[]{ USERS_GROUP, ADMINS_GROUP, TEST_GROUP }));

            LOG.warn("**IMPORTANT** remove the out-of-box {} account once you have"
                    + " used it set up an admin account for yourself", INITIAL_ADMIN);
        }
        else {
            LOG.debug("user accounts already present, skipping out-of-box account setup");
        }
    }

    /**
     * Create an account
     * @param details the id, name, email, password and optional groups to add the account to
     * @return the created account
     * @throws IllegalStateException if the account existed
     */
    public AccountDetail createAccount(final AccountDetail detail) {
        LOG.trace("create account: {}", detail.id);

        String[] groups = detail.groups == null ? new String[0] : detail.groups;
        
        if (StringUtils.isBlank(detail.id)) {
            throw new IllegalArgumentException("Account ID must not be blank");
        }
        
        if (isAccount(detail.id)) {
            throw new IllegalStateException("Account exists: %s".formatted(detail.id));
        }

        Optional<String> nonGroup = Arrays.stream(groups).filter(gid -> !isGroup(gid)).findAny();
        if (nonGroup.isPresent()) {
            throw new IllegalArgumentException("No such group: %s".formatted(nonGroup.get()));
        }
    
        LOG.info("creating account: {}", detail.id);
        
        User user = identityService.newUser(detail.id);
        user.setDisplayName(detail.name);
        user.setEmail(detail.email);
        user.setPassword(detail.password);
        identityService.saveUser(user);

        for (String gid : groups) {
            addAccountToGroup(detail.id, gid);
        }
            
        return getAccount(detail.id);
    }

    /**
     * Return true iff account id exists.
     * @param id
     * @return
     */
    public boolean isAccount(String id) {
        boolean result = identityService.createUserQuery().userId(id).count() != 0;
        LOG.trace("isAccount({}) -> {}", id, result);
        return result;
    }
    
    /**
     * Return account details for id, or null if not found.
     * @param id the account Id to look up
     * @return the account details (except password)
     */
    public AccountDetail getAccount(String id) {
        LOG.trace("get account: {}", id);

        AccountDetail account = null;

        User u = identityService.createUserQuery().userId(id).singleResult();
        if (u != null) {
            account = new AccountDetail(u.getId(), u.getDisplayName(), u.getEmail(), null, groupList(u.getId()));
        }

        return account;
    }

    /**
     * Update account with the non-null fields from update
     * @param update
     * @return the update account
     */
    public AccountDetail updateAccount(final AccountDetail update) {
        LOG.trace("update account: {}", update.id);

        String id = update.id;
        User user = identityService.createUserQuery().userId(id).singleResult();
        
        if (user == null) {
            throw new IllegalArgumentException("No such account: %s".formatted(update.id));
        }
        
        LOG.info("updating account: {}", id);

            // Regular fields
        
        if (update.name != null) {
            user.setDisplayName(update.name);
        }
        if (update.email != null) {
            user.setEmail(update.email);
        }
        if (update.name != null || update.email != null) {
            identityService.saveUser(user);
        }

            // Password needs specific call
        
        if (update.password != null) {
            updatePassword(id, update.password);
        }
        
            // Group list: update with the difference
        
        if (update.groups != null) {
            
            List<String> oldGroups = Arrays.asList(groupList(id));
            List<String> newGroups = Arrays.asList(update.groups);
            
            for (String g : oldGroups) {
                if (!newGroups.contains(g)) {
                    removeAccountFromGroup(update.id, g);
                }
            }
            for (String g : newGroups) {
                if (!oldGroups.contains(g)) {
                    addAccountToGroup(id, g);
                }
            }
        }
            
        return getAccount(id);
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
     * Retrieve all Accounts.
     * Note the account.groups fields will be set to null.
     * @return array of accounts, with the groups null-ed out.
     */
    public AccountDetail[] getAccounts() {
        LOG.trace("retrieve all accounts");

        return identityService.createUserQuery()
                .orderByUserId().asc().list().stream()
                .map(u -> new AccountDetail(u.getId(), u.getDisplayName(), u.getEmail(), null, groupList(u.getId())))
                .toArray(AccountDetail[]::new);
    }

    /**
     * Retrieve all Accounts in Group
     * Note the account.groups fields will be set to null.
     * @return array of accounts, with the groups null-ed out
     */
    public AccountDetail[] getAccountsInGroup(String groupId) {
        LOG.trace("retrieve accounts in group: {}", groupId);

        return identityService.createUserQuery()
                .memberOfGroup(groupId)
                .orderByUserId().asc().list().stream()
                .map(u -> new AccountDetail(u.getId(), u.getDisplayName(), u.getEmail(), null, groupList(u.getId())))
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
        LOG.trace("isAccountInGroup({},{}) -> {}", accountId, groupId, result);
        return result;
    }

    /**
     * Retrieve an Account in Group, or null if it is not a member
     * Note the account.groups fields will be set to null.
     * @param groupId
     * @param accountId
     * @return array of accounts, with the groups null-ed out
     */
    public AccountDetail getAccountInGroup(String groupId, String accountId) {
        LOG.trace("retrieve account in group {}: {}", groupId, accountId);

        AccountDetail account = null;

        User u = identityService.createUserQuery().memberOfGroup(groupId).userId(accountId).singleResult();
        if (u != null) {
            account = new AccountDetail(u.getId(), u.getDisplayName(), u.getEmail(), null, groupList(u.getId()));
        }

        return account;
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
        LOG.trace("check password for account: {}", id);
        return identityService.checkPassword(id, password);
    }

    /**
     * Change password for id
     * @param accountId
     * @param password
     */
    public void updatePassword(String id, String password) {
        LOG.trace("update password for account: {}", id);
        User user = identityService.createUserQuery().userId(id).singleResult();
        if (user != null) {
            user.setPassword(password);
            identityService.updateUserPassword(user);
            LOG.info("password updated for account: {}", id);
        }
        else {
            LOG.warn("account does not exist: {}", id);
        }
    }
    
    /**
     * Create a new group
     * @param flavour the authority flavour (role, client)
     * @param groupId name of the group
     * @return the GroupDetail, with (obviously empty) member list
     * @throws IllegalArgumentException if group exists
     */
    public GroupDetail createGroup(Flavour flavour, String groupId) {
        LOG.trace("create {} group {}", flavour, groupId);

        if (StringUtils.isBlank(groupId)) {
            throw new IllegalArgumentException("Group ID must not be blank");
        }
        
        if (isGroup(groupId)) {
            throw new IllegalArgumentException("Group already exists: %s".formatted(groupId));
        }

        LOG.info("creating {} group {}", flavour, groupId);
        
        Group flwGroup = identityService.newGroup(groupId);
        flwGroup.setId(groupId);
        flwGroup.setType(flavour.toString());
        identityService.saveGroup(flwGroup);

        String roleName = "ROLE_%s".formatted(groupId);
        Privilege priv = identityService.createPrivilege(roleName);

        LOG.debug("assigning privilege {} to group {}", roleName, groupId);
        identityService.addGroupPrivilegeMapping(priv.getId(), groupId);

        return getGroup(flavour, groupId);
    }

    /**
     * Check for existing group
     * @param id
     * @return true iff group id exists
     */
    public boolean isGroup(String id) {
        boolean result = identityService.createGroupQuery().groupId(id).count() != 0;
        LOG.trace("isGroup({}) -> {}", id, result);
        return result;
    }
    
    /**
     * Check for existing flavoured group
     * @param flavour
     * @param id
     * @return true iff group of flavour id exists
     */
    public boolean isGroup(Flavour flavour, String id) {
        boolean result = identityService.createGroupQuery().groupId(id).groupType(flavour.toString()).count() != 0;
        LOG.trace("isGroup({},{}) -> {}", flavour, id, result);
        return result;
    }
    
    /**
     * Retrieve details of group
     * @param id
     * @return the group details and member list 
     */
    public GroupDetail getGroup(String id) {
        LOG.trace("retrieve group: {}", id);
        
        GroupDetail group = null;
        
        Group flwGroup = identityService.createGroupQuery().groupId(id).singleResult();
        if (flwGroup != null) {
            group = new GroupDetail(flwGroup.getId(), flwGroup.getType(), memberList(id));
        }
        
        return group;
    }

    /**
     * Retrieve group with id and optional flavour
     * @param id the group Id
     * @param flavour the group type to filter on
     * @return the group details
     */
    public GroupDetail getGroup(Flavour flavour, String id) {
        LOG.trace("retrieve {} group: {}", flavour == null ? "any" : flavour, id);
        GroupDetail group = getGroup(id);
        return group != null && flavour != null && group.type.equals(flavour.toString()) ? group : null;
    }

    /**
     * Retrieve all groups
     * @return the list of all groups
     */
    public GroupDetail[] getGroups() {
        LOG.debug("retrieve all groups");
        return identityService.createGroupQuery()
                .orderByGroupId().asc().list().stream()
                .map(g -> new GroupDetail(g.getId(), g.getType(), memberList(g.getId())))
                .toArray(GroupDetail[]::new);
    }

    /**
     * Retrieve the all groups of the specified flavour
     * @param flavour
     * @return the list
     */
    public GroupDetail[] getGroups(Flavour flavour) {
        LOG.debug("retrieve all groups with flavour {}", flavour);
        return identityService.createGroupQuery()
                .groupType(flavour.toString())
                .orderByGroupId().asc().list().stream()
                .map(g -> new GroupDetail(g.getId(), g.getType(), memberList(g.getId())))
                .toArray(GroupDetail[]::new);
    }

    /**
     * Delete the group with id.  No-op if group does not exist.
     * @param id
     */
    public void deleteGroup(String id) {
        LOG.debug("delete group {}", id);
        
        Group group = identityService.createGroupQuery().groupId(id).singleResult();
        if (group != null) {
            
            String roleName = "ROLE_%s".formatted(id);
            Privilege priv = identityService.createPrivilegeQuery().privilegeName(roleName).singleResult();
            if (priv != null) {
                identityService.deletePrivilege(priv.getId());
            }
                    
            identityService.deleteGroup(id);
        }
    }

    /* Helper to return the group list for an account. */
    private String[] groupList(String accountId) {
        LOG.trace("retrieve groups for account: {}", accountId);
        return identityService.createGroupQuery()
            .groupMember(accountId).list().stream()
            .map(g -> g.getId())
            .toArray(String[]::new);
    }
 
    /* Helper to return the account list for a group. */
    private String[] memberList(String groupId) {
        LOG.trace("retrieve accounts for group: {}", groupId);
        return identityService.createUserQuery()
            .memberOfGroup(groupId).list().stream()
            .map(u -> u.getId())
            .toArray(String[]::new);
    }

    /** DTO to carry account information to and from service. */
    public final record AccountDetail(
        String id,
        String name,
        String email,
        String password,
        String[] groups) {
    }

    /** DTO to carry group information to and from service. */
    public final record GroupDetail(
        String id,
        String type,
        String[] acounts) {
    }
}
