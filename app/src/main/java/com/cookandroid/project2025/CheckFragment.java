package com.cookandroid.project2025;

import android.content.Context;
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

        getContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
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
                Toast.makeText(getContext(), "이미지를 선택해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

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

            // 자동으로 URL 구성
            String baseUrl = "https://4675-211-197-158-208.ngrok-free.app";
            String endpoint = "upload_image";
            if (!baseUrl.endsWith("/")) baseUrl += "/";
            String fullUrl = baseUrl + endpoint;

            Request request = new Request.Builder()
                    .url(fullUrl)
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
                    if (!response.isSuccessful()) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "서버 오류: 상태 코드 " + response.code(), Toast.LENGTH_SHORT).show();
                                Log.e("Upload", "Server responded with code: " + response.code());
                            });
                        }
                        return;
                    }

                    if (response.body() == null) return;
                    String responseBodyString = response.body().string();
                    Log.d("Response", responseBodyString);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBodyString);
                        JSONArray classArray = jsonResponse.getJSONArray("class");
                        JSONArray bboxArray = jsonResponse.optJSONArray("bbox");

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                StringBuilder textToShow = new StringBuilder();
                                textToShow.append("Class: ");
                                if (classArray != null) {
                                    for (int i = 0; i < classArray.length(); i++) {
                                        try {
                                            textToShow.append(classArray.getString(i));
                                            if (i < classArray.length() - 1) {
                                                textToShow.append(", ");
                                            }
                                        } catch (JSONException e) {
                                            Log.e("JSON", "Error getting class at index " + i + ": " + e.getMessage());
                                            textToShow.append("Error");
                                        }
                                    }
                                }
                                textToShow.append("\n");

                                if (bboxArray != null) {
                                    textToShow.append("Bounding Box: ");
                                    for (int i = 0; i < bboxArray.length(); i++) {
                                        try {
                                            textToShow.append(bboxArray.get(i)).append(" ");
                                        } catch (JSONException e) {
                                            Log.e("JSON", "Error getting bbox at index " + i + ": " + e.getMessage());
                                            textToShow.append("Error");
                                        }
                                    }
                                    textToShow.append("\n");
                                }

                                textView.setText(textToShow.toString());
                                Toast.makeText(getContext(), "텍스트 수신 성공!", Toast.LENGTH_SHORT).show();

                                ResultFoodFragment resultFragment = ResultFoodFragment.newInstance(classArray == null ? "[]" : classArray.toString());
                                getParentFragmentManager().beginTransaction()
                                        .replace(R.id.frame_layout, resultFragment)
                                        .addToBackStack(null)
                                        .commit();
                            });
                        }
                    } catch (JSONException e) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "JSON 파싱 오류 (CheckFragment): " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("JSON", "Error parsing JSON (CheckFragment): " + e.getMessage());
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