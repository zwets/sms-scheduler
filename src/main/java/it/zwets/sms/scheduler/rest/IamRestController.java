package it.zwets.sms.scheduler.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static java.util.function.Predicate.not;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: %s".formatted(id));
        }
        return account;
    }

    @PostMapping(path = "accounts", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins')")
    public IamService.AccountDetail postAccount(@RequestBody AccountDetail a) {
        LOG.trace("REST POST accounts");

        if (StringUtils.isBlank(a.id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account ID must not be blank");
        }
        else if (iamService.isAccount(a.id)) {
            LOG.warn("REST POST accounts: account exists: {}", a.id);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account exists: %s".formatted(a.id));
        }

        LOG.debug("REST POST accounts: creating account: {}", a.id);
        return iamService.createAccount(new IamService.AccountDetail(a.id, a.name, a.email, a.password, checkedGroups(a.groups)));
    }

    @PutMapping(path = "accounts/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins') || authentication.name == #id")
    public IamService.AccountDetail putAccount(@PathVariable String id, @RequestBody AccountDetail a) {
        LOG.trace("REST PUT accounts/{}", id);
        
        if (StringUtils.isBlank(id) || !iamService.isAccount(id)) {
            LOG.warn("REST PUT accounts/{}: account not found", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: %s".formatted(id));
        } 
        else if (StringUtils.isNotBlank(a.id) && !id.equals(a.id)) {
            LOG.warn("REST PUT accounts/{}: mismatching account id: {}", a.id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource does not match entity: %s".formatted(a.id));
        }

        String[] redactedGroups = null;
        if (a.groups != null && loginHasRole(IamService.ADMINS_GROUP)) {
            if (loginIsUser(id) && Arrays.stream(a.groups).noneMatch(IamService.ADMINS_GROUP::equals)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins cannot remove themselves from admins group");
            }
            LOG.debug("REST PUT accounts/{}: check new group memberships", id);
            redactedGroups = checkedGroups(a.groups);
        }
        
        LOG.debug("REST PUT accounts/{}: updating account", id);
        return iamService.updateAccount(new IamService.AccountDetail(id,
                StringUtils.stripToNull(a.name),
                StringUtils.stripToNull(a.email),
                StringUtils.stripToNull(a.password),
                redactedGroups));
    }
    
    @PostMapping(path = "accounts/{id}/password")
    @PreAuthorize("hasRole('admins') || authentication.name == #id")
    public void postPassword(@PathVariable String id, @RequestBody String password) {
        LOG.trace("REST POST accounts/{}/passwords", id);

        if (StringUtils.isBlank(id) || !iamService.isAccount(id)) {
            LOG.warn("REST POST accounts/{}/password: account not found", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: %s".formatted(id));
        }
        
        LOG.debug("REST POST accounts/{}/password: updating password", id);
        iamService.updatePassword(id, StringUtils.strip(password));
    }

    @DeleteMapping(path = "accounts/{id}")
    @PreAuthorize("hasRole('admins') && authentication.name != #id")
    public void deleteAccount(@PathVariable String id) {
        LOG.debug("REST DELETE accounts/{}: deleting account", id);
        iamService.deleteAccount(id);
    }

        // Roles

    @GetMapping(path = "roles", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins')")
    public IamService.GroupDetail[] getRoles() {
        LOG.trace("REST GET roles");
        return iamService.getGroups(Flavour.ROLE);
    }
   
    @GetMapping(path = "roles/{gid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins')")
    public IamService.GroupDetail getRole(@PathVariable String gid) {
        LOG.trace("REST GET roles/{}", gid);
        if (!iamService.isGroup(Flavour.ROLE, gid)) {
            LOG.warn("REST GET roles/{}: group not found", gid);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found: %s".formatted(gid));
        }
        return iamService.getGroup(Flavour.ROLE, gid);
    }

        // Role membership

    @GetMapping(path = "roles/{gid}/{uid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins') || #uid == authentication.name")
    public IamService.AccountDetail getAccountInRole(@PathVariable String gid, @PathVariable String uid) {
        LOG.trace("REST GET roles/{}/{}", gid, uid);
        IamService.AccountDetail account = iamService.getAccountInGroup(gid, uid);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return account;
    }

    @PostMapping(path = "roles/{gid}")
    @PreAuthorize("hasRole('admins')")
    public void postAccountInRole(@PathVariable String gid, @RequestBody String uid) {
        LOG.trace("REST POST roles/{}", gid);
        
        uid = StringUtils.strip(uid);
        
        if (!iamService.isGroup(Flavour.ROLE, gid)) {
            LOG.warn("REST POST roles/{}: group not found", gid);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found: %s".formatted(gid));
        }
        else if (!iamService.isAccount(uid)) {
            LOG.warn("REST POST roles/{}: invalid account: {}", gid, uid);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown account: %s".formatted(uid));
        }
        
        LOG.debug("REST POST roles/{}: adding member: {}", gid, uid);
        iamService.addAccountToGroup(uid, gid);
    }

    @DeleteMapping(path = "roles/{gid}/{uid}")
    @PreAuthorize("hasRole('admins') && (#gid != 'admins' || #uid != authentication.name)")
    public void deleteAccountInRole(@PathVariable String gid, @PathVariable String uid) {
        LOG.debug("REST DELETE roles/{}/{}: removing member", gid, uid);
	if (!iamService.isGroup(Flavour.ROLE, gid)) {
            LOG.warn("REST DELETE roles/{}/{}: role not found", gid, uid);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found: %s".formatted(gid));
        }
        iamService.removeAccountFromGroup(uid, gid);
    }

        // Clients
    
    @GetMapping(path = "clients", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins')")
    public IamService.GroupDetail[] getClients() {
        LOG.trace("REST GET clients");
        return iamService.getGroups(Flavour.CLIENT);
    }
    
    @GetMapping(path = "clients/{gid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins') || (hasRole('users') && hasRole(#gid))")
    public IamService.GroupDetail getClient(@PathVariable String gid) {
        LOG.trace("REST GET clients/{}", gid);
        if (!iamService.isGroup(Flavour.CLIENT, gid)) {
            LOG.warn("REST GET clients/{}: group not found", gid);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: %s".formatted(gid));
        }
        return iamService.getGroup(Flavour.CLIENT, gid);
    }
   
    @PostMapping(path = "clients")
    @PreAuthorize("hasRole('admins')")
    public void postClient(@RequestBody String gid) {
        LOG.trace("REST POST clients");

        gid = StringUtils.strip(gid);
        
        if (StringUtils.isBlank(gid)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client ID must not be blank");
        }
        else if (iamService.isGroup(gid)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group exists: %s".formatted(gid));
        }

        LOG.debug("REST POST clients: creating client: {}", gid);
        iamService.createGroup(Flavour.CLIENT, gid);
    }

    @DeleteMapping(path = "clients/{gid}")
    @PreAuthorize("hasRole('admins')")
    public void deleteClient(@PathVariable String gid) {
        LOG.debug("REST DELETE clients/{}: delete client", gid);
        iamService.deleteGroup(gid);
    }

        // Client Membership

    @GetMapping(path = "clients/{gid}/{uid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins') || hasRole(#gid)")
    public IamService.AccountDetail getAccountInClient(@PathVariable String gid, @PathVariable String uid) {
        LOG.trace("REST GET clients/{}/{}", gid, uid);
        IamService.AccountDetail account = iamService.getAccountInGroup(gid, uid);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return account;
    }
    
    @PostMapping(path = "clients/{gid}")
    @PreAuthorize("hasRole('admins')")
    public void postAccountInClient(@PathVariable String gid, @RequestBody String uid) {
        LOG.trace("REST POST clients/{}", gid);
        
        uid = StringUtils.strip(uid);
       
        if (!iamService.isGroup(Flavour.CLIENT, gid)) {
            LOG.warn("REST POST clients/{}: group not found", gid);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: %s".formatted(gid));
        }
        else if (!iamService.isAccount(uid)) {
            LOG.warn("REST POST clients/{}: invalid account: {}", gid, uid);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown account: %s".formatted(uid));
        } 

        LOG.debug("REST POST clients/{}: adding member: {}", gid, uid);
        iamService.addAccountToGroup(uid, gid);
    }
    
    @DeleteMapping(path = "clients/{gid}/{uid}")
    @PreAuthorize("hasRole('admins')")
    public void deleteAccountInClient(@PathVariable String gid, @PathVariable String uid) {
        LOG.debug("REST DELETE clients/{}/{}: removing member", gid, uid);
	if (!iamService.isGroup(Flavour.CLIENT, gid)) {
            LOG.warn("REST DELETE clients/{}/{}: client not found", gid, uid);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: %s".formatted(gid));
        }
        iamService.removeAccountFromGroup(uid, gid);
    }

        // Helpers

    private boolean loginIsUser(String id) {
        return id.equals(SecurityContextHolder.getContext().getAuthentication().getName());
    }
    
    private boolean loginHasRole(String role) {
        return loginHasAuthority("ROLE_" + role);
    }
    
    private boolean loginHasAuthority(String authority) {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().map(Object::toString)
                .anyMatch(authority::equals);
    }

    private String[] checkedGroups(String[] ids) {
        Optional<String> badId = ids == null ? null : Arrays.stream(ids).filter(not(iamService::isGroup)).findFirst();
        if (badId.isPresent()) {
            LOG.warn("invalid group in list: {}", badId.get());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid group in list: %s".formatted(badId.get()));
        }
        return ids;
    }

    private String getAccountHelp() {
        
        Authentication login = SecurityContextHolder.getContext().getAuthentication();
        
        if (!login.isAuthenticated()) {
            LOG.error("Unexpected: an unauthenticated user has made it through the filter chain!");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Go Away!");
        }
        
        String userId = login.getName();
        List<String> auths = login.getAuthorities().stream().map(GrantedAuthority::toString).toList();
        List<String> roles = auths.stream().filter(s -> s.startsWith("ROLE_")).map(s -> s.substring(5)).toList();
        List<String> clients = roles.stream().filter(g -> iamService.isGroup(Flavour.CLIENT, g)).toList();

        StringBuffer buf = new StringBuffer("Hello there, %s\n".formatted(userId));
        
        if (roles.contains("admins")) {
            
            buf.append("\nAs an admin you have access to:\n"
                    + "  /iam\n"
                    + "    /check        : GET this page\n"
                    + "    /accounts     : GET list of accounts, POST new account\n"
                    + "      /{id}       : GET account, PUT update, DELETE remove\n"
                    + "        /password : POST password update\n"
                    + "    /roles        : GET list of role groups, two built-in:\n"
                    + "      /admins     : GET list, POST member\n"
                    + "        /{id}     : GET account, DELETE to remove from role\n"
                    + "      /users      : GET list, POST member\n"
                    + "        /{id}     : GET account, DELETE to remove from role\n"
                    + "    /clients      : GET client list, POST new client\n"
                    + "      /test       : built in test client\n"
                    + "      /{id}       : GET client details, POST member\n"
                    + "        /{uid}    : GET member, DELETE member from client\n"
                    + "      /test       : built in test client\n");
            
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
