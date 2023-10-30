package it.zwets.sms.scheduler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;

class SlotTest {
    
//    private static final Logger LOG = LoggerFactory.getLogger(SlotTest.class);

    @Test
    void testValidSlot() {
        Slot slot = new Slot(10, 20);
        assertEquals(10, slot.getFrom());
        assertEquals(20, slot.getTill());
    }

    @Test
    void testInvalidZeroSizeSlot() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            new Slot(10, 10);
        });
        assertEquals("time slot must end after it starts", e.getMessage());
    }

    @Test
    void testInvalidInverseSlot() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            new Slot(20, 10);
        });
        assertEquals("time slot must end after it starts", e.getMessage());
    }

    @Test
    void testSlotToString() {
        assertEquals("10-20", new Slot(10, 20).toString());
    }

    @Test
    void testSlotDateDate() {
        Date now = new Date();
        Date then = new Date(now.getTime() + 10000);
        Slot s = new Slot(now, then);
        assertEquals(now.getTime() / 1000, s.from);
        assertEquals(then.getTime() / 1000, s.till);

    }

    @Test
    void testSlotInstantInstant() {
        Date now = new Date();
        Instant i1 = now.toInstant();
        Instant i2 = i1.plusSeconds(10);
        
        Slot slot = new Slot(i1, i2);
        assertEquals(i1.getEpochSecond(), slot.from);
        assertEquals(i2.getEpochSecond(), slot.till);
    }

    @Test
    void testToString() {
        Slot s = new Slot(10,20);
        assertEquals("10-20", s.toString());
    }

    @Test
    void testParse() {
        String str = "10-20";
        Slot s = Slot.parse(str);
        assertEquals(10, s.from);
        assertEquals(20, s.till);
    }
    
    @Test
    void testParseISO() {
        String str = "2023-10-30T07:32:50+03:00/2023-10-30T04:35:50Z";
        Slot s = Slot.parse(str);
        assertEquals(Instant.parse("2023-10-30T07:32:50+03:00").toEpochMilli()/1000, s.from);
        assertEquals(Instant.parse("2023-10-30T04:35:50Z").toEpochMilli()/1000, s.till);
    }

    @Test
    void testFailParseISOWithoutZoneOffset() {
        String str = "2023-10-30T07:32:50/2023-10-30T04:35:50Z";
        assertThrows(Exception.class, () -> Slot.parse(str));
    }

    @Test
    void testToJson() {
        Slot s1 = new Slot(10, 20);
        String json = s1.toJson();
        
        Slot s2 = Slot.parseJson(json);
        assertEquals(s1.from, s2.from);
        assertEquals(s1.till, s2.till);
    }

    @Test
    void testManualToJson() {
        Slot s1 = new Slot(10, 20);
        String json = s1.manualToJson();
        Slot s2 = Slot.parseJson(json);
        assertEquals(s1.from, s2.from);
        assertEquals(s1.till, s2.till);
    }
}
