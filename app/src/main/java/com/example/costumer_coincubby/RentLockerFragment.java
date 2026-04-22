package com.example.costumer_coincubby;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class RentLockerFragment extends BottomSheetDialogFragment {

    private static final String ARG_ID = "locker_id";
    private static final String ARG_SIZE = "locker_size";
    private static final String ARG_RATE = "locker_rate";

    private boolean isOpenTime = false;
    private double ratePerHr;

    public static RentLockerFragment newInstance(String id, String size, double rate) {
        RentLockerFragment fragment = new RentLockerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
        args.putString(ARG_SIZE, size);
        args.putDouble(ARG_RATE, rate);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rent_locker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String id = getArguments().getString(ARG_ID);
        String size = getArguments().getString(ARG_SIZE);
        ratePerHr = getArguments().getDouble(ARG_RATE);

        TextView title = view.findViewById(R.id.rent_title);
        TextView subtitle = view.findViewById(R.id.rent_subtitle);
        EditText etDuration = view.findViewById(R.id.et_duration);
        TextView tvTotal = view.findViewById(R.id.tv_total);
        RadioGroup rgRentalType = view.findViewById(R.id.rg_rental_type);
        RadioGroup rgPayment = view.findViewById(R.id.rg_payment);
        View llDurationInput = view.findViewById(R.id.ll_duration_input);
        View cardWallet = view.findViewById(R.id.card_wallet);

        title.setText("Rent Locker " + id);
        subtitle.setText("Size: " + size + "  Rate: ₱" + (int)ratePerHr + "/hr");

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
                updateTotal(etDuration.getText().toString(), tvTotal);
            }
        });

        etDuration.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTotal(s.toString(), tvTotal);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        view.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
        
        view.findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                String duration = isOpenTime ? "Open" : etDuration.getText().toString();
                ((MainActivity) getActivity()).showMyRentalWithDetails(id, size, isOpenTime, duration);
            }
            dismiss();
        });
    }

    private void updateTotal(String durationStr, TextView tvTotal) {
        if (isOpenTime) return;
        try {
            int hrs = Integer.parseInt(durationStr);
            tvTotal.setText("₱" + String.format("%.2f", hrs * ratePerHr));
        } catch (NumberFormatException e) {
            tvTotal.setText("₱0.00");
        }
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialog;
    }
}