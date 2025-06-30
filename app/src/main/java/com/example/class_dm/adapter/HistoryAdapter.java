// 文件路径: com/example/class_dm/adapter/HistoryAdapter.java
package com.example.class_dm.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // 【新增】
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.class_dm.R;
import com.example.class_dm.database.SessionSummary;
import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<SessionSummary> sessionList = new ArrayList<>();
    private OnItemClickListener clickListener;
    private OnDeleteClickListener deleteListener; // 【新增】

    public interface OnItemClickListener {
        void onItemClick(SessionSummary session);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    // 【新增】删除监听器接口和设置方法
    public interface OnDeleteClickListener {
        void onDeleteClick(SessionSummary session);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvSessionTime, tvSessionStats;
        ImageView ivDelete; // 【新增】删除图标的引用

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSessionTime = itemView.findViewById(R.id.tv_session_time);
            tvSessionStats = itemView.findViewById(R.id.tv_session_stats);
            ivDelete = itemView.findViewById(R.id.iv_delete_session); // 【新增】
        }
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        SessionSummary session = sessionList.get(position);
        holder.tvSessionTime.setText(session.getSessionTime());
        holder.tvSessionStats.setText(session.getStatistics());

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(session);
            }
        });

        // 【新增】为删除图标设置点击事件
        holder.ivDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(session);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    public void setSessionList(List<SessionSummary> list) {
        this.sessionList = list;
        notifyDataSetChanged();
    }
}