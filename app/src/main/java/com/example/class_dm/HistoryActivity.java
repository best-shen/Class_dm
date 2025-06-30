// 文件路径: com/example/class_dm/HistoryActivity.java
package com.example.class_dm;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.class_dm.adapter.HistoryAdapter;
import com.example.class_dm.database.AppDatabase;
import com.example.class_dm.database.AttendanceDetails;
import com.example.class_dm.database.HistorySessionInfo;
import com.example.class_dm.database.SessionSummary;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class HistoryActivity extends AppCompatActivity {

    private String currentClassName;
    private AppDatabase appDatabase;
    private HistoryAdapter historyAdapter;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        currentClassName = getIntent().getStringExtra("CLASS_NAME");
        appDatabase = AppDatabase.getDatabase(this);

        Toolbar toolbar = findViewById(R.id.toolbar_history);
        toolbar.setTitle(currentClassName + " - 历史考勤");
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recycler_view_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter();
        recyclerView.setAdapter(historyAdapter);

        historyAdapter.setOnItemClickListener(session -> {
            Intent intent = new Intent(HistoryActivity.this, HistoryDetailsActivity.class);
            intent.putExtra("SESSION_ID", session.getSessionId());
            startActivity(intent);
        });

        loadAllHistory();
    }

    private void loadAllHistory() {
        databaseExecutor.execute(() -> {
            // 1. 获取所有场次信息
            List<HistorySessionInfo> sessions = appDatabase.attendanceDao().getAllSessionsForClass(currentClassName);

            if (sessions.isEmpty()) {
                handler.post(() -> Toast.makeText(this, "该班级还没有任何考勤记录", Toast.LENGTH_SHORT).show());
                return;
            }

            List<SessionSummary> sessionSummaries = new ArrayList<>();

            // 2. 【请仔细核对这个循环】
            for (HistorySessionInfo sessionInfo : sessions) {

                // a. 【关键检查点1】
                //    这行代码必须在 for 循环内部，它为每个场次都重新查询一次详情。
                //    变量名必须是新的，比如 detailsForThisSession，避免和外部变量混淆。
                List<AttendanceDetails> detailsForThisSession = appDatabase.attendanceDao().getDetailsBySessionId(sessionInfo.sessionTimestamp);

                // b. 【关键检查点2】
                //    下面的所有 stream() 计算，都必须基于这个在循环内部刚刚创建的 `detailsForThisSession` 列表。
                long presentCount = detailsForThisSession.stream().filter(d -> d.status.equals("到课")).count();
                long absentCount = detailsForThisSession.stream().filter(d -> d.status.equals("缺勤")).count();
                long lateCount = detailsForThisSession.stream().filter(d -> d.status.equals("迟到")).count();
                long earlyCount = detailsForThisSession.stream().filter(d -> d.status.equals("早退")).count();
                long leaveCount = detailsForThisSession.stream().filter(d -> d.status.equals("请假")).count();

                // ... 后续格式化字符串和添加到 sessionSummaries 列表的代码 ...
                String sessionTitle = sessionInfo.courseName;
                String sessionSubtitle = String.format(Locale.CHINA, "%s  第%d-%d节",
                        sessionInfo.date, sessionInfo.startPeriod, sessionInfo.endPeriod);
                String statistics = String.format(Locale.CHINA,
                        "到课:%d | 缺勤:%d | 迟到:%d | 早退:%d | 请假:%d",
                        presentCount, absentCount, lateCount, earlyCount, leaveCount);

                sessionSummaries.add(new SessionSummary(sessionInfo.sessionTimestamp, sessionTitle + "\n" + sessionSubtitle, statistics));
            }

            // 3. 更新UI
            handler.post(() -> historyAdapter.setSessionList(sessionSummaries));
        });
    }
}