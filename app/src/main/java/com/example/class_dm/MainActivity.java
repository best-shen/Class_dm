// 文件路径: com/example/class_dm/MainActivity.java
package com.example.class_dm;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import com.example.class_dm.adapter.ClassAdapter;
import com.example.class_dm.database.AppDatabase;
import com.example.class_dm.database.ClassInfo;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.content.Intent;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private AppDatabase appDatabase;
    private ClassAdapter classAdapter;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化数据库实例
        appDatabase = AppDatabase.getDatabase(this);

        // 初始化RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler_view_classes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        classAdapter = new ClassAdapter();
        recyclerView.setAdapter(classAdapter);

        // 设置删除监听器
        classAdapter.setOnItemDeleteListener(this::showDeleteConfirmationDialog);

        // 设置列表项点击监听器
        classAdapter.setOnItemClickListener(classInfo -> {
            // 创建意图（Intent）以启动 StudentActivity
            Intent intent = new Intent(MainActivity.this, StudentActivity.class);
            // 将被点击的班级的名称传递给下一个Activity
            intent.putExtra("CLASS_NAME", classInfo.name);
            startActivity(intent);
        });
        // 设置悬浮按钮点击事件
        FloatingActionButton fab = findViewById(R.id.fab_add_class);
        fab.setOnClickListener(view -> showAddClassDialog());

        // 加载班级数据
        loadClasses();
    }

    private void loadClasses() {
        databaseExecutor.execute(() -> {
            List<ClassInfo> classList = appDatabase.classDao().getAllClasses();
            handler.post(() -> classAdapter.setClasses(classList));
        });
    }

    private void showAddClassDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加新班级");

        // 使用自定义布局
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_class, null);
        final EditText etClassName = dialogView.findViewById(R.id.et_class_name);
        builder.setView(dialogView);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String className = etClassName.getText().toString().trim();
            if (TextUtils.isEmpty(className)) {
                Toast.makeText(this, "班级名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            ClassInfo newClass = new ClassInfo();
            newClass.name = className;

            databaseExecutor.execute(() -> {
                appDatabase.classDao().insert(newClass);
                // 操作完成后回到主线程刷新列表
                handler.post(this::loadClasses);
            });
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDeleteConfirmationDialog(final ClassInfo classInfo) {
        new AlertDialog.Builder(this)
                .setTitle("删除班级")
                .setMessage("确定要删除班级 \"" + classInfo.name + "\" 吗？\n删除后，该班级下的所有学生信息也将被一并删除。")
                .setPositiveButton("删除", (dialog, which) -> {
                    deleteClass(classInfo);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteClass(ClassInfo classInfo) {
        databaseExecutor.execute(() -> {
            appDatabase.classDao().delete(classInfo);
            // 操作完成后回到主线程刷新列表
            handler.post(this::loadClasses);
        });
    }
}