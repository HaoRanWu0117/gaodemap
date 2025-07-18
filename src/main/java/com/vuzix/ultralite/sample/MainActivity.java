package com.vuzix.ultralite.sample;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Poi;
import com.amap.api.navi.AmapNaviPage;
import com.amap.api.navi.AmapNaviParams;
import com.amap.api.navi.AmapNaviType;
import com.amap.api.navi.AmapPageType;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private MapView mapView;//地图显示
    private AMap aMap;//地图控制
    private Button startNaviButton;//开始导航按钮
    private AMapLocationClient locationClient;//获取用户当前位置
    private AMapLocationClientOption locationOption;//配置定位参数
    private static final int PERMISSION_REQUEST_CODE=100;//权限请求唯一标识码
    private LatLng endPoint = new LatLng(40, 116); // 目标终点

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置隐私合规
        AMapLocationClient.updatePrivacyShow(this, true, true);
        AMapLocationClient.updatePrivacyAgree(this, true);

        // 初始化 MapView
        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();

        if (aMap == null) {
            Log.e("MapError", "Failed to initialize AMap");
            Toast.makeText(this, "地图初始化失败，请检查配置或重试", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取并初始化按钮
        startNaviButton = findViewById(R.id.start_navi_button);
        startNaviButton.setOnClickListener(v -> startNavigation());

        // 检查网络连接
        if (!isNetworkConnected()) {
            Toast.makeText(this, "请检查网络连接", Toast.LENGTH_LONG).show();
            return;
        }

        // 动态请求权限
        requestPermissions();
    }

    /**
     * 检查网络连接状态
     */
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    /**
     * 请求运行时权限
     */
    private void requestPermissions() {
        List<String> neededPermissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            setupMapAndMarkers();
        }
    }

    /**
     * 处理权限请求结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(this, "所有必需权限已获取", Toast.LENGTH_SHORT).show();
                setupMapAndMarkers();
            } else {
                Toast.makeText(this, "定位权限被拒绝，导航无法启动", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * 设置地图显示和标记点
     */
    private void setupMapAndMarkers() {
        if (aMap != null) {
            aMap.addMarker(new MarkerOptions().position(endPoint).title("终点").snippet("北京"));
        } else {
            Toast.makeText(this, "地图对象未准备好", Toast.LENGTH_SHORT).show();
            Log.e("MapError", "aMap object is null in setupMapAndMarkers()");
        }
    }

    /**
     * 启动高德内置导航
     */
    private void startNavigation() {
        if (!isNetworkConnected()) {
            Toast.makeText(this, "请检查网络连接", Toast.LENGTH_LONG).show();
            return;
        }

        initLocation();
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        try {
            locationClient = new AMapLocationClient(this);
            locationOption = new AMapLocationClientOption();
            locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            locationOption.setNeedAddress(true);
            locationOption.setOnceLocation(true); // 单次定位
            locationClient.setLocationOption(locationOption);
            locationClient.setLocationListener(locationListener);
            locationClient.startLocation();
        } catch (Exception e) {
            Log.e("LocationError", "Failed to initialize location client: " + e.getMessage());
            Toast.makeText(this, "定位初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 定位监听器
     */
    private AMapLocationListener locationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation location) {
            if (location != null) {
                if (location.getErrorCode() == 0) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    aMap.addMarker(new MarkerOptions().position(currentLocation).title("当前位置"));
                    startNavigationFromCurrentLocation(currentLocation);
                } else {
                    Toast.makeText(MainActivity.this, "定位失败，错误码：" + location.getErrorCode(), Toast.LENGTH_SHORT).show();
                    Log.e("LocationError", "Location failed with error code: " + location.getErrorCode());
                }
            } else {
                Toast.makeText(MainActivity.this, "定位失败，位置信息为空", Toast.LENGTH_SHORT).show();
            }
            if (locationClient != null) {
                locationClient.stopLocation();
            }
        }
    };

    /**
     * 从当前位置启动导航
     */
    private void startNavigationFromCurrentLocation(LatLng currentLocation) {
        Poi start = new Poi("当前位置", currentLocation, "");
        Poi end = new Poi("北京", endPoint, "");
        AmapNaviParams naviParams = new AmapNaviParams(start, null, end, AmapNaviType.DRIVER, AmapPageType.ROUTE);
        try {
            AmapNaviPage.getInstance().showRouteActivity(this, naviParams, null);
        } catch (Exception e) {
            Log.e("NavigationError", "Failed to start navigation: " + e.getMessage());
            Toast.makeText(this, "导航启动失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (locationClient != null) {
            locationClient.stopLocation();
            locationClient.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}