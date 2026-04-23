package com.example.costumer_coincubby;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

public class RegisterActivity extends AppCompatActivity {

    private static final String SUPABASE_URL  = "https://cjuimxgxovdmijuenagr.supabase.co";
    private static final String SUPABASE_ANON = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNqdWlteGd4b3ZkbWlqdWVuYWdyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzY0MzQ0OTEsImV4cCI6MjA5MjAxMDQ5MX0.t6ixuFiD2iYzrNZsc1QjG3gpdTdBuMY37qTKzwxdg18";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private EditText        etFirstName;
    private EditText        etLastName;
    private EditText        etEmail;
    private EditText        etPassword;
    private EditText        etConfirmPassword;
    private AppCompatButton btnRegister;
    private TextView        tvBackToLogin;

    private final OkHttpClient http = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.register);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        bindViews();
        setListeners();
    }

    private void bindViews() {
        etFirstName       = findViewById(R.id.etFirstName);
        etLastName        = findViewById(R.id.etLastName);
        etEmail           = findViewById(R.id.etEmailRegister);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister       = findViewById(R.id.btnRegister);
        tvBackToLogin     = findViewById(R.id.tvBackToLogin);
    }

    private void setListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());
        tvBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void attemptRegister() {
        String firstName  = etFirstName.getText().toString().trim();
        String lastName   = etLastName.getText().toString().trim();
        String email      = etEmail.getText().toString().trim();
        String password   = etPassword.getText().toString();
        String confirmPwd = etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("First name is required");
            etFirstName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(lastName)) {
            etLastName.setError("Last name is required");
            etLastName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPwd)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        setLoading(true);

        // ── Send email + password to Supabase Auth ────────────────────────────
        // Supabase hashes the password automatically using bcrypt.
        // We never touch the plain text password after this point.
        JSONObject body = new JSONObject();
        try {
            body.put("email",    email);
            body.put("password", password); // Supabase hashes this — we never store it ourselves

            JSONObject meta = new JSONObject();
            meta.put("first_name", firstName);
            meta.put("last_name",  lastName);
            meta.put("full_name",  firstName + " " + lastName);
            meta.put("role",       "resident");
            body.put("data", meta);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/signup")
                .addHeader("apikey",       SUPABASE_ANON)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(RegisterActivity.this,
                            "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    setLoading(false);
                    if (response.isSuccessful()) {
                        onRegistrationSuccess(responseBody);
                    } else {
                        handleAuthError(responseBody);
                    }
                });
            }
        });
    }

    private void onRegistrationSuccess(String responseBody) {
        try {
            JSONObject json        = new JSONObject(responseBody);
            String     accessToken = json.optString("access_token", "");
            String     userId      = json.optJSONObject("user") != null
                    ? json.getJSONObject("user").optString("id", "") : "";

            if (!userId.isEmpty()) {
                insertCustomerRecord(userId, accessToken);
            } else {
                Toast.makeText(this,
                        "Registration successful! Please check your email to confirm your account.",
                        Toast.LENGTH_LONG).show();
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        } catch (JSONException e) {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        }
    }

    private void insertCustomerRecord(String userId, String accessToken) {
        String firstName = etFirstName.getText().toString().trim();
        String lastName  = etLastName.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();

        // ⚠️ Password is intentionally NOT inserted here.
        // Supabase Auth stores a bcrypt hash of the password in auth.users internally.
        // Storing it again in the customers table (even hashed) is redundant and insecure.
        JSONObject body = new JSONObject();
        try {
            body.put("customer_id", userId);
            body.put("full_name",   firstName + " " + lastName);
            body.put("email",       email);
            body.put("role",        "resident");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/customers")
                .addHeader("apikey",       SUPABASE_ANON)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer",       "return=minimal")
                .post(RequestBody.create(body.toString(), JSON));

        // Use the user's own token if available (preferred), otherwise fall back to anon key
        requestBuilder.addHeader("Authorization",
                "Bearer " + (!accessToken.isEmpty() ? accessToken : SUPABASE_ANON));

        http.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("SUPABASE_INSERT", "Network error: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this,
                            "Account created but profile save failed.", Toast.LENGTH_LONG).show();
                    goToLogin();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String errorBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    android.util.Log.e("SUPABASE_INSERT", "Error " + response.code() + ": " + errorBody);
                }
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this,
                            response.isSuccessful()
                                    ? "Account created successfully!"
                                    : "Account created! (Profile record pending)",
                            Toast.LENGTH_LONG).show();
                    goToLogin();
                });
            }
        });
    }

    private void goToLogin() {
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        finish();
    }

    private void handleAuthError(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            String msg = json.optString("error_description",
                    json.optString("msg",
                            json.optString("message", "Registration failed. Try again.")));
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        } catch (JSONException e) {
            Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? "Creating account…" : "Create Account");
    }
}