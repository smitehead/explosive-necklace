package com.cookandroid.project2025;

import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import androidx.appcompat.widget.AppCompatButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
    private TextView summaryTextView, tvMonthYear;
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
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnToday = view.findViewById(R.id.btnToday);
        scrollView = view.findViewById(R.id.scrollView);
        summaryTextView = view.findViewById(R.id.summaryTextView);

        todayCalendar = Calendar.getInstance();
        currentMonthCalendar = (Calendar) todayCalendar.clone();

        btnPrevMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, -1);
            renderMonth();
        });

        btnToday.setOnClickListener(v -> {
            currentMonthCalendar = (Calendar) todayCalendar.clone();
            renderMonth();
            btnToday.post(() -> {
                for (int i = 0; i < allDates.size(); i++) {
                    if (isSameDay(allDates.get(i), todayCalendar)) {
                        allDateButtons.get(i).performClick();
                        break;
                    }
                }
            });
        });

        renderMonth();
        datesContainer.post(() -> {
            for (int i = 0; i < allDates.size(); i++) {
                if (isSameDay(allDates.get(i), todayCalendar)) {
                    allDateButtons.get(i).performClick();
                    break;
                }
            }
        });
    }

    private void renderMonth() {
        datesContainer.removeAllViews();
        allDateButtons.clear();
        allDates.clear();

        Calendar temp = (Calendar) currentMonthCalendar.clone();
        temp.set(Calendar.DAY_OF_MONTH, 1);
        int maxDay = temp.getActualMaximum(Calendar.DAY_OF_MONTH);

        tvMonthYear.setText(new SimpleDateFormat("yyyy년 MMMM", Locale.getDefault()).format(temp.getTime()));

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int buttonWidth = screenWidth / 7 - 20;
        int buttonHeight = 200;

        for (int i = 1; i <= maxDay; i++) {
            Calendar date = (Calendar) temp.clone();
            date.set(Calendar.DAY_OF_MONTH, i);

            AppCompatButton btn = new AppCompatButton(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(buttonWidth, buttonHeight);
            params.setMargins(10, 0, 10, 0);
            btn.setLayoutParams(params);
            String dayLabel = new SimpleDateFormat("E", Locale.KOREAN).format(date.getTime());
            String dayNumber = new SimpleDateFormat("d", Locale.KOREAN).format(date.getTime());
            String todayLabel = isSameDay(date, todayCalendar) ? "오늘" : dayLabel;

            SpannableString span = new SpannableString(todayLabel + "\n" + dayNumber);
            int start = todayLabel.length() + 1;
            int end = span.length();

            span.setSpan(new RelativeSizeSpan(1.2f), start, end, 0);
            span.setSpan(new android.text.style.TypefaceSpan("sans-serif-bold"), start, end, 0);

            btn.setText(span);

            btn.setAllCaps(false);
            btn.setTextSize(14);
            btn.setTag(date);

            if (isSameDay(date, todayCalendar)) {
                btn.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.date_today_background));
                btn.setTextColor(Color.WHITE);
            } else {
                btn.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.date_default_background));
                btn.setTextColor(Color.BLACK);
            }



            btn.setOnClickListener(v -> {
                Calendar clickedDate = (Calendar) btn.getTag();

                if (clickedDate.get(Calendar.MONTH) != currentMonthCalendar.get(Calendar.MONTH)
                        || clickedDate.get(Calendar.YEAR) != currentMonthCalendar.get(Calendar.YEAR)) {
                    currentMonthCalendar = (Calendar) clickedDate.clone();
                    renderMonth();
                    datesContainer.post(() -> {
                        for (int j = 0; j < allDates.size(); j++) {
                            if (isSameDay(allDates.get(j), clickedDate)) {
                                allDateButtons.get(j).performClick();
                                break;
                            }
                        }
                    });
                    return;
                }

                for (Button b : allDateButtons) {
                    Calendar bDate = (Calendar) b.getTag();
                    if (isSameDay(bDate, todayCalendar)) {
                        b.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.date_today_background));
                        b.setTextColor(Color.WHITE);
                    } else {
                        b.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.date_default_background));
                        b.setTextColor(Color.BLACK);
                    }
                }


                btn.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.date_selected_background));
                btn.setTextColor(Color.WHITE);
                selectedButton = btn;

                String selectedDateFormatted = new SimpleDateFormat("MM월 dd일", Locale.getDefault()).format(clickedDate.getTime());
                summaryTextView.setText(selectedDateFormatted + " 총 섭취량");

                scrollToDate(clickedDate);
                loadFirebaseNutritionData(clickedDate);
            });

            allDateButtons.add(btn);
            allDates.add(date);
            datesContainer.addView(btn);
        }
    }

    private void scrollToDate(Calendar targetDate) {
        for (int i = 0; i < allDates.size(); i++) {
            if (isSameDay(allDates.get(i), targetDate)) {
                View target = allDateButtons.get(i);
                if (target != null) {
                    int scrollX = target.getLeft() - (scrollView.getWidth() / 2) + (target.getWidth() / 2);
                    scrollView.post(() -> scrollView.smoothScrollTo(scrollX, 0));
                }
                break;
            }
        }
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private void loadFirebaseNutritionData(Calendar selectedDate) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        String selectedDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());

        DatabaseReference userInfoRef = FirebaseDatabase.getInstance().getReference("UserAccount").child(uid);
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("UserNutritionData").child(uid).child(selectedDateStr);

        userInfoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String gender = snapshot.child("gender").getValue(String.class);
                int age = snapshot.child("age").getValue(Integer.class);

                double kcalGoal = ("여자".equals(gender)) ? (age < 30 ? 2100 : 1900) : (age < 30 ? 2600 : 2400);
                double carbsGoal = ("여자".equals(gender)) ? (age < 30 ? 300 : 270) : (age < 30 ? 330 : 300);
                double proteinGoal = ("여자".equals(gender)) ? (age < 30 ? 80 : 70) : (age < 30 ? 100 : 90);
                double fatGoal = ("여자".equals(gender)) ? (age < 30 ? 60 : 55) : (age < 30 ? 80 : 70);

                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        double totalEnergy = 0, totalCarbs = 0, totalProtein = 0, totalFat = 0;

                        for (DataSnapshot foodSnap : snapshot.getChildren()) {
                            String value = foodSnap.getValue(String.class);
                            if (value == null) continue;

                            String[] lines = value.split("\n");
                            for (String line : lines) {
                                line = line.toLowerCase();
                                if (line.contains("에너지") || line.contains("kcal") || line.contains("칼로리")) {
                                    totalEnergy += extractFloat(line);
                                } else if (line.contains("탄수화물")) {
                                    totalCarbs += extractFloat(line);
                                } else if (line.contains("단백질")) {
                                    totalProtein += extractFloat(line);
                                } else if (line.contains("지방")) {
                                    totalFat += extractFloat(line);
                                }
                            }
                        }

                        kcalTextView.setText(String.format("%,d k", (int) totalEnergy));
                        carbsTextView.setText(String.format("%,d g", (int) totalCarbs));
                        proteinTextView.setText(String.format("%,d g", (int) totalProtein));
                        fatTextView.setText(String.format("%,d g", (int) totalFat));

                        progressKcal.setProgress(Math.min((int) ((totalEnergy / kcalGoal) * 100), 100));
                        progressCarbs.setProgress(Math.min((int) ((totalCarbs / carbsGoal) * 100), 100));
                        progressProtein.setProgress(Math.min((int) ((totalProtein / proteinGoal) * 100), 100));
                        progressFat.setProgress(Math.min((int) ((totalFat / fatGoal) * 100), 100));

                        // 색상 설정 (100% 초과 시 빨강, 이하면 파랑)
                        if ((totalEnergy / kcalGoal) * 100 > 100) {
                            progressKcal.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.red));
                        } else {
                            progressKcal.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.blue));
                        }

                        if ((totalCarbs / carbsGoal) * 100 > 100) {
                            progressCarbs.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.red));
                        } else {
                            progressCarbs.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.blue));
                        }

                        if ((totalProtein / proteinGoal) * 100 > 100) {
                            progressProtein.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.red));
                        } else {
                            progressProtein.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.blue));
                        }

                        if ((totalFat / fatGoal) * 100 > 100) {
                            progressFat.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.red));
                        } else {
                            progressFat.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.blue));
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "사용자 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double extractFloat(String line) {
        try {
            String value = line.split(":")[1].trim().replaceAll("[^\\d.]", "");
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }
}
