package com.example.everything_v1_02;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.everything_v1_02.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    // ------------------------- Variable -------------------------
    // for linux vm
    private String id = "owg7689";
    private String password = "dhkdrms365";
    private String url_internal = "jdbc:mysql://192.168.29.246:3306/Practice";
    private String url_external = "jdbc:mysql://180.70.146.134:3306/Practice?autoReconnect=true";

    // for mobile
    private String id_mobile = "root";
    private String password_mobile = "qwer1234";
    private String url_mobile = "jdbc:mysql://localhost:3306/Practice";

    private String error = "";
    private ListViewAdapter adapter = null;
    private DBControl dbConThread = null;
    private int lastNo = 0;
    private PreparedStatement prepareStatement;
    private Connection mConn = null;
    private int DreamEnergy = 0;
    private int miracleContinueSuccess = 0;
    private int miracleSuccess = 0;
    private int miracleFail = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        Log.i("MainActivity", "onDestroy() end");
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
        // 1. DBControl thread 가 살아있다면 완전히 종료를 시키고 앱이 onDestroy 하도록 메시지를 보냄
    }

    // 어댑터 클래스, 보여줄 아이템의 구성에 따라 구현 결정.
    private class ListViewAdapter extends BaseAdapter {
        private ArrayList<Record> recordList = new ArrayList<Record>();

        public ListViewAdapter() {
        }

        @Override
        public int getCount() {
            return recordList.size();
        }

        @Override
        public Object getItem(int position) {
            return recordList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Context context = parent.getContext();

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.listview_item, parent, false);
            }

            tvNo = (TextView) convertView.findViewById(R.id.tv_no);
            tvWhen = (TextView) convertView.findViewById(R.id.tv_date);
            tvDescription = (TextView) convertView.findViewById(R.id.tv_description);

            Record record = recordList.get(position);
            tvNo.setText("" + record.getNo());
            tvNo.setTextSize(7);
            tvWhen.setText(record.getDate());
            tvWhen.setTextSize(7);
            tvDescription.setText(record.getDescription());
            tvDescription.setTextSize(10);

            return convertView;
        }

        public void addItem(int no, String when, String description) {
            Record item = new Record(no, when, description);
            recordList.add(item);
        }
    }
    // 아이템 하나를 구성
    private class Record {
        int no;
        String when;
        String description;

        public Record(int no, String when, String description) {
            setNo(no);
            setDate(when);
            setDescription(description);
        }

        public int getNo() {
            return this.no;
        }

        public String getDate() {
            return this.when;
        }

        public String getDescription() {
            return this.description;
        }

        public void setNo(int no) {
            this.no = no;
        }

        public void setDate(String date) {
            this.when = date;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}