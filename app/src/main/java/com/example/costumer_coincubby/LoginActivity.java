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


import com.example.costumer_coincubby.shared.SessionManager;

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

public class LoginActivity extends AppCompatActivity {

    private static final String SUPABASE_URL  = "https://cjuimxgxovdmijuenagr.supabase.co";
    private static final String SUPABASE_ANON = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
            + ".eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNqdWlteGd4b3ZkbWlqdWVuYWdyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzY0MzQ0OTEsImV4cCI6MjA5MjAxMDQ5MX0"
            + ".t6ixuFiD2iYzrNZsc1QjG3gpdTdBuMY37qTKzwxdg18";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private EditText        etEmail;
    private EditText        etPassword;
    private AppCompatButton btnContinue;
    private TextView        tvCreateAccount;

    private final OkHttpClient http = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // ── If already logged in, skip login screen entirely ──────────────────
        if (SessionManager.isLoggedIn(this)) {
            goToMain();
            return;
        }

        setContentView(R.layout.login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etEmail         = findViewById(R.id.etEmail);
        etPassword      = findViewById(R.id.etPassword);
        btnContinue     = findViewById(R.id.btnContinue);
        tvCreateAccount = findViewById(R.id.tvCreateAccount);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnContinue.setOnClickListener(v -> attemptLogin());
        tvCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Please enter your email");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Please enter your password");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        JSONObject body = new JSONObject();
        try {
            body.put("email",    email);
            body.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/token?grant_type=password")
                .addHeader("apikey",       SUPABASE_ANON)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this,
                            "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    setLoading(false);
                    if (response.isSuccessful()) {
                        onLoginSuccess(responseBody);
                    } else {
                        handleAuthError(responseBody);
                    }
                });
            }
        });
    }

    private void onLoginSuccess(String responseBody) {
        try {
            JSONObject json        = new JSONObject(responseBody);
            String     accessToken = json.optString("access_token", "");

            if (!accessToken.isEmpty()) {

                // ── Extract user info from the Supabase response ──────────────
                String userId   = "";
                String fullName = "";
                String email    = "";

                JSONObject userObj = json.optJSONObject("user");
                if (userObj != null) {
                    userId = userObj.optString("id", "");
                    email  = userObj.optString("email", "");

                    // full_name was stored in user_metadata during registration
                    JSONObject meta = userObj.optJSONObject("user_metadata");
                    if (meta != null) {
                        fullName = meta.optString("full_name", "");
                    }
                }

                // ── Save session so ProfileFragment can read it ───────────────
                SessionManager.saveSession(this, accessToken, userId, fullName, email);

                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                goToMain();

            } else {
                Toast.makeText(this, "Login failed. No token received.", Toast.LENGTH_LONG).show();
            }

        } catch (JSONException e) {
            Toast.makeText(this, "Unexpected response. Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleAuthError(String responseBody) {
        try {
            JSONObject json  = new JSONObject(responseBody);
            String     error = json.optString("error", "");
            String     msg   = json.optString("error_description",
                    json.optString("msg",
                            json.optString("message", "Invalid email or password.")));

            if (error.equals("invalid_grant") && msg.contains("Email not confirmed")) {
                Toast.makeText(this,
                        "Please confirm your email before logging in.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Invalid email or password.", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        btnContinue.setEnabled(!loading);
        btnContinue.setText(loading ? "Logging in…" : "Continue");
    }

    private void showForgotPasswordDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int paddingDp = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(paddingDp, paddingDp, paddingDp, paddingDp);

        final android.widget.EditText etResetEmail = new android.widget.EditText(this);
        etResetEmail.setHint("email@example.com");
        etResetEmail.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        String currentEmail = etEmail.getText().toString().trim();
        if (!currentEmail.isEmpty()) {
            etResetEmail.setText(currentEmail);
        }

        layout.addView(etResetEmail);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your email address to receive a password reset link.")
                .setView(layout)
                .setPositiveButton("Send Reset Link", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String email = etResetEmail.getText().toString().trim();
            if (email.isEmpty()) {
                etResetEmail.setError("Email is required");
                etResetEmail.requestFocus();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etResetEmail.setError("Enter a valid email address");
                etResetEmail.requestFocus();
                return;
            }

            dialog.dismiss();
            sendResetPasswordEmail(email);
        });
    }

    private void sendResetPasswordEmail(String email) {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/recover")
                .addHeader("apikey",       SUPABASE_ANON)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this,
                            "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Password reset email sent! Please check your inbox.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            String msg = json.optString("message", "Failed to send reset link.");
                            Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Toast.makeText(LoginActivity.this,
                                    "Failed to send reset link.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}