package it.zwets.sms.scheduler.rest;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import it.zwets.sms.scheduler.TargetBlockerService;

/**
 * REST Controller for the /block endpoint
 * 
 * Provides for blocking targets from having anything scheduled to them.
 */
@RestController
@RequestMapping(value = "/block")
@EnableMethodSecurity
public class BlockerRestController {

    private static final Logger LOG = LoggerFactory.getLogger(BlockerRestController.class);

    @Autowired
    private TargetBlockerService theService;

    /**
     * PUT a new blocked SMS.
     * @param clientId path variable identifies the client (scope) that defined the target
     * @param targetId path variable defining the block
     */
    @PutMapping(path = "{clientId}/{targetId}")
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public void putBlock(@PathVariable String clientId, @PathVariable String targetId) {
        LOG.debug("REST PUT /block/{}/{}", clientId, targetId);
        theService.blockTarget(clientId, targetId);
    }
    
    /**
     * DELETE an existing block.
     * @param clientId path variable identifies the client (scope) that defined the target
     * @param targetId path variable defining the block
     */
    @DeleteMapping(path = "{clientId}/{targetId}")
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public void deleteBlock(@PathVariable String clientId, @PathVariable String targetId) {
        LOG.debug("REST DELETE /block/{}/{}", clientId, targetId);
        theService.unblockTarget(clientId, targetId);
    }
    
    /**
     * GET the list of blocks.
     * @param clientId path variable identifies the client (scope) that defined the target
     * @param targetId path variable defining the block
     * @return a plain text list of blocked targets
     */
    @GetMapping(path = "{clientId}", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public String getBlocks(@PathVariable String clientId) {
        LOG.trace("REST GET /block/{}", clientId);
        return theService.getBlockedTargets(clientId);
    }
    
    /**
     * GET an existing block.
     * @param clientId path variable identifies the client (scope) that defined the target
     * @param targetId path variable defining the block
     * @return NOT_FOUND or OK
     */
    @GetMapping(path = "{clientId}/{targetId}")
    @PreAuthorize("hasRole('users') && hasRole(#clientId)")
    public void getBlock(@PathVariable String clientId, @PathVariable String targetId) {
        LOG.trace("REST GET /block/{}/{}", clientId, targetId);
        if (!theService.isTargetBlocked(clientId, targetId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}    
