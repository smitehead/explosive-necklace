package com.cookandroid.project2025;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import android.content.Intent;

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
    private Button selfUploadButton;
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
        imageView = view.findViewById(R.id.imageView);
        textView = view.findViewById(R.id.textView);
        uploadButton = view.findViewById(R.id.uploadButton);
        labelScanButton = view.findViewById(R.id.buttonLabelScan);
        selfUploadButton = view.findViewById(R.id.selfUploadButton);

        selfUploadButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SelfCheckActivity.class);
            startActivity(intent);
        });

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

            Bitmap resizedBitmap = resizeImageWithRotation(context, uri, 640, 640);
            byte[] imageBytes = bitmapToByteArray(resizedBitmap);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "uploaded_image.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                    .build();

            String url = "https://c773-118-39-131-129.ngrok-free.app/upload_image";

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.body() == null) return;
                    String responseBodyString = response.body().string();
                    Log.d("Response", responseBodyString);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBodyString);
                        JSONArray classArray = jsonResponse.getJSONArray("class");

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                ResultFoodFragment resultFragment = ResultFoodFragment.newInstance(classArray.toString());
                                getParentFragmentManager().beginTransaction()
                                        .replace(R.id.frame_layout, resultFragment)
                                        .addToBackStack(null)
                                        .commit();
                            });
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getContext(), "JSON 파싱 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

        } catch (IOException e) {
            Toast.makeText(getContext(), "이미지 업로드 오류", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap resizeImageWithRotation(Context context, Uri imageUri, int width, int height) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        if (inputStream == null) {
            throw new IOException("이미지 파일 열기 실패");
        }
        Bitmap original = BitmapFactory.decodeStream(inputStream);
        if (original == null) {
            throw new IOException("이미지를 디코드하지 못했습니다.");
        }

        InputStream exifInputStream = context.getContentResolver().openInputStream(imageUri);
        if (exifInputStream == null) {
            throw new IOException("EXIF 정보 접근 실패");
        }
        ExifInterface exif = new ExifInterface(exifInputStream);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int degrees = exifToDegrees(orientation);
        Bitmap rotated = rotateBitmap(original, degrees);

        return Bitmap.createScaledBitmap(rotated, width, height, true);
    }

    private int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) return 90;
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) return 180;
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) return 270;
        return 0;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        if (degrees == 0) return bitmap;
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }
}
