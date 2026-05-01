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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.costumer_coincubby.SupabaseHelper.SupabaseHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private LockerAdapter adapter;
    private final List<Locker> lockers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.lockers_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        adapter = new LockerAdapter(lockers, locker -> {
            if (locker.getStatus() == Locker.Status.AVAILABLE) {
                showRentDialog(locker);
            } else {
                Toast.makeText(getContext(),
                        "Locker " + locker.getId() + " is not available.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setAdapter(adapter);
        loadLockers();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh every time this tab is shown so status changes are reflected
        loadLockers();
    }

    // ── Fetch all lockers from Supabase ───────────────────────────────────────
    private void loadLockers() {
        SupabaseHelper.fetchLockers(new SupabaseHelper.Callback() {
            @Override
            public void onSuccess(String body) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> parseAndDisplay(body));
                }
            }
            @Override
            public void onError(String error) {
                Log.e("HomeFragment", "fetchLockers error: " + error);
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "Failed to load lockers.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void parseAndDisplay(String json) {
        lockers.clear();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj    = arr.getJSONObject(i);
                int    dbId       = obj.getInt("locker_id");
                String number     = obj.optString("locker_number", String.valueOf(dbId));
                String statusStr  = obj.optString("status", "Available");
                int    sizeTypeId = obj.optInt("size_type_id", 1);

                // Map size_type_id → display label and hourly rate
                // Adjust these if your storage_size_type rows differ
                String size;
                double rate;
                switch (sizeTypeId) {
                    case 2:  size = "Medium M"; rate = 20.0; break;
                    case 3:  size = "Large L";  rate = 30.0; break;
                    default: size = "Small S";  rate = 10.0; break;
                }

                // Map DB status string → enum
                Locker.Status status;
                switch (statusStr) {
                    case "Occupied":          status = Locker.Status.OCCUPIED;         break;
                    case "Payment Required":  status = Locker.Status.PAYMENT_REQUIRED; break;
                    case "Maintenance":       status = Locker.Status.MAINTENANCE;      break;
                    default:                  status = Locker.Status.AVAILABLE;        break;
                }

                Locker locker = new Locker(number, size, status, rate);
                locker.setDbId(dbId);
                lockers.add(locker);
            }
        } catch (Exception e) {
            Log.e("HomeFragment", "parse error: " + e.getMessage());
        }

        adapter.notifyDataSetChanged();
        updateCounts();
    }

    // ── Update the header counts ──────────────────────────────────────────────
    private void updateCounts() {
        if (getView() == null) return;

        TextView tvTotal     = getView().findViewById(R.id.tv_total_count);
        TextView tvAvailable = getView().findViewById(R.id.tv_available_count);

        if (tvTotal != null) {
            tvTotal.setText(String.valueOf(lockers.size()));
        }

        if (tvAvailable != null) {
            long available = 0;
            for (Locker l : lockers) {
                if (l.getStatus() == Locker.Status.AVAILABLE) available++;
            }
            tvAvailable.setText(available + " available");
        }
    }

    // ── Open the rent bottom sheet ────────────────────────────────────────────
    private void showRentDialog(Locker locker) {
        RentLockerFragment fragment = RentLockerFragment.newInstance(
                locker.getId(),
                locker.getSize(),
                locker.getRate(),
                locker.getDbId());

        fragment.show(getParentFragmentManager(), "rent_locker");
    }
}