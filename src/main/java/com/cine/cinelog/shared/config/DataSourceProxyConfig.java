package com.cine.cinelog.shared.config;

import com.cine.cinelog.shared.observability.RequestContext;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

/**
 * Configuração do DataSource que aplica variáveis de sessão por conexão.
 * Cada vez que uma conexão é obtida do DataSource, variáveis de sessão são
 * definidas com base no contexto da requisição atual.
 */
@Configuration
public class DataSourceProxyConfig {

    /**
     * DataSource bruto, sem proxy.
     * 
     * @param properties
     * @return
     */
    @Bean(name = "rawDataSource")
    public DataSource rawDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    /**
     * DataSource proxy que aplica variáveis de sessão.
     * 
     * @param raw
     * @return
     */
    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("rawDataSource") DataSource raw) {

        /**
         * Listener que aplica variáveis de sessão após a obtenção da conexão.
         */
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
            }
        };

        /**
         * Listener de logging SQL que registra execuções de queries.
         */
        var logListener = new SLF4JQueryLoggingListener();
        logListener.setLogLevel(SLF4JLogLevel.INFO);

        /**
         * Customizador de entradas de log SQL para o formato desejado.
         */
        var entryCreator = new DefaultQueryLogEntryCreator() {
            protected void writeQueryEntry(StringBuilder sb, String dataSourceName, String query, List<String> params,
                    long elapsedTime) {
                sb.append("sql_exec ds=").append(dataSourceName)
                        .append(" tookMs=").append(elapsedTime)
                        .append(" sql=").append(oneLine(query))
                        .append(" params=").append(params);
            }

            private String oneLine(String q) {
                return q == null ? "" : q.replaceAll("\\s+", " ");
            }
        };
        logListener.setQueryLogEntryCreator(entryCreator);

        return ProxyDataSourceBuilder.create(raw)
                .name("cinelog-ds")
                .methodListener(setSessionVarsListener)
                .listener(logListener)
                .build();
    }

    /**
     * Aplica variáveis de sessão na conexão com base no contexto da requisição.
     * 
     * @param conn
     */
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
