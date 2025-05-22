package com.cookandroid.project2025;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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

public class SearchActivity extends Fragment {

    private EditText editTextSearch, editTextGram;
    private TextView textViewResult;
    private Button buttonSearch, buttonSave;
    private ListView recentListView;

    private DataSnapshot latestSnapshot;

    private ArrayList<String> recentList = new ArrayList<>();
    private ArrayAdapter<String> recentAdapter;

    private static final String PREF_NAME = "RecentSearches";
    private static final String KEY_RECENT = "recent_keywords";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_search_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        editTextSearch = view.findViewById(R.id.editTextSearch);
        editTextGram = view.findViewById(R.id.editTextGram);
        textViewResult = view.findViewById(R.id.textViewResult);
        buttonSearch = view.findViewById(R.id.buttonSearch);
        buttonSave = view.findViewById(R.id.buttonSave);
        recentListView = view.findViewById(R.id.recentListView);

        loadRecentSearches();

        recentAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, recentList);
        recentListView.setAdapter(recentAdapter);

        recentListView.setOnItemClickListener((parent, view1, position, id) -> {
            String selected = recentList.get(position);
            editTextSearch.setText(selected);
            searchFood(selected);
        });

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
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("UserNutritionData").child("데이터");

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
                Toast.makeText(getContext(), "검색 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveFoodData() {
        if (latestSnapshot == null) {
            Toast.makeText(getContext(), "먼저 음식을 검색하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        int gram = 100;
        try {
            gram = Integer.parseInt(editTextGram.getText().toString().trim());
        } catch (NumberFormatException ignored) {}

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
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String foodName = latestSnapshot.child("음식명").getValue(String.class);

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("UserNutritionData").child(uid).child(today);

        userRef.child(foodName).setValue(adjustedSummary.toString())
                .addOnSuccessListener(unused -> Toast.makeText(getContext(), "섭취 정보 저장 완료!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveRecentSearch(String keyword) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> set = new LinkedHashSet<>(prefs.getStringSet(KEY_RECENT, new LinkedHashSet<>()));

        set.remove(keyword); // 중복 제거
        set.add(keyword);    // 최신 항목으로 추가

        // 최대 5개 유지
        while (set.size() > 5) {
            Iterator<String> it = set.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }

        prefs.edit().putStringSet(KEY_RECENT, set).apply();
        loadRecentSearches();
    }

    private void loadRecentSearches() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> set = prefs.getStringSet(KEY_RECENT, new LinkedHashSet<>());
        recentList.clear();
        recentList.addAll(set);
        if (recentAdapter != null) recentAdapter.notifyDataSetChanged();
    }
}
