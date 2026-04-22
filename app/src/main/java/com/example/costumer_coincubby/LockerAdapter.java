package com.example.costumer_coincubby;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class LockerAdapter extends RecyclerView.Adapter<LockerAdapter.ViewHolder> {

    private final List<Locker> lockers;
    private final OnLockerClickListener listener;

    public interface OnLockerClickListener {
        void onLockerClick(Locker locker);
    }

    public LockerAdapter(List<Locker> lockers, OnLockerClickListener listener) {
        this.lockers = lockers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_locker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Locker locker = lockers.get(position);
        holder.idText.setText(locker.getId());
        holder.sizeText.setText(locker.getSize());
        
        Context context = holder.itemView.getContext();
        int statusColor;
        int bgColor;
        int strokeColor;

        switch (locker.getStatus()) {
            case AVAILABLE:
                statusColor = ContextCompat.getColor(context, R.color.status_available);
                bgColor = ColorUtils.adjustAlpha(statusColor, 0.1f);
                strokeColor = statusColor;
                break;
            case PAYMENT_REQUIRED:
                statusColor = ContextCompat.getColor(context, R.color.status_payment_required);
                bgColor = ColorUtils.adjustAlpha(statusColor, 0.1f);
                strokeColor = statusColor;
                break;
            case OCCUPIED:
                statusColor = ContextCompat.getColor(context, R.color.status_occupied);
                bgColor = ColorUtils.adjustAlpha(statusColor, 0.1f);
                strokeColor = statusColor;
                break;
            case MAINTENANCE:
                statusColor = ContextCompat.getColor(context, R.color.status_maintenance);
                bgColor = ColorUtils.adjustAlpha(statusColor, 0.1f);
                strokeColor = statusColor;
                break;
            default:
                statusColor = ContextCompat.getColor(context, R.color.gray_text);
                bgColor = ContextCompat.getColor(context, R.color.white);
                strokeColor = ContextCompat.getColor(context, R.color.light_gray);
        }

        holder.statusDot.setBackgroundTintList(ColorStateList.valueOf(statusColor));
        holder.card.setCardBackgroundColor(bgColor);
        holder.card.setStrokeColor(strokeColor);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onLockerClick(locker);
        });
    }

    @Override
    public int getItemCount() {
        return lockers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView statusDot;
        TextView idText;
        TextView sizeText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.locker_card);
            statusDot = itemView.findViewById(R.id.locker_status_dot);
            idText = itemView.findViewById(R.id.locker_id);
            sizeText = itemView.findViewById(R.id.locker_size);
        }
    }
}