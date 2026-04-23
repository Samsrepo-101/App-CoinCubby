package com.example.costumer_coincubby.shared;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores the Supabase access token in SharedPreferences so it
 * persists across activities and fragments.
 *
 * Usage:
 *   Save on login:   SessionManager.saveAccessToken(context, token);
 *   Read anywhere:   SessionManager.getAccessToken(context);
 *   Clear on logout: SessionManager.clearSession(context);
 */
public class SessionManager {

    private static final String PREF_NAME     = "coincubby_session";
    private static final String KEY_TOKEN     = "access_token";
    private static final String KEY_USER_ID   = "user_id";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_EMAIL     = "email";

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    public static void saveAccessToken(Context ctx, String token) {
        prefs(ctx).edit().putString(KEY_TOKEN, token).apply();
    }

    public static void saveSession(Context ctx, String token, String userId,
                                   String fullName, String email) {
        prefs(ctx).edit()
                .putString(KEY_TOKEN,     token)
                .putString(KEY_USER_ID,   userId)
                .putString(KEY_FULL_NAME, fullName)
                .putString(KEY_EMAIL,     email)
                .apply();
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public static String getAccessToken(Context ctx) {
        return prefs(ctx).getString(KEY_TOKEN, null);
    }

    public static String getUserId(Context ctx) {
        return prefs(ctx).getString(KEY_USER_ID, null);
    }

    public static String getFullName(Context ctx) {
        return prefs(ctx).getString(KEY_FULL_NAME, null);
    }

    public static String getEmail(Context ctx) {
        return prefs(ctx).getString(KEY_EMAIL, null);
    }

    public static boolean isLoggedIn(Context ctx) {
        String token = getAccessToken(ctx);
        return token != null && !token.isEmpty();
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    public static void clearSession(Context ctx) {
        prefs(ctx).edit().clear().apply();
    }
}