package com.example.hondak_linediagnostic.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.hondak_linediagnostic.MainActivity;
import com.example.hondak_linediagnostic.R;
import com.ftdi.j2xx.D2xxManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HondaKLineService extends Service {

    private final String TAG = this.getClass().getName();

    ScheduledExecutorService scheduledExecutorService;
    private final IBinder binder = new Binder();

    private EcuService ecuService;

    private boolean isEcuService = false;
    private boolean isEcuInit = false;

    public HondaKLineService() {
    }

    @Override
    public IBinder onBind(Intent intent) {

        D2xxManager d2xxManager;
        try {
            d2xxManager = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException e) {
            throw new RuntimeException(e);
        }

        ecuService = new EcuService(this, d2xxManager);
        isEcuService = true;

        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(
                () -> {
                    try {
                        if (isEcuService) {
                            Log.i(TAG, "Running");
                            if (!ecuService.isOpen()) {
                                MainActivity.textConnectStatus.setText(R.string.no_connection);
                                ecuService.findFtdi();
                                Log.i(TAG, "No FTDI Device, try again.");
                            }
                            if (ecuService.isOpen() && !isEcuInit) {
                                String deviceConnecting = ecuService.getDeviceSerial() + " " + "Connecting...";
                                MainActivity.textConnectStatus.setText(deviceConnecting);
                                isEcuInit = ecuService.initEcu();
                                Log.i(TAG, "Got FTDI device, ECU connecting...");
                            }
                            if (isEcuInit) {
                                String deviceConnected = ecuService.getDeviceSerial() + " " + R.string.connected;
                                MainActivity.textConnectStatus.setText(deviceConnected);
                                ecuService.getDataTable17();
                                MainActivity.textRpmNum.setText(String.valueOf(ecuService.getRpm()));
                                MainActivity.textTpsNum.setText(String.valueOf(ecuService.getTps()));
                                MainActivity.textAfrNum.setText(String.valueOf(ecuService.getAfr()));
                                MainActivity.textInjNum.setText(String.valueOf(ecuService.getInj()));
                                MainActivity.textIgnNum.setText(String.valueOf(ecuService.getIgn()));
                                MainActivity.textEotNum.setText(String.valueOf(ecuService.getEot()));
                                isEcuInit = ecuService.stillConnect();
                                Log.i(TAG, "updates, check ECU connected " + isEcuInit);
                            }
                        }
                    } catch ( Exception e ) {
                        Log.e(TAG, e.toString());
                    }
                }, 1, 100, TimeUnit.MILLISECONDS
        );
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        isEcuService = false;
        ecuService.close();
        scheduledExecutorService.shutdown();
        Log.i(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

}