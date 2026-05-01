package com.example.costumer_coincubby;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.costumer_coincubby.SupabaseHelper.SupabaseHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MyRentalFragment extends Fragment {

    private static final String TAG         = "MyRental";
    private static final String CUSTOMER_ID = "00000000-0000-0000-0000-000000000001";

    private RentalAdapter adapter;
    private final List<RentalItem> rentals = new ArrayList<>();

    // Keep legacy newInstance for compatibility
    public static MyRentalFragment newInstance(String id, String size,
                                               boolean isOpenTime, String duration,
                                               double rate) {
        return new MyRentalFragment();
    }

    public static MyRentalFragment newInstance(String id, String size) {
        return new MyRentalFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_rental, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rv_rentals);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new RentalAdapter(rentals, this::returnLocker);
        rv.setAdapter(adapter);

        loadRentals();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRentals();
    }

    // ── Fetch active rentals ──────────────────────────────────────────────────
    private void loadRentals() {
        if (getView() == null) return;
        getView().findViewById(R.id.layout_loading).setVisibility(View.VISIBLE);
        getView().findViewById(R.id.layout_empty).setVisibility(View.GONE);

        SupabaseHelper.fetchActiveRentals(CUSTOMER_ID, new SupabaseHelper.Callback() {
            @Override
            public void onSuccess(String body) {
                if (getActivity() != null)
                    getActivity().runOnUiThread(() -> parseAndDisplay(body));
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "fetchActiveRentals error: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        hideLoading();
                        Toast.makeText(getContext(),
                                "Failed to load rentals.", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void parseAndDisplay(String json) {
        rentals.clear();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject tx = arr.getJSONObject(i);

                String transactionId = tx.getString("transaction_id");
                String startTimeStr  = tx.optString("start_time", "");
                String endTimeStr    = tx.optString("end_time", "");
                String qrToken       = tx.optString("qr_token", "—");
                boolean isOpenTime   = endTimeStr.isEmpty() || endTimeStr.equals("null");

                // Locker info from joined table
                JSONObject lockerObj = tx.optJSONObject("lockers");
                String lockerNumber  = lockerObj != null
                        ? lockerObj.optString("locker_number", "?") : "?";
                int sizeTypeId       = lockerObj != null
                        ? lockerObj.optInt("size_type_id", 1) : 1;
                int lockerId         = tx.optInt("locker_id", 0);

                // Rate info from joined table
                JSONObject rateObj        = tx.optJSONObject("rates");
                double pricePerMinute     = rateObj != null
                        ? rateObj.optDouble("price_per_minute", 0.17) : 0.17;
                int minChargeMinutes      = rateObj != null
                        ? rateObj.optInt("min_charge_minutes", 60) : 60;
                double ratePerHr          = pricePerMinute * 60;

                String sizeName;
                switch (sizeTypeId) {
                    case 2:  sizeName = "Medium"; break;
                    case 3:  sizeName = "Large";  break;
                    default: sizeName = "Small";  break;
                }

                // Parse start time
                long startMs = parseTimestamp(startTimeStr);
                long endMs   = isOpenTime ? -1 : parseTimestamp(endTimeStr);

                rentals.add(new RentalItem(
                        transactionId, lockerNumber, sizeName,
                        startMs, endMs, isOpenTime,
                        ratePerHr, qrToken, lockerId));
            }
        } catch (Exception e) {
            Log.e(TAG, "parseAndDisplay error: " + e.getMessage());
        }

        hideLoading();
        adapter.notifyDataSetChanged();

        if (getView() == null) return;
        TextView tvCount = getView().findViewById(R.id.tv_active_count);
        tvCount.setText(rentals.size() + " locker(s) rented");

        if (rentals.isEmpty()) {
            getView().findViewById(R.id.layout_empty).setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (getView() == null) return;
        getView().findViewById(R.id.layout_loading).setVisibility(View.GONE);
    }

    // ── Return locker ─────────────────────────────────────────────────────────
    private void returnLocker(RentalItem item) {
        SupabaseHelper.updateTransactionStatus(
                item.transactionId, "Completed",
                new SupabaseHelper.Callback() {
                    @Override public void onSuccess(String body) {
                        SupabaseHelper.updateLockerStatus(
                                item.lockerId, "Available",
                                new SupabaseHelper.Callback() {
                                    @Override public void onSuccess(String b) {
                                        Toast.makeText(getContext(),
                                                "Locker " + item.lockerNumber
                                                        + " returned!", Toast.LENGTH_SHORT).show();
                                        loadRentals();
                                    }
                                    @Override public void onError(String e) {
                                        Log.e(TAG, "updateLockerStatus error: " + e);
                                        loadRentals();
                                    }
                                });
                    }
                    @Override public void onError(String error) {
                        Log.e(TAG, "updateTransactionStatus error: " + error);
                        Toast.makeText(getContext(),
                                "Could not return locker.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Timestamp parser ──────────────────────────────────────────────────────
    private long parseTimestamp(String ts) {
        if (ts == null || ts.isEmpty() || ts.equals("null")) return -1;
        // Supabase returns "2026-04-18T18:31:00" or with microseconds
        String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss"
        };
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(ts);
                if (d != null) return d.getTime();
            } catch (ParseException ignored) {}
        }
        Log.w(TAG, "Could not parse timestamp: " + ts);
        return -1;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // RentalItem model
    // ═════════════════════════════════════════════════════════════════════════
    public static class RentalItem {
        public final String  transactionId;
        public final String  lockerNumber;
        public final String  sizeName;
        public final long    startMs;
        public final long    endMs;
        public final boolean isOpenTime;
        public final double  ratePerHr;
        public final String  qrToken;
        public final int     lockerId;

        public RentalItem(String transactionId, String lockerNumber, String sizeName,
                          long startMs, long endMs, boolean isOpenTime,
                          double ratePerHr, String qrToken, int lockerId) {
            this.transactionId = transactionId;
            this.lockerNumber  = lockerNumber;
            this.sizeName      = sizeName;
            this.startMs       = startMs;
            this.endMs         = endMs;
            this.isOpenTime    = isOpenTime;
            this.ratePerHr     = ratePerHr;
            this.qrToken       = qrToken;
            this.lockerId      = lockerId;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // RentalAdapter
    // ═════════════════════════════════════════════════════════════════════════
    public static class RentalAdapter
            extends RecyclerView.Adapter<RentalAdapter.ViewHolder> {

        public interface OnReturnClick { void onReturn(RentalItem item); }

        private final List<RentalItem> items;
        private final OnReturnClick    listener;

        public RentalAdapter(List<RentalItem> items, OnReturnClick listener) {
            this.items    = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_rental_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            h.bind(items.get(position), listener);
        }

        @Override public int getItemCount() { return items.size(); }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvLockerId, tvSize, tvTimer,
                    tvTrackerLabel, tvCost, tvStarted, tvExpires, tvQrToken;
            private final com.google.android.material.button.MaterialButton btnReturn;

            private final Handler  handler = new Handler(Looper.getMainLooper());
            private       Runnable timerRunnable;

            public ViewHolder(@NonNull View v) {
                super(v);
                tvLockerId    = v.findViewById(R.id.tv_locker_id);
                tvSize        = v.findViewById(R.id.tv_locker_size);
                tvTimer       = v.findViewById(R.id.tv_timer);
                tvTrackerLabel = v.findViewById(R.id.tv_tracker_label);
                tvCost        = v.findViewById(R.id.tv_running_cost);
                tvStarted     = v.findViewById(R.id.tv_started);
                tvExpires     = v.findViewById(R.id.tv_expires);
                tvQrToken     = v.findViewById(R.id.tv_qr_token);
                btnReturn     = v.findViewById(R.id.btn_return);
            }

            public void bind(RentalItem item, OnReturnClick listener) {
                // Stop any previous timer on this recycled view
                if (timerRunnable != null) handler.removeCallbacks(timerRunnable);

                SimpleDateFormat sdf = new SimpleDateFormat(
                        "MMM dd, yyyy, h:mm a", Locale.getDefault());
                // Supabase stores in UTC — convert to local
                sdf.setTimeZone(TimeZone.getDefault());

                tvLockerId.setText(item.lockerNumber);
                tvSize.setText(item.sizeName);
                tvQrToken.setText(item.qrToken);
                tvStarted.setText(item.startMs > 0
                        ? sdf.format(new Date(item.startMs)) : "—");

                if (item.isOpenTime) {
                    tvTrackerLabel.setText("ELAPSED TIME");
                    tvExpires.setText("N/A (Open Time)");
                    startElapsedTimer(item);
                } else {
                    tvTrackerLabel.setText("TIME REMAINING");
                    tvExpires.setText(item.endMs > 0
                            ? sdf.format(new Date(item.endMs)) : "—");
                    // Show prepaid cost
                    if (item.startMs > 0 && item.endMs > 0) {
                        long durationMs = item.endMs - item.startMs;
                        double hours    = durationMs / 3600000.0;
                        tvCost.setText("Prepaid: ₱"
                                + String.format(Locale.getDefault(),
                                "%.2f", hours * item.ratePerHr));
                    }
                    startCountdownTimer(item);
                }

                btnReturn.setOnClickListener(v -> listener.onReturn(item));
            }

            private void startElapsedTimer(RentalItem item) {
                timerRunnable = new Runnable() {
                    @Override public void run() {
                        long elapsed = System.currentTimeMillis() - item.startMs;
                        tvTimer.setText(formatDuration(elapsed));
                        // Minimum 1-hour charge
                        double cost = Math.max((elapsed / 3600000.0) * item.ratePerHr,
                                item.ratePerHr);
                        tvCost.setText("Current Bill: ₱"
                                + String.format(Locale.getDefault(), "%.2f", cost));
                        handler.postDelayed(this, 1000);
                    }
                };
                handler.post(timerRunnable);
            }

            private void startCountdownTimer(RentalItem item) {
                timerRunnable = new Runnable() {
                    @Override public void run() {
                        long remaining = item.endMs - System.currentTimeMillis();
                        if (remaining <= 0) {
                            tvTimer.setText("00:00:00");
                            return;
                        }
                        tvTimer.setText(formatDuration(remaining));
                        handler.postDelayed(this, 1000);
                    }
                };
                handler.post(timerRunnable);
            }

            private String formatDuration(long millis) {
                if (millis < 0) millis = 0;
                int totalSec = (int) (millis / 1000);
                int hours    = totalSec / 3600;
                int minutes  = (totalSec % 3600) / 60;
                int seconds  = totalSec % 60;
                return String.format(Locale.getDefault(),
                        "%02d:%02d:%02d", hours, minutes, seconds);
            }
        }
    }
}