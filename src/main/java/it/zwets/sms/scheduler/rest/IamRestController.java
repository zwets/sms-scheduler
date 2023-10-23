package it.zwets.sms.scheduler.rest;

import java.util.List;

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

import it.zwets.sms.scheduler.security.IamService;

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

        String res = "Hello there, %s\n".formatted(userId);
        
        if (roles.contains("ADMIN")) {
            
            res += "\nAs an admin you have access to:\n"
                    + "  /iam/accounts          : lists all accounts, post new accounts\n"
                    + "  /iam/accounts/{id}     : manage individual accounts (CRUD and set password)\n"
                    + "  /iam/roles             : lists available roles; we have two built-in:\n"
                    + "  /iam/roles/users       : accounts authorised for the application\n"
                    + "  /iam/roles/users/{id}  : use DELETE here to remove account from the role\n"
                    + "  /iam/roles/admins      : accounts authorised to manage accounts\n"
                    + "  /iam/roles/admins/{id} : use DELETE here to take id out of the role\n"
                    + "  /iam/clients           : lists the clients of the service\n"
                    + "  /iam/clients/test      : accounts for the out-of-the-box test client\n";
            
            if (!roles.contains("USER")) {
                
	            res += "\nNOTE: your account is a member of /iam/roles/admins,\n"
	                    + "but not of /iam/roles/users.  This is fine, it just\n"
	                    + "means that you can manage the application's users\n"
	                    + "but not use the application yourself.\n";
            }
        }
        else if (roles.contains("USER"))
            res += "\nPlease find your account information at: /iam/account/%s\n".formatted(userId);
        else
            res += "\nYou have a working login but no access to the application.\n"
                    + "An administrator will need to add you to the 'users' group.\n";
        
        if (clients.isEmpty())
            if (!roles.contains("USER"))
                res += "\nYou will also need to be added to one or more 'client' groups.\n";
            else
	            res += "\nYou are not authorised for any clients.\n"
	                    + "An administrator can add you to the appropriate 'clients' groups.\n";
        else {
            res += "\nYou are authorised for client(s):";
            for (String c : clients)
                res += " " + c;
            res += ".\n";
        }
        
        res += "\nBye now.\n";
            
        return res;
    }
    
        // Accounts
    
    @GetMapping(path = "accounts", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public IamService.AccountDetail[] getAccounts() {
        LOG.trace("REST GET accounts");
        return iamService.getAccounts();
    }

    private final record AccountDetail(
        String id,
        String name,
        String email,
        String password,
        String[] groups) { }
    
    @PostMapping(path = "accounts", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public IamService.AccountDetail createAccount(@RequestBody AccountDetail a) {
        LOG.trace("REST POST accounts");
        return iamService.createAccount(new IamService.AccountDetail(a.id, a.name, a.email, a.groups), a.password);
    }

    @GetMapping(path = "accounts/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN') || authentication.name == #id")
    public IamService.AccountDetail getAccount(@PathVariable String id) {
        LOG.trace("REST GET accounts/{}", id);
        IamService.AccountDetail account = iamService.getAccount(id);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + id);
        }
        return account;
    }

    @PutMapping(path = "accounts/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public IamService.AccountDetail updateAccount(@PathVariable String id) {
        LOG.trace("REST PUT accounts/{}", id);
        IamService.AccountDetail account = iamService.createAccount(null, id);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + id);
        }
        return account;
    }

    @DeleteMapping(path = "accounts/{id}")
    @PreAuthorize("hasRole('ADMIN') && authentication.name != #id")
    public void deleteAccount(@PathVariable String id) {
        LOG.trace("REST DELETE accounts/{}", id);
        iamService.deleteAccount(id);
    }

        // Groups
    
    @GetMapping(path = "groups", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public IamService.GroupDetail[] getGroups() {
        LOG.trace("REST GET groups");
        return iamService.getGroups();
    }
    
    @GetMapping(path = "groups/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public IamService.GroupDetail getGroup(@PathVariable String id) {
        LOG.trace("REST GET groups/{}", id);
        IamService.GroupDetail group = iamService.getGroup(id);
        if (group == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found: " + id);
        }
        return group;
    }

    @GetMapping(path = "roles", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public IamService.GroupDetail[] getRoles() {
        LOG.trace("REST GET roles");
        return iamService.getRoles();
    }
    
    @GetMapping(path = "roles/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public IamService.GroupDetail getRole(@PathVariable String id) {
        LOG.trace("REST GET roles/{}", id);
        IamService.GroupDetail role = iamService.getClient(id);
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found: " + id);
        }
        return role;
    }

    @GetMapping(path = "clients", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public IamService.GroupDetail[] getClients() {
        LOG.trace("REST GET clients");
        return iamService.getClients();
    }
    
    @GetMapping(path = "clients/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ROLE_' + 'DMIN') || (hasRole('USER') && hasAuthority('CLIENT_' + #id))")
    public IamService.GroupDetail getClient(@PathVariable String id) {
        LOG.trace("REST GET clients/{}", id);
        IamService.GroupDetail client = iamService.getClient(id);
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + id);
        }
        return client;
    }
}
