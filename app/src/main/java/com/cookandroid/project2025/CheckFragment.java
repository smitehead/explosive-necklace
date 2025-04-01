package com.cookandroid.project2025;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

public class CheckFragment extends Fragment {

    private ImageView imageView;
    private TextView textView;
    private Button uploadButton;
    private Button selfUploadButton; // "직접 업로드" 버튼
    private Uri selectedImageUri;
    private final OkHttpClient client = new OkHttpClient();
    private ActivityResultLauncher<String> getContent;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_check, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imageView = view.findViewById(R.id.imageView);
        textView = view.findViewById(R.id.textView);
        uploadButton = view.findViewById(R.id.uploadButton);
        selfUploadButton = view.findViewById(R.id.selfUploadButton); // XML에 정의된 "직접 업로드" 버튼

        // 이미지 선택을 위한 ActivityResultLauncher 등록
        getContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        imageView.setImageURI(uri);
                    }
                });

        // 이미지뷰 클릭 시 이미지 선택
        imageView.setOnClickListener(v -> getContent.launch("image/*"));

        // "이미지 업로드" 버튼 클릭 시 선택한 이미지를 업로드
        uploadButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadImage(selectedImageUri);
            } else {
                Toast.makeText(getContext(), "이미지를 선택해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        // "직접 업로드" 버튼 클릭 시 SelfCheckActivity로 전환하여 해당 XML 화면을 보여줌
        selfUploadButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SelfCheckActivity.class);
            startActivity(intent);
        });
    }

    // 선택한 이미지를 서버로 업로드하는 메서드
    private void uploadImage(Uri uri) {
        try {
            Context context = getContext();
            if (context == null) return;

            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            byte[] imageBytes = convertInputStreamToByteArray(inputStream);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "uploaded_image.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                    .build();

            Request request = new Request.Builder()
                    .url("https://d916-118-39-131-129.ngrok-free.app/upload")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Upload", "Error: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.body() == null) return;
                    String responseBodyString = response.body().string();
                    Log.d("Response", responseBodyString);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBodyString);
                        String className = jsonResponse.getString("class");
                        double confidence = jsonResponse.getDouble("confidence");
                        JSONArray bboxArray = jsonResponse.getJSONArray("bbox");

                        StringBuilder textToShow = new StringBuilder();
                        textToShow.append("Class: ").append(className).append("\n");
                        textToShow.append("Confidence: ").append(String.format("%.2f", confidence)).append("\n");
                        textToShow.append("Bounding Box: ");
                        for (int i = 0; i < bboxArray.length(); i++) {
                            textToShow.append(bboxArray.get(i)).append(" ");
                        }
                        textToShow.append("\n");

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                textView.setText(textToShow.toString());
                                Toast.makeText(getContext(), "텍스트 수신 성공!", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (JSONException e) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "JSON 파싱 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("JSON", "Error parsing JSON: " + e.getMessage());
                                textView.setText("JSON 파싱 오류: " + e.getMessage());
                            });
                        }
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "이미지 업로드 준비 중 오류 발생.", Toast.LENGTH_SHORT).show();
        }
    }

    // InputStream을 byte 배열로 변환하는 헬퍼 메서드
    private byte[] convertInputStreamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
    }
}
