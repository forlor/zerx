package com.zerx.spring.data.datascope;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataScopeInterceptorTest {

    @Mock
    private DataScopeHandler dataScopeHandler;

    @Mock
    private DataScopeUserProvider userProvider;

    @Mock
    private ProceedingJoinPoint joinPoint;

    private DataScopeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new DataScopeInterceptor(dataScopeHandler, userProvider);
    }

    @AfterEach
    void tearDown() {
        DataScopeContext.clear();
    }

    private DataScope createMockDataScope(String column, DataScope.Type type) {
        return new DataScope() {
            @Override
            public String column() { return column; }
            @Override
            public Type type() { return type; }
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return DataScope.class; }
        };
    }

    @Nested
    @DisplayName("around(ProceedingJoinPoint, DataScope)")
    class AroundMethod {

        @Test
        @DisplayName("proceeds directly when no user context is present")
        void proceedsDirectly_whenNoUserContext() throws Throwable {
            DataScope dataScope = createMockDataScope("dept_id", DataScope.Type.DEPT);
            when(userProvider.getCurrentUser()).thenReturn(Optional.empty());
            Object expectedResult = new Object();
            when(joinPoint.proceed()).thenReturn(expectedResult);

            Object result = interceptor.around(joinPoint, dataScope);

            assertEquals(expectedResult, result);
            verify(joinPoint).proceed();
            verify(dataScopeHandler, never()).generateCondition(any(), any());
            assertNull(DataScopeContext.current());
        }

        @Test
        @DisplayName("applies data scope filter when user is present and condition is returned")
        void appliesDataScope_whenUserPresent() throws Throwable {
            DataScope dataScope = createMockDataScope("dept_id", DataScope.Type.DEPT);
            DataScopeUser user = new DataScopeUser(100L, List.of(1L), List.of("admin"));
            DataScopeHandler.DataScopeSql sqlCondition =
                    new DataScopeHandler.DataScopeSql("dept_id IN (?,?,?)", List.of(1L, 2L, 3L));

            when(userProvider.getCurrentUser()).thenReturn(Optional.of(user));
            when(dataScopeHandler.generateCondition(dataScope, user)).thenReturn(sqlCondition);
            Object expectedResult = new Object();
            when(joinPoint.proceed()).thenReturn(expectedResult);

            Object result = interceptor.around(joinPoint, dataScope);

            assertEquals(expectedResult, result);
            verify(userProvider).getCurrentUser();
            verify(dataScopeHandler).generateCondition(dataScope, user);
            verify(joinPoint).proceed();
            assertNull(DataScopeContext.current(),
                    "DataScopeContext should be cleared after execution in finally block");
        }

        @Test
        @DisplayName("skips filtering when handler returns null (ALL scope type)")
        void skipsFiltering_whenAllScopeType() throws Throwable {
            DataScope dataScope = createMockDataScope("dept_id", DataScope.Type.ALL);
            DataScopeUser user = new DataScopeUser(100L, List.of(1L), List.of("admin"));

            when(userProvider.getCurrentUser()).thenReturn(Optional.of(user));
            when(dataScopeHandler.generateCondition(dataScope, user)).thenReturn(null);
            Object expectedResult = new Object();
            when(joinPoint.proceed()).thenReturn(expectedResult);

            Object result = interceptor.around(joinPoint, dataScope);

            assertEquals(expectedResult, result);
            verify(userProvider).getCurrentUser();
            verify(dataScopeHandler).generateCondition(dataScope, user);
            verify(joinPoint).proceed();
            assertNull(DataScopeContext.current());
        }

        @Test
        @DisplayName("clears context even when proceed throws an exception")
        void clearsContext_evenOnException() throws Throwable {
            DataScope dataScope = createMockDataScope("dept_id", DataScope.Type.DEPT);
            DataScopeUser user = new DataScopeUser(100L, List.of(1L), List.of("admin"));
            DataScopeHandler.DataScopeSql sqlCondition =
                    new DataScopeHandler.DataScopeSql("dept_id IN (?)", List.of(1L));

            when(userProvider.getCurrentUser()).thenReturn(Optional.of(user));
            when(dataScopeHandler.generateCondition(dataScope, user)).thenReturn(sqlCondition);
            RuntimeException expectedException = new RuntimeException("test error");
            when(joinPoint.proceed()).thenThrow(expectedException);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> interceptor.around(joinPoint, dataScope));

            assertEquals("test error", thrown.getMessage());
            // Context should be cleared in finally block even after exception
            assertNull(DataScopeContext.current(),
                    "DataScopeContext must be cleared even when an exception occurs");
        }

        @Test
        @DisplayName("sets DataScopeContext with correct SQL before proceeding")
        void setsContextWithCorrectSql_beforeProceeding() throws Throwable {
            DataScope dataScope = createMockDataScope("dept_id", DataScope.Type.DEPT);
            DataScopeUser user = new DataScopeUser(100L, List.of(1L), List.of("admin"));
            String expectedSql = "dept_id IN (?,?,?)";
            List<Object> expectedParams = List.of(1L, 2L, 3L);
            DataScopeHandler.DataScopeSql sqlCondition =
                    new DataScopeHandler.DataScopeSql(expectedSql, expectedParams);

            when(userProvider.getCurrentUser()).thenReturn(Optional.of(user));
            when(dataScopeHandler.generateCondition(dataScope, user)).thenReturn(sqlCondition);

            // Use an Answer to inspect DataScopeContext at the exact moment proceed() is called
            when(joinPoint.proceed()).thenAnswer(invocation -> {
                DataScopeHandler.DataScopeSql contextSql = DataScopeContext.current();
                assertNotNull(contextSql, "DataScopeContext should be set when proceed() is invoked");
                assertEquals(expectedSql, contextSql.condition());
                assertEquals(expectedParams, contextSql.params());
                return "ok";
            });

            Object result = interceptor.around(joinPoint, dataScope);

            assertEquals("ok", result);
            assertNull(DataScopeContext.current(),
                    "DataScopeContext should be cleared after execution");
        }

        @Test
        @DisplayName("passes through proceed result unchanged")
        void passesThroughProceedResult_unchanged() throws Throwable {
            DataScope dataScope = createMockDataScope("dept_id", DataScope.Type.SELF);
            DataScopeUser user = new DataScopeUser(100L, List.of(1L), List.of("admin"));
            DataScopeHandler.DataScopeSql sqlCondition =
                    new DataScopeHandler.DataScopeSql("create_by = ?", List.of(100L));

            when(userProvider.getCurrentUser()).thenReturn(Optional.of(user));
            when(dataScopeHandler.generateCondition(dataScope, user)).thenReturn(sqlCondition);
            String expectedResult = "query-result";
            when(joinPoint.proceed()).thenReturn(expectedResult);

            Object result = interceptor.around(joinPoint, dataScope);

            assertEquals(expectedResult, result);
        }
    }
}
