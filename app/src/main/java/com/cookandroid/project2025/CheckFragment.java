package com.cookandroid.project2025;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

public class CheckFragment extends Fragment {

    private ImageView imageView;
    private TextView textView;
    private Button uploadButton;
    private Uri selectedImageUri;
    private ImageView overlayIcon;
    private TextView overlayText;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(180, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .build();

    private ActivityResultLauncher<String> getContent;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri photoUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result) {
                        selectedImageUri = photoUri;
                        imageView.setImageURI(photoUri);

                        if (overlayIcon != null && overlayText != null) {
                            overlayIcon.setVisibility(View.GONE);
                            overlayText.setVisibility(View.GONE);
                        }
                    } else {
                        Toast.makeText(getContext(), "사진 촬영이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_check, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        imageView = view.findViewById(R.id.imageView);
        textView = view.findViewById(R.id.textView);
        uploadButton = view.findViewById(R.id.uploadButton);
        overlayIcon = view.findViewById(R.id.overlayIcon);
        overlayText = view.findViewById(R.id.overlayText);

        ImageButton selfUploadButton = view.findViewById(R.id.selfUploadButton);
        selfUploadButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            startActivity(intent);
        });

        ImageButton buttonLabelScan = view.findViewById(R.id.buttonLabelScan);
        buttonLabelScan.setOnClickListener(v -> {
            LabelFragment labelFragment = new LabelFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frame_layout, labelFragment)
                    .addToBackStack(null)
                    .commit();
        });

        getContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        imageView.setImageURI(uri);
                        overlayIcon.setVisibility(View.GONE);
                        overlayText.setVisibility(View.GONE);
                    }
                });

        imageView.setOnClickListener(v -> getContent.launch("image/*"));

        FloatingActionButton cameraFab = view.findViewById(R.id.cameraFab);
        cameraFab.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, 100);
            } else {
                launchCamera();
            }
        });

        uploadButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadImage(selectedImageUri);
            } else {
                Toast.makeText(getContext(), "이미지를 선택해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
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

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inSampleSize = 1;

            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(inputStream, null, options);

            byte[] imageBytes = bitmapToByteArray(original);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "uploaded_image.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                    .build();

            Request request = new Request.Builder()
                    .url("https://5a21-118-39-131-129.ngrok-free.app/upload_image")
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

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            try {
                                JSONObject jsonResponse = new JSONObject(responseBodyString);
                                JSONArray classArray = jsonResponse.getJSONArray("class");
                                textView.setText("인식된 음식: " + classArray.toString());

                                ResultFragment resultFragment = ResultFragment.newInstance(classArray.toString());
                                getParentFragmentManager().beginTransaction()
                                        .replace(R.id.frame_layout, resultFragment)
                                        .addToBackStack(null)
                                        .commit();
                            } catch (JSONException e) {
                                Toast.makeText(getContext(), "JSON 파싱 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "이미지 업로드 오류", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }
}