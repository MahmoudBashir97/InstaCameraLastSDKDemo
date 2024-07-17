package com.arashivision.sdk.demo.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.arashivision.sdk.demo.R;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkcamera.camera.callback.IScanBleListener;
import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BleActivity extends BaseObserveCameraActivity implements IScanBleListener {

    public static final String[] LOCATION_PERMISSIONS = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    public static final String[] BLUETOOTH_PERMISSIONS = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT};
    public static final int REQUEST_PERMISSION_CODE = 100;
    private TextView mTvScanState;
    private RecyclerView mRvBleDevice;
    private BleDeviceAdapter mBleDeviceAdapter;
    BleManager bleManager;

    private MaterialDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        setTitle(R.string.ble_toolbar_title);

        mTvScanState = findViewById(R.id.tv_scan_state);

        mRvBleDevice = findViewById(R.id.rv_ble_device);
        mRvBleDevice.setLayoutManager(new LinearLayoutManager(this));
        mRvBleDevice.setAdapter(mBleDeviceAdapter = new BleDeviceAdapter());
        bleManager = new BleManager();
        mBleDeviceAdapter.setOnItemClickListener(bleDevice -> {
            InstaCameraManager.getInstance().stopBleScan();
            InstaCameraManager.getInstance().connectBle(bleDevice);
            Log.d("??", "bleConfigData: " + bleDevice.getDevice() + " mac: " + bleDevice.getMac() + " man: " + bleDevice.getManufacturerSpecificData() + " "
                    + bleDevice.getKey() + " " + Arrays.toString(bleDevice.getScanRecord()));

//            SparseArray<byte[]> m =bleDevice.getManufacturerSpecificData();
//            for (int i = 0; i < m.size(); i++) {
//                int key = m.keyAt(i);
//                byte[] value = m.valueAt(i);
//
//                // Print the key and its corresponding byte array
//                System.out.print("Key: " + key + ", Value: ");
//                Log.d("??","bleConfigData: "+"Key: " + key + ", Value: ");
//                for (byte b : value) {
//                    Log.d("??","bleConfigData: "+b + " ");
//                }
//                System.out.println();
//            }
//
//            for (byte elem : bleDevice.getScanRecord()){
//                Log.d("??","bleConfigData: "+"record : "+elem);
//            }
            showConnectingDialog();
        });
        checkBlePermission();
    }

    private void receiveData() {
        byte[] buffer = new byte[256];
        int bytes;
        String readRecord = new String(buffer, 0, 0);
    }

    private void checkBlePermission() {
        List<String> permissionList = new ArrayList<>();
        permissionList.addAll(Arrays.asList(LOCATION_PERMISSIONS));
        permissionList.addAll(Arrays.asList(BLUETOOTH_PERMISSIONS));
        List<String> requestPermissions = checkPermissions(permissionList);
        if (requestPermissions.isEmpty()) {
            InstaCameraManager.getInstance().setScanBleListener(this);
            InstaCameraManager.getInstance().startBleScan(30_000);
        } else {
            ActivityCompat.requestPermissions(this, requestPermissions.toArray(new String[0]), REQUEST_PERMISSION_CODE);
        }
    }

    public List<String> checkPermissions(List<String> permissions) {
        List<String> requestPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions.add(permission);
            }
        }
        return requestPermissions;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            InstaCameraManager.getInstance().setScanBleListener(null);
            InstaCameraManager.getInstance().stopBleScan();
        }
    }

    @Override
    public void onScanStartSuccess() {
        mTvScanState.setText(R.string.ble_state_start_scan);
    }

    @Override
    public void onScanStartFail() {
        mTvScanState.setText(R.string.ble_state_start_failed);
    }

    @Override
    public void onScanning(BleDevice bleDevice) {
        mTvScanState.setText(R.string.ble_state_scanning);
        mBleDeviceAdapter.addData(bleDevice);
        Log.d("??", "bleConfigData: " + bleDevice.getDevice() + " mac: " + bleDevice.getMac() + " man: " + bleDevice.getManufacturerSpecificData().clone() + " ");
    }

    @Override
    public void onScanFinish(List<BleDevice> bleDeviceList) {
        mTvScanState.setText(R.string.ble_state_scan_finish);
        mBleDeviceAdapter.updateData(bleDeviceList);
    }

    private void showConnectingDialog() {
        mDialog = new MaterialDialog.Builder(this)
                .content(R.string.ble_state_connecting)
                .progress(true, 100)
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .show();
    }

    private void hideConnectingDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    @Override
    public void onCameraStatusChanged(boolean enabled) {
        super.onCameraStatusChanged(enabled);
        if (enabled) {
            hideConnectingDialog();
            String ssid = InstaCameraManager.getInstance().getWifiInfo().getSsid();
            String password = InstaCameraManager.getInstance().getWifiInfo().getPwd();
            Log.d("???", "bleConfigData : ssid :" + ssid + " " + password);
            //finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            checkBlePermission();
        }
    }

    private static class BleDeviceAdapter extends RecyclerView.Adapter<BleDeviceAdapter.MyHolder> {

        private final List<BleDevice> deviceList = new ArrayList<>();

        private void addData(BleDevice bleDevice) {
            deviceList.add(bleDevice);
            notifyDataSetChanged();
        }

        private void updateData(List<BleDevice> list) {
            deviceList.clear();
            deviceList.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public BleDeviceAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recycler_item_ble_device, parent, false);
            return new MyHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyHolder holder, int position) {
            BleDevice bleDevice = deviceList.get(position);
            holder.tvDeviceName.setText(bleDevice.getName() + "  " + bleDevice.getMac());
            holder.itemView.setOnClickListener(v -> {
                if (mItemClickListener != null) {
                    mItemClickListener.onItemClick(bleDevice);
                }
            });
        }


        @Override
        public int getItemCount() {
            return deviceList.size();
        }

        private static class MyHolder extends RecyclerView.ViewHolder {
            TextView tvDeviceName;

            MyHolder(@NonNull View itemView) {
                super(itemView);
                tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            }
        }

        private IItemClickListener mItemClickListener;

        private void setOnItemClickListener(IItemClickListener listener) {
            mItemClickListener = listener;
        }

        private interface IItemClickListener {
            void onItemClick(BleDevice bleDevice);
        }
    }

}
