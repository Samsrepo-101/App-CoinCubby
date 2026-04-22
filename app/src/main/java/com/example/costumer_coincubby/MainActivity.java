package com.example.costumer_coincubby;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    private LinearLayout navHome, navRent, navHistory, navProfile;
    private TextView navHomeText, navRentText, navHistoryText, navProfileText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navHome = findViewById(R.id.nav_home);
        navRent = findViewById(R.id.nav_rent);
        navHistory = findViewById(R.id.nav_history);
        navProfile = findViewById(R.id.nav_profile);

        navHomeText = findViewById(R.id.nav_home_text);
        navRentText = findViewById(R.id.nav_rent_text);
        navHistoryText = findViewById(R.id.nav_history_text);
        navProfileText = findViewById(R.id.nav_profile_text);

        navHome.setOnClickListener(v -> selectTab(0));
        navRent.setOnClickListener(v -> selectTab(1));
        navHistory.setOnClickListener(v -> selectTab(2));
        navProfile.setOnClickListener(v -> selectTab(3));

        // Start with Home
        selectTab(0);
    }

    public void selectTab(int index) {
        Fragment fragment = null;
        
        // Reset all tabs
        navHome.setBackgroundResource(0);
        navRent.setBackgroundResource(0);
        navHistory.setBackgroundResource(0);
        navProfile.setBackgroundResource(0);
        
        navHomeText.setVisibility(View.GONE);
        navRentText.setVisibility(View.GONE);
        navHistoryText.setVisibility(View.GONE);
        navProfileText.setVisibility(View.GONE);

        switch (index) {
            case 0:
                fragment = new HomeFragment();
                navHome.setBackgroundResource(R.drawable.nav_item_selected_bg);
                navHomeText.setVisibility(View.VISIBLE);
                break;
            case 1:
                fragment = new MyRentalFragment();
                navRent.setBackgroundResource(R.drawable.nav_item_selected_bg);
                navRentText.setVisibility(View.VISIBLE);
                break;
            case 2:
                fragment = new HistoryFragment();
                navHistory.setBackgroundResource(R.drawable.nav_item_selected_bg);
                navHistoryText.setVisibility(View.VISIBLE);
                break;
            case 3:
                fragment = new ProfileFragment();
                navProfile.setBackgroundResource(R.drawable.nav_item_selected_bg);
                navProfileText.setVisibility(View.VISIBLE);
                break;
        }

        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    public void showMyRentalWithDetails(String lockerId, String size, boolean isOpenTime, String duration) {
        MyRentalFragment fragment = MyRentalFragment.newInstance(lockerId, size, isOpenTime, duration);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        
        // Update Bottom Nav UI manually to reflect "Rent" tab selection
        navHome.setBackgroundResource(0);
        navRent.setBackgroundResource(R.drawable.nav_item_selected_bg);
        navHistory.setBackgroundResource(0);
        navProfile.setBackgroundResource(0);
        
        navHomeText.setVisibility(View.GONE);
        navRentText.setVisibility(View.VISIBLE);
        navHistoryText.setVisibility(View.GONE);
        navProfileText.setVisibility(View.GONE);
    }
}