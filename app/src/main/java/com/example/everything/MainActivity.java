package com.example.everything;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity {
    // Widget
    private Button bFetch = null;
    private Button bDream = null;
    private Button bMiracle = null;
    private Button bPlm = null;
    private EditText etInput = null;
    private ListView lvResult = null;
    private TextView tvNo = null;
    private TextView tvWhen = null;
    private TextView tvDescription = null;

    // Variable
    private String id = "owg7689";
    private String id_mobile = "root";
    private String password = "dhkdrms365";
    private String password_mobile = "qwer1234";
    private String url_internal = "jdbc:mysql://192.168.29.246:3306/Practice";
    private String url_mobile = "jdbc:mysql://localhost:3306/Practice";
    private String url_external = "jdbc:mysql://180.70.146.134:3306/Practice?autoReconnect=true";
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

    // Message
    private final int MSG_CONNECT_SUCCESS = 0;
    private final int MSG_CONNECT_FAIL = 1;
    private final int MSG_INSERT_SUCCESS = 2;
    private final int MSG_INSERT_FAIL = 3;
    private final int MSG_SELECT_SUCCESS = 4;
    private final int MSG_SELECT_FAIL = 5;
    private final int MSG_DREAM_SUCCESS = 6;
    private final int MSG_DREAM_FAIL = 7;
    private final int MSG_MIRACLE_SUCCESS = 8;
    private final int MSG_MIRACLE_FAIL = 9;
    private final int MSG_PLM_SUCCESS = 10;
    private final int MSG_PLM_FAIL = 11;

    // WorkType(for thread) : DBControl 한테 줄 일의 타입
    private final int WT_FETCH_ALL_AND_ADD_ALL_ITEMS_TO_ADAPTER = 50;
    private final int WT_INSERT_ITEM_TO_DB_AND_ADD_ITEM_TO_ADAPTER = 51;
    private final int WT_SEARCH_ITEMS_FROM_DB_AND_ADD_ITEM_TO_ADAPTER = 52;
    private final int WT_SEARCH_DREAM_AND_ADD_ITEM_TO_ADAPTER = 53;
    private final int WT_SEARCH_MIRACLE_AND_ADD_ITEM_TO_ADAPTER = 54;
    private final int WT_SEARCH_PLM_AND_ADD_ITEM_TO_ADAPTER = 55;

    // Handler : DBControl(thread)가 처리한 결과를 전달 받는 역할, thread 에서 역시 mHandler 를 통해 이 함수로 보낸다.
    public final Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_CONNECT_SUCCESS:
                    Log.i("MainActivity", "MSG_CONNECT_SUCCESS");
                    etInput.setText(null);
                    break;
                case MSG_CONNECT_FAIL:
                    Log.i("MainActivity", "MSG_CONNECT_FAIL");
                    Toast.makeText(getApplicationContext(), "MSG_CONNECT_FAIL", Toast.LENGTH_LONG).show();
                    break;
                case MSG_INSERT_SUCCESS:
                    Log.i("MainActivity", "MSG_INSERT_SUCCESS");
                    etInput.setText(null);
                    dbConThread = new DBControl(WT_FETCH_ALL_AND_ADD_ALL_ITEMS_TO_ADAPTER);
                    dbConThread.start();
                    break;
                case MSG_INSERT_FAIL:
                    Log.i("MainActivity", "MSG_INSERT_FAIL" + error);
                    Toast.makeText(getApplicationContext(), "MSG_INSERT_FAIL : " + error, Toast.LENGTH_LONG).show();
                    break;
                case MSG_SELECT_SUCCESS:
                    Log.i("MainActivity", "MSG_SELECT_SUCCESS");
                    adapter.notifyDataSetChanged();

                    if (etInput.getText().length() != 0) {
                        etInput.setText(null);
                    }
                    etInput.setTextColor(Color.BLACK);
                    break;
                case MSG_SELECT_FAIL:
                    Log.i("MainActivity", "MSG_SELECT_FAIL");
                    Toast.makeText(getApplicationContext(), "MSG_SELECT_FAIL", Toast.LENGTH_LONG).show();
                    break;
                case MSG_DREAM_SUCCESS:
                    Log.i("MainActivity", "MSG_DREAM_SUCCESS");
                    adapter.notifyDataSetChanged();

                    if (etInput.getText().length() != 0) {
                        etInput.setText(null);
                    }
                    etInput.setText("Energy : " + DreamEnergy);
                    etInput.setTextColor(Color.BLUE);
                    break;
                case MSG_MIRACLE_SUCCESS:
                    Log.i("MainActivity", "MSG_MIRACLE_SUCCESS");
                    adapter.notifyDataSetChanged();

                    if (etInput.getText().length() != 0) {
                        etInput.setText(null);
                    }
                    etInput.setText("연속 성공 : " + miracleContinueSuccess + ", 성공 : " + miracleSuccess +
                            ", 실패 : " + miracleFail);
                    etInput.setTextColor(Color.BLUE);
                    break;
                case MSG_PLM_SUCCESS:
                    Log.i("MainActivity", "MSG_PLM_SUCCESS");
                    adapter.notifyDataSetChanged();
                    if (etInput.getText().length() != 0) {
                        etInput.setText(null);
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("MainActivity", "onCreate(...) start");

        // 'FETCH' button
        bFetch = (Button) findViewById(R.id.btn_fetch);
        bFetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //SELECT * FROM tEverything WHERE description LIKE '%apn%';
                // 1. editText의 내용을 가져 온다.
                // 2. DBControl에게 검색 임무를 준다.
                dbConThread = new DBControl(WT_SEARCH_ITEMS_FROM_DB_AND_ADD_ITEM_TO_ADAPTER);
                dbConThread.start();
            }
        });

        // 'DREAM' button
        bDream = (Button) findViewById(R.id.btn_dream);
        bDream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbConThread = new DBControl(WT_SEARCH_DREAM_AND_ADD_ITEM_TO_ADAPTER);
                dbConThread.start();
            }
        });

        // 'MIRACLE' button
        bMiracle = (Button) findViewById(R.id.btn_miracle);
        bMiracle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbConThread = new DBControl(WT_SEARCH_MIRACLE_AND_ADD_ITEM_TO_ADAPTER);
                dbConThread.start();
            }
        });

        // 'PLM' button
        bPlm = (Button) findViewById(R.id.btn_plm);
        bPlm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbConThread = new DBControl(WT_SEARCH_PLM_AND_ADD_ITEM_TO_ADAPTER);
                dbConThread.start();
            }
        });
        // EditText
        etInput = (EditText) findViewById(R.id.et_input);
        etInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                switch (keyCode) {
                    case (KeyEvent.KEYCODE_ENTER):
                        if (event.getAction() == KeyEvent.ACTION_UP) {
                            String content = etInput.getText().toString();
                            content = content.replaceAll("\\R", "");
                            etInput.setText(content);
                            Log.i("MainActivity", "onKey(KeyEvent.KEYCODE_ENTER) : " + content);

                            if (content.contains(".")) {
                                Log.i("MainActivity", "onKey(KeyEvent.KEYCODE_ENTER) - 51");
                                dbConThread = new DBControl(WT_INSERT_ITEM_TO_DB_AND_ADD_ITEM_TO_ADAPTER);

                            } else {
                                Log.i("MainActivity", "onKey(KeyEvent.KEYCODE_ENTER) - 52");
                                dbConThread = new DBControl(WT_SEARCH_ITEMS_FROM_DB_AND_ADD_ITEM_TO_ADAPTER);
                            }
                            dbConThread.start();
                        }
                        break;
                }
                return false;
            }
        });

        // ListView 와 Adapter 를 만들고 그 둘을 연결
        lvResult = (ListView) findViewById(R.id.lv_result);
        adapter = new ListViewAdapter();
        lvResult.setAdapter(adapter);

        //DBControl 클래스의 객체를 만들고, 생성자로 할일의 타입을 넘겨 줌
        dbConThread = new DBControl(WT_FETCH_ALL_AND_ADD_ALL_ITEMS_TO_ADAPTER);
        dbConThread.start();

        Log.i("MainActivity", "onCreate(...) end");
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

    // 충실한 일꾼 클래스, SQL 관련 모든 작업을 한다, 생성 될 때 메시지를 받고, 완료되면 메시지를 전송 한다
    private class DBControl extends Thread {
        private int workType = 0;
        private Statement statement = null;
        private ResultSet resultSet = null;
        private int no = 0;
        private String when = null;
        private String description = null;

        // 구조상, 생성될 때 마다 일감을 받으니 하나의 작업 당 하나의 객체가 만들어 짐.
        public DBControl(int workType) {

            this.workType = workType;

        }

        // run() 을 한 번 완수하면 사라지나? 한번 만들어진 객체는 언제 사라질까?
        public void run() {
            Log.i("DBControl", "run() start, workType : " + workType);
            Message message = null;
            try {
                if (mConn == null) {
                    message = mHandler.obtainMessage();
                    //mConn = DriverManager.getConnection(url_external, id, password);
                    mConn = DriverManager.getConnection(url_mobile, id_mobile, password_mobile);
                    message.what = MSG_CONNECT_SUCCESS;
                    mHandler.sendMessage(message);
                }

                switch (workType) {
                    case WT_FETCH_ALL_AND_ADD_ALL_ITEMS_TO_ADAPTER:
                        adapter.recordList.clear();
                        String sqlSelectAll = "SELECT * FROM tEverything ORDER BY no desc;";
                        statement = mConn.createStatement();
                        Log.i("DBControl", "sqlSelectAll  : " + sqlSelectAll);

                        if (statement.execute(sqlSelectAll)) {
                            resultSet = statement.getResultSet();
                        }
                        //Add all items to Adapter
                        while (resultSet.next()) {
                            no = resultSet.getInt(1);
                            if (lastNo < no)
                                lastNo = no;
                            String when = resultSet.getString(2);
                            String description = resultSet.getString(3);
                            adapter.addItem(no, when, description);
                        }
                        message = mHandler.obtainMessage();
                        message.what = MSG_SELECT_SUCCESS;
                        mHandler.sendMessage(message);
                        Log.i("DBControl", "Add all items to Adapter");
                        break;
                    case WT_INSERT_ITEM_TO_DB_AND_ADD_ITEM_TO_ADAPTER:
                        String when = getCurrentDateTime();
                        String sqlInsert = "INSERT INTO `tEverything`";

                        sqlInsert += " (`no`, `when`, `description`)";
                        sqlInsert += " values (? , ? , ?)";
                        Log.i("DBControl", "sqlInsert  : " + sqlInsert);

                        if (etInput.getText().length() != 0) {
                            try {
                                prepareStatement = mConn.prepareStatement(sqlInsert);
                                prepareStatement.setInt(1, lastNo + 1);
                                prepareStatement.setString(2, when);
                                prepareStatement.setString(3, String.valueOf(etInput.getText()));
                                prepareStatement.executeUpdate();
                                prepareStatement.close();
                                adapter.addItem(lastNo + 1, when, String.valueOf(etInput.getText()));
                                Log.i("DBControl", sqlInsert + " success");
                                message = mHandler.obtainMessage();
                                message.what = MSG_INSERT_SUCCESS;
                                mHandler.sendMessage(message);
                            } catch (SQLException e) {
                                error = "";
                                e.printStackTrace();
                                error = "" + e.toString();
                                Log.i("DBControl", sqlInsert + " fail");
                                message = mHandler.obtainMessage();
                                message.what = MSG_INSERT_FAIL;
                                mHandler.sendMessage(message);
                            }
                        }
                        break;
                    case WT_SEARCH_ITEMS_FROM_DB_AND_ADD_ITEM_TO_ADAPTER:
                        if (etInput.getText().length() != 0 && !etInput.getText().toString().contains("Energy :")
                                && !etInput.getText().toString().contains("Miracle :") ) {
                            String sqlSelectKeyword = "SELECT * FROM `tEverything` WHERE `description` LIKE ";
                            sqlSelectKeyword += "'%" + etInput.getText() + "%'";
                            sqlSelectKeyword += "ORDER BY no desc";
                            statement = mConn.createStatement();
                            Log.i("DBControl", "sqlSelectAll  : " + sqlSelectKeyword);

                            if (statement.execute(sqlSelectKeyword)) {
                                resultSet = statement.getResultSet();
                                adapter.recordList.clear();
                            }
                            //Add keyword items to Adapter
                            while (resultSet.next()) {
                                int no = resultSet.getInt(1);
                                if (lastNo < no)
                                    lastNo = no;
                                when = resultSet.getString(2);
                                description = resultSet.getString(3);
                                adapter.addItem(no, when, description);
                            }
                            message = mHandler.obtainMessage();
                            message.what = MSG_SELECT_SUCCESS;
                            mHandler.sendMessage(message);
                            Log.i("DBControl", "Add all items to Adapter(result of keyword)");
                        } else {
                            adapter.recordList.clear();
                            dbConThread = new DBControl(WT_FETCH_ALL_AND_ADD_ALL_ITEMS_TO_ADAPTER);
                            dbConThread.start();
                            Log.i("DBControl", "Add all items to Adapter");
                        }
                        break;
                    case WT_SEARCH_DREAM_AND_ADD_ITEM_TO_ADAPTER:
                        DreamEnergy = 0;
                        String sqlSelectDream = "SELECT * FROM `tEverything` WHERE `description` LIKE '%획득+%' OR `description` LIKE '%소진-%'";
                        sqlSelectDream += "ORDER BY no desc";
                        statement = mConn.createStatement();
                        Log.i("DBControl", "sqlSelectAll  : " + sqlSelectDream);

                        if (statement.execute(sqlSelectDream)) {
                            resultSet = statement.getResultSet();
                            adapter.recordList.clear();
                        }
                        //Add keyword items to Adapter
                        while (resultSet.next()) {
                            int no = resultSet.getInt(1);
                            if (lastNo < no)
                                lastNo = no;
                            when = resultSet.getString(2);
                            description = resultSet.getString(3);

                            int start = 0;
                            String numString = null;
                            String subDescription = null;

                            if ((start = description.indexOf("-")) >= 0) {
                                subDescription = description.substring(start);
                                //numString = subDescription.replaceAll("[^0-9]","");
//                                numString = subDescription.substring(start, start+3);
//                                numString = numString.replaceAll("[^0-9]","");

                                StringTokenizer str = new StringTokenizer(subDescription, " ");
                                while (str.hasMoreTokens()) {
                                    DreamEnergy -= Integer.parseInt(str.nextToken().replaceAll("[^0-9]",""));
                                    break;
                                }
//                              Log.i("DBControl", "DreamEnergy  : " + Integer.parseInt(numString) + "description :" + description);
                            } else {
                                start = description.indexOf("+");
                                subDescription = description.substring(start);

                                StringTokenizer str = new StringTokenizer(subDescription, " ");
                                while (str.hasMoreTokens()) {
                                    DreamEnergy += Integer.parseInt(str.nextToken().replaceAll("[^0-9]",""));
                                    break;
                                }
//                              Log.i("DBControl", "DreamEnergy  : " + Integer.parseInt(numString) + "description :" + description);
                            }
                            adapter.addItem(no, when, description);
                        }
                        message = mHandler.obtainMessage();
                        message.what = MSG_DREAM_SUCCESS;
                        mHandler.sendMessage(message);
                        Log.i("DBControl", "Add all items to Adapter(Dream)");
                        break;
                    case WT_SEARCH_MIRACLE_AND_ADD_ITEM_TO_ADAPTER:
                        String sqlSelectMiracle = "SELECT * FROM `tEverything` WHERE `description` " +
                                "LIKE '%기상%' OR `description` LIKE '%취침%' OR `description` LIKE '%미라클모닝%'";
                        sqlSelectMiracle += "ORDER BY no desc";
                        statement = mConn.createStatement();
                        miracleSuccess = 0;
                        miracleFail = 0;
                        miracleContinueSuccess = 0;
                        Log.i("DBControl", "sqlSelectAll  : " + sqlSelectMiracle);

                        if (statement.execute(sqlSelectMiracle)) {
                            resultSet = statement.getResultSet();
                            adapter.recordList.clear();
                        }
                        //Add keyword items to Adapter
                        while (resultSet.next()) {
                            int no = resultSet.getInt(1);
                            if (lastNo < no)
                                lastNo = no;
                            when = resultSet.getString(2);
                            description = resultSet.getString(3);

                            int startPlus = description.indexOf("+");
                            if (startPlus > 0) {
                                miracleSuccess++;
                                if (miracleFail == 0) {
                                    miracleContinueSuccess++;
                                }
                            }

                            int startMinus = description.indexOf("-");
                            if (startMinus > 0) {
                                miracleFail++;
                            }
                            adapter.addItem(no, when, description);
                        }

                        Log.i("DBControl", "Add all items to Adapter(Miracle)");
                        message = mHandler.obtainMessage();
                        message.what = MSG_MIRACLE_SUCCESS;
                        mHandler.sendMessage(message);
                        break;
                    case WT_SEARCH_PLM_AND_ADD_ITEM_TO_ADAPTER:
                        String sqlSelectPlm = "SELECT * FROM `tEverything` WHERE `description` LIKE '%업무%'";
                        sqlSelectPlm += " ORDER BY no desc";
                        statement = mConn.createStatement();
                        Log.i("DBControl", "sqlSelectAll  : " + sqlSelectPlm);

                        if (statement.execute(sqlSelectPlm)) {
                            resultSet = statement.getResultSet();
                            adapter.recordList.clear();
                        }
                        //Add keyword items to Adapter
                        while (resultSet.next()) {
                            int no = resultSet.getInt(1);
                            if (lastNo < no)
                                lastNo = no;
                            when = resultSet.getString(2);
                            description = resultSet.getString(3);
                            adapter.addItem(no, when, description);
                        }

                        Log.i("DBControl", "Add all items to Adapter(sqlSelectPlm)");
                        message = mHandler.obtainMessage();
                        message.what = MSG_PLM_SUCCESS;
                        mHandler.sendMessage(message);
                        break;
                }
            } catch (SQLException e) {
                message = mHandler.obtainMessage();
                message.what = MSG_CONNECT_FAIL;
                mHandler.sendMessage(message);
                e.printStackTrace();
            }
            Log.i("DBControl", "run() end, workType : " + workType);
        }
    }

    public String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmm");
        String currentDateTime = sdf.format(new Date());
        return currentDateTime;
    }

    public String getStringAssetFile(Activity activity) throws Exception {
        AssetManager as = activity.getAssets();
        InputStream is = as.open("everything.txt");

        String text = convertStreamToString(is);
        is.close();

        return text;
    }

    public String convertStreamToString(InputStream is) throws IOException {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        int i = is.read();
        while (i != -1) {
            bs.write(i);
            i = is.read();
        }
        return bs.toString();
    }
}