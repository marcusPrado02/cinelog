package com.cine.cinelog.shared.config;

import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class DataSourceProxyConfigTest {

    @Test
    void toSql_handlesNullAndEscapes() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method toSql = DataSourceProxyConfig.class.getDeclaredMethod("toSql", Object.class);
        toSql.setAccessible(true);

        Object nullResult = toSql.invoke(null, new Object[] { null });
        assertEquals("NULL", nullResult);

        Object simple = toSql.invoke(null, "abc");
        assertEquals("'abc'", simple);

        Object escaped = toSql.invoke(null, "O'Reilly");
        assertEquals("'O''Reilly'", escaped);
    }

    @Test
    void applySessionVariables_doesNotThrow_whenStatementExecuteFails() throws Exception {
        Connection conn = mock(Connection.class);
        Statement st = mock(Statement.class);

        when(conn.createStatement()).thenReturn(st);
        doThrow(new SQLException("boom")).when(st).execute(anyString());

        Method apply = DataSourceProxyConfig.class.getDeclaredMethod("applySessionVariables", Connection.class);
        apply.setAccessible(true);

        // should not throw despite the statement.execute throwing
        assertDoesNotThrow(() -> apply.invoke(null, conn));

        verify(conn, times(1)).createStatement();
        verify(st, times(1)).execute(anyString());
    }

    @Test
    void dataSource_proxyInvokesApplySessionVariables_onGetConnection() throws Exception {
        DataSource raw = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        Statement st = mock(Statement.class);

        when(raw.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(st);

        DataSource proxied = new DataSourceProxyConfig().dataSource(raw);

        // invoke the proxied getConnection which should trigger the listener that calls
        // applySessionVariables
        Connection returned = proxied.getConnection();

        // returned may be a proxy or the same connection; interaction should have
        // caused a createStatement + execute
        verify(conn, atLeastOnce()).createStatement();
        verify(st, atLeastOnce()).execute(anyString());

        // ensure we got a connection back (not null)
        // also allow ProxyDataSource to return a proxied connection wrapper, so just
        // check non-null
        assertEquals(false, returned == null);
    }
}