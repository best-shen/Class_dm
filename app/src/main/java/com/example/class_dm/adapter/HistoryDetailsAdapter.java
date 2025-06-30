// 文件路径: com/example/class_dm/adapter/HistoryDetailsAdapter.java
package com.example.class_dm.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.class_dm.R;
import com.example.class_dm.database.AttendanceDetails; // 复用这个数据模型
import java.util.ArrayList;
import java.util.List;

public class HistoryDetailsAdapter extends RecyclerView.Adapter<HistoryDetailsAdapter.DetailsViewHolder> {

    private List<AttendanceDetails> detailsList = new ArrayList<>();
    private OnItemClickListener listener; // 【新增】
    // 【新增】定义一个监听器接口
    public interface OnItemClickListener {
        void onItemClick(AttendanceDetails details);
    }

    // 【新增】提供一个设置监听器的方法
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    @NonNull
    @Override
    public DetailsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_details, parent, false);
        return new DetailsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DetailsViewHolder holder, int position) {
        AttendanceDetails details = detailsList.get(position);
        holder.tvStudentName.setText(details.studentName);
        holder.tvStudentNumber.setText(details.studentNumber);
        holder.tvStatus.setText(details.status);
        // 【新增】为列表项（itemView）设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(details);
            }
        });
        // 根据不同状态设置不同颜色，保持体验一致
        switch (details.status) {
            case "到课":
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // 绿色
                break;
            case "缺勤":
                holder.tvStatus.setTextColor(Color.parseColor("#F44336")); // 红色
                break;
            case "迟到":
                holder.tvStatus.setTextColor(Color.parseColor("#FF9800")); // 橙色
                break;
            case "早退":
                holder.tvStatus.setTextColor(Color.parseColor("#9C27B0")); // 中等紫色
                break;
            case "请假":
                holder.tvStatus.setTextColor(Color.parseColor("#03A9F4")); // 蓝色
                break;
            default:
                holder.tvStatus.setTextColor(Color.BLACK);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return detailsList.size();
    }

    public void setDetailsList(List<AttendanceDetails> list) {
        this.detailsList = list;
        notifyDataSetChanged();
    }

    static class DetailsViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvStudentNumber, tvStatus;

        public DetailsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tv_student_name_details);
            tvStudentNumber = itemView.findViewById(R.id.tv_student_number_details);
            tvStatus = itemView.findViewById(R.id.tv_status_details);
        }
    }
}