package it.zwets.sms.scheduler.rest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import it.zwets.sms.scheduler.Schedule;
import it.zwets.sms.scheduler.SmsSchedulerService;
import it.zwets.sms.scheduler.SmsSchedulerService.SmsStatus;
import it.zwets.sms.scheduler.iam.IamService;

/**
 * REST Controller for the /schedule endpoint
 */
@RestController
@RequestMapping(value = "/schedule")
@EnableMethodSecurity
public class SchedulerRestController {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerRestController.class);

    @Autowired
    private SmsSchedulerService theService;

    @GetMapping(path = { "", "/" }, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('admins') || hasRole('users')")
    public List<SmsStatus> getRoot() {
        LOG.trace("REST GET /schedule");
        if (loginHasRole(IamService.ADMINS_GROUP)) {
            return theService.getStatusList();
        }
        else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Restricted to admins");
        }
    }

    @GetMapping(path = "{clientId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public List<SmsStatus> getClient(@PathVariable String clientId) {
        LOG.trace("REST GET /schedule/{}", clientId);
        return theService.getStatusList(clientId);
    }

    @PostMapping(path = "{clientId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public SmsStatus postClient(@PathVariable String clientId, @RequestBody Request req) {
        LOG.trace("REST POST /schedule/{}", clientId);
        return theService.scheduleSms(clientId, req.targetId, req.uniqueId, req.schedule, req.payload);
    }
    
    @GetMapping(path = "{clientId}/{targetId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public List<SmsStatus> getClientTarget(@PathVariable String clientId, @PathVariable String targetId) {
        LOG.trace("REST GET /schedule/{}/{}", clientId, targetId);
        return theService.getStatusList(clientId, targetId);
    }

    @GetMapping(path = "{clientId}/{targetId}/{uniqueId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public SmsStatus getClientTargetUnique(@PathVariable String clientId, @PathVariable String targetId, @PathVariable String uniqueId) {
        LOG.trace("REST GET /schedule/{}/{}/{}", clientId, targetId, uniqueId);
        SmsStatus smsStatus = theService.getSmsStatus(clientId, targetId, clientId);
        if (smsStatus == null) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
        }
        return smsStatus;
    }

    @DeleteMapping(path = "{clientId}")
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public void deleteClient(@PathVariable String clientId, @RequestParam String confirm) {
        LOG.trace("REST DELETE /schedule/{}", clientId);
        if ("yes-i-am-sure".equals(confirm)) {
            theService.cancelAllForClient(clientId);
        }
        else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This requires parameter confirm=yes-i-am-sure");
        }
    }

    @DeleteMapping(path = "{clientId}/{targetId}")
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public void deleteClientTarget(@PathVariable String clientId, @PathVariable String targetId) {
        LOG.trace("REST DELETE /schedule/{}/{}", clientId, targetId);
        theService.cancelAllForTarget(clientId, targetId);
    }

    @DeleteMapping(path = "{clientId}/{targetId}/{uniqueId}")
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public void deleteClientTargetUnique(@PathVariable String clientId, @PathVariable String targetId, @PathVariable String uniqueId) {
        LOG.trace("REST DELETE /schedule/{}/{}/{}", clientId, targetId, uniqueId);
        theService.cancelSms(clientId, targetId, uniqueId);
    }

    private final record Request(
        String targetId,
        String uniqueId,
        Schedule schedule,
        String payload) { }
    
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
}
