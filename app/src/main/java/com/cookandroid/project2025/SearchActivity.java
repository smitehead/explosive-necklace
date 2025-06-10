package com.cookandroid.project2025;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import android.graphics.Color;
import android.view.Gravity;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;

import java.text.SimpleDateFormat;
import java.util.*;

public class SearchActivity extends AppCompatActivity {

    private EditText editTextSearch, editTextGram;
    private TextView textViewResult;
    private ImageButton backButton, buttonSearch;
    private Button buttonSave;
    private LinearLayout recentSearchContainer;

    private DataSnapshot latestSnapshot;
    private LinkedHashSet<String> recentSet = new LinkedHashSet<>();

    private static final String PREF_NAME = "RecentSearches";
    private static final String KEY_RECENT = "recent_keywords";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_fragment);

        editTextSearch = findViewById(R.id.editTextSearch);
        editTextGram = findViewById(R.id.editTextGram);
        textViewResult = findViewById(R.id.textViewResult);
        buttonSearch = findViewById(R.id.buttonSearch);
        buttonSave = findViewById(R.id.buttonSave);
        recentSearchContainer = findViewById(R.id.recentSearchContainer);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> {
            finish();
        });

        loadRecentSearches();

        buttonSearch.setOnClickListener(v -> {
            String query = editTextSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                saveRecentSearch(query);
                searchFood(query);
            }
        });

        buttonSave.setOnClickListener(v -> saveFoodData());
    }

    private void searchFood(String foodName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("데이터");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot item : snapshot.getChildren()) {
                    String dbName = item.child("음식명").getValue(String.class);
                    if (dbName != null && dbName.trim().equalsIgnoreCase(foodName)) {
                        latestSnapshot = item;

                        StringBuilder result = new StringBuilder();
                        for (DataSnapshot nutrient : item.getChildren()) {
                            result.append(nutrient.getKey()).append(": ").append(nutrient.getValue()).append("\n");
                        }
                        textViewResult.setText(result.toString());
                        return;
                    }
                }
                textViewResult.setText("검색 결과가 없습니다.");
                latestSnapshot = null;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SearchActivity.this, "검색 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveFoodData() {
        if (latestSnapshot == null) {
            Toast.makeText(this, "먼저 음식을 검색하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        int gram = 100;
        try {
            gram = Integer.parseInt(editTextGram.getText().toString().trim());
        } catch (NumberFormatException ignored) {
        }

        double ratio = gram / 100.0;

        StringBuilder adjustedSummary = new StringBuilder();
        for (DataSnapshot nutrient : latestSnapshot.getChildren()) {
            String key = nutrient.getKey();
            if (key.equals("음식명") || key.equals("중량")) continue;

            try {
                double value = Double.parseDouble(nutrient.getValue().toString());
                double adjusted = Math.round(value * ratio * 10) / 10.0;
                adjustedSummary.append(key).append(": ").append(adjusted).append("\n");
            } catch (Exception e) {
                adjustedSummary.append(key).append(": ").append(nutrient.getValue()).append("\n");
            }
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String foodName = latestSnapshot.child("음식명").getValue(String.class);

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("UserNutritionData")
                .child(uid)
                .child(today);

        userRef.child(foodName).setValue(adjustedSummary.toString())
                .addOnSuccessListener(unused -> Toast.makeText(this, "섭취 정보 저장 완료!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveRecentSearch(String keyword) {
        recentSet.remove(keyword);
        recentSet.add(keyword);

        while (recentSet.size() > 5) {
            Iterator<String> it = recentSet.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(KEY_RECENT, recentSet).apply();

        refreshChipView();
    }

    private void loadRecentSearches() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> savedSet = prefs.getStringSet(KEY_RECENT, new LinkedHashSet<>());
        recentSet.clear();
        recentSet.addAll(savedSet);

        refreshChipView();
    }

    private void refreshChipView() {
        recentSearchContainer.removeAllViews();

        for (String keyword : recentSet) {
            TextView chip = new TextView(this);
            chip.setText(keyword);
            chip.setBackgroundResource(R.drawable.chip_background);
            chip.setPadding(40, 20, 40, 20);
            chip.setTextColor(Color.BLACK);
            chip.setTextSize(14);
            chip.setGravity(Gravity.CENTER);

            Typeface font = ResourcesCompat.getFont(this, R.font.pretendard_medium);
            chip.setTypeface(font);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 24, 0);
            chip.setLayoutParams(params);

            chip.setOnClickListener(v -> {
                editTextSearch.setText(keyword);
                searchFood(keyword);
            });

            recentSearchContainer.addView(chip);
        }
    }
}