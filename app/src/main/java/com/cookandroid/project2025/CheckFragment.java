package com.cookandroid.project2025;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    private Button labelScanButton;
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
        labelScanButton = view.findViewById(R.id.buttonLabelScan);

        labelScanButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new LabelFragment())
                    .addToBackStack(null)
                    .commit();
        });

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

            // ✅ 리사이즈: 640x640로 모델에 최적화
            Bitmap resizedBitmap = resizeImage(context, uri, 640, 640);
            byte[] imageBytes = bitmapToByteArray(resizedBitmap);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "uploaded_image.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                    .build();

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
                    if (!response.isSuccessful() || response.body() == null) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "서버 오류: 상태 코드 " + response.code(), Toast.LENGTH_SHORT).show());
                        }
                        return;
                    }

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
                                for (int i = 0; i < classArray.length(); i++) {
                                    try {
                                        textToShow.append(classArray.getString(i));
                                        if (i < classArray.length() - 1) textToShow.append(", ");
                                    } catch (JSONException e) {
                                        textToShow.append("Error ");
                                    }
                                }
                                textToShow.append("\n");

                                if (bboxArray != null) {
                                    textToShow.append("Bounding Box: ");
                                    for (int i = 0; i < bboxArray.length(); i++) {
                                        try {
                                            textToShow.append(bboxArray.get(i)).append(" ");
                                        } catch (JSONException e) {
                                            textToShow.append("Error ");
                                        }
                                    }
                                    textToShow.append("\n");
                                }

                                textView.setText(textToShow.toString());
                                Toast.makeText(getContext(), "텍스트 수신 성공!", Toast.LENGTH_SHORT).show();

                                ResultFoodFragment resultFragment = ResultFoodFragment.newInstance(classArray.toString());
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
                                textView.setText("JSON 파싱 오류: " + e.getMessage());
                            });
                        }
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "이미지 업로드 오류", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ 640x640 리사이즈 함수
    private Bitmap resizeImage(Context context, Uri imageUri, int width, int height) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        Bitmap original = BitmapFactory.decodeStream(inputStream);
        return Bitmap.createScaledBitmap(original, width, height, true);
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }
}
