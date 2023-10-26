package it.zwets.sms.scheduler.rest;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import it.zwets.sms.scheduler.iam.IamService;
import it.zwets.sms.scheduler.iam.IamService.Flavour;

/**
 * REST controller for the /iam endpoint.
 * 
 * @author zwets
 */
@RestController
@RequestMapping(value = "/iam")
@EnableMethodSecurity
public class IamRestController {

    private static final Logger LOG = LoggerFactory.getLogger(IamRestController.class);

    private IamService iamService;

    IamRestController(IamService IamService) {
        this.iamService = IamService;
    }
 
    /**
     * Put some helpful plaintext information on the root of the /iam context. 
     */
    @GetMapping(path = { "", "/", "/check" }, produces = MediaType.TEXT_PLAIN_VALUE)
    public String getCheck() {
        return getAccountHelp();
    }
    
        // Accounts
    
    @GetMapping(path = "accounts", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins')")
    public IamService.AccountDetail[] getAccounts() {
        LOG.trace("REST GET accounts");
        return iamService.getAccounts();
    }

    private final record AccountDetail(
        String id, String name, String email, String password, String[] groups) { }

    @GetMapping(path = "accounts/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins') || authentication.name == #id")
    public IamService.AccountDetail getAccount(@PathVariable String id) {
        LOG.trace("REST GET accounts/{}", id);
        IamService.AccountDetail account = iamService.getAccount(id);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + id);
        }
        return account;
    }

    @PostMapping(path = "accounts", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins')")
    public IamService.AccountDetail postAccount(@RequestBody AccountDetail a) {
        LOG.trace("REST POST accounts");

        if (StringUtils.isBlank(a.id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account must not be blank");
        }
        else if (iamService.isAccount(a.id)) {
            LOG.warn("REST POST accounts \"a.id\": account already exists.", a.id);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account exists: %s".formatted(a.id));
        }

        LOG.debug("REST POST accounts \"{}\": creating account", a.id);
        return iamService.createAccount(new IamService.AccountDetail(a.id, a.name, a.email, a.password, checkedGroups(a.groups)));
    }

    @PutMapping(path = "accounts/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins') || authentication.name == #id")
    public IamService.AccountDetail putAccount(@PathVariable String id, @RequestBody AccountDetail a) {
        LOG.trace("REST PUT accounts/{}", id);
        
        if (StringUtils.isBlank(id) || !iamService.isAccount(id)) {
            LOG.warn("REST PUT accounts/{}: non-existing account", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such account: " + id);
        } 
        else if (StringUtils.isNotBlank(a.id) && !id.equals(a.id)) {
            LOG.warn("REST PUT accounts/{}: mismatching account id: {}", a.id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource does not match entity: %s".formatted(a.id));
        }

        LOG.debug("REST PUT UPDATE accounts/{}", id);
        return iamService.updateAccount(new IamService.AccountDetail(id,
                StringUtils.stripToNull(a.name),
                StringUtils.stripToNull(a.email),
                StringUtils.stripToNull(a.password),
                loginHasRole(IamService.ADMINS_GROUP) ? checkedGroups(a.groups) : null));
    }
    
    @PostMapping(path = "accounts/{id}/password")
    @PreAuthorize("hasRole('admins') || authentication.name == #id")
    public void updatePassword(@PathVariable String id, @RequestBody String password) {
        LOG.trace("REST POST accounts/{}/passwords", id);

        if (StringUtils.isBlank(id) || !iamService.isAccount(id)) {
            LOG.warn("REST POST accounts/{}/password: non-existing account", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such account: " + id);
        }

        LOG.debug("REST POST UPDATE accounts/{}/password", id);
        iamService.updatePassword(id, password);
    }

    @DeleteMapping(path = "accounts/{id}")
    @PreAuthorize("hasRole('admins') && authentication.name != #id")
    public void deleteAccount(@PathVariable String id) {
        LOG.debug("REST DELETE accounts/{}", id);
        iamService.deleteAccount(id);
    }

        // Group Methods

    @GetMapping(path = "groups", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins')")
    public IamService.GroupDetail[] getGroups() {
        LOG.trace("REST GET groups");
        return iamService.getGroups();
    }
    
    @GetMapping(path = "groups/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins')")
    public IamService.AccountDetail[] getGroupAccounts(@PathVariable String id) {
        LOG.trace("REST GET groups/{}", id);
        if (!iamService.isGroup(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such group: %s".formatted(id));
        }
        return iamService.getAccountsInGroup(id);
    }
    
    @PostMapping(path = "groups/{id}")
    @PreAuthorize("hasRole('admins')")
    public void postAccountToGroup(@PathVariable String id, @RequestBody String uid) {
        LOG.trace("REST POST groups/{} \"{}\"", id, uid);
        if (!iamService.isGroup(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such group: %s".formatted(id));
        }
        else if (!iamService.isAccount(uid)) {
            LOG.warn("REST PUT group/{}: account {} does not exist", id, uid);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such account: %s".formatted(uid));
        }
        iamService.addAccountToGroup(uid, id);
    }

    @GetMapping(path = "groups/{gid}/{uid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins') || #uid == authentication.name")
    public IamService.AccountDetail getAccountInGroup(@PathVariable String gid, @PathVariable String uid) {
        LOG.trace("REST GET groups/{}/{}", gid, uid);
        IamService.AccountDetail account = iamService.getAccountInGroup(gid, uid);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such account in group: %s/%s".formatted(gid,uid));
        }
        return account;
    }

    @DeleteMapping(path = "groups/{gid}/{uid}")
    @PreAuthorize("hasRole('admins') && (#gid != 'admins' || #uid != authentication.name)")
    public void deleteAccountFromGroup(@PathVariable String gid, @PathVariable String uid) {
        LOG.trace("REST DELETE groups/{}/{}", gid, uid);
        iamService.removeAccountFromGroup(uid, gid);
    }

        // Clients
    
    @GetMapping(path = "clients", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins')")
    public IamService.GroupDetail[] getClients() {
        LOG.trace("REST GET clients");
        return iamService.getGroups(Flavour.CLIENT);
    }
    
    @GetMapping(path = "clients/{cid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins') || (hasRole('users') && hasRole(#cid))")
    public IamService.GroupDetail getClient(@PathVariable String cid) {
        LOG.trace("REST GET clients/{}", cid);
        IamService.GroupDetail client = iamService.getGroup(Flavour.CLIENT, cid);
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: %s".formatted(cid));
        }
        return client;
    }
    
    @PostMapping(path = "clients")
    @PreAuthorize("hasRole('admins')")
    public void postClient(@RequestBody String cid) {
        LOG.trace("REST POST clients \"{}\"", cid);
       
        if (StringUtils.isBlank(cid)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client ID must not be blank");
        }
        else if (iamService.isGroup(cid)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group exists: %s".formatted(cid));
        }

        LOG.trace("REST POST clients \"{}\"", cid);
        iamService.createGroup(Flavour.CLIENT, cid);
    }

    @DeleteMapping(path = "clients/{cid}")
    @PreAuthorize("hasRole('admins')")
    public void deleteClient(@PathVariable String cid) {
        LOG.trace("REST DELETE clients/{}", cid);
        iamService.deleteGroup(cid);
    }

        // Client Members

    @GetMapping(path = "clients/{cid}/{uid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins') || (hasRole('users') && hasRole(#cid))")
    public IamService.AccountDetail getAccountInClient(@PathVariable String cid, @PathVariable String uid) {
        LOG.trace("REST GET clients/{}/{}", cid, uid);
        if (!iamService.isGroup(Flavour.CLIENT, cid)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such client: %s".formatted(cid));
        }
        return iamService.getAccountInGroup(cid, uid);
    }
    
    @PutMapping(path = "clients/{cid}/{uid}")
    @PreAuthorize("hasRole('admins')")
    public void putAccountInClient(@PathVariable String cid, @PathVariable String uid) {
        LOG.trace("REST PUT clients/{}/{}", cid, uid);
       
        if (!iamService.isGroup(Flavour.CLIENT, cid)) {
            LOG.warn("REST PUT clients/{}/{}: no such client: {}", cid, uid, cid);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such client: %s".formatted(cid));
        }
        else if (!iamService.isAccount(uid)) {
            LOG.warn("REST PUT clients/{}/{}: no such account: {}", cid, uid, cid);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such account: %s".formatted(uid));
        } 
        else if (iamService.isAccountInGroup(uid, cid)) {
            LOG.debug("REST PUT clients/{}/{}: already there", cid, uid);
        }

        LOG.debug("REST PUT clients/{}/{}", cid, uid);
        iamService.addAccountToGroup(uid, cid);
    }
    
    @DeleteMapping(path = "clients/{cid}/{uid}")
    @PreAuthorize("hasRole('admins')")
    public void deleteAccountInClient(@PathVariable String cid, @PathVariable String uid) {
        LOG.trace("REST DELETE clients/{}/{}", cid, uid);
        iamService.removeAccountFromGroup(uid, cid);
    }

        // Helpers

    private static Object[] stripToNull(Object[] list) {
        return list == null || list.length == 0 ? null : list;
    }
    
    private String[] checkedAccounts(String[] ids) {
        if (ids != null && Arrays.stream(ids).anyMatch(g -> !iamService.isAccount(g))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid account ID in list");
        }
        return ids;
    }
   
    private String[] checkedGroups(String[] ids) {
        if (ids != null && Arrays.stream(ids).anyMatch(g -> !iamService.isGroup(g))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "non-existent group in list");
        }
        return ids;
    }

    private boolean loginHasRole(String role) {
        return loginHasAuthority("ROLE_" + role);
    }
    
    private boolean loginHasAuthority(String authority) {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().map(Object::toString)
                .anyMatch(authority::equals);
    }

    /**
     * Returns plain text help for logged on user.
     * @return a string blurb
     */
    private String getAccountHelp() {
        
        Authentication login = SecurityContextHolder.getContext().getAuthentication();
        
        if (!login.isAuthenticated()) {
            LOG.error("Unexpected: an unauthenticated user has made it through the filter chain!");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Go Away!");
        }
        
        String userId = login.getName();
        List<String> auths = login.getAuthorities().stream().map(s -> s.toString()).toList();
        List<String> roles = auths.stream().filter(s -> s.startsWith("ROLE_")).map(s -> s.substring(5)).toList();
        List<String> clients = roles.stream().filter(s -> !("admins8".equals(s) || "users".equals(s))).toList();

        StringBuffer buf = new StringBuffer("Hello there, %s\n".formatted(userId));
        
        if (roles.contains("admins")) {
            
            buf.append("\nAs an admin you have access to:\n"
                    + "  /iam/accounts          : lists all accounts, post new accounts\n"
                    + "  /iam/accounts/{id}     : manage individual accounts (CRUD and set password)\n"
                    + "  /iam/roles             : lists available roles; we have two built-in:\n"
                    + "  /iam/roles/users       : accounts authorised for the application\n"
                    + "  /iam/roles/users/{id}  : use DELETE here to remove account from the role\n"
                    + "  /iam/roles/admins      : accounts authorised to manage accounts\n"
                    + "  /iam/roles/admins/{id} : use DELETE here to take id out of the role\n"
                    + "  /iam/clients           : lists the clients of the service\n"
                    + "  /iam/clients/test      : accounts for the out-of-the-box test client\n");
            
            if (!roles.contains("users")) {
                
                buf.append("\nNOTE: your account is a member of /iam/roles/admins,\n"
                        + "but not of /iam/roles/users.  This is fine, it just\n"
                        + "means that you can manage the application's users\n"
                        + "but not use the application yourself.\n");
            }
        }
        
        buf.append("\nPlease find your account information at: /iam/account/%s\n".formatted(userId));

        if (!roles.contains(IamService.USERS_GROUP)) {
            buf.append("\nYou have a working login but no access to the application.\n" 
                       + "An administrator will need to add you to the 'users' group.\n");
        }
        else {
            buf.append(clients.isEmpty() ? "\nYou are not authorised for any clients" : "\nYou are authorised for client(s):");
            for (String c : clients) buf.append(" " + c);
	        buf.append(".\n");
        }
        
        buf.append("\nBye now.\n");
        return buf.toString();
    }
}

// vim: sts=4:sw=4:et:ai:si
