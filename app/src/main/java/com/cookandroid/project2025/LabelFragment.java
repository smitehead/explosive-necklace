package com.cookandroid.project2025;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
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

public class LabelFragment extends Fragment {

    private ImageView imageView;
    private Button uploadButton;
    private TextView textView;
    private Uri selectedImageUri;
    private final OkHttpClient client = new OkHttpClient();
    private ActivityResultLauncher<String> getContent;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_label_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        imageView = view.findViewById(R.id.imageView);
        uploadButton = view.findViewById(R.id.uploadButton);
        textView = view.findViewById(R.id.textView);

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
                uploadLabelImage(selectedImageUri);
            } else {
                Toast.makeText(getContext(), "이미지를 선택해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadLabelImage(Uri uri) {
        try {
            Context context = getContext();
            if (context == null) return;

            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            byte[] imageBytes = convertInputStreamToByteArray(inputStream);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "nutrition_label.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                    .build();

            Request request = new Request.Builder()
                    .url("https://4675-211-197-158-208.ngrok-free.app/upload_text")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseString = response.body().string();
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        LabelResultFragment resultFragment = LabelResultFragment.newInstance(responseString);
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.frame_layout, resultFragment)
                                .addToBackStack(null)
                                .commit();
                    });
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "이미지 업로드 오류", Toast.LENGTH_SHORT).show();
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