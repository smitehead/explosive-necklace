package com.cookandroid.project2025;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LabelResultActivity extends Fragment {

    private static final String ARG_JSON = "nutritionJson";
    private String nutritionJson;
    private TextView nutritionTextView;
    private Button buttonConfirmSave;

    private final String[] nutritionNames = {
            "에너지", "탄수화물", "당류", "지방", "단백질", "칼슘", "인",
            "나트륨", "칼륨", "마그네슘", "철", "아연", "콜레스테롤", "트랜스지방"
    };

    public static LabelResultActivity newInstance(String json) {
        LabelResultActivity fragment = new LabelResultActivity();
        Bundle args = new Bundle();
        args.putString(ARG_JSON, json);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_label_result_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nutritionTextView = view.findViewById(R.id.nutritionTextView);
        buttonConfirmSave = view.findViewById(R.id.buttonConfirmSave);

        if (getArguments() != null) {
            nutritionJson = getArguments().getString(ARG_JSON);
            parseAndDisplay(nutritionJson);
        }

        buttonConfirmSave.setOnClickListener(v -> saveNutritionToFirebase());
    }

    private void parseAndDisplay(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray textArray = jsonObject.getJSONArray("text").getJSONArray(2);

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < nutritionNames.length && i < textArray.length(); i++) {
                String value = textArray.isNull(i) ? "N/A" : textArray.getString(i);
                builder.append(nutritionNames[i]).append(": ").append(value).append("\n");
            }

            nutritionTextView.setText(builder.toString());

        } catch (JSONException e) {
            nutritionTextView.setText("JSON 파싱 오류: " + e.getMessage());
        }
    }

    private void saveNutritionToFirebase() {
        try {
            JSONObject jsonObject = new JSONObject(nutritionJson);
            JSONArray textArray = jsonObject.getJSONArray("text").getJSONArray(2);

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < nutritionNames.length && i < textArray.length(); i++) {
                String value = textArray.isNull(i) ? "N/A" : textArray.getString(i);
                builder.append(nutritionNames[i]).append(": ").append(value).append("\n");
            }

            String summary = builder.toString();

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = user.getUid();
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String timestamp = new SimpleDateFormat("HHmmss", Locale.getDefault()).format(new Date());

            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("UserNutritionData").child(uid).child(today);

            ref.child("성분표_" + timestamp).setValue(summary)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(getContext(), "오늘 섭취량에 저장되었습니다.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        } catch (JSONException e) {
            Toast.makeText(getContext(), "JSON 파싱 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}