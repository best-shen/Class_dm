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
import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher; // 【新增】
import androidx.activity.result.contract.ActivityResultContracts; // 【新增】
public class HistoryActivity extends AppCompatActivity {

    private String currentClassName;
    private AppDatabase appDatabase;
    private HistoryAdapter historyAdapter;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<Intent> detailsLauncher; // 【新增】
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        currentClassName = getIntent().getStringExtra("CLASS_NAME");
        appDatabase = AppDatabase.getDatabase(this);
        // 【新增】初始化Launcher。当从详情页返回时，无论结果如何，都调用loadAllHistory()刷新列表
        detailsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> loadAllHistory()
        );
        Toolbar toolbar = findViewById(R.id.toolbar_history);
        toolbar.setTitle(currentClassName + " - 历史考勤");
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recycler_view_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter();
        recyclerView.setAdapter(historyAdapter);

        // 【修改】将原来的startActivity(intent)改为使用launcher启动
        historyAdapter.setOnItemClickListener(session -> {
            Intent intent = new Intent(HistoryActivity.this, HistoryDetailsActivity.class);
            intent.putExtra("SESSION_ID", session.getSessionId());
            detailsLauncher.launch(intent); // 【修改】
        });

        // 【新增】设置删除按钮监听器
        historyAdapter.setOnDeleteClickListener(session -> {
            // 弹出一个确认对话框，防止误删
            new AlertDialog.Builder(this)
                    .setTitle("删除记录")
                    .setMessage("确定要删除这次的点名记录吗？\n" + session.getSessionTime())
                    .setPositiveButton("删除", (dialog, which) -> deleteSession(session.getSessionId()))
                    .setNegativeButton("取消", null)
                    .show();
        });

        loadAllHistory();
    }
    // 【新增】删除场次记录的方法
    private void deleteSession(long sessionId) {
        databaseExecutor.execute(() -> {
            appDatabase.attendanceDao().deleteBySessionId(sessionId);

            // 【核心修改】在后台任务完成删除后，回到主线程做两件事：
            handler.post(() -> {
                // 1. 给出正确的成功提示
                Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                // 2. 再去重新加载数据，刷新界面
                loadAllHistory();
            });
        });
    }
    // 在 HistoryActivity.java 中
    private void loadAllHistory() {
        databaseExecutor.execute(() -> {
            // 1. 正常从数据库获取所有场次信息
            List<HistorySessionInfo> sessions = appDatabase.attendanceDao().getAllSessionsForClass(currentClassName);

            // 2. 准备一个（可能是空的）列表，用于存放最终展示在界面上的卡片信息
            List<SessionSummary> sessionSummaries = new ArrayList<>();

            // 3. 【关键】如果sessions列表不为空，才进行遍历和数据处理
            if (!sessions.isEmpty()) {
                for (HistorySessionInfo sessionInfo : sessions) {
                    // ... 这部分内部逻辑完全不变 ...
                    List<AttendanceDetails> detailsForThisSession = appDatabase.attendanceDao().getDetailsBySessionId(sessionInfo.sessionTimestamp);
                    long presentCount = detailsForThisSession.stream().filter(d -> d.status.equals("到课")).count();
                    long absentCount = detailsForThisSession.stream().filter(d -> d.status.equals("缺勤")).count();
                    long lateCount = detailsForThisSession.stream().filter(d -> d.status.equals("迟到")).count();
                    long earlyCount = detailsForThisSession.stream().filter(d -> d.status.equals("早退")).count();
                    long leaveCount = detailsForThisSession.stream().filter(d -> d.status.equals("请假")).count();
                    String sessionTitle = sessionInfo.courseName;
                    String sessionSubtitle = String.format(Locale.CHINA, "%s  第%d-%d节",
                            sessionInfo.date, sessionInfo.startPeriod, sessionInfo.endPeriod);
                    String statistics = String.format(Locale.CHINA,
                            "到课:%d | 缺勤:%d | 迟到:%d | 早退:%d | 请假:%d",
                            presentCount, absentCount, lateCount, earlyCount, leaveCount);
                    sessionSummaries.add(new SessionSummary(sessionInfo.sessionTimestamp, sessionTitle + "\n" + sessionSubtitle, statistics));
                }
            }

            // 4. 【关键】无论sessionSummaries是否为空，都把这个最终的列表交给Adapter去刷新UI。
            // 如果列表是空的，RecyclerView就会被清空。
            handler.post(() -> historyAdapter.setSessionList(sessionSummaries));
        });
    }
}