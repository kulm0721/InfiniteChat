package com.shanyangcode.aiservice.monitor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MonitorContextHolder {

    private static final ThreadLocal<MonitorContext> CONTEXT_HOLDER = new InheritableThreadLocal<>();

    public static MonitorContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    public static void setContext(MonitorContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static void clearContext() {
        CONTEXT_HOLDER.remove();
    }
}
