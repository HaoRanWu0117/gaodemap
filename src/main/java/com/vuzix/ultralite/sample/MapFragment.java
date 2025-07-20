package com.vuzix.ultralite.sample;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.navi.AmapNaviPage;
import com.amap.api.navi.AmapNaviParams;
import com.amap.api.navi.AmapNaviType;
import com.amap.api.navi.AmapPageType;
import com.amap.api.maps.model.Poi;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    private MapView mapView;
    private AMap aMap;
    private Button startNaviButton;
    private AMapLocationClient locationClient;
    private AMapLocationClientOption locationOption;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private LatLng endPoint = new LatLng(40, 116); // 目标终点

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // 初始化 MapView
        mapView = view.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();

        if (aMap == null) {
            Log.e("MapError", "Failed to initialize AMap");
            Toast.makeText(getContext(), "地图初始化失败，请检查配置或重试", Toast.LENGTH_SHORT).show();
            return view;
        }

        // 初始化按钮
        startNaviButton = view.findViewById(R.id.start_navi_button);
        startNaviButton.setOnClickListener(v -> startNavigation());

        // 检查网络连接
        if (!isNetworkConnected()) {
            Toast.makeText(getContext(), "请检查网络连接", Toast.LENGTH_LONG).show();
            return view;
        }

        // 请求权限
        requestPermissions();

        return view;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    private void requestPermissions() {
        List<String> neededPermissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(getActivity(), neededPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            setupMapAndMarkers();
        }
    }

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
                Toast.makeText(getContext(), "所有必需权限已获取", Toast.LENGTH_SHORT).show();
                setupMapAndMarkers();
            } else {
                Toast.makeText(getContext(), "定位权限被拒绝，导航无法启动", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupMapAndMarkers() {
        if (aMap != null) {
            aMap.addMarker(new MarkerOptions().position(endPoint).title("终点").snippet("北京"));
        } else {
            Toast.makeText(getContext(), "地图对象未准备好", Toast.LENGTH_SHORT).show();
            Log.e("MapError", "aMap object is null in setupMapAndMarkers()");
        }
    }

    private void startNavigation() {
        if (!isNetworkConnected()) {
            Toast.makeText(getContext(), "请检查网络连接", Toast.LENGTH_LONG).show();
            return;
        }
        initLocation();
    }

    private void initLocation() {
        try {
            locationClient = new AMapLocationClient(getContext());
            locationOption = new AMapLocationClientOption();
            locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            locationOption.setNeedAddress(true);
            locationOption.setOnceLocation(true);
            locationClient.setLocationOption(locationOption);
            locationClient.setLocationListener(locationListener);
            locationClient.startLocation();
        } catch (Exception e) {
            Log.e("LocationError", "Failed to initialize location client: " + e.getMessage());
            Toast.makeText(getContext(), "定位初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    private final AMapLocationListener locationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation location) {
            if (location != null) {
                if (location.getErrorCode() == 0) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    aMap.addMarker(new MarkerOptions().position(currentLocation).title("当前位置"));
                    startNavigationFromCurrentLocation(currentLocation);
                } else {
                    Toast.makeText(getContext(), "定位失败，错误码：" + location.getErrorCode(), Toast.LENGTH_SHORT).show();
                    Log.e("LocationError", "Location failed with error code: " + location.getErrorCode());
                }
            } else {
                Toast.makeText(getContext(), "定位失败，位置信息为空", Toast.LENGTH_SHORT).show();
            }
            if (locationClient != null) {
                locationClient.stopLocation();
            }
        }
    };

    private void startNavigationFromCurrentLocation(LatLng currentLocation) {
        Poi start = new Poi("当前位置", currentLocation, "");
        Poi end = new Poi("北京", endPoint, "");
        AmapNaviParams naviParams = new AmapNaviParams(start, null, end, AmapNaviType.DRIVER, AmapPageType.ROUTE);
        try {
            AmapNaviPage.getInstance().showRouteActivity(getContext(), naviParams, null);
        } catch (Exception e) {
            Log.e("NavigationError", "Failed to start navigation: " + e.getMessage());
            Toast.makeText(getContext(), "导航启动失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (locationClient != null) {
            locationClient.stopLocation();
            locationClient.onDestroy();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}