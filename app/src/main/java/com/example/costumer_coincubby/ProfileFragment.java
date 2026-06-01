package com.example.costumer_coincubby;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.costumer_coincubby.LoginActivity;
import com.example.costumer_coincubby.R;
import com.example.costumer_coincubby.shared.SessionManager;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileFragment extends Fragment {

    private static final String SUPABASE_URL  = "https://cjuimxgxovdmijuenagr.supabase.co";
    private static final String SUPABASE_ANON = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
            + ".eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNqdWlteGd4b3ZkbWlqdWVuYWdyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzY0MzQ0OTEsImV4cCI6MjA5MjAxMDQ5MX0"
            + ".t6ixuFiD2iYzrNZsc1QjG3gpdTdBuMY37qTKzwxdg18";

    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
    private static final String TAG = "ProfileFragment";

    // IDs match fragment_profile.xml exactly:
    //   tvProfileFullName  → full name below avatar
    //   tvProfileContact   → contact/email below full name
    //   tvPrivateKey       → key badge inside the card
    //   btnSignOut         → MaterialButton at the bottom
    private TextView       tvFullName;
    private TextView       tvContact;
    private TextView       tvPrivateKey;
    private MaterialButton btnSignOut;

    private final OkHttpClient http = new OkHttpClient();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Wire up views — IDs match the XML above
        tvFullName   = view.findViewById(R.id.tvProfileFullName);
        tvContact    = view.findViewById(R.id.tvProfileContact);
        tvPrivateKey = view.findViewById(R.id.tvPrivateKey);
        btnSignOut   = view.findViewById(R.id.btnSignOut);

        android.widget.ImageView btnHome = view.findViewById(R.id.btnHome);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).selectTab(0);
                }
            });
        }

        com.google.android.material.card.MaterialCardView cardChangePassword = view.findViewById(R.id.cardChangePassword);
        if (cardChangePassword != null) {
            cardChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }

        // Show cached data immediately — no blank screen
        showCachedData();

        // Fetch fresh data from Supabase
        fetchProfile();

        // Sign out ONLY on button click — never auto-redirect
        btnSignOut.setOnClickListener(v -> signOut());
    }

    // ── Step 1: Show SessionManager cached data instantly ────────────────────

    private void showCachedData() {
        String fullName = SessionManager.getFullName(requireContext());
        String email    = SessionManager.getEmail(requireContext());
        String userId   = SessionManager.getUserId(requireContext());

        tvFullName.setText((fullName != null && !fullName.isEmpty() && !fullName.equalsIgnoreCase("null")) ? fullName : "Loading…");
        tvContact.setText((email    != null && !email.isEmpty() && !email.equalsIgnoreCase("null"))    ? email    : "");

        String lockerToken = SessionManager.getLockerToken(requireContext());
        if (lockerToken != null && !lockerToken.isEmpty()) {
            tvPrivateKey.setText(lockerToken);
        } else {
            tvPrivateKey.setText("—");
        }
    }

    // ── Step 2: Get current user's UUID from Supabase Auth ───────────────────

    private void fetchProfile() {
        String accessToken = SessionManager.getAccessToken(requireContext());

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "No access token found — showing cached data only");
            // DO NOT redirect here — just show whatever cached data we have
            return;
        }

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/user")
                .addHeader("apikey",        SUPABASE_ANON)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Auth fetch failed: " + e.getMessage());
                // DO NOT redirect — cached data is already showing
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "/auth/v1/user [" + response.code() + "]: " + body);

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Auth token error: " + body);
                    return;
                }

                try {
                    JSONObject user = new JSONObject(body);
                    String userId   = user.optString("id",    "");
                    String email    = user.optString("email", "");

                    String fullName = "";
                    JSONObject meta = user.optJSONObject("user_metadata");
                    if (meta != null) {
                        fullName = meta.optString("full_name", "");
                    }

                    Log.d(TAG, "Auth user — id: " + userId + ", name: " + fullName);

                    final String fEmail    = email;
                    final String fFullName = fullName;

                    if (!isAdded()) return;

                    requireActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        // Update name + contact in the avatar section
                        if (!fFullName.isEmpty() && !fFullName.equalsIgnoreCase("null")) tvFullName.setText(fFullName);
                        if (!fEmail.isEmpty() && !fEmail.equalsIgnoreCase("null"))    tvContact.setText(fEmail);
                        // Update private key badge in the card
                        String lockerToken = SessionManager.getLockerToken(requireContext());
                        if (lockerToken != null && !lockerToken.isEmpty()) {
                            tvPrivateKey.setText(lockerToken);
                        }
                    });

                    if (!userId.isEmpty()) {
                        fetchCustomerRecord(userId, accessToken);
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "JSON error: " + e.getMessage());
                }
            }
        });
    }

    // ── Step 3: Fetch from customers table using customer_id ─────────────────

    private void fetchCustomerRecord(String userId, String accessToken) {
        String url = SUPABASE_URL
                + "/rest/v1/customers"
                + "?customer_id=eq." + userId
                + "&select=customer_id,full_name,email,contact_number";

        Log.d(TAG, "Fetching customers: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey",        SUPABASE_ANON)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Accept",        "application/json")
                .get()
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Customers fetch failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Customers [" + response.code() + "]: " + body);

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Customers query failed — check RLS policy");
                    return;
                }

                try {
                    JSONArray arr = new JSONArray(body);

                    if (arr.length() == 0) {
                        Log.w(TAG, "No customer row found for customer_id=" + userId);
                        return;
                    }

                    JSONObject customer  = arr.getJSONObject(0);
                    String fullName      = customer.isNull("full_name") ? "" : customer.optString("full_name", "");
                    if (fullName.equalsIgnoreCase("null")) fullName = "";

                    String email         = customer.isNull("email") ? "" : customer.optString("email", "");
                    if (email.equalsIgnoreCase("null")) email = "";

                    String contactNumber = customer.isNull("contact_number") ? "" : customer.optString("contact_number", "");
                    if (contactNumber.equalsIgnoreCase("null")) contactNumber = "";

                    // Prefer contact number; fall back to email (shown below the name)
                    String displayContact = (!contactNumber.isEmpty()) ? contactNumber : email;

                    final String fName    = fullName;
                    final String fContact = displayContact;

                    if (!isAdded()) return;

                    requireActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        // tvProfileFullName — name below avatar circle
                        if (!fName.isEmpty() && !fName.equalsIgnoreCase("null"))    tvFullName.setText(fName);
                        // tvProfileContact — contact/email below name
                        if (!fContact.isEmpty() && !fContact.equalsIgnoreCase("null")) tvContact.setText(fContact);
                        // tvPrivateKey — badge inside the "Your Private Key" card
                        String lockerToken = SessionManager.getLockerToken(requireContext());
                        if (lockerToken != null && !lockerToken.isEmpty()) {
                            tvPrivateKey.setText(lockerToken);
                        }
                    });

                } catch (JSONException e) {
                    Log.e(TAG, "JSON parse error: " + e.getMessage());
                }
            }
        });
    }

    // ── Sign Out — only place we redirect to login ────────────────────────────

    private void signOut() {
        String accessToken = SessionManager.getAccessToken(requireContext());

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/logout")
                .addHeader("apikey",        SUPABASE_ANON)
                .addHeader("Authorization", "Bearer " + (accessToken != null ? accessToken : ""))
                .post(RequestBody.create("", JSON_MEDIA))
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() -> clearAndRedirect());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                requireActivity().runOnUiThread(() -> clearAndRedirect());
            }
        });
    }

    private void clearAndRedirect() {
        SessionManager.clearSession(requireContext());
        Toast.makeText(requireContext(), "Signed out successfully.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showChangePasswordDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int paddingDp = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(paddingDp, paddingDp, paddingDp, paddingDp);

        final android.widget.EditText etNewPassword = new android.widget.EditText(requireContext());
        etNewPassword.setHint("New Password");
        etNewPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNewPassword);

        final android.widget.EditText etConfirmPassword = new android.widget.EditText(requireContext());
        etConfirmPassword.setHint("Confirm Password");
        etConfirmPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        etConfirmPassword.setLayoutParams(params);
        layout.addView(etConfirmPassword);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Change Password")
                .setView(layout)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newPassword = etNewPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();

            if (newPassword.isEmpty()) {
                etNewPassword.setError("Password cannot be empty");
                etNewPassword.requestFocus();
                return;
            }
            if (newPassword.length() < 6) {
                etNewPassword.setError("Password must be at least 6 characters");
                etNewPassword.requestFocus();
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                etConfirmPassword.setError("Passwords do not match");
                etConfirmPassword.requestFocus();
                return;
            }

            dialog.dismiss();
            updatePassword(newPassword);
        });
    }

    private void updatePassword(String newPassword) {
        String accessToken = SessionManager.getAccessToken(requireContext());
        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(requireContext(), "You are not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("password", newPassword);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody requestBody = RequestBody.create(body.toString(), JSON_MEDIA);
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/user")
                .addHeader("apikey",        SUPABASE_ANON)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .put(requestBody)
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull java.io.IOException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws java.io.IOException {
                final String responseBody = response.body() != null ? response.body().string() : "";
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            try {
                                JSONObject json = new JSONObject(responseBody);
                                String msg = json.optString("message", "Could not update password.");
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                            } catch (JSONException e) {
                                Toast.makeText(requireContext(), "Failed to update password.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }
}