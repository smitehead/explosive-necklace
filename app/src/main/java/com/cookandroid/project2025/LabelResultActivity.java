package com.cookandroid.project2025;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

public class LabelResultActivity extends AppCompatActivity {

    private TextView nutritionTextView;
    private Button buttonConfirmSave;
    private String nutritionJson;

    private final String[] nutritionNames = {
            "에너지", "탄수화물", "지방", "단백질"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label_result_fragment);

        nutritionTextView = findViewById(R.id.nutritionTextView);
        buttonConfirmSave = findViewById(R.id.buttonConfirmSave);
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // 전달받은 JSON 문자열 받기
        nutritionJson = getIntent().getStringExtra("nutritionJson");

        if (nutritionJson != null) {
            parseAndDisplay(nutritionJson);
        } else {
            nutritionTextView.setText("영양 정보가 없습니다.");
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
                Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = user.getUid();
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String timestamp = new SimpleDateFormat("HHmmss", Locale.getDefault()).format(new Date());

            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("UserNutritionData").child(uid).child(today);

            ref.child("성분표_" + timestamp).setValue(summary)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "오늘 섭취량에 저장되었습니다.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        } catch (JSONException e) {
            Toast.makeText(this, "JSON 파싱 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
