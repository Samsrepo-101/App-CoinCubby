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

        tvFullName.setText((fullName != null && !fullName.isEmpty()) ? fullName : "Loading…");
        tvContact.setText((email    != null && !email.isEmpty())    ? email    : "");

        if (userId != null && userId.length() >= 8) {
            tvPrivateKey.setText("COIN-" + userId.substring(0, 8).toUpperCase());
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

                    final String fUserId   = userId;
                    final String fEmail    = email;
                    final String fFullName = fullName;

                    if (!isAdded()) return;

                    requireActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        // Update name + contact in the avatar section
                        if (!fFullName.isEmpty()) tvFullName.setText(fFullName);
                        if (!fEmail.isEmpty())    tvContact.setText(fEmail);
                        // Update private key badge in the card
                        if (fUserId.length() >= 8) {
                            tvPrivateKey.setText("COIN-" + fUserId.substring(0, 8).toUpperCase());
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
                    String fullName      = customer.optString("full_name",      "");
                    String email         = customer.optString("email",          "");
                    String contactNumber = customer.optString("contact_number", "");
                    String customerId    = customer.optString("customer_id",    userId);

                    // Private key format matches the card badge in the layout
                    String privateKey = customerId.length() >= 8
                            ? "COIN-" + customerId.substring(0, 8).toUpperCase()
                            : "COIN-" + customerId.toUpperCase();

                    // Prefer contact number; fall back to email (shown below the name)
                    String displayContact = !contactNumber.isEmpty() ? contactNumber : email;

                    final String fName    = fullName;
                    final String fContact = displayContact;
                    final String fKey     = privateKey;

                    if (!isAdded()) return;

                    requireActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        // tvProfileFullName — name below avatar circle
                        if (!fName.isEmpty())    tvFullName.setText(fName);
                        // tvProfileContact — contact/email below name
                        if (!fContact.isEmpty()) tvContact.setText(fContact);
                        // tvPrivateKey — badge inside the "Your Private Key" card
                        tvPrivateKey.setText(fKey);
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
}