package it.zwets.sms.scheduler.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import it.zwets.sms.scheduler.security.IamService;

/**
 * REST controller for the /iam endpoint
 * @author zwets
 */
@RestController
@RequestMapping("/iam")
@EnableMethodSecurity
public class IamRestController {

    private static final Logger LOG = LoggerFactory.getLogger(IamRestController.class);

    private IamService IamService;

    IamRestController(IamService IamService) {
        this.IamService = IamService;
    }
 
//    @ResponseStatus(code = HttpStatus.NOT_FOUND)
//    public final class NotFoundException extends RuntimeException {
//        public NotFoundException(String msg) { super(msg); }
//    }
    
    @GetMapping(path = "accounts", produces = MediaType.APPLICATION_JSON_VALUE)
    public IamService.AccountDetail[] getAccounts() {
        LOG.debug("REST GET accounts");
        return IamService.getAccounts();
    }

//    @GetMapping(path = "accounts/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
//    @PreAuthorize("hasRole('ADMIN') || authentication.name == #id")
//    public ResponseEntity<IamService.AccountDetail> getAccount(@PathVariable String id) {
//        LOG.debug("REST GET accounts/{}", id);
//        IamService.AccountDetail account = IamService.getAccount(id);
//        if (account == null) {
//            return new ResponseEntity<IamService.AccountDetail>(HttpStatus.NOT_FOUND);
//        }
//        return new ResponseEntity<IamService.AccountDetail>(account, HttpStatus.OK);
//    }

    @GetMapping(path = "accounts/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN') || authentication.name == #id")
    public IamService.AccountDetail getAccount1(@PathVariable String id) {
        LOG.debug("REST GET accounts/{}", id);
        IamService.AccountDetail account = IamService.getAccount(id);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.OK, "Account not found: " + id);
        }
        return account;
    }

    @GetMapping(path = "groups", produces = MediaType.APPLICATION_JSON_VALUE)
    public IamService.GroupDetail[] getGroups() {
        LOG.debug("REST GET groups");
        return IamService.getGroups();
    }
    
    @GetMapping(path = "groups/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public IamService.GroupDetail getGroup(@PathVariable String id) {
        LOG.debug("REST GET groups/{}", id);
        IamService.GroupDetail group = IamService.getGroup(id);
        if (group == null) {
            throw new ResponseStatusException(HttpStatus.OK, "Group not found: " + id);
        }
        return group;
    }

    @GetMapping(path = "roles", produces = MediaType.APPLICATION_JSON_VALUE)
    public IamService.GroupDetail[] getRoles() {
        LOG.debug("REST GET roles");
        return IamService.getRoles();
    }
    
    @GetMapping(path = "roles/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public IamService.GroupDetail getRole(String id) {
        LOG.debug("REST GET roles/{}", id);
        IamService.GroupDetail role = IamService.getClient(id);
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.OK, "Role not found: " + id);
        }
        return role;
    }

    @GetMapping(path = "clients", produces = MediaType.APPLICATION_JSON_VALUE)
    public IamService.GroupDetail[] getClients() {
        LOG.debug("REST GET clients");
        return IamService.getClients();
    }
    
    @GetMapping(path = "clients/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public IamService.GroupDetail getClient(String id) {
        LOG.debug("REST GET clients/{}", id);
        IamService.GroupDetail client = IamService.getClient(id);
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.OK, "Client not found: " + id);
        }
        return client;
    }
}
