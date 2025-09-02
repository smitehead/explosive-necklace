package com.cookandroid.project2025;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

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
    private ImageButton backButton;
    private Button uploadButton;
    private Uri selectedImageUri;
    private ImageView overlayIcon;
    private TextView overlayText, overlayText2;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(180, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .build();

    private ActivityResultLauncher<String> getContent;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri photoUri;

    // 권한 요청을 위한 새로운 ActivityResultLauncher 추가
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 갤러리 이미지 선택을 위한 ActivityResultLauncher
        getContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        imageView.setImageURI(uri);
                        overlayIcon.setVisibility(View.GONE);
                        overlayText.setVisibility(View.GONE);
                        overlayText2.setVisibility(View.GONE);
                    }
                });

        // 카메라 촬영을 위한 ActivityResultLauncher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result) {
                        selectedImageUri = photoUri;
                        imageView.setImageURI(photoUri);
                        overlayIcon.setVisibility(View.GONE);
                        overlayText.setVisibility(View.GONE);
                        overlayText2.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(getContext(), "사진 촬영이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 권한 요청 결과 처리
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allPermissionsGranted = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (Boolean.FALSE.equals(permissions.getOrDefault(Manifest.permission.CAMERA, false)) ||
                                Boolean.FALSE.equals(permissions.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false))) {
                            allPermissionsGranted = false;
                        }
                    } else {
                        if (Boolean.FALSE.equals(permissions.getOrDefault(Manifest.permission.CAMERA, false)) ||
                                Boolean.FALSE.equals(permissions.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false))) {
                            allPermissionsGranted = false;
                        }
                    }

                    if (allPermissionsGranted) {
                        launchCamera();
                    } else {
                        Toast.makeText(getContext(), "카메라 및 갤러리 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_label_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        imageView = view.findViewById(R.id.imageView);
        uploadButton = view.findViewById(R.id.uploadButton);
        backButton = view.findViewById(R.id.backButton);
        overlayIcon = view.findViewById(R.id.overlayIcon);
        overlayText = view.findViewById(R.id.overlayText);
        overlayText2 = view.findViewById(R.id.overlayText2);

        // XML 레이아웃에 FloatingActionButton이 있다고 가정하고 추가
        FloatingActionButton cameraFab = view.findViewById(R.id.cameraFab);

        backButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // 갤러리 이미지 선택 리스너
        imageView.setOnClickListener(v -> getContent.launch("image/*"));

        // 카메라 버튼 리스너
        cameraFab.setOnClickListener(v -> checkAndRequestPermissions());

        uploadButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadImage(selectedImageUri);
            } else {
                Toast.makeText(getContext(), "이미지를 선택해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndRequestPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        boolean allGranted = true;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            launchCamera();
        } else {
            requestPermissionLauncher.launch(permissions);
        }
    }

    private void launchCamera() {
        File photoFile = new File(requireContext().getExternalFilesDir(null), "temp_photo.jpg");
        photoUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                photoFile
        );
        cameraLauncher.launch(photoUri);
    }

    private void uploadImage(Uri uri) {
        try {
            Context context = getContext();
            if (context == null) return;

            Bitmap resizedBitmap = resizeImageWithRotation(context, uri, 640, 640);
            byte[] imageBytes = bitmapToByteArray(resizedBitmap);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "label_image.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                    .build();

            Request request = new Request.Builder()
                    .url("https://5c62-211-197-158-208.ngrok-free.app/upload_text")
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
                    Log.d("LabelUploadResponse", responseBodyString);

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Intent intent = new Intent(getActivity(), LabelResultActivity.class);
                            intent.putExtra("nutritionJson", responseBodyString);
                            startActivity(intent);
                        });
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "이미지 업로드 오류", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap resizeImageWithRotation(Context context, Uri imageUri, int width, int height) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        Bitmap original = BitmapFactory.decodeStream(inputStream);

        InputStream exifInputStream = context.getContentResolver().openInputStream(imageUri);
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