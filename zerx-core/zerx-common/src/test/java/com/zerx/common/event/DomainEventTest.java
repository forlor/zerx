package com.zerx.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DomainEvent}
 */
class DomainEventTest {

    /**
     * Concrete test subclass of DomainEvent
     */
    static class TestEvent extends DomainEvent {
        private final String data;

        TestEvent(String aggregateType, String eventType, String data) {
            super(aggregateType, eventType);
            this.data = data;
        }

        TestEvent(String eventId, String aggregateType, String eventType,
                  Instant occurredAt, int version, String data) {
            super(eventId, aggregateType, eventType, occurredAt, version);
            this.data = data;
        }

        String getData() {
            return data;
        }
    }

    // ======================== Constructor ========================

    @Test
    @DisplayName("constructor generates auto UUIDv7 eventId")
    void constructorGeneratesEventId() {
        TestEvent event = new TestEvent("order", "OrderCreated", "payload");
        assertNotNull(event.getEventId());
        // UUIDv7 is 36 chars with hyphens
        assertEquals(36, event.getEventId().length());
        // Should be a valid UUID
        assertDoesNotThrow(() -> UUID.fromString(event.getEventId()));
    }

    @Test
    @DisplayName("constructor sets eventType")
    void constructorSetsEventType() {
        TestEvent event = new TestEvent("order", "OrderCreated", "data");
        assertEquals("OrderCreated", event.getEventType());
    }

    @Test
    @DisplayName("constructor sets aggregateType")
    void constructorSetsAggregateType() {
        TestEvent event = new TestEvent("order", "OrderCreated", "data");
        assertEquals("order", event.getAggregateType());
    }

    @Test
    @DisplayName("constructor sets occurredAt to current time")
    void constructorSetsOccurredAt() {
        Instant before = Instant.now();
        TestEvent event = new TestEvent("order", "OrderCreated", "data");
        Instant after = Instant.now();

        assertNotNull(event.getOccurredAt());
        assertFalse(event.getOccurredAt().isBefore(before));
        assertFalse(event.getOccurredAt().isAfter(after));
    }

    @Test
    @DisplayName("constructor sets default version to 1")
    void constructorDefaultVersion() {
        TestEvent event = new TestEvent("order", "OrderCreated", "data");
        assertEquals(1, event.getVersion());
    }

    @Test
    @DisplayName("full constructor sets all fields")
    void fullConstructor() {
        String eventId = "custom-event-id-123";
        Instant occurredAt = Instant.parse("2025-01-01T00:00:00Z");

        TestEvent event = new TestEvent(eventId, "order", "OrderCreated",
                occurredAt, 2, "payload");

        assertEquals(eventId, event.getEventId());
        assertEquals("order", event.getAggregateType());
        assertEquals("OrderCreated", event.getEventType());
        assertEquals(occurredAt, event.getOccurredAt());
        assertEquals(2, event.getVersion());
        assertEquals("payload", event.getData());
    }

    // ======================== isType ========================

    @Test
    @DisplayName("isType returns true for matching type")
    void isTypeTrue() {
        TestEvent event = new TestEvent("order", "OrderCreated", "data");
        assertTrue(event.isType("OrderCreated"));
    }

    @Test
    @DisplayName("isType returns false for non-matching type")
    void isTypeFalse() {
        TestEvent event = new TestEvent("order", "OrderCreated", "data");
        assertFalse(event.isType("OrderCancelled"));
    }

    @Test
    @DisplayName("isType returns false for null input")
    void isTypeNull() {
        TestEvent event = new TestEvent("order", "OrderCreated", "data");
        assertFalse(event.isType(null));
    }

    // ======================== isAggregateType ========================

    @Test
    @DisplayName("isAggregateType returns true for matching type")
    void isAggregateTypeTrue() {
        TestEvent event = new TestEvent("order", "OrderCreated", "data");
        assertTrue(event.isAggregateType("order"));
    }

    @Test
    @DisplayName("isAggregateType returns false for non-matching type")
    void isAggregateTypeFalse() {
        TestEvent event = new TestEvent("order", "OrderCreated", "data");
        assertFalse(event.isAggregateType("user"));
    }

    @Test
    @DisplayName("isAggregateType returns false for null")
    void isAggregateTypeNull() {
        TestEvent event = new TestEvent("order", "OrderCreated", "data");
        assertFalse(event.isAggregateType(null));
    }

    // ======================== isBefore / isAfter ========================

    @Test
    @DisplayName("isBefore returns true when event is before given instant")
    void isBeforeTrue() {
        Instant occurredAt = Instant.parse("2025-01-01T00:00:00Z");
        TestEvent event = new TestEvent("id", "order", "OrderCreated", occurredAt, 1, "data");
        assertTrue(event.isBefore(Instant.parse("2025-12-31T00:00:00Z")));
    }

    @Test
    @DisplayName("isBefore returns false when event is after given instant")
    void isBeforeFalse() {
        Instant occurredAt = Instant.parse("2025-06-01T00:00:00Z");
        TestEvent event = new TestEvent("id", "order", "OrderCreated", occurredAt, 1, "data");
        assertFalse(event.isBefore(Instant.parse("2025-01-01T00:00:00Z")));
    }

    @Test
    @DisplayName("isBefore returns false when event is at same instant")
    void isBeforeSame() {
        Instant occurredAt = Instant.parse("2025-01-01T00:00:00Z");
        TestEvent event = new TestEvent("id", "order", "OrderCreated", occurredAt, 1, "data");
        assertFalse(event.isBefore(occurredAt));
    }

    @Test
    @DisplayName("isAfter returns true when event is after given instant")
    void isAfterTrue() {
        Instant occurredAt = Instant.parse("2025-06-01T00:00:00Z");
        TestEvent event = new TestEvent("id", "order", "OrderCreated", occurredAt, 1, "data");
        assertTrue(event.isAfter(Instant.parse("2025-01-01T00:00:00Z")));
    }

    @Test
    @DisplayName("isAfter returns false when event is before given instant")
    void isAfterFalse() {
        Instant occurredAt = Instant.parse("2025-01-01T00:00:00Z");
        TestEvent event = new TestEvent("id", "order", "OrderCreated", occurredAt, 1, "data");
        assertFalse(event.isAfter(Instant.parse("2025-12-31T00:00:00Z")));
    }

    @Test
    @DisplayName("isAfter returns false when event is at same instant")
    void isAfterSame() {
        Instant occurredAt = Instant.parse("2025-01-01T00:00:00Z");
        TestEvent event = new TestEvent("id", "order", "OrderCreated", occurredAt, 1, "data");
        assertFalse(event.isAfter(occurredAt));
    }

    // ======================== toString ========================

    @Test
    @DisplayName("toString contains all fields")
    void toStringContainsFields() {
        TestEvent event = new TestEvent("order", "OrderCreated", "data");
        String str = event.toString();
        assertTrue(str.contains("DomainEvent"));
        assertTrue(str.contains(event.getEventId()));
        assertTrue(str.contains("OrderCreated"));
        assertTrue(str.contains("order"));
        assertTrue(str.contains("version=1"));
    }

    // ======================== Immutability ========================

    @Test
    @DisplayName("events with different data have different identities")
    void eventIdentity() {
        TestEvent event1 = new TestEvent("order", "OrderCreated", "data1");
        TestEvent event2 = new TestEvent("order", "OrderCreated", "data2");
        assertNotEquals(event1.getEventId(), event2.getEventId());
    }
}
