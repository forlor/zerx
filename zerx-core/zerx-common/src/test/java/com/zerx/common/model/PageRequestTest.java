package com.zerx.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PageRequest Tests")
class PageRequestTest {

    // ======================== Compact Constructor Tests ========================

    @Nested
    @DisplayName("Compact Constructor - Boundary Validation")
    class CompactConstructorTest {

        @Test
        @DisplayName("Should accept valid page and size")
        void shouldAcceptValidPageAndSize() {
            PageRequest request = new PageRequest(1, 10);
            assertEquals(1, request.page());
            assertEquals(10, request.size());
        }

        @Test
        @DisplayName("Should clamp page to 1 when page is 0")
        void shouldClampPageWhenZero() {
            PageRequest request = new PageRequest(0, 10);
            assertEquals(1, request.page());
        }

        @Test
        @DisplayName("Should clamp page to 1 when page is negative")
        void shouldClampPageWhenNegative() {
            PageRequest request = new PageRequest(-5, 10);
            assertEquals(1, request.page());
        }

        @Test
        @DisplayName("Should clamp size to 1 when size is 0")
        void shouldClampSizeWhenZero() {
            PageRequest request = new PageRequest(1, 0);
            assertEquals(10, request.size()); // DEFAULT_SIZE
        }

        @Test
        @DisplayName("Should clamp size to 1 when size is negative")
        void shouldClampSizeWhenNegative() {
            PageRequest request = new PageRequest(1, -10);
            assertEquals(10, request.size()); // DEFAULT_SIZE
        }

        @Test
        @DisplayName("Should clamp size to MAX_SIZE (200) when size exceeds max")
        void shouldClampSizeWhenExceedsMax() {
            PageRequest request = new PageRequest(1, 500);
            assertEquals(200, request.size());
        }

        @Test
        @DisplayName("Should accept size exactly at MAX_SIZE (200)")
        void shouldAcceptSizeAtMax() {
            PageRequest request = new PageRequest(1, 200);
            assertEquals(200, request.size());
        }

        @Test
        @DisplayName("Should handle null orders by defaulting to empty list")
        void shouldHandleNullOrders() {
            PageRequest request = new PageRequest(1, 10, null);
            assertNotNull(request.orders());
            assertTrue(request.orders().isEmpty());
        }

        @Test
        @DisplayName("Should create defensive copy of orders list")
        void shouldCreateDefensiveCopyOfOrders() {
            List<PageRequest.OrderItem> originalOrders = new java.util.ArrayList<>(
                    List.of(new PageRequest.OrderItem("name"))
            );
            PageRequest request = new PageRequest(1, 10, originalOrders);
            assertNotSame(originalOrders, request.orders());
            assertEquals(originalOrders, request.orders());
        }

        @Test
        @DisplayName("Should clamp both page and size simultaneously when invalid")
        void shouldClampBothPageAndSize() {
            PageRequest request = new PageRequest(-1, 999);
            assertEquals(1, request.page());
            assertEquals(200, request.size());
        }
    }

    // ======================== Default Constructor Tests ========================

    @Nested
    @DisplayName("Constructors")
    class ConstructorTest {

        @Test
        @DisplayName("Default constructor should use page=1, size=10, empty orders")
        void defaultConstructor() {
            PageRequest request = new PageRequest();
            assertEquals(1, request.page());
            assertEquals(10, request.size());
            assertNotNull(request.orders());
            assertTrue(request.orders().isEmpty());
        }

        @Test
        @DisplayName("Two-arg constructor should use null orders")
        void twoArgConstructor() {
            PageRequest request = new PageRequest(2, 20);
            assertEquals(2, request.page());
            assertEquals(20, request.size());
            assertNotNull(request.orders());
            assertTrue(request.orders().isEmpty());
        }

        @Test
        @DisplayName("Three-arg constructor with orders")
        void threeArgConstructorWithOrders() {
            List<PageRequest.OrderItem> orders = List.of(
                    new PageRequest.OrderItem("name", PageRequest.Direction.ASC),
                    new PageRequest.OrderItem("age", PageRequest.Direction.DESC)
            );
            PageRequest request = new PageRequest(3, 50, orders);
            assertEquals(3, request.page());
            assertEquals(50, request.size());
            assertEquals(2, request.orders().size());
        }
    }

    // ======================== Offset Tests ========================

    @Nested
    @DisplayName("offset() Tests")
    class OffsetTest {

        @Test
        @DisplayName("Offset should be 0 for page 1")
        void offsetPage1() {
            PageRequest request = new PageRequest(1, 10);
            assertEquals(0, request.offset());
        }

        @Test
        @DisplayName("Offset should be 10 for page 2 with size 10")
        void offsetPage2() {
            PageRequest request = new PageRequest(2, 10);
            assertEquals(10, request.offset());
        }

        @Test
        @DisplayName("Offset should be 20 for page 3 with size 10")
        void offsetPage3() {
            PageRequest request = new PageRequest(3, 10);
            assertEquals(20, request.offset());
        }

        @Test
        @DisplayName("Offset should handle different size")
        void offsetWithDifferentSize() {
            PageRequest request = new PageRequest(5, 25);
            assertEquals(100, request.offset());
        }
    }

    // ======================== OrderItem Tests ========================

    @Nested
    @DisplayName("OrderItem Tests")
    class OrderItemTest {

        @Test
        @DisplayName("Should create OrderItem with field and direction")
        void createWithFieldAndDirection() {
            PageRequest.OrderItem item = new PageRequest.OrderItem("name", PageRequest.Direction.DESC);
            assertEquals("name", item.field());
            assertEquals(PageRequest.Direction.DESC, item.direction());
        }

        @Test
        @DisplayName("Single-arg constructor should default to ASC")
        void defaultDirectionIsAsc() {
            PageRequest.OrderItem item = new PageRequest.OrderItem("age");
            assertEquals("age", item.field());
            assertEquals(PageRequest.Direction.ASC, item.direction());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for null field")
        void nullFieldThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PageRequest.OrderItem(null, PageRequest.Direction.ASC));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for blank field")
        void blankFieldThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PageRequest.OrderItem("   ", PageRequest.Direction.ASC));
        }

        @Test
        @DisplayName("Should default to ASC when direction is null")
        void nullDirectionDefaultsToAsc() {
            PageRequest.OrderItem item = new PageRequest.OrderItem("name", null);
            assertEquals(PageRequest.Direction.ASC, item.direction());
        }

        @Test
        @DisplayName("OrderItem equals and hashCode")
        void orderItemEqualsHashCode() {
            PageRequest.OrderItem item1 = new PageRequest.OrderItem("name", PageRequest.Direction.ASC);
            PageRequest.OrderItem item2 = new PageRequest.OrderItem("name", PageRequest.Direction.ASC);
            PageRequest.OrderItem item3 = new PageRequest.OrderItem("name", PageRequest.Direction.DESC);
            assertEquals(item1, item2);
            assertEquals(item1.hashCode(), item2.hashCode());
            assertNotEquals(item1, item3);
        }

        @Test
        @DisplayName("OrderItem toString")
        void orderItemToString() {
            PageRequest.OrderItem item = new PageRequest.OrderItem("name", PageRequest.Direction.ASC);
            String str = item.toString();
            assertNotNull(str);
            assertTrue(str.contains("name"));
            assertTrue(str.contains("ASC"));
        }
    }

    // ======================== Direction Enum Tests ========================

    @Nested
    @DisplayName("Direction Enum Tests")
    class DirectionTest {

        @Test
        @DisplayName("Direction should have ASC and DESC values")
        void directionValues() {
            PageRequest.Direction[] values = PageRequest.Direction.values();
            assertEquals(2, values.length);
            assertEquals(PageRequest.Direction.ASC, values[0]);
            assertEquals(PageRequest.Direction.DESC, values[1]);
        }

        @Test
        @DisplayName("Direction valueOf should work correctly")
        void directionValueOf() {
            assertEquals(PageRequest.Direction.ASC, PageRequest.Direction.valueOf("ASC"));
            assertEquals(PageRequest.Direction.DESC, PageRequest.Direction.valueOf("DESC"));
            assertThrows(IllegalArgumentException.class, () -> PageRequest.Direction.valueOf("INVALID"));
        }
    }

    // ======================== Equals / HashCode / ToString ========================

    @Nested
    @DisplayName("Equals, HashCode, ToString")
    class ObjectMethodsTest {

        @Test
        @DisplayName("Equal PageRequests should have same hashCode")
        void equalInstances() {
            PageRequest r1 = new PageRequest(1, 10, List.of(new PageRequest.OrderItem("name")));
            PageRequest r2 = new PageRequest(1, 10, List.of(new PageRequest.OrderItem("name")));
            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
        }

        @Test
        @DisplayName("Different PageRequests should not be equal")
        void differentInstances() {
            PageRequest r1 = new PageRequest(1, 10);
            PageRequest r2 = new PageRequest(2, 10);
            assertNotEquals(r1, r2);
        }

        @Test
        @DisplayName("Should not equal null")
        void notEqualNull() {
            PageRequest request = new PageRequest();
            assertNotEquals(null, request);
        }

        @Test
        @DisplayName("Should not equal different type")
        void notEqualDifferentType() {
            PageRequest request = new PageRequest();
            assertNotEquals("string", request);
        }

        @Test
        @DisplayName("ToString should contain page, size, and orders info")
        void toStringContainsFields() {
            PageRequest request = new PageRequest(2, 20, List.of(new PageRequest.OrderItem("id")));
            String str = request.toString();
            assertNotNull(str);
            assertTrue(str.contains("2"));
            assertTrue(str.contains("20"));
        }

        @Test
        @DisplayName("Same instance should be equal")
        void sameInstanceEqual() {
            PageRequest request = new PageRequest();
            assertEquals(request, request);
        }
    }

    // ======================== Constants Tests ========================

    @Nested
    @DisplayName("Constants")
    class ConstantsTest {

        @Test
        @DisplayName("Should have correct default constants")
        void constants() {
            assertEquals(1, PageRequest.DEFAULT_PAGE);
            assertEquals(10, PageRequest.DEFAULT_SIZE);
            assertEquals(200, PageRequest.MAX_SIZE);
        }
    }
}
