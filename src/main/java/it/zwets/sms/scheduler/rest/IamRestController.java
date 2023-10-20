package it.zwets.sms.scheduler.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import it.zwets.sms.scheduler.security.IamService;

/**
 * REST controller for the /admin endpoint
 * @author zwets
 */
@RestController
@RequestMapping("/admin")
public class IamRestController {

    private static final Logger LOG = LoggerFactory.getLogger(IamRestController.class);

    private IamService IamService;

    IamRestController(IamService IamService) {
        this.IamService = IamService;
    }
    
    @GetMapping(path = "accounts", produces = MediaType.APPLICATION_JSON_VALUE)
    public IamService.AccountDetail[] getAccounts() {
        LOG.debug("REST GET /admin/accounts");
        return IamService.getAccounts();
    }
    
    @GetMapping(path = "accounts/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public IamService.AccountDetail getAccount(@PathVariable String id) {
        LOG.debug("REST GET /admin/accounts/{}", id);
        IamService.AccountDetail account = IamService.getAccount(id);
        if (account == null) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Account not found: ".concat(id));
        }
        return account;
    }

    @GetMapping(path = "groups", produces = MediaType.APPLICATION_JSON_VALUE)
    public IamService.GroupDetail[] getGroups() {
        LOG.debug("REST GET /admin/groups");
        return IamService.getGroups();
    }
    
    @GetMapping(path = "groups/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public IamService.AccountDetail getGroup(@PathVariable String id) {
        LOG.debug("REST GET /admin/groups/{}", id);
        IamService.AccountDetail account = IamService.getAccount(id);
        if (account == null) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Account not found: ".concat(id));
        }
        return account;
    }

    @GetMapping(path = "roles", produces = MediaType.APPLICATION_JSON_VALUE)
    public IamService.GroupDetail[] getRoles() {
        return IamService.getRoles();
    }
    
    @GetMapping(path = "roles/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public IamService.GroupDetail getRole(String id) {
        IamService.GroupDetail role = IamService.getClient(id);
        if (role == null) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Role not found: ".concat(id));
        }
        return role;
    }

    @GetMapping(path = "clients", produces = MediaType.APPLICATION_JSON_VALUE)
    public IamService.GroupDetail[] getClients() {
        return IamService.getClients();
    }
    
    @GetMapping(path = "clients/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public IamService.GroupDetail getClient(String id) {
        IamService.GroupDetail client = IamService.getClient(id);
        if (client == null) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Client not found: ".concat(id));
        }
        return client;
    }
}
