package com.cookandroid.ex1;//2

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView textView;
    private Button uploadButton;
    private Uri selectedImageUri;
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        uploadButton = findViewById(R.id.uploadButton);

        ActivityResultLauncher<String> getContent = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        imageView.setImageURI(uri);
                    }
                });

        imageView.setOnClickListener(v -> getContent.launch("image/*"));

        uploadButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadImage(selectedImageUri);
            } else {
                Toast.makeText(this, "이미지를 선택해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadImage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] imageBytes = convertInputStreamToByteArray(inputStream);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "uploaded_image.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imageBytes)) // image/jpeg MIME type
                    .build();

            Request request = new Request.Builder()
                    .url("https://f93e-118-39-131-129.ngrok-free.app/upload") // ngrok URL
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Upload", "Error: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (response.isSuccessful() && responseBody != null) {
                            String responseBodyString = responseBody.string();
                            Log.d("Response", responseBodyString);

                            try {
                                JSONObject jsonResponse = new JSONObject(responseBodyString);
                                JSONArray predictionsArray = jsonResponse.getJSONArray("predictions");

                                StringBuilder textToShow = new StringBuilder();

                                for (int i = 0; i < predictionsArray.length(); i++) {
                                    JSONObject prediction = predictionsArray.getJSONObject(i);
                                    String className = prediction.getString("class");
                                    double confidence = prediction.getDouble("confidence");
                                    JSONArray bboxArray = prediction.getJSONArray("bbox");
                                    textToShow.append(className).append(": ").append(String.format("%.2f", confidence)).append("\n");
                                    textToShow.append("Bounding Box: ");
                                    for (int j = 0; j < bboxArray.length(); j++) {
                                        textToShow.append(bboxArray.get(j)).append(" ");
                                    }
                                    textToShow.append("\n");

                                }

                                runOnUiThread(() -> {
                                    textView.setText(textToShow.toString());
                                    Toast.makeText(MainActivity.this, "텍스트 수신 성공!", Toast.LENGTH_SHORT).show();
                                });

                            } catch (JSONException e) {
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this, "JSON 파싱 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("JSON", "Error parsing JSON: " + e.getMessage());
                                    textView.setText("JSON 파싱 오류: " + e.getMessage());
                                });
                            }

                        } else {
                            runOnUiThread(() -> {
                                String errorMessage = (response.body() != null) ? response.code() + ": " + response.message() : "서버 응답 없음";
                                Toast.makeText(MainActivity.this, "업로드 실패: " + errorMessage, Toast.LENGTH_SHORT).show();
                                Log.e("Upload", "Error: " + errorMessage);
                                textView.setText("업로드 실패: " + errorMessage);
                            });
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "이미지 업로드 준비 중 오류 발생.", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] convertInputStreamToByteArray(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        return outputStream.toByteArray();
    }
}