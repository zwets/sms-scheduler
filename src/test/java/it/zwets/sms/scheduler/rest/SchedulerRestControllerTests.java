package it.zwets.sms.scheduler.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.zwets.sms.scheduler.SmsSchedulerConfiguration.Constants;
import it.zwets.sms.scheduler.SmsSchedulerService;
import it.zwets.sms.scheduler.SmsSchedulerService.SmsStatus;
import it.zwets.sms.scheduler.TargetBlockerService;
import it.zwets.sms.scheduler.iam.IamService;
import it.zwets.sms.scheduler.util.Scheduler;
import it.zwets.sms.scheduler.util.Slot;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
class SchedulerRestControllerTests {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerRestControllerTests.class);

    @LocalServerPort
    int port;

    @Autowired
    private IamService iamService;
    
    @Autowired
    private SmsSchedulerService schedulerService;
    
    @Autowired
    private TargetBlockerService targetBlockerService;

    private RestTestHelper rest;

    @BeforeAll
    public void beforeAll() {
        rest = new RestTestHelper(iamService, "http://localhost", port, "tester", "test");
        rest.createDefaultAccount(IamService.USERS_GROUP, IamService.TEST_CLIENT);
    }

    @AfterAll
    public void afterAll() {
        rest.deleteDefaultAccount();
    }
    
    @BeforeEach
    public void setup() {
    }

    @AfterEach
    public void teardown() {
        schedulerService.deleteAllForClient(IamService.TEST_CLIENT);
    }

        // Security tests

    @Test
    public void anonymousRequestOnRootUnauthorised() {
        ResponseEntity<String> response = new TestRestTemplate()
                .getForEntity("http://localhost:" + port + "/schedule", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
    
    @Test
    public void perpetratorRequestOnRootUnauthorised() {
        ResponseEntity<String> response = new TestRestTemplate("test", "hacker")
                .getForEntity("http://localhost:" + port + "/schedule", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
    
    @Test
    public void perpetratorRequestOnClientUnauthorised() {
        ResponseEntity<String> response = new TestRestTemplate("test", "hacker")
                .getForEntity("http://localhost:" + port + "/schedule/test", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
    
    @Test
    public void authenticatedRequestOnOtherClientForbidden() {
        ResponseEntity<String> response = rest.POST("/schedule/not-my-client", simpleRequest(0));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
    
    @Test
    public void testBasics() {
        
        ResponseEntity<String> response = rest.POST("/schedule/test", simpleRequest(10));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        SmsStatus s = deserializeStatus(response);
        String id = s.id();
        assertNotNull(id);
        assertEquals("test", s.client());
        assertNull(s.target());
        assertNull(s.key());
        assertEquals("NEW", s.status());
        assertNotNull(s.started());
        assertNull(s.ended());
        assertEquals(0, s.retries());

        response = rest.GET("/schedule/test/by-id/" + id);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        SmsStatus r = deserializeStatus(response);
        assertNotNull(r);
        
        assertEquals(Constants.SMS_STATUS_SCHEDULED, r.status());
        assertEquals(s.id(), r.id());
        assertEquals(s.client(), r.client());
        assertEquals(s.target(), r.target());
        assertEquals(s.key(), r.key());
        assertEquals(s.started(), r.started());
        assertEquals(s.ended(), r.ended());
        assertEquals(s.retries(), r.retries());
    
        response = rest.DELETE("/schedule/test/by-id/" + id);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        response = rest.GET("/schedule/test/by-id/" + id);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        r = deserializeStatus(response);
        assertNotNull(r);
        
        assertEquals(Constants.SMS_STATUS_CANCELED, r.status());
        assertNotNull(r.ended());

        assertEquals(s.id(), r.id());
        assertEquals(s.client(), r.client());
        assertEquals(s.target(), r.target());
        assertEquals(s.key(), r.key());
        assertEquals(s.started(), r.started());
        assertEquals(s.retries(), r.retries());
        
        response = rest.POST("/schedule/test", simpleRequest(10));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        response = rest.GET("/schedule/test");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<SmsStatus> ss = deserializeStatusList(response);
        assertEquals(2, ss.size());
        
        for (SmsStatus sm : ss) {
            schedulerService.deleteInstance(sm.id());
        }
    }

    @Test
    public void testOmitPatchParamDoesNotCancelAll() {

        ResponseEntity<String> response;
        SmsStatus ss1, ss2;
        String id1, id2;
        
        response = rest.POST("/schedule/test", simpleRequest("batch1", null, null, 1000));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ss1 = deserializeStatus(response);
        id1 = ss1.id();

        response = rest.POST("/schedule/test", simpleRequest(null, null, null, 1000));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ss2 = deserializeStatus(response);
        id2 = ss2.id();
        
        response = rest.GET("/schedule/test/by-id/" + id1);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ss1 = deserializeStatus(response);
        assertEquals(Constants.SMS_STATUS_SCHEDULED, ss1.status());

        response = rest.GET("/schedule/test/by-id/" + id2);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ss2 = deserializeStatus(response);
        assertEquals(Constants.SMS_STATUS_SCHEDULED, ss2.status());

        response = rest.DELETE("/schedule/test/by-batch/");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        response = rest.DELETE("/schedule/test/by-id/");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        response = rest.DELETE("/schedule/test/by-key/");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        response = rest.DELETE("/schedule/test/by-target/");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        response = rest.DELETE("/schedule/test/by-batch/batch1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        response = rest.GET("/schedule/test/by-id/" + id1);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ss1 = deserializeStatus(response);
        assertEquals(Constants.SMS_STATUS_CANCELED, ss1.status());

        response = rest.GET("/schedule/test/by-id/" + id2);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ss2 = deserializeStatus(response);
        assertEquals(Constants.SMS_STATUS_SCHEDULED, ss2.status());

        schedulerService.deleteInstance(id1);
        schedulerService.deleteInstance(id2);
    }

    @Test
    public void testAlreadyExpired() {
        
        ResponseEntity<String> response = rest.POST("/schedule/test", simpleRequest(-100));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        SmsStatus s = deserializeStatus(response);
        String id = s.id();
        assertEquals(Constants.SMS_STATUS_NEW, s.status());
        assertNotNull(s.started());
        assertNull(s.ended());

        response = rest.GET("/schedule/test/by-id/" + id);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        SmsStatus r = deserializeStatus(response);
        assertEquals(Constants.SMS_STATUS_EXPIRED, r.status());
        assertNotNull(r.ended());
    
        response = rest.DELETE("/schedule/test/by-id/" + id);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        response = rest.GET("/schedule/test/by-id/" + id);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        r = deserializeStatus(response);
        assertEquals(Constants.SMS_STATUS_EXPIRED, r.status());
        
        schedulerService.deleteInstance(id);
    }

    @Test
    public void testBlockTarget() {
        final String TARGET = "block-me";
        
        ResponseEntity<String> response = rest.PUT("/block/test/" + TARGET, "");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(targetBlockerService.isTargetBlocked("test", TARGET));

        response = rest.GET("/block/test/" + TARGET);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        response = rest.DELETE("/block/test/" + TARGET);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(targetBlockerService.isTargetBlocked("test", TARGET));

        response = rest.DELETE("/block/test/" + TARGET);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "deleting twice should be fine");

        response = rest.GET("/block/test/" + TARGET);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
        
    @Test
    public void testBlockMultiTarget() {
        final String TARGET1 = "block-me-1";
        final String TARGET2 = "block-me-2";
        
        ResponseEntity<String> response = rest.PUT("/block/test/" + TARGET1, "");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(targetBlockerService.isTargetBlocked("test", TARGET1));
        
        response = rest.PUT("/block/test/" + TARGET2, "");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(targetBlockerService.isTargetBlocked("test", TARGET2));

        response = rest.GET("/block/test");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, deserializeBlockList(response).length);
        
        response = rest.DELETE("/block/test/" + TARGET1);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        response = rest.DELETE("/block/test/" + TARGET2);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        response = rest.GET("/block/test");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, deserializeBlockList(response).length);
    }
        
    @Test
    public void testBlockedDoesNotSchedule() {
        final String TARGET = "block-me";
        
        ResponseEntity<String> response = rest.PUT("/block/test/" + TARGET, "");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        response = rest.POST("/schedule/test", simpleRequest("batch", "key", TARGET, 0));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        SmsStatus s = deserializeStatus(response);
        String id = s.id();

        response = rest.GET("/schedule/test/by-id/" + id);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        SmsStatus r = deserializeStatus(response);
        assertEquals(Constants.SMS_STATUS_BLOCKED, r.status());
        assertNotNull(r.ended());
    
        response = rest.DELETE("/block/test/" + TARGET);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(targetBlockerService.isTargetBlocked("test", TARGET));
        
        schedulerService.deleteInstance(id);
    }
        
    @Test
    void testImmediateWithWait() {

        ResponseEntity<String> response = rest.POST("/schedule/test", simpleRequest(0));
        assertEquals(HttpStatus.OK, response.getStatusCode());

        SmsStatus s = deserializeStatus(response);
        String id = s.id();
        
        waitForAsync(1);

        response = rest.GET("/schedule/test/by-id/" + id);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        s = deserializeStatus(response);
        assertEquals(Constants.SMS_STATUS_ENROUTE, s.status());
        assertNull(s.ended());

        schedulerService.deleteInstance(id);
    }

    @Test
    void testDelayedWithWait() {

        ResponseEntity<String> response = rest.POST("/schedule/test", simpleRequest(1));
        assertEquals(HttpStatus.OK, response.getStatusCode());

        SmsStatus s = deserializeStatus(response);
        String id = s.id();
        
        s = deserializeStatus(rest.GET("/schedule/test/by-id/" + id));
        assertEquals(Constants.SMS_STATUS_SCHEDULED, s.status());
        
        waitForAsync(2);

        s = deserializeStatus(rest.GET("/schedule/test/by-id/" + id));
        assertEquals(Constants.SMS_STATUS_ENROUTE, s.status());
        assertNull(s.ended());

        schedulerService.deleteInstance(id);
    }

    @Test
    void testNoSendAck() {

        ResponseEntity<String> response = rest.POST("/schedule/test", simpleRequest(0));
        assertEquals(HttpStatus.OK, response.getStatusCode());

        SmsStatus s = deserializeStatus(response);
        String id = s.id();
        
        waitForAsync(5);

        response = rest.GET("/schedule/test/by-id/" + id);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        s = deserializeStatus(response);
        assertEquals(Constants.SMS_STATUS_ENROUTE, s.status());
        assertNotNull(s.ended());

        schedulerService.deleteInstance(id);
    }


    // Helpers - deserialise JSON -----------------------------------------------------------------
    
    private JsonNode asJson(ResponseEntity<String> response) {
        return parseJson(response.getBody());
    }
    
    private JsonNode parseJson(String s) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(s);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    
    private SmsStatus deserialize(JsonNode node) {
        return new SmsStatus(
                node.get("id").asText(null),
                node.get("client").asText(null),
                node.get("batch").asText(null),
                node.get("key").asText(null),
                node.get("target").asText(null),
                node.get("status").asText(null),
                node.get("due").asText(null),
                node.get("deadline").asText(null),
                node.get("started").asText(null),
                node.get("ended").asText(null),
                node.get("retries").asInt(-2));
    }
    
    private SmsStatus deserializeStatus(ResponseEntity<String> entity) {
        JsonNode root = asJson(entity);
        return deserialize(root);
    }
    
    private List<SmsStatus> deserializeStatusList(ResponseEntity<String> entity) {
        List<SmsStatus> result = new ArrayList<SmsStatus>();
        Iterator<JsonNode> iter = asJson(entity).elements();
        
        while (iter.hasNext()) {
            result.add(deserialize(iter.next()));
        }
        
        return result;
    }

    private String[] deserializeBlockList(ResponseEntity<String> entity) {
        return Arrays.stream(entity.getBody().split("\n")).filter(StringUtils::isNotBlank).toArray(String[]::new);
    }

        // Helpers - serialise to JSON

    private String simpleRequest(int seconds) {
        return simpleRequest(null, null, null, seconds);
    }
    
    private String simpleRequest(String batch, String key, String target, int seconds) {
        long due = Instant.now().getEpochSecond() + seconds;
        String schedule = new Scheduler(new Slot[] { new Slot(due, due+5) }).toString();
        return smsJson(batch, key, target, schedule, 
                "DummyRequest for B:K:T %s:%s:%s in %ds)"
                .formatted(StringUtils.defaultString(batch), StringUtils.defaultString(key), StringUtils.defaultString(target), seconds));
    }
    
    private String smsJson(String batch, String key, String target, String schedule, String payload) {

        String sep = "";
        StringBuilder b = new StringBuilder();
        b.append("{ ");
        if (batch != null) { b.append(sep).append(quotedField("batch", batch)); sep = ", "; }
        if (key != null) { b.append(sep).append(quotedField("key", key)); sep = ", "; }
        if (target != null) { b.append(sep).append(quotedField("target", target)); sep = ", "; }
        if (schedule != null) { b.append(sep).append(quotedField("schedule", schedule)); sep = ", "; }
        if (payload != null) { b.append(sep).append(quotedField("payload", payload)); sep = ", "; }
        b.append(" }");
        
        LOG.debug("JSON: {}", b.toString());
        return b.toString();
    }
    
    private String quotedField(String name, String value) {
        return "\"%s\": %s".formatted(name, quote(value));
    }
    
    private String quote(String s) {
        return s == null ? "null" : "\"%s\"".formatted(s);
    }
    
    private void waitForAsync(double seconds) {
        LOG.debug("waiting for job executor {}s", seconds);
        try {
            Thread.sleep(Math.round(1000 * seconds));
        }
        catch (InterruptedException e) {
            // ignore
        }
    }
}