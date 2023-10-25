package it.zwets.sms.scheduler.rest;

import java.util.List;

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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + id);
        }
        return account;
    }

    @PostMapping(path = "accounts", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins')")
    public IamService.AccountDetail postAccount(@RequestBody AccountDetail a) {
        LOG.debug("REST POST accounts");
        if (iamService.isAccount(a.id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account %s already exists".formatted(a.id));
        }
        return iamService.createAccount(new IamService.AccountDetail(a.id, a.name, a.email, a.password, a.groups));
    }

    @PutMapping(path = "accounts/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins') || authentication.name == #id")
    public IamService.AccountDetail putAccount(@PathVariable String id, @RequestBody AccountDetail a) {
        LOG.debug("REST PUT accounts/{}", a.id);
        
        if (StringUtils.isNotBlank(a.id) && !a.id.equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account URL %s does not match enitity: %s".formatted(id, a.id));
        }
        
        if (iamService.isAccount(id)) {
	        LOG.debug("REST PUT UPDATE accounts/{}", id);
	        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
	                .getAuthorities().stream().map(Object::toString)
	                .anyMatch(IamService.ADMINS_GROUP::equals);
	        return iamService.updateAccount(new IamService.AccountDetail(id, a.name, a.email, a.password, 
	                isAdmin ? a.groups : null));
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + id);
        }
    }
    
    @PutMapping(path = "accounts/{id}/password", consumes = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasRole('admins') || authentication.name == #id")
    public void updatePassword(@PathVariable String id, @PathVariable String password) {
        LOG.trace("REST PUT accounts/{}", id);
        iamService.updatePassword(id, password);
    }

    @DeleteMapping(path = "accounts/{id}")
    @PreAuthorize("hasRole('admins') && authentication.name != #id")
    public void deleteAccount(@PathVariable String id) {
        LOG.trace("REST DELETE accounts/{}", id);
        iamService.deleteAccount(id);
    }

        // Role: Users
    
    @GetMapping(path = "users", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins')")
    public IamService.AccountDetail[] getUsers() {
        LOG.trace("REST GET users");
        return iamService.getAccountsInGroup(IamService.USERS_GROUP);
    }
    
    @PostMapping(path = "users")
    @PreAuthorize("hasRole('admins')")
    public void postUser(@RequestBody String id) {
        LOG.trace("REST POST users \"{}\"", id);
        iamService.addAccountToGroup(id, IamService.USERS_GROUP);
    }

    @PutMapping(path = "users/{id}")
    @PreAuthorize("hasRole('admins')")
    public void putUser(@PathVariable String id) {
        LOG.trace("REST PUT users/{}", id);
        iamService.addAccountToGroup(id, IamService.USERS_GROUP);
    }

    @DeleteMapping(path = "users/{id}")
    @PreAuthorize("hasRole('admins')")
    public void deleteAdmin(@PathVariable String id) {
        LOG.trace("REST DELETE users/{}", id);
        iamService.removeAccountFromGroup(id, IamService.USERS_GROUP);
    }

    @GetMapping(path = "users/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins') || authentication.name == #id")
    public IamService.AccountDetail getUser(@PathVariable String id) {
        LOG.trace("REST GET users/{}", id);
        IamService.AccountDetail account = iamService.getAccountInGroup(IamService.USERS_GROUP, id);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found in users: " + id);
        }
        return account;
    }

        // Role: Admins
    
    @GetMapping(path = "admins", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins')")
    public IamService.AccountDetail[] getAdmins() {
        LOG.trace("REST GET users");
        return iamService.getAccountsInGroup(IamService.ADMINS_GROUP);
    }
    
    @PostMapping(path = "admins")
    @PreAuthorize("hasRole('admins')")
    public void postAdmin(@RequestBody String id) {
        LOG.trace("REST POST admins \"{}\"", id);
        iamService.addAccountToGroup(id, IamService.ADMINS_GROUP);
    }

    @PutMapping(path = "admins/{id}")
    @PreAuthorize("hasRole('admins')")
    public void putAdmin(@PathVariable String id) {
        LOG.trace("REST PUT admins/{}", id);
        iamService.addAccountToGroup(id, IamService.ADMINS_GROUP);
    }

    @GetMapping(path = "admins/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins') || authentication.name == #id")
    public IamService.AccountDetail getAdmin(@PathVariable String id) {
        LOG.trace("REST GET admins/{}", id);
        IamService.AccountDetail account = iamService.getAccountInGroup(IamService.ADMINS_GROUP, id);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found in admins: " + id);
        }
        return account;
    }

        // Clients
    
    @GetMapping(path = "clients", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins')")
    public IamService.GroupDetail[] getClients() {
        LOG.trace("REST GET clients");
        return iamService.getGroups(Flavour.CLIENT);
    }
    
    @GetMapping(path = "clients/{gid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins') || (hasRole('users') && hasAuthority('CLIENT_' + #gid))")
    public IamService.GroupDetail getClient(@PathVariable String gid) {
        LOG.trace("REST GET clients/{}", gid);
        IamService.GroupDetail client = iamService.getGroup(Flavour.CLIENT, gid);
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + gid);
        }
        return client;
    }
    
    @DeleteMapping(path = "groups/{gid}")
    @PreAuthorize("hasRole('admins') && #gid != 'admins' && #gid != 'users'")
    public void deleteGroup(@PathVariable String gid) {
        LOG.trace("REST DELETE groups/{}", gid);
        iamService.deleteGroup(gid);
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
        List<String> clients = auths.stream().filter(s -> s.startsWith("CLIENT_")).map(s -> s.substring(7)).toList();

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
