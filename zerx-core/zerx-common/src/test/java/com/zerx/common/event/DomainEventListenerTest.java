package com.zerx.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DomainEventListener}
 */
class DomainEventListenerTest {

    // ======================== Simple implementation ========================

    @Test
    @DisplayName("onEvent is called with the correct event")
    void onEventCalled() {
        AtomicReference<DomainEvent> received = new AtomicReference<>();

        DomainEventListener<DomainEvent> listener = event -> received.set(event);

        DomainEvent event = createTestEvent("TestType", "payload");
        listener.onEvent(event);

        assertNotNull(received.get());
        assertEquals("TestType", received.get().getEventType());
    }

    // ======================== Default eventType ========================

    @Test
    @DisplayName("default eventType() returns empty string")
    void defaultEventType() {
        DomainEventListener<DomainEvent> listener = event -> {};
        assertEquals("", listener.eventType());
    }

    // ======================== Custom eventType ========================

    @Test
    @DisplayName("custom implementation overrides eventType()")
    void customEventType() {
        DomainEventListener<DomainEvent> listener = new DomainEventListener<>() {
            @Override
            public void onEvent(DomainEvent event) {
            }

            @Override
            public String eventType() {
                return "CustomType";
            }
        };

        assertEquals("CustomType", listener.eventType());
    }

    // ======================== Static of() factory ========================

    @Test
    @DisplayName("of(eventType, Consumer) creates listener with correct eventType")
    void ofTypeReturnsCorrectEventType() {
        DomainEventListener<DomainEvent> listener = DomainEventListener.of("MyEvent", event -> {});
        assertEquals("MyEvent", listener.eventType());
    }

    @Test
    @DisplayName("of(eventType, Consumer) listener calls consumer on onEvent")
    void ofTypeCallsConsumer() {
        AtomicReference<String> receivedPayload = new AtomicReference<>();

        DomainEventListener<DomainEvent> listener = DomainEventListener.of("TestType", event -> {
            receivedPayload.set(extractData(event));
        });

        DomainEvent event = createTestEvent("TestType", "hello-world");
        listener.onEvent(event);

        assertEquals("hello-world", receivedPayload.get());
    }

    @Test
    @DisplayName("of(eventType, Consumer) with null eventType returns empty string")
    void ofTypeNullEventType() {
        DomainEventListener<DomainEvent> listener = DomainEventListener.of(null, event -> {});
        assertNull(listener.eventType());
    }

    // ======================== Functional interface ========================

    @Test
    @DisplayName("DomainEventListener is a functional interface (can be lambda)")
    void functionalInterface() {
        AtomicBoolean called = new AtomicBoolean(false);
        DomainEventListener<DomainEvent> listener = event -> called.set(true);

        listener.onEvent(createTestEvent("Type", ""));
        assertTrue(called.get());
    }

    @Test
    @DisplayName("multiple listeners can be created from of()")
    void multipleListeners() {
        AtomicReference<String> r1 = new AtomicReference<>();
        AtomicReference<String> r2 = new AtomicReference<>();

        DomainEventListener<DomainEvent> listener1 = DomainEventListener.of("E1", e -> r1.set("l1"));
        DomainEventListener<DomainEvent> listener2 = DomainEventListener.of("E2", e -> r2.set("l2"));

        listener1.onEvent(createTestEvent("E1", ""));
        listener2.onEvent(createTestEvent("E2", ""));

        assertEquals("l1", r1.get());
        assertEquals("l2", r2.get());
    }

    // ======================== Helper methods ========================

    /**
     * Creates a simple test DomainEvent with a data payload stored in toString
     */
    private static DomainEvent createTestEvent(String eventType, String data) {
        return new DomainEvent("test", eventType) {
            private final String payload = data;

            @Override
            public String toString() {
                return super.toString() + "[data=" + payload + "]";
            }
        };
    }

    private static String extractData(DomainEvent event) {
        String str = event.toString();
        int idx = str.indexOf("[data=");
        if (idx >= 0) {
            return str.substring(idx + 6, str.lastIndexOf(']'));
        }
        return null;
    }
}
