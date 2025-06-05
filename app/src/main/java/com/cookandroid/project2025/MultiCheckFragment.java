package com.cookandroid.project2025;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

public class MultiCheckFragment extends Fragment {

    private static final int REQUEST_CODE_SELECT_IMAGE = 1001;

    private ImageView imageView;
    private TextView textView;
    private Button uploadButton;

    private Uri imageUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_multi_check, container, false);

        imageView = view.findViewById(R.id.imageView);
        textView = view.findViewById(R.id.textView);
        uploadButton = view.findViewById(R.id.uploadButton);

        imageView.setOnClickListener(v -> openGallery());

        uploadButton.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadImageToServer(imageUri);
            } else {
                Toast.makeText(getContext(), "이미지를 먼저 선택하세요", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == getActivity().RESULT_OK && data != null) {
            imageUri = data.getData();
            imageView.setImageURI(imageUri);
        }
    }

    private void uploadImageToServer(Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            byte[] imageBytes = getBytes(inputStream);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "upload.jpg",
                            RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                    .build();

            Request request = new Request.Builder()
                    .url("http://yourserver.com/upload") // 실제 서버 주소로 바꿔야 함
                    .post(requestBody)
                    .build();

            OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "업로드 실패", Toast.LENGTH_SHORT).show());
                    Log.e("Upload", "에러", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (getActivity() == null) return;
                    if (response.isSuccessful()) {
                        String result = response.body().string();
                        getActivity().runOnUiThread(() -> textView.setText(result));
                    } else {
                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "서버 오류", Toast.LENGTH_SHORT).show());
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "이미지 처리 실패", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}
