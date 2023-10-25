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
    
    public RestTestHelper(String baseUrl, int port) {
        this.baseUrl = baseUrl + ":" + port;
    }
    
    public ResponseEntity<String> GET(String id, String url) {
        return exchange(id, url, HttpMethod.GET, null);
    }
    
    public ResponseEntity<String> DELETE(String id, String url) {
        return exchange(id, url, HttpMethod.DELETE, null);
    }
    
    public ResponseEntity<String> POST(String id, String url, String body) {
        return exchange(id, url, HttpMethod.POST, makeEntity(body));
    }
    
    public ResponseEntity<String> PUT(String id, String url, String body) {
        return exchange(id, url, HttpMethod.PUT, makeEntity(body));
    }
    
    public ResponseEntity<String> exchange(String uid, String url, HttpMethod verb, HttpEntity<String> entity) {
        return new TestRestTemplate(uid,uid).exchange(baseUrl + url, verb, entity, String.class);
    }
    
    private HttpEntity<String> makeEntity(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));
        return new HttpEntity<String>(json, headers);        
    }
}
