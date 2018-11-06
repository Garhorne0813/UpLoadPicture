package com.garhorne.uploadpicture;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,PermissionInterface{

    private Button btn_takePhoto;
    private Button btn_upLoadPicture;
    private ImageView img_photo;
    private AlertDialog aBuilder;

    private Uri imageUri;

    private PermissionHelper permissionHelper;

    private static final int TAKE_PHOTO = 1;
    private static final int REQ_CODE_PICK_PHOTO = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionHelper = new PermissionHelper(this,this);

        initViews();

    }

    private void initViews() {
        btn_takePhoto = (Button)findViewById(R.id.btn_take_photo);
        btn_upLoadPicture = (Button)findViewById(R.id.btn_upload_picture);
        img_photo = (ImageView)findViewById(R.id.img_photo);

        btn_takePhoto.setOnClickListener(this);
        btn_upLoadPicture.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_take_photo:
                permissionHelper.requestPermissions(Manifest.permission.CAMERA,REQ_CODE_PICK_PHOTO);
                break;
            case R.id.btn_upload_picture:
                aBuilder = new AlertDialog.Builder(MainActivity.this)
                        .setView(R.layout.dialoglayout)
                        .setCancelable(false)
                        .create();
                aBuilder.show();
                UpLoadPicture();
                break;
            default:
                break;
        }
    }

    /**
     * 拍照操作
     */
    private void TakePhoto() {
        File outputImage = new File(getExternalCacheDir(),"output_image.jpg");
        try{
            if(outputImage.exists()){
                outputImage.delete();
            }
            outputImage.createNewFile();
        }catch (IOException e){
            e.printStackTrace();
        }
        if(Build.VERSION.SDK_INT >= 24){
            imageUri = FileProvider.getUriForFile(MainActivity.this,"com.garhorne.uploadpicture.fileprovider",outputImage);
        } else {
            imageUri = Uri.fromFile(outputImage);
        }
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        startActivityForResult(intent,TAKE_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case TAKE_PHOTO:
                if(resultCode == RESULT_OK){
                    try{
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        img_photo.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * 利用OKhttp上传图片
     */
    private void UpLoadPicture() {
        //判断文件夹中是否有文件存在
        File file = new File(getExternalCacheDir(),"output_image.jpg");
        if (!file.exists()){
            Toast.makeText(getApplicationContext(),"文件不存在",Toast.LENGTH_SHORT).show();
            return;
        }
        OkHttpClient client = new OkHttpClient();
        String imagePath = getExternalCacheDir().getPath() + "/output_image.jpg";
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.addFormDataPart("image",imagePath,RequestBody.create(MediaType.parse("image/jpg"),new File(imagePath)));
        RequestBody requestBody = builder.build();
        Request.Builder reqBuilder = new Request.Builder();
        Request request = reqBuilder
                .url(Constant.URL)
                .post(requestBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        aBuilder.dismiss();
                        Toast.makeText(getApplicationContext(),"上传失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String resp = response.body().toString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        aBuilder.dismiss();
                        Toast.makeText(getApplicationContext(),resp,Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionHelper.requestPermissionResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //已获取到权限，执行操作
    @Override
    public void requestPermissionSuccess(int callBackCode) {
        if (callBackCode == REQ_CODE_PICK_PHOTO){
            TakePhoto();
        }
    }

    //未获取到权限，提示用户
    @Override
    public void requestPermissionFailure(int callBackCode) {
        if (callBackCode == REQ_CODE_PICK_PHOTO){
            Toast.makeText(getApplicationContext(),"未授予相机权限",Toast.LENGTH_SHORT).show();
        }
    }
}
