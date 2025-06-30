// 文件路径: com/example/class_dm/HistoryDetailsActivity.java
package com.example.class_dm;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.class_dm.adapter.HistoryDetailsAdapter;
import com.example.class_dm.database.AppDatabase;
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

        // 1. 从Intent中获取场次ID
        sessionId = getIntent().getLongExtra("SESSION_ID", -1);
        if (sessionId == -1) {
            Toast.makeText(this, "无法加载详情，场次ID无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. 初始化UI
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

        // 3. 加载数据
        loadAttendanceDetails();
    }

    private void loadAttendanceDetails() {
        databaseExecutor.execute(() -> {
            // 使用我们之前在DAO中定义好的方法
            List<AttendanceDetails> details = appDatabase.attendanceDao().getDetailsBySessionId(sessionId);
            handler.post(() -> adapter.setDetailsList(details));
        });
    }
}