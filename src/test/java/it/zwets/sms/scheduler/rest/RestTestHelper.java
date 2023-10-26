package it.zwets.sms.scheduler.rest;

import java.util.List;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class RestTestHelper {

    private String baseUrl;
    private String passPfx;
    
    public RestTestHelper(String baseUrl, int port, String passPfx) {
        this.baseUrl = baseUrl + ":" + port;
        this.passPfx = passPfx; 
    }

    public String getPassword(String id) {
        return passPfx + id;
    }
    
    public ResponseEntity<String> GET(String id, String url) {
        return GET(id, getPassword(id), url);
    }
    
    public ResponseEntity<String> DELETE(String id, String url) {
        return DELETE(id, getPassword(id), url);
    }
    
    public ResponseEntity<String> POST(String id, String url, String body) {
        return POST(id, getPassword(id), url, body);
    }
    
    public ResponseEntity<String> PUT(String id, String url, String body) {
        return PUT(id, getPassword(id), url, body);
    }
    
    public ResponseEntity<String> GET(String id, String password, String url) {
        return exchange(id, password, url, HttpMethod.GET, null);
    }
    
    public ResponseEntity<String> DELETE(String id, String password, String url) {
        return exchange(id, password, url, HttpMethod.DELETE, null);
    }
    
    public ResponseEntity<String> POST(String id, String password, String url, String body) {
        return exchange(id, password, url, HttpMethod.POST, makeEntity(body));
    }
    
    public ResponseEntity<String> PUT(String id, String password, String url, String body) {
        return exchange(id, password, url, HttpMethod.PUT, makeEntity(body));
    }
    
    public ResponseEntity<String> exchange(String uid, String password, String url, HttpMethod verb, HttpEntity<String> entity) {
        return new TestRestTemplate(uid, password).exchange(baseUrl + url, verb, entity, String.class);
    }
    
    private HttpEntity<String> makeEntity(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));
        return new HttpEntity<String>(json, headers);        
    }
}
