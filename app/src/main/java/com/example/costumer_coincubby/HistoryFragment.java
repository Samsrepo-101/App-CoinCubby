package com.example.costumer_coincubby;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.costumer_coincubby.SupabaseHelper.SupabaseHelper;
import com.example.costumer_coincubby.shared.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";

    private HistoryAdapter adapter;
    private final List<HistoryItem> items = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rv_history);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(items);
        rv.setAdapter(adapter);

        loadHistory();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }

    // ── Fetch ─────────────────────────────────────────────────────────────────
    private void loadHistory() {
        if (getView() == null || getContext() == null) return;

        String customerId = SessionManager.getUserId(requireContext());
        if (customerId == null || customerId.isEmpty()) {
            hideLoading();
            getView().findViewById(R.id.layout_empty).setVisibility(View.VISIBLE);
            return;
        }

        getView().findViewById(R.id.layout_loading).setVisibility(View.VISIBLE);
        getView().findViewById(R.id.layout_empty).setVisibility(View.GONE);

        SupabaseHelper.fetchRentalHistory(customerId, new SupabaseHelper.Callback() {
            @Override
            public void onSuccess(String body) {
                if (getActivity() != null)
                    getActivity().runOnUiThread(() -> parseAndDisplay(body));
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "fetchRentalHistory error: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        hideLoading();
                        Toast.makeText(getContext(),
                                "Failed to load history.", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void parseAndDisplay(String json) {
        items.clear();
        double totalSpent   = 0;
        int    completedCnt = 0;

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject tx = arr.getJSONObject(i);

                String status    = tx.optString("status", "Active");
                String qrToken   = tx.optString("qr_token", "—");
                String startTime = tx.optString("start_time", "");

                // Locker info
                JSONObject lockerObj   = tx.optJSONObject("lockers");
                String     lockerNum   = lockerObj != null
                        ? lockerObj.optString("locker_number", "?") : "?";
                int        sizeTypeId  = lockerObj != null
                        ? lockerObj.optInt("size_type_id", 1) : 1;

                String sizeName;
                switch (sizeTypeId) {
                    case 2:  sizeName = "Medium"; break;
                    case 3:  sizeName = "Large";  break;
                    default: sizeName = "Small";  break;
                }

                // Payment info — sum all payments for this transaction
                double amount = 0;
                String paymentMethod = null;
                JSONArray paymentsArr = tx.optJSONArray("payments");
                if (paymentsArr != null) {
                    for (int j = 0; j < paymentsArr.length(); j++) {
                        JSONObject p = paymentsArr.getJSONObject(j);
                        amount += p.optDouble("amount", 0);
                        if (paymentMethod == null) {
                            paymentMethod = p.optString("payment_method", null);
                        }
                    }
                }

                totalSpent += amount;
                if ("Completed".equals(status)) completedCnt++;

                int durationMinutes = tx.optInt("duration_minutes", 0);

                items.add(new HistoryItem(
                        lockerNum, sizeName, qrToken,
                        amount, paymentMethod,
                        status, startTime, durationMinutes));
            }
        } catch (Exception e) {
            Log.e(TAG, "parse error: " + e.getMessage());
        }

        hideLoading();
        adapter.notifyDataSetChanged();

        if (getView() == null) return;

        // Update summary banner
        ((TextView) getView().findViewById(R.id.tv_total_rentals))
                .setText(String.valueOf(items.size()));
        ((TextView) getView().findViewById(R.id.tv_total_spent))
                .setText(String.format(Locale.getDefault(), "₱%.2f", totalSpent));
        ((TextView) getView().findViewById(R.id.tv_completed_count))
                .setText(String.valueOf(completedCnt));

        getView().findViewById(R.id.layout_empty)
                .setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void hideLoading() {
        if (getView() != null)
            getView().findViewById(R.id.layout_loading).setVisibility(View.GONE);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HistoryItem model
    // ═════════════════════════════════════════════════════════════════════════
    public static class HistoryItem {
        public final String lockerNumber;
        public final String sizeName;
        public final String qrToken;
        public final double amount;
        public final String paymentMethod; // null = Pay at Device
        public final String status;
        public final String startTimeRaw;
        public final int    durationMinutes;

        public HistoryItem(String lockerNumber, String sizeName, String qrToken,
                           double amount, String paymentMethod,
                           String status, String startTimeRaw, int durationMinutes) {
            this.lockerNumber    = lockerNumber;
            this.sizeName        = sizeName;
            this.qrToken         = qrToken;
            this.amount          = amount;
            this.paymentMethod   = paymentMethod;
            this.status          = status;
            this.startTimeRaw    = startTimeRaw;
            this.durationMinutes = durationMinutes;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HistoryAdapter
    // ═════════════════════════════════════════════════════════════════════════
    public static class HistoryAdapter
            extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

        private final List<HistoryItem> items;

        public HistoryAdapter(List<HistoryItem> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_row, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            h.bind(items.get(position));
        }

        @Override public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvLockerNum, tvQrToken, tvAmount, tvStatus, tvStartTime, tvSize;

            ViewHolder(@NonNull View v) {
                super(v);
                tvLockerNum = v.findViewById(R.id.tv_locker_number);
                tvQrToken   = v.findViewById(R.id.tv_qr_token);
                tvAmount    = v.findViewById(R.id.tv_amount);
                tvStatus    = v.findViewById(R.id.tv_status);
                tvStartTime = v.findViewById(R.id.tv_start_time);
                tvSize      = v.findViewById(R.id.tv_size);
            }

            void bind(HistoryItem item) {
                tvLockerNum.setText(item.lockerNumber);
                tvQrToken.setText(item.qrToken);
                tvSize.setText(item.sizeName);

                // Amount — sum of all payments
                if (item.amount > 0) {
                    String durationStr = item.durationMinutes > 0
                            ? " (" + formatMinutes(item.durationMinutes) + ")" : "";
                    tvAmount.setText(String.format(Locale.getDefault(),
                            "₱%.2f", item.amount) + durationStr);
                } else {
                    tvAmount.setText("—");
                }

                // Start time — format from UTC
                tvStartTime.setText(formatTimestamp(item.startTimeRaw));

                // Status badge color
                switch (item.status) {
                    case "Completed":
                        tvStatus.setText("Completed");
                        tvStatus.setBackgroundResource(R.drawable.status_completed_bg);
                        tvStatus.setTextColor(ContextCompat.getColor(
                                itemView.getContext(), R.color.black));
                        break;
                    case "Active":
                        tvStatus.setText("Active");
                        tvStatus.setBackgroundResource(R.drawable.status_active_bg);
                        tvStatus.setTextColor(ContextCompat.getColor(
                                itemView.getContext(), R.color.black));
                        break;
                    default:
                        tvStatus.setText(item.status);
                        tvStatus.setBackgroundResource(0);
                        tvStatus.setTextColor(ContextCompat.getColor(
                                itemView.getContext(), R.color.gray_text));
                }
            }

            private String formatTimestamp(String raw) {
                if (raw == null || raw.isEmpty()) return "—";
                String[] formats = {
                        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                        "yyyy-MM-dd'T'HH:mm:ss",
                        "yyyy-MM-dd HH:mm:ss"
                };
                for (String fmt : formats) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.getDefault());
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        Date d = sdf.parse(raw);
                        if (d != null) {
                            SimpleDateFormat out = new SimpleDateFormat(
                                    "MMM dd, yyyy h:mm a", Locale.getDefault());
                            out.setTimeZone(TimeZone.getDefault());
                            return out.format(d);
                        }
                    } catch (ParseException ignored) {}
                }
                return raw;
            }

            private String formatMinutes(int totalMin) {
                if (totalMin < 60) return totalMin + "m";
                int hrs = totalMin / 60;
                int min = totalMin % 60;
                if (min == 0) return hrs + "h";
                return hrs + "h " + min + "m";
            }
        }
    }
}