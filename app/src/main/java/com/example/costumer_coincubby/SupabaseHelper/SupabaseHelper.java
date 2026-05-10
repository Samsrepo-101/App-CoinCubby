package com.example.costumer_coincubby.SupabaseHelper;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseHelper {

    private static final String BASE_URL = "https://cjuimxgxovdmijuenagr.supabase.co/rest/v1";
    private static final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
            + ".eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNqdWlteGd4b3ZkbWlqdWVuYWdyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzY0MzQ0OTEsImV4cCI6MjA5MjAxMDQ5MX0"
            + ".t6ixuFiD2iYzrNZsc1QjG3gpdTdBuMY37qTKzwxdg18";

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(String responseBody);
        void onError(String error);
    }

    // ── Upsert customer ───────────────────────────────────────────────────────
    public static void upsertCustomer(String customerId, String fullName, String email, Callback cb) {
        String json = String.format(
                "{\"customer_id\":\"%s\",\"full_name\":\"%s\",\"email\":\"%s\"}",
                customerId, fullName, email);
        post("/customers?on_conflict=customer_id", json, true, cb);
    }

    // ── Insert transaction (let DB generate transaction_id) ───────────────────
    public static void insertTransaction(String txJson, Callback cb) {
        // Prefer: return=representation so we get the generated UUID back
        postAndReturn("/transactions", txJson, cb);
    }

    // ── Update locker status ──────────────────────────────────────────────────
    public static void updateLockerStatus(int lockerId, String status, Callback cb) {
        String json = "{\"status\":\"" + status + "\"}";
        patch("/lockers?locker_id=eq." + lockerId, json, cb);
    }

    // ── Fetch rates (to get correct rate_id) ─────────────────────────────────
    public static void fetchRates(Callback cb) {
        get("/rates?select=rate_id,size_type_id,price_per_minute,min_charge_minutes", cb);
    }

    // ── Fetch lockers with their current status ───────────────────────────────
    public static void fetchLockers(Callback cb) {
        get("/lockers?select=locker_id,locker_number,status,size_type_id", cb);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private HTTP helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static void post(String path, String jsonBody, boolean upsert, Callback cb) {
        executor.execute(() -> {
            try {
                HttpURLConnection conn = openConnection(path, "POST");
                conn.setRequestProperty("Prefer", upsert
                        ? "resolution=merge-duplicates,return=minimal"
                        : "return=minimal");
                writeBody(conn, jsonBody);
                handleResponse(conn, cb);
            } catch (Exception e) {
                Log.e("SupabaseHelper", "post failed", e);
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    // POST and return the created row (so we can read generated fields)
    private static void postAndReturn(String path, String jsonBody, Callback cb) {
        executor.execute(() -> {
            try {
                HttpURLConnection conn = openConnection(path, "POST");
                conn.setRequestProperty("Prefer", "return=representation");
                writeBody(conn, jsonBody);
                handleResponse(conn, cb);
            } catch (Exception e) {
                Log.e("SupabaseHelper", "postAndReturn failed", e);
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    private static void patch(String path, String jsonBody, Callback cb) {
        executor.execute(() -> {
            try {
                HttpURLConnection conn = openConnection(path, "PATCH");
                conn.setRequestProperty("Prefer", "return=minimal");
                writeBody(conn, jsonBody);
                handleResponse(conn, cb);
            } catch (Exception e) {
                Log.e("SupabaseHelper", "patch failed", e);
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    private static void get(String path, Callback cb) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + ANON_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                handleResponse(conn, cb);
            } catch (Exception e) {
                Log.e("SupabaseHelper", "get failed", e);
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    private static HttpURLConnection openConnection(String path, String method) throws Exception {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("apikey", ANON_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + ANON_KEY);
        conn.setDoOutput(true);
        return conn;
    }

    private static void writeBody(HttpURLConnection conn, String body) throws Exception {
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void handleResponse(HttpURLConnection conn, Callback cb) throws Exception {
        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            // Read body — useful when return=representation
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            final String body = sb.toString();
            mainHandler.post(() -> cb.onSuccess(body.isEmpty() ? "ok" : body));
        } else {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            final String err = "HTTP " + code + ": " + sb;
            Log.e("SupabaseHelper", err);
            mainHandler.post(() -> cb.onError(err));
        }
    }
    public static void insertPayment(String paymentJson, Callback cb) {
        post("/payments", paymentJson, false, cb);
    }
    public static void fetchActiveRentals(String customerId, Callback cb) {
        get("/transactions?select=transaction_id,locker_id,rate_id,start_time,end_time,"
                + "duration_minutes,status,qr_token,"
                + "lockers(locker_number,size_type_id),"
                + "rates(price_per_minute,min_charge_minutes)"
                + "&customer_id=eq." + customerId
                + "&status=eq.Active"
                + "&order=start_time.desc", cb);
    }

    public static void updateTransactionStatus(String transactionId, String status, Callback cb) {
        String json = "{\"status\":\"" + status + "\"}";
        patch("/transactions?transaction_id=eq." + transactionId, json, cb);
    }

    public static void completeTransaction(String transactionId, String endTime, int durationMinutes, Callback cb) {
        String json = String.format(Locale.US,
                "{\"status\":\"Completed\",\"end_time\":\"%s\",\"duration_minutes\":%d}",
                endTime, durationMinutes);
        patch("/transactions?transaction_id=eq." + transactionId, json, cb);
    }
    public static void fetchRentalHistory(String customerId, Callback cb) {
        get("/transactions?select=transaction_id,start_time,end_time,duration_minutes,"
                + "status,qr_token,"
                + "lockers(locker_number,size_type_id),"
                + "payments(amount,payment_method)"
                + "&customer_id=eq." + customerId
                + "&order=start_time.desc", cb);
    }
}