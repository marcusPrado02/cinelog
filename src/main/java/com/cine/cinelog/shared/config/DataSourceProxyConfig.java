package com.cine.cinelog.shared.config;

import com.cine.cinelog.shared.observability.RequestContext;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Configuration
public class DataSourceProxyConfig {

    // DataSource real criado pelo Spring Boot (Hikari usa este como "delegate")
    @Bean(name = "rawDataSource")
    public DataSource rawDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    // DataSource proxied: intercepta getConnection() para aplicar variáveis de
    // sessão
    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("rawDataSource") DataSource raw) {

        MethodExecutionListener setSessionVarsListener = new MethodExecutionListener() {
            @Override
            public void afterMethod(MethodExecutionContext ctx) {
                // intercepta chamadas ao DataSource#getConnection
                if (ctx.getTarget() instanceof DataSource
                        && "getConnection".equals(ctx.getMethod().getName())) {
                    Object result = ctx.getResult();
                    if (result instanceof Connection conn) {
                        applySessionVariables(conn);
                    }
                }
            }

            @Override
            public void beforeMethod(MethodExecutionContext executionContext) {
                // no-op
            }
        };

        return ProxyDataSourceBuilder.create(raw)
                .name("cinelog-ds")
                .methodListener(setSessionVarsListener)
                .build();
    }

    private static void applySessionVariables(Connection conn) {
        var ctx = RequestContext.get();
        try (Statement st = conn.createStatement()) {
            String sql = """
                    SET
                      @app_user_id    = %s,
                      @app_user_email = %s,
                      @source_app     = %s,
                      @app_version    = %s,
                      @trace_id       = %s,
                      @span_id        = %s,
                      @client_ip      = %s,
                      @user_agent     = %s,
                      @tx_id          = %s
                    """.formatted(
                    toSql(ctx != null ? ctx.userId : null),
                    toSql(ctx != null ? ctx.userEmail : null),
                    toSql(ctx != null ? ctx.sourceApp : "CineLog"),
                    toSql(ctx != null ? ctx.appVersion : "v1"),
                    toSql(ctx != null ? ctx.traceId : null),
                    toSql(ctx != null ? ctx.spanId : null),
                    toSql(ctx != null ? ctx.clientIp : null),
                    toSql(ctx != null ? ctx.userAgent : null),
                    toSql(ctx != null ? ctx.txId : null));
            st.execute(sql);
        } catch (Exception ignored) {
            // não quebra a requisição; auditoria cai com NULLs se falhar o SET
        }
    }

    private static String toSql(Object v) {
        if (v == null)
            return "NULL";
        String s = v.toString().replace("'", "''");
        return "'" + s + "'";
    }
}
