package com.vuzix.ultralite.sample;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private List<Fragment> fragmentList;
    private List<String> titleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置隐私合规
        com.amap.api.location.AMapLocationClient.updatePrivacyShow(this, true, true);
        com.amap.api.location.AMapLocationClient.updatePrivacyAgree(this, true);

        // 初始化控件
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        // 初始化 Fragment 和标题
        fragmentList = new ArrayList<>();
        titleList = new ArrayList<>();

        fragmentList.add(new MapFragment());
        fragmentList.add(new BlankFragment());
        titleList.add("地图导航");
        titleList.add("空白页面");

        // 设置 ViewPager 适配器
        MyPagerAdapter adapter = new MyPagerAdapter(getSupportFragmentManager(), fragmentList, titleList);
        viewPager.setAdapter(adapter);

        // 关联 TabLayout 和 ViewPager
        tabLayout.setupWithViewPager(viewPager);
    }
}