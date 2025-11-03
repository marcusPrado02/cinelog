package com.cine.cinelog.shared.observability;

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

    public RequestContext(Long userId, String userEmail, String sourceApp, String appVersion,
            String traceId, String spanId, String clientIp, String userAgent, String txId) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.sourceApp = sourceApp;
        this.appVersion = appVersion;
        this.traceId = traceId;
        this.spanId = spanId;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.txId = txId;
    }
}
