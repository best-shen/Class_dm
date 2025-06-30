// 文件路径: com/example/class_dm/HistoryDetailsActivity.java
package com.example.class_dm;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog; // 【新增】
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.class_dm.adapter.HistoryDetailsAdapter;
import com.example.class_dm.database.AppDatabase;
import com.example.class_dm.database.Attendance; // 【新增】
import com.example.class_dm.database.AttendanceDetails;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryDetailsActivity extends AppCompatActivity {

    private long sessionId;
    private AppDatabase appDatabase;
    private HistoryDetailsAdapter adapter;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_details);

        sessionId = getIntent().getLongExtra("SESSION_ID", -1);
        if (sessionId == -1) {
            Toast.makeText(this, "无法加载详情，场次ID无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar_history_details);
        toolbar.setTitle("点名详情");
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recycler_view_history_details);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryDetailsAdapter();
        recyclerView.setAdapter(adapter);

        appDatabase = AppDatabase.getDatabase(this);

        // 【核心修改】为Adapter设置点击监听器
        adapter.setOnItemClickListener(details -> {
            // 当一个学生被点击时，弹出状态选择对话框
            showStatusSelectionDialog(details);
        });

        loadAttendanceDetails();
    }

    private void loadAttendanceDetails() {
        databaseExecutor.execute(() -> {
            List<AttendanceDetails> details = appDatabase.attendanceDao().getDetailsBySessionId(sessionId);
            handler.post(() -> adapter.setDetailsList(details));
        });
    }

    // 【新增】显示状态选择对话框的方法
    private void showStatusSelectionDialog(final AttendanceDetails details) {
        // 定义所有可能的考勤状态
        final String[] statuses = {"到课", "缺勤", "迟到", "早退", "请假"};

        new AlertDialog.Builder(this)
                .setTitle("修改 " + details.studentName + " 的状态")
                // .setItems 用于显示一个简单的列表对话框
                .setItems(statuses, (dialog, which) -> {
                    // which 参数是用户点击的选项的索引
                    String newStatus = statuses[which];
                    // 调用更新数据库的方法
                    updateStudentStatus(details, newStatus);
                })
                .show();
    }

    // 【新增】更新学生状态到数据库的方法
    private void updateStudentStatus(final AttendanceDetails details, final String newStatus) {
        databaseExecutor.execute(() -> {
            // 1. 根据学生ID和场次ID，从数据库找到那条原始的 Attendance 记录
            Attendance recordToUpdate = appDatabase.attendanceDao().getAttendanceByStudentAndSession(details.studentId, sessionId);

            if (recordToUpdate != null) {
                // 2. 修改它的状态
                recordToUpdate.status = newStatus;
                // 3. 将修改后的对象更新回数据库
                appDatabase.attendanceDao().update(recordToUpdate);
                // 【新增】数据已发生变化，设置一个成功的返回结果
                setResult(RESULT_OK);
                // 4. 操作成功后，回到主线程刷新整个详情列表
                handler.post(this::loadAttendanceDetails);
            }
        });
    }
}
