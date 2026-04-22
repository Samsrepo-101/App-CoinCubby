package com.example.costumer_coincubby;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyRentalFragment extends Fragment {

    private static final String ARG_ID = "locker_id";
    private static final String ARG_SIZE = "locker_size";
    private static final String ARG_IS_OPEN = "is_open_time";
    private static final String ARG_DURATION = "duration";

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTime;
    private boolean isOpenTime;
    private double rate = 10.0; // Assume 10 for simplicity or pass it

    public static MyRentalFragment newInstance(String id, String size, boolean isOpenTime, String duration) {
        MyRentalFragment fragment = new MyRentalFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
        args.putString(ARG_SIZE, size);
        args.putBoolean(ARG_IS_OPEN, isOpenTime);
        args.putString(ARG_DURATION, duration);
        fragment.setArguments(args);
        return fragment;
    }

    // Overload for existing calls if any
    public static MyRentalFragment newInstance(String id, String size) {
        return newInstance(id, size, false, "1");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_rental, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        String id = args != null ? args.getString(ARG_ID) : "S1";
        String size = args != null ? args.getString(ARG_SIZE) : "Small";
        isOpenTime = args != null && args.getBoolean(ARG_IS_OPEN);
        String duration = args != null ? args.getString(ARG_DURATION) : "1";

        TextView tvId = view.findViewById(R.id.tv_locker_id);
        TextView tvSize = view.findViewById(R.id.tv_locker_size);
        TextView tvTimer = view.findViewById(R.id.tv_timer);
        TextView tvRunningCost = view.findViewById(R.id.tv_running_cost);
        TextView tvTrackerLabel = view.findViewById(R.id.tv_tracker_label);
        TextView tvStarted = view.findViewById(R.id.tv_started);
        TextView tvExpires = view.findViewById(R.id.tv_expires);

        tvId.setText(id);
        tvSize.setText(size);

        startTime = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault());
        tvStarted.setText(sdf.format(new Date(startTime)));

        if (isOpenTime) {
            tvTrackerLabel.setText("ELAPSED TIME");
            tvExpires.setText("N/A (Open Time)");
            startTimer(tvTimer, tvRunningCost);
        } else {
            tvTrackerLabel.setText("TIME REMAINING");
            int hrs = 1;
            try { hrs = Integer.parseInt(duration); } catch (Exception e) {}
            long endTime = startTime + (hrs * 3600000L);
            tvExpires.setText(sdf.format(new Date(endTime)));
            tvRunningCost.setText("Prepaid: ₱" + String.format("%.2f", hrs * rate));
            startCountdown(tvTimer, endTime);
        }

        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(0);
            }
        });

        view.findViewById(R.id.btn_return).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(0);
            }
        });
    }

    private void startTimer(TextView tvTimer, TextView tvCost) {
        timerHandler.post(new Runnable() {
            @Override
            public void run() {
                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                int hours = minutes / 60;
                seconds = seconds % 60;
                minutes = minutes % 60;

                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
                
                double currentCost = (millis / 3600000.0) * rate;
                if (currentCost < rate && millis > 0) currentCost = rate; // Minimum 1 hour charge usually
                tvCost.setText("Current Bill: ₱" + String.format("%.2f", currentCost));
                
                timerHandler.postDelayed(this, 1000);
            }
        });
    }

    private void startCountdown(TextView tvTimer, long endTime) {
        timerHandler.post(new Runnable() {
            @Override
            public void run() {
                long millis = endTime - System.currentTimeMillis();
                if (millis < 0) {
                    tvTimer.setText("00:00:00");
                    return;
                }
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                int hours = minutes / 60;
                seconds = seconds % 60;
                minutes = minutes % 60;

                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacksAndMessages(null);
    }
}