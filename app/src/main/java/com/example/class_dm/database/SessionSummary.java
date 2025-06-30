// 文件路径: com/example/class_dm/database/SessionSummary.java
package com.example.class_dm.database;

// 用于封装一个点名场次的概要信息，方便在Adapter中使用
public class SessionSummary {
    private final long sessionId;
    private final String sessionTime;
    private final String statistics;

    public SessionSummary(long sessionId, String sessionTime, String statistics) {
        this.sessionId = sessionId;
        this.sessionTime = sessionTime;
        this.statistics = statistics;
    }

    public long getSessionId() {
        return sessionId;
    }

    public String getSessionTime() {
        return sessionTime;
    }

    public String getStatistics() {
        return statistics;
    }
}