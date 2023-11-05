package it.zwets.sms.scheduler.rest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

import it.zwets.sms.scheduler.SmsSchedulerService;
import it.zwets.sms.scheduler.SmsSchedulerService.SmsStatus;

/**
 * REST Controller for the /schedule endpoint
 * 
 * Provides for scheduling on the /schedule/{client} endpoints,
 * and querying and canceling/deleting on three variables:
 * <ul>
 * <li><b>by-id</b>: on the unique process instance ID assigned by the server</li>
 * <li><b>by-key</b>: on the optionally unique business key assigned by the client</li>
 * <li><b>by-target</b>: on the target ID (semantically: the recipient) optionally assigned by the client</li>
 * <li></li>
 * </ul>
 */
@RestController
@RequestMapping(value = "/schedule")
@EnableMethodSecurity
public class SchedulerRestController {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerRestController.class);

    @Autowired
    private SmsSchedulerService theService;

    // SCHEDULE -----------------------------------------------------------------------------------
    
    /* Request from client; all fields except schedule and payload can be absent. */
    private final record Request(String target, String key, String schedule, String payload) { }

    /**
     * POST a new scheduled SMS.
     * @param clientId path variable identifying the client (tenant) for whom the send is
     * @param req body Request object with with at least fields schedule and payload, optional target and key
     * @return the status of the SMS (@TODO@ will always be NEW or BLOCKED?)
     */
    @PostMapping(path = "{clientId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public SmsStatus postClient(@PathVariable String clientId, @RequestBody Request req) {
        LOG.trace("REST POST /schedule/{}", clientId);
        return theService.scheduleSms(clientId, req.target, req.key, req.schedule, req.payload);
    }
    
    // QUERY --------------------------------------------------------------------------------------
    
    /**
     * GET the list of scheduled SMS
     * @param clientId path variable identifying the client (tenant)
     * @return list of SMS status objects
     */
    @GetMapping(path = "{clientId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public List<SmsStatus> getClient(@PathVariable String clientId) {
        LOG.trace("REST GET /schedule/{}", clientId);
        return theService.getStatusList(clientId);
    }

    /**
     * GET the status of exactly one SMS
     * @param clientId path variable identifying the client (tenant)
     * @param instanceId the unique server-provided instance ID of the schedule
     * @return the SMS status or NOT_FOUND
     */
    @GetMapping(path = "{clientId}/by-id/{instanceId}")
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public SmsStatus getById(@PathVariable String clientId, @PathVariable String instanceId) {
        LOG.trace("REST GET /schedule/{}/by-id/{}", clientId, instanceId);
        
        SmsStatus smsStatus = theService.getSmsStatus(instanceId);
        if (smsStatus == null || !clientId.equals(smsStatus.client())) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
        }
        return smsStatus;
    }
    
    /**
     * GET status list for all SMS scheduled for a target
     * @param clientId path variable identifying the client (tenant)
     * @param targetId the client-provided identifier of the target (recipient)
     * @return list of SMS status objects
     */
    @GetMapping(path = "{clientId}/by-target/{targetId}")
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public List<SmsStatus> getByTarget(@PathVariable String clientId, @PathVariable String targetId) {
        LOG.trace("REST GET /schedule/{}/by-target/{}", clientId, targetId);
        return theService.getStatusListByTarget(clientId, targetId);
    }
    
    /**
     * GET status list for all SMS marked by the client with a certain client key
     * (ideally unique but this is up to the client).
     * @param clientId path variable identifying the client (tenant)
     * @param clientKey the client-provided identifier of the message send
     * @return list of SMS status objects
     */
    @GetMapping(path = "{clientId}/by-key/{clientKey}")
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public List<SmsStatus> getByKey(@PathVariable String clientId, @PathVariable String clientKey) {
        LOG.trace("REST GET /schedule/{}/by-key/{}", clientId, clientKey);
        return theService.getStatusListByClientKey(clientId, clientKey);
    }

    // CANCEL -------------------------------------------------------------------------------------
    
    /**
     * DELETE (cancel) ALL scheduled SMS for a client
     * @param clientId the client ID for which to delete the SMS
     * @param confirm request parameter that must be present with value 'yes-i-am-sure' as a safeguard
     */
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

    /**
     * DELETE (cancel) one scheduled SMS
     * @param clientId path variable identifying the client (tenant)
     * @param targetId the client-provided identifier of the target (recipient)
     */
    @DeleteMapping(path = "{clientId}/by-id/{instanceId}")
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public void deleteByInstanceId(@PathVariable String clientId, @PathVariable String instanceId) {
        LOG.error("REST DELETE /schedule/{}/by-id/{}", clientId, instanceId);
        theService.cancelSms(instanceId);
    }

    /**
     * DELETE (cancel) all scheduled SMS for a target
     * @param clientId path variable identifying the client (tenant)
     * @param targetId the client-provided identifier of the target (recipient)
     */
    @DeleteMapping(path = "{clientId}/by-target/{targetId}")
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public void deleteByTarget(@PathVariable String clientId, @PathVariable String targetId) {
        LOG.trace("REST DELETE /schedule/{}/by-target/{}", clientId, targetId);
        theService.cancelAllForTarget(clientId, targetId);
    }

    /**
     * DELETE (cancel) scheduled SMS by client-provided key
     * @param clientId path variable identifying the client (tenant)
     * @param clientKey the client-provided identifier of the SMS
     */
    @DeleteMapping(path = "{clientId}/by-key/{clientKey}")
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public void deleteByKey(@PathVariable String clientId, @PathVariable String clientKey) {
        LOG.trace("REST DELETE /schedule/{}/by-key/{}", clientId, clientKey);
        theService.cancelByClientKey(clientId, clientKey);
    }

    // Helpers

//    private boolean loginIsUser(String id) {
//        return id.equals(SecurityContextHolder.getContext().getAuthentication().getName());
//    }
//
//    private boolean loginHasRole(String role) {
//        return loginHasAuthority("ROLE_" + role);
//    }
//    
//    private boolean loginHasAuthority(String authority) {
//        return SecurityContextHolder.getContext().getAuthentication()
//                .getAuthorities().stream().map(Object::toString)
//                .anyMatch(authority::equals);
//    }
}
