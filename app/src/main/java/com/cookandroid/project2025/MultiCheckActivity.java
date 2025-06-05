package com.cookandroid.project2025;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

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

public class MultiCheckActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SELECT_IMAGE = 1001;

    private ImageView imageView;
    private TextView textView;
    private Button uploadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_check);  // ⚠ XML 파일명도 수정 필요!

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        uploadButton = findViewById(R.id.uploadButton);

        imageView.setOnClickListener(v -> openGallery());

        uploadButton.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadImageToServer(imageUri);
            } else {
                Toast.makeText(this, "이미지를 먼저 선택하세요", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Uri imageUri;

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imageView.setImageURI(imageUri);
        }
    }

    private void uploadImageToServer(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            byte[] imageBytes = getBytes(inputStream);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "upload.jpg",
                            RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                    .build();

            Request request = new Request.Builder()
                    .url("http://yourserver.com/upload")  // ✅ 실제 서버 주소로 변경
                    .post(requestBody)
                    .build();

            OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(MultiCheckActivity.this, "업로드 실패", Toast.LENGTH_SHORT).show());
                    Log.e("Upload", "에러", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String result = response.body().string();
                        runOnUiThread(() -> textView.setText(result));
                    } else {
                        runOnUiThread(() -> Toast.makeText(MultiCheckActivity.this, "서버 오류", Toast.LENGTH_SHORT).show());
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "이미지 처리 실패", Toast.LENGTH_SHORT).show();
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
