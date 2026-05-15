package com.zerx.common.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SimpleEventBus} (testing the {@link EventBus} interface)
 */
class EventBusTest {

    private SimpleEventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new SimpleEventBus();
    }

    // ======================== Helper ========================

    private static DomainEvent createEvent(String eventType) {
        return new DomainEvent("test", eventType) {};
    }

    // ======================== Subscribe / Publish ========================

    @Nested
    @DisplayName("subscribe / publish")
    class SubscribePublish {

        @Test
        @DisplayName("published event reaches subscribed listener")
        void eventReachesListener() {
            AtomicBoolean received = new AtomicBoolean(false);
            eventBus.subscribe("OrderCreated", event -> received.set(true));

            eventBus.publish(createEvent("OrderCreated"));

            assertTrue(received.get());
        }

        @Test
        @DisplayName("listener only receives its subscribed event type")
        void listenerReceivesOwnType() {
            AtomicInteger orderCount = new AtomicInteger(0);
            AtomicInteger userCount = new AtomicInteger(0);

            eventBus.subscribe("OrderCreated", event -> orderCount.incrementAndGet());
            eventBus.subscribe("UserRegistered", event -> userCount.incrementAndGet());

            eventBus.publish(createEvent("OrderCreated"));
            eventBus.publish(createEvent("OrderCreated"));
            eventBus.publish(createEvent("UserRegistered"));

            assertEquals(2, orderCount.get());
            assertEquals(1, userCount.get());
        }

        @Test
        @DisplayName("publish to event type with no subscribers does nothing")
        void publishNoSubscribers() {
            // Should not throw
            assertDoesNotThrow(() -> eventBus.publish(createEvent("UnknownType")));
        }

        @Test
        @DisplayName("publish null event does nothing")
        void publishNull() {
            eventBus.subscribe("Test", event -> fail("Should not be called"));
            assertDoesNotThrow(() -> eventBus.publish(null));
        }

        @Test
        @DisplayName("subscribe with null eventType is ignored")
        void subscribeNullEventType() {
            eventBus.subscribe(null, event -> fail("Should not be called"));
            assertFalse(eventBus.hasListeners(null));
        }

        @Test
        @DisplayName("subscribe with null listener is ignored")
        void subscribeNullListener() {
            eventBus.subscribe("Test", null);
            assertFalse(eventBus.hasListeners("Test"));
        }
    }

    // ======================== Multiple listeners ========================

    @Nested
    @DisplayName("Multiple listeners")
    class MultipleListeners {

        @Test
        @DisplayName("multiple listeners for same event type all receive event")
        void allListenersReceive() {
            AtomicInteger count1 = new AtomicInteger(0);
            AtomicInteger count2 = new AtomicInteger(0);
            AtomicInteger count3 = new AtomicInteger(0);

            eventBus.subscribe("Test", event -> count1.incrementAndGet());
            eventBus.subscribe("Test", event -> count2.incrementAndGet());
            eventBus.subscribe("Test", event -> count3.incrementAndGet());

            eventBus.publish(createEvent("Test"));

            assertEquals(1, count1.get());
            assertEquals(1, count2.get());
            assertEquals(1, count3.get());
        }

        @Test
        @DisplayName("getListeners returns all subscribed listeners")
        void getListenersReturnsAll() {
            var l1 = DomainEventListener.of("Test", event -> {});
            var l2 = DomainEventListener.of("Test", event -> {});
            var l3 = DomainEventListener.of("Test", event -> {});

            eventBus.subscribe("Test", l1);
            eventBus.subscribe("Test", l2);
            eventBus.subscribe("Test", l3);

            List<DomainEventListener<?>> listeners = eventBus.getListeners("Test");
            assertEquals(3, listeners.size());
            assertTrue(listeners.contains(l1));
            assertTrue(listeners.contains(l2));
            assertTrue(listeners.contains(l3));
        }
    }

    // ======================== Listener exception isolation ========================

    @Nested
    @DisplayName("Listener exception isolation")
    class ExceptionIsolation {

        @Test
        @DisplayName("one listener throwing does not prevent others from executing")
        void exceptionIsolation() {
            AtomicInteger secondCalled = new AtomicInteger(0);

            eventBus.subscribe("Test", event -> {
                throw new RuntimeException("First listener fails");
            });
            eventBus.subscribe("Test", event -> secondCalled.incrementAndGet());

            // Set a custom uncaught exception handler to prevent test failure from the default handler
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                // Swallow the exception during test
            });

            try {
                eventBus.publish(createEvent("Test"));
            } finally {
                Thread.setDefaultUncaughtExceptionHandler(null);
            }

            assertEquals(1, secondCalled.get(), "Second listener should still be called");
        }

        @Test
        @DisplayName("exception in last listener does not affect earlier ones")
        void lastListenerException() {
            AtomicInteger firstCalled = new AtomicInteger(0);

            eventBus.subscribe("Test", event -> firstCalled.incrementAndGet());
            eventBus.subscribe("Test", event -> {
                throw new RuntimeException("Last listener fails");
            });

            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {});
            try {
                eventBus.publish(createEvent("Test"));
            } finally {
                Thread.setDefaultUncaughtExceptionHandler(null);
            }

            assertEquals(1, firstCalled.get());
        }
    }

    // ======================== Unsubscribe ========================

    @Nested
    @DisplayName("Unsubscribe")
    class Unsubscribe {

        @Test
        @DisplayName("unsubscribe by type removes all listeners for that type")
        void unsubscribeByType() {
            eventBus.subscribe("TypeA", event -> {});
            eventBus.subscribe("TypeA", event -> {});
            eventBus.subscribe("TypeB", event -> {});

            eventBus.unsubscribe("TypeA");

            assertFalse(eventBus.hasListeners("TypeA"));
            assertTrue(eventBus.hasListeners("TypeB"));
        }

        @Test
        @DisplayName("unsubscribe by listener removes specific listener")
        void unsubscribeByListener() {
            AtomicBoolean l1Called = new AtomicBoolean(false);
            AtomicBoolean l2Called = new AtomicBoolean(false);

            var listener1 = DomainEventListener.of("Test", event -> l1Called.set(true));
            var listener2 = DomainEventListener.of("Test", event -> l2Called.set(true));

            eventBus.subscribe("Test", listener1);
            eventBus.subscribe("Test", listener2);

            eventBus.unsubscribe("Test", listener1);

            eventBus.publish(createEvent("Test"));

            assertFalse(l1Called.get());
            assertTrue(l2Called.get());
        }

        @Test
        @DisplayName("unsubscribe by type with null does nothing")
        void unsubscribeByTypeNull() {
            assertDoesNotThrow(() -> eventBus.unsubscribe(null));
        }

        @Test
        @DisplayName("unsubscribe by listener with null does nothing")
        void unsubscribeByListenerNull() {
            assertDoesNotThrow(() -> eventBus.unsubscribe("Test", null));
        }

        @Test
        @DisplayName("unsubscribe by listener with null type does nothing")
        void unsubscribeByListenerNullType() {
            var listener = DomainEventListener.of("Test", event -> {});
            assertDoesNotThrow(() -> eventBus.unsubscribe(null, listener));
        }

        @Test
        @DisplayName("unsubscribe non-existent type does nothing")
        void unsubscribeNonExistentType() {
            assertDoesNotThrow(() -> eventBus.unsubscribe("NonExistent"));
        }
    }

    // ======================== hasListeners ========================

    @Nested
    @DisplayName("hasListeners")
    class HasListeners {

        @Test
        @DisplayName("hasListeners returns false for non-subscribed type")
        void hasListenersFalse() {
            assertFalse(eventBus.hasListeners("UnknownType"));
        }

        @Test
        @DisplayName("hasListeners returns true for subscribed type")
        void hasListenersTrue() {
            eventBus.subscribe("Test", event -> {});
            assertTrue(eventBus.hasListeners("Test"));
        }

        @Test
        @DisplayName("hasListeners returns false for null type")
        void hasListenersNull() {
            assertFalse(eventBus.hasListeners(null));
        }
    }

    // ======================== getListeners ========================

    @Nested
    @DisplayName("getListeners")
    class GetListeners {

        @Test
        @DisplayName("getListeners returns empty list for non-subscribed type")
        void getListenersEmpty() {
            List<DomainEventListener<?>> listeners = eventBus.getListeners("UnknownType");
            assertNotNull(listeners);
            assertTrue(listeners.isEmpty());
        }

        @Test
        @DisplayName("getListeners returns empty list for null type")
        void getListenersNull() {
            List<DomainEventListener<?>> listeners = eventBus.getListeners(null);
            assertNotNull(listeners);
            assertTrue(listeners.isEmpty());
        }

        @Test
        @DisplayName("getListeners returns unmodifiable list")
        void getListenersUnmodifiable() {
            eventBus.subscribe("Test", event -> {});
            List<DomainEventListener<?>> listeners = eventBus.getListeners("Test");
            assertThrows(UnsupportedOperationException.class, () -> listeners.add(event -> {}));
        }
    }

    // ======================== Clear ========================

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("clear removes all subscriptions")
        void clearRemovesAll() {
            eventBus.subscribe("A", event -> {});
            eventBus.subscribe("B", event -> {});
            eventBus.subscribe("C", event -> {});

            eventBus.clear();

            assertFalse(eventBus.hasListeners("A"));
            assertFalse(eventBus.hasListeners("B"));
            assertFalse(eventBus.hasListeners("C"));
        }

        @Test
        @DisplayName("clear on empty bus does nothing")
        void clearEmpty() {
            assertDoesNotThrow(() -> eventBus.clear());
        }
    }

    // ======================== Thread safety ========================

    @Nested
    @DisplayName("Thread safety")
    class ThreadSafety {

        @Test
        @DisplayName("concurrent subscribe and publish work correctly")
        void concurrentSubscribePublish() throws InterruptedException {
            int threadCount = 10;
            int eventsPerThread = 100;
            AtomicInteger totalReceived = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(threadCount);

            // Subscribe a counting listener
            eventBus.subscribe("ConcurrentTest", event -> totalReceived.incrementAndGet());

            // Publish events from multiple threads
            for (int t = 0; t < threadCount; t++) {
                new Thread(() -> {
                    try {
                        for (int i = 0; i < eventsPerThread; i++) {
                            eventBus.publish(createEvent("ConcurrentTest"));
                        }
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            latch.await();
            assertEquals(threadCount * eventsPerThread, totalReceived.get());
        }

        @Test
        @DisplayName("concurrent subscribe from multiple threads")
        void concurrentSubscribe() throws InterruptedException {
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int t = 0; t < threadCount; t++) {
                final int id = t;
                new Thread(() -> {
                    try {
                        eventBus.subscribe("SharedType", event -> {});
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            latch.await();
            assertEquals(threadCount, eventBus.getListeners("SharedType").size());
        }
    }
}
