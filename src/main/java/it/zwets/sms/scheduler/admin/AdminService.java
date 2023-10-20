package it.zwets.sms.scheduler.admin;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Manages user accounts and their membership of clients.
 * 
 * @param userId
 * @param clientId
 */
@Component
public class AdminService {
    
    private static final Logger LOG = LoggerFactory.getLogger(AdminService.class);
    
    public static final String USERS_GROUP_NAME = "users";
    public static final String ADMINS_GROUP_NAME = "admins";
    private static final String NORMAL_GROUP_TYPE = "group";
    private static final String CLIENT_GROUP_TYPE = "client";

    @Autowired
    private IdentityService identityService;
    
    /**
     * Create a regular account.  No action if it already exists.
     * @param userId
     * @param name
     * @param email
     * @param password
     * @return the Account details
     */
    public AccountDetail createAccount(String userId, String name, String email, String password) {
        LOG.info("creating account: {}", userId);        

        AccountDetail account = getAccount(userId);
        
        if (account == null) {
            
            User user = identityService.newUser(userId);
            user.setDisplayName(name);
            user.setEmail(email);
            user.setPassword(password);
            identityService.saveUser(user);
            
            account = new AccountDetail(userId, name, email);
            account.groups = new String[0];
        }
        
        return account;
    }

    /**
     * Create a user account (is an account in the users group)
     *
     */
    public AccountDetail createUserAccount(String userId, String name, String email, String password) {
        LOG.info("creating user account: {}", userId);        

        if (identityService.createGroupQuery().groupId(USERS_GROUP_NAME).singleResult() == null) {
            LOG.info("creating group '{}'", USERS_GROUP_NAME);
            Group group = identityService.newGroup(USERS_GROUP_NAME);
            group.setName("SMS Scheduler Users");
            group.setType(NORMAL_GROUP_TYPE);
            identityService.saveGroup(group);
        }

        AccountDetail account = getAccount(userId);       
        if (account == null) {
            account = createAccount(userId, name, email, password);
        }
        
        Set<String> groups = new HashSet<String>();
        groups.addAll(Arrays.asList(account.groups));

        if (!groups.contains(USERS_GROUP_NAME)) {
            LOG.info("adding account '{}' to group '{}'", userId, USERS_GROUP_NAME);
            identityService.createMembership(userId, USERS_GROUP_NAME);
            groups.add(USERS_GROUP_NAME);
        }
        
        account.groups = (String[]) groups.toArray();
        
        return account;
    }

    /**
     * Create an account for an admin user.
     * The account is added to all client groups.
     * @param userId
     * @param name
     * @param email
     * @param password
     */
    public AccountDetail createAdminAccount(String userId, String name, String email, String password) {
        LOG.info("creating admin account: {}", userId);

        if (identityService.createGroupQuery().groupId(ADMINS_GROUP_NAME).singleResult() == null) {
            LOG.info("creating admins group");
            Group group = identityService.newGroup(ADMINS_GROUP_NAME);
            group.setName("SMS Scheduler Administrators");
            group.setType(NORMAL_GROUP_TYPE);
            identityService.saveGroup(group);
        }

        AccountDetail account = getAccount(userId);
        if (account == null) {
            account = createUserAccount(userId, name, email, password);
        }
        
        Set<String> groups = new HashSet<String>();
        groups.addAll(Arrays.asList(account.groups));
        
        // Add the account to all groups it isn't in yet
        for (Group g : identityService.createGroupQuery().list()) {
            if (!groups.contains(g.getId())) {
                LOG.info("adding admin account {} to group {}", userId, g.getId());
                identityService.createMembership(userId, g.getId());
                groups.add(g.getId());
            }
        }

        account.groups = (String[]) groups.toArray();
        
        return account;
    }

    /**
     * Return details for userId, or null if not found.
     * @param userId
     * @param withClients to fill the 
     * @return
     */
    public AccountDetail getAccount(String userId) {
        LOG.debug("retrieve account: {}", userId);
        
        AccountDetail account = null;
        
        User user = identityService.createUserQuery().userId(userId).singleResult();
        if (user != null) {
       
            account = new AccountDetail(user.getId(), user.getDisplayName(), user.getEmail());
            
            LOG.debug("retrieve groups/clients for account: {}", userId);
            account.groups = (String[]) identityService.createGroupQuery()
                .groupMember(userId).list().stream()
                .map(g -> g.getId())
                .toArray();
        }
        
        return account;
    }

    /**
     * Retrieve all Accounts (without looking up the groups)
     * @return the array of accounts
     */
    public AccountDetail[] getAccounts() {
        LOG.debug("retrieve all accounts");
        
        return (AccountDetail[]) identityService.createUserQuery()
                .orderByUserId().asc().list().stream()
                .map(u -> new AccountDetail(u.getId(), u.getDisplayName(), u.getEmail()))
                .toArray();
    }

    /**
     * Retrieve all Accounts in Group (without looking up their groups)
     * @return the array of accounts
     */
    public AccountDetail[] getAccountsInGroup(String groupId) {
        LOG.debug("retrieve all accounts in group '{}'", groupId);
        
        return (AccountDetail[]) identityService.createUserQuery()
                .memberOfGroup(groupId)
                .orderByUserId().asc().list().stream()
                .map(u -> new AccountDetail(u.getId(), u.getDisplayName(), u.getEmail()))
                .toArray();
    }

    /**
     * Return true iff account userId exists.
     * @param userId
     * @return
     */
    public boolean isAccount(String userId) {
        boolean result = identityService.createUserQuery()
                .userId(userId)
                .singleResult() != null;
        LOG.debug("isAccount({}) -> {}", userId, result);
        return result;
    }
    
    /**
     * Delete account, ignored if it does not exist.
     * @param userId
     */
    public void deleteAccount(String userId) {
        LOG.info("delete account: {}", userId);
        identityService.deleteUser(userId);
    }

    /**
     * Change password for userId
     * @param userId
     * @param password
     */
    public void updateAccountPassword(String userId, String password) {
        LOG.debug("update password for account: {}", userId);
        User user = identityService.createUserQuery().userId(userId).singleResult();
        if (user != null) {
            user.setPassword(password);
            identityService.updateUserPassword(user);
            LOG.debug("password updated for account: {}", userId);
        }
        else {
            LOG.warn("no such user: {}", userId);
        }
    }
    
    /**
     * Create a new client (group), adds all admins to it.
     * @param clientId
     * @param name
     * @return the ClientDetail, with group list
     */
    public ClientDetail createClient(String clientId, String name) {
        LOG.info("creating client (group): {}", clientId);        

        ClientDetail client = getClient(clientId);
        
        if (client == null) {
            
            Group group = identityService.newGroup(clientId);
            group.setId(clientId);
            group.setName(name);
            group.setType(CLIENT_GROUP_TYPE);
            identityService.saveGroup(group);
            
            client = new ClientDetail(clientId, name);
            
            // add all admins to this client/group
            client.accounts = (String[]) identityService.createUserQuery()
                    .memberOfGroup(ADMINS_GROUP_NAME).list().stream()
                    .map(u -> u.getId())
                    .toArray();
            for (String adminId : client.accounts) {
                LOG.debug("adding admin {} to client {}", adminId, clientId);
                identityService.createMembership(adminId, clientId);
            }
        }
        
        return client;
    }

    /**
     * Retrieve client details for clientID, optionally with accounts in it.
     * @param groupId
     * @param withAccounts
     * @return The ClientDetail
     */
    public ClientDetail getClient(String clientId) {
        LOG.debug("retrieve client: {}", clientId);
        
        ClientDetail client = null;
        
        Group group = identityService.createGroupQuery().groupId(clientId).groupType(CLIENT_GROUP_TYPE).singleResult();
        if (group != null) {
        
            client = new ClientDetail(group.getId(), group.getName());

            LOG.debug("retrieve accounts for client: {}", clientId);
            client.accounts = (String[]) identityService.createUserQuery()
                .memberOfGroup(clientId).list().stream()
                .map(u -> u.getId())
                .toArray();
        }
        
        return client;    
    }

    /**
     * Retrieve group details for groupId, with member accounts.
     * @param groupId
     * @return The ClientDetail
     */
    public ClientDetail getGroup(String groupId) {
        LOG.debug("retrieve group: {}", groupId);
        
        ClientDetail detail = null;
        
        Group group = identityService.createGroupQuery().groupId(groupId).groupType(NORMAL_GROUP_TYPE).singleResult();
        if (group != null) {
        
            detail = new ClientDetail(group.getId(), group.getName());

            LOG.debug("retrieve accounts for group: {}", groupId);
            detail.accounts = (String[]) identityService.createUserQuery()
                .memberOfGroup(groupId).list().stream()
                .map(u -> u.getId())
                .toArray();
        }
        
        return detail;    
    }

    /**
     * Retrieve all Clients (not the groups)
     * @return the list of all clients
     */
    public ClientDetail[] getClients() {
        LOG.debug("retrieve all clients");
        
        return (ClientDetail[]) identityService.createGroupQuery()
                .groupType(CLIENT_GROUP_TYPE)
                .orderByGroupId().asc().list().stream()
                .map(g -> new ClientDetail(g.getId(), g.getName()))
                .toArray();
    }

    /**
     * Return true iff clientId is a registered client.
     * @param clientId
     * @return
     */
    public boolean isClient(String clientId) {
        boolean result = identityService.createGroupQuery()
                .groupId(clientId)
                .groupType(CLIENT_GROUP_TYPE)
                .singleResult() != null;
        LOG.debug("isClient({}) -> {}", clientId, result);
        return result;
    }
    
    /**
     * Retrieve all Groups (including the clients)
     * @return the list of all groups
     */
    public ClientDetail[] getGroups() {
        LOG.debug("retrieve all groups");
        
        return (ClientDetail[]) identityService.createGroupQuery()
                .groupType(NORMAL_GROUP_TYPE)
                .orderByGroupId().asc().list().stream()
                .map(g -> new ClientDetail(g.getId(), g.getName()))
                .toArray();
    }

    /**
     * Return true iff userId is a member of the admins group.
     * @param userId
     * @return
     */
    public boolean isAdmin(String userId) {
        return isMemberOf(userId, ADMINS_GROUP_NAME);
    }

    /**
     * Return true iff userId is a member of groupId.
     * @p6aram userId
     * @param groupId
     * @return
     */
    public boolean isMemberOf(String userId, String groupId) {
        boolean member = identityService.createGroupQuery()
                .groupId(groupId)
                .groupMember(userId)
                .singleResult() != null;
        LOG.debug("isMemberOf({},{}) -> {}", userId, groupId, member);
        return member;
    }

    public void addMember(String userId, String groupId) {
        LOG.info("adding user {} to group {}", userId, groupId);
        if (!isMemberOf(userId, groupId)) {
            identityService.createMembership(userId, groupId);
        }
    }
    
    public void removeMember(String userId, String groupId) {
        LOG.info("removing user {} from group {}", userId, groupId);
        identityService.deleteMembership(userId, groupId);
    }
    
    public static class AccountDetail implements Serializable {
        private static final long serialVersionUID = 1L;
        private String userId;
        private String name;
        private String email;
        private String[] groups;
        public AccountDetail(String userId, String name, String email) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.groups = null;
        }
        public String getUserId() {
            return userId;
        }
        public void setUserId(String userId) {
            this.userId = userId;
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
    
    public static class ClientDetail implements Serializable {
        private static final long serialVersionUID = 1L;
        private String clientId;
        private String name;
        private String[] accounts;
        public ClientDetail(String clientId, String name) {
            this.clientId = clientId;
            this.name = name;
            this.accounts = null;
        }
        public String getClientId() {
            return clientId;
        }
        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String[] getAccounts() {
            return accounts;
        }
        public void setAccounts(String[] accounts) {
            this.accounts = accounts;
        }
    }
}
