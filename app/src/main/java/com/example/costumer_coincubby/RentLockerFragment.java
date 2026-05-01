package com.example.costumer_coincubby;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.costumer_coincubby.SupabaseHelper.SupabaseHelper;
import com.example.costumer_coincubby.shared.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class RentLockerFragment extends BottomSheetDialogFragment {

    private static final String TAG = "RentLocker";

    private static final String ARG_ID           = "locker_id";
    private static final String ARG_SIZE         = "locker_size";
    private static final String ARG_RATE         = "locker_rate";
    private static final String ARG_DB_LOCKER_ID = "db_locker_id";
    private boolean isWalletSelected = true;
    private boolean isOpenTime = false;
    private double  ratePerHr;

    // ── Factory ───────────────────────────────────────────────────────────────
    public static RentLockerFragment newInstance(String id, String size,
                                                 double rate, int dbLockerId) {
        RentLockerFragment f = new RentLockerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
        args.putString(ARG_SIZE, size);
        args.putDouble(ARG_RATE, rate);
        args.putInt(ARG_DB_LOCKER_ID, dbLockerId);
        f.setArguments(args);
        return f;
    }

    public static RentLockerFragment newInstance(String id, String size, double rate) {
        return newInstance(id, size, rate, fallbackDbId(id));
    }

    private static int fallbackDbId(String lockerId) {
        switch (lockerId) {
            case "S1": return 1; case "S2": return 2;
            case "S3": return 3; case "S4": return 4;
            case "M1": return 5; case "M2": return 6;
            case "L1": return 7; case "L2": return 8;
            default:   return 1;
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rent_locker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String id      = getArguments().getString(ARG_ID);
        String size    = getArguments().getString(ARG_SIZE);
        ratePerHr      = getArguments().getDouble(ARG_RATE);
        int dbLockerId = getArguments().getInt(ARG_DB_LOCKER_ID, fallbackDbId(id));

        TextView   title           = view.findViewById(R.id.rent_title);
        TextView   subtitle        = view.findViewById(R.id.rent_subtitle);
        EditText   etDuration      = view.findViewById(R.id.et_duration);
        TextView   tvTotal         = view.findViewById(R.id.tv_total);
        RadioGroup rgRentalType    = view.findViewById(R.id.rg_rental_type);
        RadioGroup rgPayment       = view.findViewById(R.id.rg_payment);
        View       llDurationInput = view.findViewById(R.id.ll_duration_input);
        View       cardWallet      = view.findViewById(R.id.card_wallet);

        title.setText("Rent Locker " + id);
        subtitle.setText("Size: " + size + "  Rate: ₱" + (int) ratePerHr + "/hr");

        android.widget.RadioButton rbWallet = view.findViewById(R.id.rb_wallet);
        android.widget.RadioButton rbDevice = view.findViewById(R.id.rb_device);

        rbWallet.setOnClickListener(v -> {
            rbWallet.setChecked(true);
            rbDevice.setChecked(false);
            isWalletSelected = true;
        });

        rbDevice.setOnClickListener(v -> {
            rbDevice.setChecked(true);
            rbWallet.setChecked(false);
            isWalletSelected = false;
        });

        view.findViewById(R.id.card_wallet).setOnClickListener(v -> {
            rbWallet.setChecked(true);
            rbDevice.setChecked(false);
            isWalletSelected = true;
        });

        rbWallet.setChecked(true);
        rbDevice.setChecked(false);

        view.findViewById(R.id.card_device).setOnClickListener(v -> {
            rbDevice.setChecked(true);
            rbWallet.setChecked(false);
            isWalletSelected = false;
        });

        // ── Rental type toggle ────────────────────────────────────────────────
        rgRentalType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_open) {
                isOpenTime = true;
                llDurationInput.setVisibility(View.GONE);
                rgPayment.check(R.id.rb_device);
                cardWallet.setVisibility(View.GONE);
                tvTotal.setText("Running...");
            } else {
                isOpenTime = false;
                llDurationInput.setVisibility(View.VISIBLE);
                cardWallet.setVisibility(View.VISIBLE);
                rgPayment.check(R.id.rb_wallet);
                isWalletSelected = true;
                updateTotal(etDuration.getText().toString(), tvTotal);
            }
        });

        etDuration.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                updateTotal(s.toString(), tvTotal);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        view.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());

        // ── Confirm button ────────────────────────────────────────────────────
        view.findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            String duration = isOpenTime ? "Open" : etDuration.getText().toString();

            // Validate
            if (!isOpenTime) {
                if (duration.isEmpty()) {
                    Toast.makeText(getContext(),
                            "Please enter duration in hours.", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    if (Integer.parseInt(duration) <= 0) {
                        Toast.makeText(getContext(),
                                "Duration must be at least 1 hour.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(),
                            "Invalid duration.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Disable to prevent double-tap
            view.findViewById(R.id.btn_confirm).setEnabled(false);
            view.findViewById(R.id.btn_confirm).setAlpha(0.6f);

            startRentalFlow(id, size, duration, dbLockerId);
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Rental flow: upsertCustomer → fetchRate → insertTransaction
    //              → insertPayment → updateLockerStatus → navigate
    // ═════════════════════════════════════════════════════════════════════════

    // Step 1 ──────────────────────────────────────────────────────────────────
    private void startRentalFlow(String lockerId, String size,
                                 String duration, int dbLockerId) {
        String customerId = getCustomerId();
        if (customerId == null || customerId.isEmpty()) {
            showError("You must be logged in to rent a locker.");
            resetConfirmButton();
            return;
        }

        SupabaseHelper.upsertCustomer(
                customerId, getCustomerName(), getCustomerEmail(),
                new SupabaseHelper.Callback() {
                    @Override public void onSuccess(String body) {
                        fetchRateAndContinue(lockerId, size, duration, dbLockerId);
                    }
                    @Override public void onError(String error) {
                        Log.w(TAG, "Customer upsert warning (continuing): " + error);
                        fetchRateAndContinue(lockerId, size, duration, dbLockerId);
                    }
                });
    }

    // Step 2 ──────────────────────────────────────────────────────────────────
    private void fetchRateAndContinue(String lockerId, String size,
                                      String duration, int dbLockerId) {
        SupabaseHelper.fetchRates(new SupabaseHelper.Callback() {
            @Override
            public void onSuccess(String body) {
                Log.d(TAG, "Raw rates response: '" + body + "'");

                if (body.trim().equals("[]")) {
                    showError("Rates table is empty. Please contact support.");
                    resetConfirmButton();
                    return;
                }

                int rateId = parseRateId(body, size);
                if (rateId == -1) {
                    Log.e(TAG, "No rate matched for size='" + size + "'");
                    showError("Rate not found for size: " + size);
                    resetConfirmButton();
                    return;
                }

                double amount = calculateAmount(duration);
                Log.d(TAG, "Matched rate_id=" + rateId + " amount=₱" + amount);
                insertTransaction(lockerId, size, duration, dbLockerId, rateId, amount);
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "fetchRates error: " + error);
                showError("Could not fetch rates: " + error);
                resetConfirmButton();
            }
        });
    }

    // Step 3 ──────────────────────────────────────────────────────────────────
    private void insertTransaction(String lockerId, String size,
                                   String duration, int dbLockerId,
                                   int rateId, double amount) {
        String customerId = getCustomerId();
        String qrToken    = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
        String startTime  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()).format(new Date());

        StringBuilder json = new StringBuilder("{");
        json.append("\"customer_id\":\"").append(customerId).append("\",");
        json.append("\"rate_id\":").append(rateId).append(",");
        json.append("\"locker_id\":").append(dbLockerId).append(",");
        json.append("\"start_time\":\"").append(startTime).append("\",");
        json.append("\"status\":\"Active\",");
        json.append("\"qr_token\":\"").append(qrToken).append("\"");

        if (!isOpenTime) {
            try {
                int hrs = Integer.parseInt(duration);
                int durationMinutes = hrs * 60;
                long endMs = System.currentTimeMillis() + (hrs * 3600000L);
                String endTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                        Locale.getDefault()).format(new Date(endMs));
                json.append(",\"end_time\":\"").append(endTime).append("\"");
                json.append(",\"duration_minutes\":").append(durationMinutes);
            } catch (NumberFormatException ignored) {}
        }
        json.append("}");

        Log.d(TAG, "Inserting transaction: " + json);

        SupabaseHelper.insertTransaction(json.toString(), new SupabaseHelper.Callback() {
            @Override
            public void onSuccess(String body) {
                Log.d(TAG, "Transaction saved. Response: " + body);
                String transactionId = extractTransactionId(body);
                if (transactionId != null) {
                    insertPayment(transactionId, amount, duration);
                } else {
                    Log.w(TAG, "Could not extract transaction_id — skipping payment");
                    updateLockerStatus(dbLockerId, lockerId, size, duration);
                }
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "Transaction insert failed: " + error);
                showError("Could not save rental: " + error);
                resetConfirmButton();
            }
        });
    }

    // Step 4 ──────────────────────────────────────────────────────────────────
    private void insertPayment(String transactionId, double amount, String duration) {
        boolean isWallet = !isOpenTime && isWalletSelected;

        StringBuilder json = new StringBuilder("{");
        json.append("\"transaction_id\":\"").append(transactionId).append("\",");
        json.append("\"amount\":").append(String.format(Locale.getDefault(), "%.2f", amount));
        if (isWallet) {
            json.append(",\"payment_method\":\"Wallet\"");
        }
        json.append("}");

        Log.d(TAG, "Inserting payment: " + json);

        SupabaseHelper.insertPayment(json.toString(), new SupabaseHelper.Callback() {
            @Override public void onSuccess(String body) {
                Log.d(TAG, "Payment saved.");
                proceedAfterPayment();
            }
            @Override public void onError(String error) {
                Log.w(TAG, "Payment insert warning (continuing): " + error);
                proceedAfterPayment(); // non-fatal
            }
        });
    }

    private void proceedAfterPayment() {
        if (getArguments() == null) return;
        int dbLockerId  = getArguments().getInt(ARG_DB_LOCKER_ID,
                fallbackDbId(getArguments().getString(ARG_ID, "S1")));
        String lockerId = getArguments().getString(ARG_ID, "S1");
        String size     = getArguments().getString(ARG_SIZE, "Small S");
        String duration = isOpenTime ? "Open"
                : (getView() != null
                ? ((EditText) getView().findViewById(R.id.et_duration))
                .getText().toString()
                : "1");
        updateLockerStatus(dbLockerId, lockerId, size, duration);
    }

    // Step 5 ──────────────────────────────────────────────────────────────────
    private void updateLockerStatus(int dbLockerId, String lockerId,
                                    String size, String duration) {
        SupabaseHelper.updateLockerStatus(dbLockerId, "Occupied",
                new SupabaseHelper.Callback() {
                    @Override public void onSuccess(String body) {
                        Log.d(TAG, "Locker " + dbLockerId + " → Occupied");
                        navigateToMyRental(lockerId, size, duration);
                    }
                    @Override public void onError(String error) {
                        Log.e(TAG, "Locker status update failed: " + error);
                        // Transaction is saved — still navigate
                        navigateToMyRental(lockerId, size, duration);
                    }
                });
    }

    // Step 6 ──────────────────────────────────────────────────────────────────
    private void navigateToMyRental(String lockerId, String size, String duration) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity())
                    .showMyRentalWithDetails(lockerId, size, isOpenTime, duration, ratePerHr);
        }
        dismiss();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═════════════════════════════════════════════════════════════════════════

    private int parseRateId(String json, String size) {
        int target = sizeTypeIdForSize(size);
        Log.d(TAG, "Matching size='" + size + "' → size_type_id=" + target);
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Log.d(TAG, "Rate row: " + obj);
                if (obj.getInt("size_type_id") == target) {
                    return obj.getInt("rate_id");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "parseRateId error: " + e.getMessage());
        }
        return -1;
    }

    private int sizeTypeIdForSize(String size) {
        if (size == null) return 1;
        String s = size.trim().toLowerCase(Locale.getDefault());
        if (s.contains("medium")) return 2;
        if (s.contains("large"))  return 3;
        return 1; // Small is default
    }

    private double calculateAmount(String duration) {
        if (isOpenTime) return 0.0;
        try {
            return Integer.parseInt(duration) * ratePerHr;
        } catch (NumberFormatException e) {
            return ratePerHr;
        }
    }

    private String extractTransactionId(String body) {
        try {
            JSONArray arr = new JSONArray(body);
            if (arr.length() > 0) {
                return arr.getJSONObject(0).getString("transaction_id");
            }
        } catch (Exception e) {
            Log.e(TAG, "extractTransactionId error: " + e.getMessage());
        }
        return null;
    }

    private void updateTotal(String durationStr, TextView tvTotal) {
        if (isOpenTime) return;
        try {
            int hrs = Integer.parseInt(durationStr);
            tvTotal.setText("₱" + String.format(Locale.getDefault(),
                    "%.2f", hrs * ratePerHr));
        } catch (NumberFormatException e) {
            tvTotal.setText("₱0.00");
        }
    }

    private void showError(String msg) {
        if (getContext() != null)
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }

    private void resetConfirmButton() {
        if (getView() != null) {
            View btn = getView().findViewById(R.id.btn_confirm);
            btn.setEnabled(true);
            btn.setAlpha(1.0f);
        }
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialog;
    }
    private String getCustomerId() {
        return SessionManager.getUserId(requireContext());
    }
    private String getCustomerName() {
        String n = SessionManager.getFullName(requireContext());
        return n != null ? n : "User";
    }
    private String getCustomerEmail() {
        String e = SessionManager.getEmail(requireContext());
        return e != null ? e : "";
    }
}