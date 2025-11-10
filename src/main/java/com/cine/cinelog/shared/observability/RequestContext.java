package com.cine.cinelog.shared.observability;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
public final class RequestContext {
    private static final ThreadLocal<RequestContext> context = new ThreadLocal<>();

    public static void set(RequestContext c) {
        context.set(c);
    }

    public static RequestContext get() {
        return context.get();
    }

    public static void clear() {
        context.remove();
    }

    public final Long userId;
    public final String userEmail;
    public final String sourceApp;
    public final String appVersion;
    public final String traceId;
    public final String spanId;
    public final String clientIp;
    public final String userAgent;
    public final String txId;
    public final Boolean diagSql;

}
