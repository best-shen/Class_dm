// 文件路径: com/example/class_dm/adapter/RollCallAdapter.java
package com.example.class_dm.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.class_dm.R;
import com.example.class_dm.database.Student;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.graphics.Color; // 【新增】
public class RollCallAdapter extends RecyclerView.Adapter<RollCallAdapter.RollCallViewHolder> {

    private final List<Student> studentList;
    // 使用一个Map来存储每个学生ID对应的考勤状态，默认为“到课”
    private final Map<Integer, String> attendanceStatusMap = new HashMap<>();
    private int highlightedPosition = -1; // 【新增】用于记录高亮位置，-1代表无高亮
    public static final String STATUS_PRESENT = "到课";
    public static final String STATUS_ABSENT = "缺勤";
    public static final String STATUS_LATE = "迟到";
    public static final String STATUS_LEAVE_EARLY = "早退";
    public static final String STATUS_LEAVE_OF_ABSENCE = "请假";

    public RollCallAdapter(List<Student> studentList) {
        this.studentList = studentList;
        // 初始化所有学生的状态为“到课”
        for (Student student : studentList) {
            attendanceStatusMap.put(student.id, STATUS_PRESENT);
        }
    }

    @NonNull
    @Override
    public RollCallViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_roll_call, parent, false);
        return new RollCallViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RollCallViewHolder holder, int position) {
        Student currentStudent = studentList.get(position);
        holder.tvStudentName.setText(currentStudent.name + " (" + currentStudent.studentNumber + ")");

        // 关键：为RadioGroup设置监听器，当用户选择不同状态时，更新Map中的记录
        holder.rgStatus.setOnCheckedChangeListener(null); // 先移除旧的监听器，防止复用出错
        // 根据Map中的状态，设置哪个RadioButton被选中
        // 【新增】根据是否为高亮位置，设置背景颜色
        if (position == highlightedPosition) {
            holder.itemView.setBackgroundColor(Color.parseColor("#E0F7FA")); // 设置一个淡蓝色作为高亮
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE); // 恢复默认白色背景
        }
        String status = attendanceStatusMap.get(currentStudent.id);
        if (status != null) {
            switch (status) {
                case STATUS_PRESENT: holder.rgStatus.check(R.id.rb_present); break;
                case STATUS_ABSENT: holder.rgStatus.check(R.id.rb_absent); break;
                case STATUS_LATE: holder.rgStatus.check(R.id.rb_late); break;
                case STATUS_LEAVE_EARLY: holder.rgStatus.check(R.id.rb_leave_early); break;
                case STATUS_LEAVE_OF_ABSENCE: holder.rgStatus.check(R.id.rb_leave_of_absence); break;
            }
        }

        holder.rgStatus.setOnCheckedChangeListener((group, checkedId) -> {
            String newStatus = STATUS_PRESENT;
            if (checkedId == R.id.rb_present) {
                newStatus = STATUS_PRESENT;
            } else if (checkedId == R.id.rb_absent) {
                newStatus = STATUS_ABSENT;
            } else if (checkedId == R.id.rb_late) {
                newStatus = STATUS_LATE;
            } else if (checkedId == R.id.rb_leave_early) {
                newStatus = STATUS_LEAVE_EARLY;
            } else if (checkedId == R.id.rb_leave_of_absence) {
                newStatus = STATUS_LEAVE_OF_ABSENCE;
            }
            attendanceStatusMap.put(currentStudent.id, newStatus);
        });
    }
    // 【新增】提供一个设置高亮位置并刷新的方法
    public void setHighlightedPosition(int position) {
        int previousPosition = this.highlightedPosition;
        this.highlightedPosition = position;

        // 刷新之前高亮和当前高亮的项
        if (previousPosition != -1) {
            notifyItemChanged(previousPosition);
        }
        if (highlightedPosition != -1) {
            notifyItemChanged(highlightedPosition);
        }
    }
    @Override
    public int getItemCount() {
        return studentList.size();
    }

    // 提供一个方法给Activity，用于获取最终的点名结果
    public Map<Integer, String> getAttendanceStatusMap() {
        return attendanceStatusMap;
    }

    static class RollCallViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName;
        RadioGroup rgStatus;

        public RollCallViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tv_student_name_roll_call);
            rgStatus = itemView.findViewById(R.id.rg_status);
        }
    }
}