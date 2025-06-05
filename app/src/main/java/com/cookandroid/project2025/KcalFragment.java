package com.cookandroid.project2025;

import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class KcalFragment extends Fragment {

    private TextView kcalTextView, carbsTextView, proteinTextView, fatTextView;
    private ProgressBar progressKcal, progressCarbs, progressProtein, progressFat;

    private LinearLayout datesContainer, detailLayout;
    private TextView detailText, tvMonthYear;
    private ImageButton btnPrevMonth;
    private Button btnToday;
    private HorizontalScrollView scrollView;

    private Calendar todayCalendar;
    private Calendar currentMonthCalendar;
    private Button selectedButton = null;
    private final List<Button> allDateButtons = new ArrayList<>();
    private final List<Calendar> allDates = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_kcal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        kcalTextView = view.findViewById(R.id.kcalText);
        carbsTextView = view.findViewById(R.id.carbsText);
        proteinTextView = view.findViewById(R.id.proteinText);
        fatTextView = view.findViewById(R.id.fatText);

        progressKcal = view.findViewById(R.id.progressKcal);
        progressCarbs = view.findViewById(R.id.progressCarbs);
        progressProtein = view.findViewById(R.id.progressProtein);
        progressFat = view.findViewById(R.id.progressFat);

        datesContainer = view.findViewById(R.id.datesContainer);
        detailLayout = view.findViewById(R.id.detailLayout);
        detailText = view.findViewById(R.id.detailText);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnToday = view.findViewById(R.id.btnToday);
        scrollView = view.findViewById(R.id.scrollView);

        todayCalendar = Calendar.getInstance();
        currentMonthCalendar = (Calendar) todayCalendar.clone();

        btnPrevMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, -1);
            renderMonth();
        });

        btnToday.setOnClickListener(v -> {
            currentMonthCalendar = (Calendar) todayCalendar.clone();
            renderMonth();
        });

        renderMonth();
    }

    private void renderMonth() {
        datesContainer.removeAllViews();
        allDateButtons.clear();
        allDates.clear();

        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentMonthCalendar.getTime()));

        Calendar calendar = (Calendar) currentMonthCalendar.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 1; i <= maxDay; i++) {
            Calendar date = (Calendar) calendar.clone();
            date.set(Calendar.DAY_OF_MONTH, i);
            allDates.add(date);

            Button dayButton = new Button(requireContext());
            dayButton.setText(String.valueOf(i));
            dayButton.setTextColor(Color.BLACK);
            dayButton.setBackgroundResource(android.R.drawable.btn_default);
            datesContainer.addView(dayButton);

            dayButton.setOnClickListener(v -> {
                if (selectedButton != null) {
                    selectedButton.setBackgroundResource(android.R.drawable.btn_default);
                }
                selectedButton = dayButton;
                selectedButton.setBackgroundColor(Color.LTGRAY);

                fetchNutritionData(date);
            });

            allDateButtons.add(dayButton);
        }
    }

    private void fetchNutritionData(Calendar date) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateKey = sdf.format(date.getTime());

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                .child(user.getUid())
                .child("foodRecords")
                .child(dateKey);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalKcal = 0, totalCarbs = 0, totalProtein = 0, totalFat = 0;

                for (DataSnapshot item : snapshot.getChildren()) {
                    totalKcal += item.child("kcal").getValue(Integer.class);
                    totalCarbs += item.child("carbs").getValue(Integer.class);
                    totalProtein += item.child("protein").getValue(Integer.class);
                    totalFat += item.child("fat").getValue(Integer.class);
                }

                kcalTextView.setText(totalKcal + "kcal");
                carbsTextView.setText(totalCarbs + "g");
                proteinTextView.setText(totalProtein + "g");
                fatTextView.setText(totalFat + "g");

                progressKcal.setProgress(totalKcal);
                progressCarbs.setProgress(totalCarbs);
                progressProtein.setProgress(totalProtein);
                progressFat.setProgress(totalFat);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "데이터 불러오기 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
