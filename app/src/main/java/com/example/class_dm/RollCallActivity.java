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
import androidx.appcompat.app.AlertDialog; // 【新增】
import java.util.Random; // 【新增】
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

        // 【新增】设置“随机点名”按钮的点击事件
        Button btnRandom = findViewById(R.id.btn_random_roll_call);
        btnRandom.setOnClickListener(v -> performRandomRollCall());

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
        if (rollCallAdapter == null) {
            return;
        }

        Map<Integer, String> statusMap = rollCallAdapter.getAttendanceStatusMap();
        List<Attendance> attendanceRecords = new ArrayList<>();

        for (Map.Entry<Integer, String> entry : statusMap.entrySet()) {
            Attendance attendance = new Attendance();
            attendance.studentId = entry.getKey();
            attendance.className = currentClassName;
            attendance.status = entry.getValue();

            // 确保所有记录都使用了正确的、从上一个页面传来的场次信息
            attendance.sessionTimestamp = this.currentSessionId;
            attendance.date = this.date;
            attendance.courseName = this.courseName;
            attendance.startPeriod = this.startPeriod;
            attendance.endPeriod = this.endPeriod;

            attendanceRecords.add(attendance);
        }

        // 【核心改造】使用数据库事务来保证数据一致性
        databaseExecutor.execute(() -> {
            appDatabase.runInTransaction(() -> {
                // 1. 先根据场次ID删除该场次已存在的所有旧记录
                appDatabase.attendanceDao().deleteBySessionId(currentSessionId);
                // 2. 再插入本次点名的全新记录
                appDatabase.attendanceDao().insertAll(attendanceRecords);
            });

            // 操作完成后，在主线程提示用户
            handler.post(() -> {
                Toast.makeText(this, "点名记录已保存！", Toast.LENGTH_SHORT).show();
                finish(); // 保存成功后关闭点名页面
            });
        });
    }
    private void performRandomRollCall() {
        if (studentList == null || studentList.isEmpty()) {
            Toast.makeText(this, "班级中没有学生，无法随机点名", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. 随机抽取一名学生
        int randomIndex = new Random().nextInt(studentList.size());
        Student selectedStudent = studentList.get(randomIndex);

        // 2. 弹窗显示学生姓名
        new AlertDialog.Builder(this)
                .setTitle("随机选中")
                .setMessage("选中学员: " + selectedStudent.name)
                .setPositiveButton("确定", null)
                .show();

        // 3. 高亮显示并滚动到该学生位置
        // a. 让Adapter知道要高亮哪一项
        rollCallAdapter.setHighlightedPosition(randomIndex);

        // b. 让RecyclerView平滑地滚动到那一项
        RecyclerView recyclerView = findViewById(R.id.recycler_view_roll_call);
        recyclerView.smoothScrollToPosition(randomIndex);
    }
}