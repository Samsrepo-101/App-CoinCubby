package com.example.costumer_coincubby;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.lockers_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        List<Locker> lockers = new ArrayList<>();
        lockers.add(new Locker("S1", "Small S", Locker.Status.AVAILABLE, 10.0));
        lockers.add(new Locker("S2", "Small S", Locker.Status.AVAILABLE, 10.0));
        lockers.add(new Locker("S3", "Small S", Locker.Status.MAINTENANCE, 10.0));
        lockers.add(new Locker("S4", "Small S", Locker.Status.PAYMENT_REQUIRED, 10.0));
        lockers.add(new Locker("M1", "Medium M", Locker.Status.OCCUPIED, 20.0));
        lockers.add(new Locker("M2", "Medium M", Locker.Status.OCCUPIED, 20.0));
        lockers.add(new Locker("L1", "Large L", Locker.Status.OCCUPIED, 30.0));
        lockers.add(new Locker("L2", "Large L", Locker.Status.OCCUPIED, 30.0));

        LockerAdapter adapter = new LockerAdapter(lockers, locker -> {
            if (locker.getStatus() == Locker.Status.AVAILABLE) {
                showRentDialog(locker);
            }
        });
        recyclerView.setAdapter(adapter);

        return view;
    }

    private void showRentDialog(Locker locker) {
        RentLockerFragment fragment = RentLockerFragment.newInstance(locker.getId(), locker.getSize(), locker.getRate());
        fragment.show(getParentFragmentManager(), "rent_locker");
    }
}