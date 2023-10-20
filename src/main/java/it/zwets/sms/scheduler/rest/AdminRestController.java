package it.zwets.sms.scheduler.rest;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import it.zwets.sms.scheduler.admin.AdminService;

/**
 * REST controller for the /admin endpoint
 * @author zwets
 */
@RestController
@RequestMapping("/admin")
public class AdminRestController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminRestController.class);

    @Autowired
    private AdminService adminService;

    @GetMapping(path = "accounts", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdminService.AccountDetail[] getAccounts() {
        return adminService.getAccounts();
    }
    
    @GetMapping(path = "accounts/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdminService.AccountDetail getAccount(String id) {
        AdminService.AccountDetail account = adminService.getAccount(id);
        if (account == null) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Account not found: ".concat(id));
        }
        return account;
    }

    @GetMapping(path = "users", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdminService.AccountDetail[] getUsers() {
        return adminService.getAccountsInGroup(AdminService.USERS_GROUP_NAME);
    }
    
    @GetMapping(path = "users/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdminService.AccountDetail getUser(String id) {
        AdminService.AccountDetail account = adminService.getAccount(id);
        if (account == null) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Account not found: ".concat(id));
        }
        else if (!Arrays.asList(account.getGroups()).contains(AdminService.USERS_GROUP_NAME)) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Account not in users group: ".concat(id));
        }
        return account;
    }

    @GetMapping(path = "admins", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdminService.AccountDetail[] getAdmins() {
        return adminService.getAccountsInGroup(AdminService.ADMINS_GROUP_NAME);
    }
    
    @GetMapping(path = "admins/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdminService.AccountDetail getAdmin(String id) {
        AdminService.AccountDetail account = adminService.getAccount(id);
        if (account == null) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Admin not found: ".concat(id));
        }
        else if (!Arrays.asList(account.getGroups()).contains(AdminService.ADMINS_GROUP_NAME)) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Account not in admins group: ".concat(id));
        }
        return account;
    }

    @GetMapping(path = "clients", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdminService.ClientDetail[] getClients() {
        return adminService.getClients();
    }
    
    @GetMapping(path = "clients/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdminService.ClientDetail getClient(String id) {
        AdminService.ClientDetail client = adminService.getClient(id);
        if (client == null) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Client not found: ".concat(id));
        }
        return client;
    }

    @GetMapping(path = "groups", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdminService.ClientDetail[] getGroups() {
        return adminService.getGroups();
    }
    
    @GetMapping(path = "groups/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdminService.ClientDetail getGroup(String id) {
        AdminService.ClientDetail group = adminService.getGroup(id);
        if (group == null) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Group not found: ".concat(id));
        }
        return group;
    }

}
