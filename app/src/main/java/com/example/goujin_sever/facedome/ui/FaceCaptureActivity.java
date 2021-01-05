package com.example.goujin_sever.facedome.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.goujin_sever.facedome.R;
import com.example.goujin_sever.facedome.capture.CameraPreview;
import com.example.goujin_sever.facedome.capture.CircleCameraLayout;
import com.example.goujin_sever.facedome.capture.FaceHelper;
import com.example.goujin_sever.facedome.capture.ToolsFile;
import com.example.goujin_sever.facedome.capture.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FaceCaptureActivity extends AppCompatActivity implements CameraPreview.OnPreviewFrameListener, View.OnClickListener {
    private static final int PERMISSION_REQUEST_CODE = 10;
    private String tempImagePath;
    private String[] mPermissions = {Manifest.permission.CAMERA};
    private CameraPreview cameraPreview;
    private boolean hasPermissions;
    private boolean resume = false;//解决home键黑屏问题
    private Dialog mSuccessDialog;
    private Dialog mFailDialog;
    private CircleCameraLayout mCircleCameraLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_capture);
        mCircleCameraLayout = findViewById(R.id.activity_camera_layout);


        if (Util.checkPermissionAllGranted(this, mPermissions)) {
            hasPermissions = true;
        } else {
            ActivityCompat.requestPermissions(this, mPermissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasPermissions) {
            startCamera();
            resume = true;
        }
    }

    private void startCamera() {
        if (null != cameraPreview) cameraPreview.releaseCamera();
        cameraPreview = new CameraPreview(this, this);
        mCircleCameraLayout.removeAllViews();
        mCircleCameraLayout.setCameraPreview(cameraPreview);
        if (!hasPermissions || resume) {
            mCircleCameraLayout.startView();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                cameraPreview.startPreview();
            }
        }, 200);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.activity_camera_title_back_view:
                finish();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != cameraPreview) {
            cameraPreview.releaseCamera();
        }
        mCircleCameraLayout.release();
    }

    /**
     * 申请权限结果返回处理
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean isAllGranted = true;
            for (int grant : grantResults) {  // 判断是否所有的权限都已经授予了
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }
            if (isAllGranted) { // 所有的权限都授予了
                startCamera();
            } else {// 提示需要权限的原因
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("拍照需要允许权限, 是否再次开启?")
                        .setTitle("提示")
                        .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(FaceCaptureActivity.this, mPermissions, PERMISSION_REQUEST_CODE);
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                finish();
                            }
                        });
                builder.create().show();
            }
        }
    }

    @Override
    public void onPreviewFrame(Bitmap bitmap) {
        Log.d("onPreviewFrame", "bitmap:" + bitmap);
        File tempImageFile = null;
        Bitmap faceBitmap = null;
        try {
            tempImageFile = ToolsFile.createTempImageFile(this);
            tempImagePath = tempImageFile.getPath();
            Log.d("tempImagePath", "tempImagePath:" + tempImagePath);
            faceBitmap = FaceHelper.genFaceBitmap(bitmap);
            saveBitmap(faceBitmap);
            compressPic();
            Log.d("FaceHelper", "bitmap1:" + faceBitmap + ",Width：" + (faceBitmap == null ? "0" : faceBitmap.getWidth()));
        } catch (Exception e) {
            faceBitmap = null;
        }
        //如果截取的图片宽小于350，高小于400则重新获取。
        if (faceBitmap == null || faceBitmap.getHeight() < 400) {
            return;
        }
        if (null != cameraPreview) {
            cameraPreview.releaseCamera();
        }
        mCircleCameraLayout.release();
        showAuthenticationSuccessDialog(faceBitmap);
    }

    private void compressPic() {
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inSampleSize = 1; // 这个数字越大,图片就越小.图片就越不清晰
        Bitmap pic = null;
        pic = BitmapFactory.decodeFile(tempImagePath, op);  //先从本地读照片，然后利用op参数对图片进行处理
        FileOutputStream b = null;
        try {
            b = new FileOutputStream(tempImagePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (pic != null) {
            pic.compress(Bitmap.CompressFormat.JPEG, 50, b);
        }
    }

    public void saveBitmap(Bitmap bm) {
        File f = new File(tempImagePath);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
            Log.d("OnFaceCollected", "保存成功：" + tempImagePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAuthenticationSuccessDialog(final Bitmap faceBitmap) {
        View view = LayoutInflater.from(FaceCaptureActivity.this).inflate(R.layout.dialog_authentication_success, null);
        TextView authenticationView = view.findViewById(R.id.dialog_know_tv);
        mSuccessDialog = new Dialog(FaceCaptureActivity.this, R.style.custom_noActionbar_window_style);
        mSuccessDialog.show();
        mSuccessDialog.setContentView(view);
        mSuccessDialog.setCanceledOnTouchOutside(false);
        mSuccessDialog.setCancelable(false);
        Window win = mSuccessDialog.getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        win.setAttributes(lp);
        authenticationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle b = new Bundle();
                b.putParcelable("bitmap", faceBitmap);
                Intent intent = getIntent();
                intent.putExtras(b);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }
}
