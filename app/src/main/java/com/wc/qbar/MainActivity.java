package com.wc.qbar;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.wc.qbar.scan.CaptureActivity;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;


public class MainActivity extends AppCompatActivity {
    private TextView tv_content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new CompositeDisposable().add(new RxPermissions(MainActivity.this).requestEach(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) throws Exception {
                        if (permission.granted) {// 用户已经同意该权限
                        } else {//拒绝授权  (permission.shouldShowRequestPermissionRationale 没有选中『不再询问』)
                        }
                    }
                }));
        tv_content = findViewById(R.id.tv_content);
        findViewById(R.id.bt_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MainActivity.this, CaptureActivity.class), 123);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == 123 && data != null) {
            tv_content.setText("识别成功--->" + data.getStringExtra("codedContent"));
        }
    }
}
