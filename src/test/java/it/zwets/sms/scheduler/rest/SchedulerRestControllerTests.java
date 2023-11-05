package it.zwets.sms.scheduler.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.ArrayList;
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
        ResponseEntity<String> response = rest.POST("/schedule/not-my-client", simpleRequest());
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
    
    @Test
    public void testBasics() {
        
        ResponseEntity<String> response = rest.POST("/schedule/test", simpleRequest());
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
    }

        // Helpers - deserialise JSON
    
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
                node.get("target").asText(null),
                node.get("key").asText(null),
                node.get("status").asText(null),
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

        // Helpers - serialise to JSON

    private String simpleRequest() {
        return simpleRequest(null, null);
    }
    
    private String simpleRequest(String target, String key) {
        long now = Instant.now().getEpochSecond();
        String schedule = new Scheduler(new Slot[] { new Slot(now+20, now+30), new Slot(now, now+10) }).toString();
        return smsJson(target, key, schedule, 
                "DummyRequest for target:%s key:%s)".formatted(StringUtils.defaultString(target), StringUtils.defaultString(key)));
    }
    
    private String smsJson(String target, String key, String schedule, String payload) {

        String sep = "";
        StringBuilder b = new StringBuilder();
        b.append("{ ");
        if (target != null) { b.append(sep).append(quotedField("target", target)); sep = ", "; }
        if (key != null) { b.append(sep).append(quotedField("key", key)); sep = ", "; }
        if (schedule != null) { b.append(sep).append(quotedField("schedule", schedule)); sep = ", "; }
        if (payload != null) { b.append(sep).append(quotedField("payload", payload)); sep = ", "; }
        b.append(" }");
        
        LOG.debug("JSON: {}", b.toString());
        return b.toString();
    }
    
    private String quotedField(String name, String value) {
        return "\"%s\": %s".formatted(name, quote(value));
    }
    
//    private String quotedField(String name, String[] value) {
//        return "\"%s\": %s".formatted(name, quote(value));
//    }
    
    private String quote(String s) {
        return s == null ? "null" : "\"%s\"".formatted(s);
    }
    
//    private String quote(String[] ss) {
//        return ss == null ? "null" : 
//            "[ " + Arrays.stream(ss).map(s -> "\"%s\"".formatted(s)).collect(Collectors.joining(",")) + " ]";
//    }
}
