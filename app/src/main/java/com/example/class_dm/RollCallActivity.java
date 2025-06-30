// 文件路径: com/example/class_dm/RollCallActivity.java
package com.example.class_dm;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.class_dm.adapter.RollCallAdapter;
import com.example.class_dm.database.AppDatabase;
import com.example.class_dm.database.Attendance;
import com.example.class_dm.database.Student;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RollCallActivity extends AppCompatActivity {

    private String currentClassName;
    private AppDatabase appDatabase;
    private RollCallAdapter rollCallAdapter;
    private List<Student> studentList;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    // 【新增/修改成员变量】
    private String courseName, date;
    private int startPeriod, endPeriod;
    private long currentSessionId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roll_call);
        // 【用新的方式获取所有信息】
        currentClassName = getIntent().getStringExtra("CLASS_NAME");
        currentSessionId = getIntent().getLongExtra("SESSION_ID", -1);
        courseName = getIntent().getStringExtra("COURSE_NAME");
        date = getIntent().getStringExtra("DATE");
        startPeriod = getIntent().getIntExtra("START_PERIOD", 0);
        endPeriod = getIntent().getIntExtra("END_PERIOD", 0);

        appDatabase = AppDatabase.getDatabase(this);

        // 获取当前日期
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // 设置Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_roll_call);
        toolbar.setTitle(courseName + " (" + date + ")");
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 设置“完成点名”按钮的点击事件
        Button btnFinish = findViewById(R.id.btn_finish_roll_call);
        btnFinish.setOnClickListener(v -> saveAttendanceRecords());

        // 加载学生并设置RecyclerView
        loadStudentsAndSetupAdapter();
    }

    private void loadStudentsAndSetupAdapter() {
        databaseExecutor.execute(() -> {
            studentList = appDatabase.studentDao().getStudentsByClass(currentClassName);
            handler.post(() -> {
                RecyclerView recyclerView = findViewById(R.id.recycler_view_roll_call);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                rollCallAdapter = new RollCallAdapter(studentList);
                recyclerView.setAdapter(rollCallAdapter);
            });
        });
    }

    private void saveAttendanceRecords() {
        if (rollCallAdapter == null) return;

        // 从适配器获取最终的点名结果
        Map<Integer, String> statusMap = rollCallAdapter.getAttendanceStatusMap();
        List<Attendance> attendanceRecords = new ArrayList<>();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // 将Map数据转换为List<Attendance>，准备存入数据库
        for (Map.Entry<Integer, String> entry : statusMap.entrySet()) {
            Attendance attendance = new Attendance();
            attendance.studentId = entry.getKey();
            attendance.className = currentClassName;
            attendance.date = currentDate;
            attendance.status = entry.getValue();
            // 【新增】设置新的字段
            attendance.date = this.date; // 使用从弹窗传入的日期
            attendance.courseName = this.courseName;
            attendance.startPeriod = this.startPeriod;
            attendance.endPeriod = this.endPeriod;
            attendanceRecords.add(attendance);
        }

        // 在后台线程执行数据库插入操作
        databaseExecutor.execute(() -> {
            // 注意：这里是简单地插入。如果需要防止同一天重复点名，需要先查询再插入或更新。
            // 为简化流程，我们先直接插入。
            appDatabase.attendanceDao().insertAll(attendanceRecords);
            handler.post(() -> {
                Toast.makeText(this, "点名记录已保存！", Toast.LENGTH_SHORT).show();
                finish(); // 保存成功后关闭点名页面
            });
        });
    }
}