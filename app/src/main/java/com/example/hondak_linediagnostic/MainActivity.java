package com.example.hondak_linediagnostic;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hondak_linediagnostic.service.HondaKLineService;


public class MainActivity extends AppCompatActivity {
    
    private final String TAG = this.getClass().getName();
    @SuppressLint("StaticFieldLeak")
    public static TextView textConnectStatus, textRpmNum, textTpsNum, textAfrNum, textInjNum, textIgnNum, textEotNum;
    private boolean isPermissionPassed = false;

    boolean boundService;

    private void getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        } else {
            isPermissionPassed = true;
        }
        Log.i(TAG,"isPermissionPassed " + isPermissionPassed);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isPermissionPassed = true;
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this
                        , Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "Not Allowed", Toast.LENGTH_SHORT).show();
                    getPermission();
                }
            }
        }
    }

    @SuppressLint({"SourceLockedOrientationActivity"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Lock portrait and Keep screen on.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getPermission();

        // defined texts
        textConnectStatus = findViewById(R.id.ftdi_status);
        textRpmNum = findViewById(R.id.rpm_num);
        textTpsNum = findViewById(R.id.tps_num);
        textAfrNum = findViewById(R.id.afr_num);
        textInjNum = findViewById(R.id.inj_num);
        textIgnNum = findViewById(R.id.ign_num);
        textEotNum = findViewById(R.id.eot_num);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }


    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            boundService = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            boundService = false;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = new Intent(MainActivity.this, HondaKLineService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (boundService) {
            unbindService(connection);
            boundService = false;
        }
    }

}