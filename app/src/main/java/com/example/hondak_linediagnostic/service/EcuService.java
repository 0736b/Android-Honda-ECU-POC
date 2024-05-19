package com.example.hondak_linediagnostic.service;

import android.content.Context;
import android.util.Log;

import com.example.hondak_linediagnostic.ByteUtil;
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

public class EcuService {

    private final String TAG = this.getClass().getName();

    private final Context mainContext;

    private final D2xxManager d2xxManager;
    private FT_Device ftDevice;
    private boolean gotDevice = false;
    private boolean ecuConnected = false;

    private static final int BAUD_RATE = 10400;
    private static final int WAIT_RECV = 500;
    private static final byte[] TX_LOW = {0x00};
    private static final byte[] TX_HIGH = {0x01};
    private static final byte[] CMD_PING = {(byte) 0xFE, 0x04, 0x72, (byte) 0x8C};
    private static final byte[] CMD_DIAG = {0x72, 0x05, 0x00, (byte) 0xF0, (byte) 0x99};
    private static final byte[] CMD_REQUEST_TABLE_17 = {0x72, 0x05, 0x71, 0x17, 0x01};
    private static final int SIZE_RECV_TABLE_17 = 0x1D;
    private static final int SIZE_RECV_PING = 0x08;
    private static final int SIZE_RECV_DIAG = 0x09;
    private static final String RECV_PING = "0E 04 72 7C";
    private static final String RECV_DIAG = "02 04 00 FA";
    private static final int RPM_OFFSET = 0x09; // 2 bytes
    private static final int TPS_OFFSET = 0x0C; // 1 byte

    // private static final int INJ_OFFSET = 0x??; // no spoon-feeding
    // private static final int EOT_OFFSET = 0x??; // no spoon-feeding
    // private static final int IGN_OFFSET = 0x??; // no spoon-feeding
    // private static final int AFR_OFFSET = 0x??; // no spoon-feeding

    private float rpm = 0, tps = 0, afr = 0, inj = 0, ign = 0, eot = 0;

    public EcuService(Context parentContext, D2xxManager d2xxManager) {
        this.mainContext = parentContext;
        this.d2xxManager = d2xxManager;
        this.gotDevice = this.findFtdi();
        Log.i(TAG, "Initialized");
    }

    public boolean findFtdi() {
        if (ftDevice != null) gotDevice = true;
        int devCount = d2xxManager.createDeviceInfoList(mainContext);
        if (devCount > 0) {
            ftDevice = d2xxManager.openByIndex(mainContext, 0);
            ftDevice.setBaudRate(BAUD_RATE);
            gotDevice = true;
        } else {
            gotDevice = false;
        }
        Log.i(TAG, "findFtdi: " + gotDevice);
        return gotDevice;
    }

    public String getDeviceSerial() {
        if (this.ftDevice != null) {
            return ftDevice.getDeviceInfo().serialNumber;
        }
        return "None";
    }

    public boolean isOpen() {
        if (this.ftDevice == null) return false;
        return this.ftDevice.isOpen();
    }

    public void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    public boolean initEcu() {
        if (ftDevice == null) return false;
        ftDevice.purge((byte) (D2xxManager.FT_PURGE_RX | D2xxManager.FT_PURGE_TX));
        ftDevice.setBitMode((byte) 1, D2xxManager.FT_BITMODE_ASYNC_BITBANG);
        ftDevice.write(TX_LOW);
        waitFor(70);
        ftDevice.write(TX_HIGH);
        ftDevice.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
        waitFor(120);
        ftDevice.purge((byte) (D2xxManager.FT_PURGE_RX | D2xxManager.FT_PURGE_TX));
        int writeBytesPing = ftDevice.write(CMD_PING, CMD_PING.length, true);
        if (writeBytesPing == CMD_PING.length) {
            byte[] readPing = new byte[SIZE_RECV_PING];
            int readBytesPing = ftDevice.read(readPing, SIZE_RECV_PING, WAIT_RECV);
            if (readBytesPing == SIZE_RECV_PING) {
                String recvString = ByteUtil.toHexString(readPing);
                ecuConnected = recvString.contains(RECV_PING);
            }
        }
        ftDevice.purge((byte) (D2xxManager.FT_PURGE_RX | D2xxManager.FT_PURGE_TX));
        int writeBytesDiag = ftDevice.write(CMD_DIAG, CMD_DIAG.length, true);
        if (writeBytesDiag == CMD_DIAG.length) {
            byte[] readDiag = new byte[SIZE_RECV_DIAG];
            int readBytesDiag = ftDevice.read(readDiag, SIZE_RECV_DIAG, WAIT_RECV);
            if (readBytesDiag == SIZE_RECV_DIAG) {
                String recvString = ByteUtil.toHexString(readDiag);
                ecuConnected = (ecuConnected && recvString.contains(RECV_DIAG));
            }
        }
        this.ftDevice.purge((byte) (D2xxManager.FT_PURGE_RX | D2xxManager.FT_PURGE_TX));
        return ecuConnected;
    }

    public boolean stillConnect() {
        return this.ecuConnected;
    }

    public float convertToTps(byte[] dataTable17) {
        if (dataTable17.length != SIZE_RECV_TABLE_17) return this.tps;
        float _tps = (dataTable17[TPS_OFFSET] & 0xFF) / 2.0f;
        if (_tps >= 0.0 && _tps <= 100.0) {
           this.tps = _tps;
        }
        return this.tps;
    }

    public float convertToRpm(byte[] dataTable17) {
        if (dataTable17.length != SIZE_RECV_TABLE_17) return this.rpm;
        float _rpm = ((dataTable17[RPM_OFFSET] & 0xFF) << 8) | (dataTable17[RPM_OFFSET + 1] & 0xFF);
        if (_rpm >= 0.0 && _rpm <= 15000.0) {
            this.rpm = _rpm;
        }
        return this.rpm;
    }

    // no spoon-feeding for converting another ^_^

    public void getDataTable17() {
        if (this.ftDevice == null) {
            this.ecuConnected = false;
            return;
        }
        ftDevice.purge((byte) (D2xxManager.FT_PURGE_RX | D2xxManager.FT_PURGE_TX));
        int writeBytesReqTable17 = ftDevice.write(CMD_REQUEST_TABLE_17, CMD_REQUEST_TABLE_17.length, true);
        if (writeBytesReqTable17 == CMD_REQUEST_TABLE_17.length) {
            byte[] readTable17 = new byte[SIZE_RECV_TABLE_17];
            int readBytesTable17 = ftDevice.read(readTable17, SIZE_RECV_TABLE_17, WAIT_RECV);
            if (readBytesTable17 == SIZE_RECV_TABLE_17) {
                this.ecuConnected = true;
                this.tps = convertToTps(readTable17);
                this.rpm = convertToRpm(readTable17);
            } else {
                this.ecuConnected = false;
            }
        } else {
            this.ecuConnected = false;
        }
        ftDevice.purge((byte) (D2xxManager.FT_PURGE_RX | D2xxManager.FT_PURGE_TX));
        Log.i(TAG, "getDataTable17");
    }

    public float getRpm() {
        return this.rpm;
    }

    public float getTps() {
        return this.tps;
    }

    public float getAfr() { return this.afr; }

    public float getInj() { return this.inj; }

    public float getIgn() { return this.ign; }

    public float getEot() { return this.eot; }

    public void close() {
        if (this.ftDevice != null) {
            this.ftDevice.close();
            Log.i(TAG, "close: device closed");
        }
    }

}
