package it.zwets.sms.scheduler.util;

import static org.junit.jupiter.api.Assertions.*;

import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DateHelperTests {

    public final Logger LOG = LoggerFactory.getLogger(DateHelperTests.class);

    @Test
    void testDateHelper() {
        DateHelper dh = new DateHelper(ZoneOffset.of("+03:00"));
        
        String utcDate = "2023-10-28T00:23:30Z";
        Date parsedUtc = dh.parse(utcDate);
        LOG.info("parsedUtc: {}", parsedUtc);
        String formatted = dh.format(parsedUtc);
        
        assertEquals("2023-10-28T03:23:30+03:00", formatted);
    }
    
    @Test
    void testFailsWithoutZone() {
        DateHelper dh = new DateHelper(ZoneOffset.of("+03:00"));
        assertThrows(DateTimeParseException.class, () -> dh.parse("2023-10-28T03:00"));
    }
}
