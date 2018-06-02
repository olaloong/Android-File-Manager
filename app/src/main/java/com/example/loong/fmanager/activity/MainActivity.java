package com.example.loong.fmanager.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.example.loong.fmanager.ActivityCollector;
import com.example.loong.fmanager.BaseActivity;
import com.example.loong.fmanager.FileManage;
import com.example.loong.fmanager.R;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface.OnClickListener;

public class MainActivity extends BaseActivity {

    private TextView currentPathTextView;
    private ListView fileListView;
    private FileManage fileManage = new FileManage(MainActivity.this);
    private LocalReceiver localReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private IntentFilter intentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        currentPathTextView = (TextView) findViewById(R.id.path_label);
        fileListView = (ListView) findViewById(R.id.file_list);
        final FloatingActionButton pasteButton = (FloatingActionButton) findViewById(R.id.paste_button);
        pasteButton.setVisibility(View.GONE);

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }else{
            initFileList(Environment.getExternalStorageDirectory());
        }

        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = fileManage.getFiles()[position];
                if (file.isDirectory()) {
                    initFileList(file);
                } else {
                    fileManage.openFile(file);
                }
            }
        });

        fileListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                final File file = fileManage.getFiles()[position];
                    if (file.canRead()) {
                        AlertDialog.Builder selection = new AlertDialog.Builder(MainActivity.this);
                        selection.setTitle("请选择操作");
                        String[] items;
                        if (file.canWrite()) {
                            items = new String[] { "复制", "剪切", "删除" };
                        } else {
                            items = new String[] { "复制" };
                        }
                        selection.setItems(items, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                switch (i) {
                                    case 0:
                                        fileManage.setCopyFile(file);
                                        fileManage.setCutFile(null);
                                        pasteButton.setVisibility(View.VISIBLE);
                                        break;
                                    case 1:
                                        fileManage.setCutFile(file);
                                        fileManage.setCopyFile(null);
                                        pasteButton.setVisibility(View.VISIBLE);
                                        break;
                                    case 2:
                                        fileManage.deleteFile(file);
                                        break;
                                }
                            }
                        });
                        selection.show();
                    } else {
                        alert("此文件/文件夹不能进行任何操作~");
                    }
                return true;
            }
        });

        pasteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileManage.pasteFile();
                pasteButton.setVisibility(View.GONE);
            }
        });

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.loong.fmanager.FLASH_FILES_LIST");
        localReceiver = new LocalReceiver();
        localBroadcastManager.registerReceiver(localReceiver,intentFilter);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(localReceiver);
    }

    private class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            initFileList(fileManage.getCurrentDirectory());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    initFileList(Environment.getExternalStorageDirectory());
                }else{
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.home:
                initFileList(Environment.getExternalStorageDirectory());
                return true;
            case R.id.create:
                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.create_layout, null);
                final EditText name = (EditText) view.findViewById(R.id.create_name);
                final RadioGroup category = (RadioGroup) view.findViewById(R.id.create_category);
                Builder builder = new Builder(MainActivity.this);
                builder.setTitle("新建");
                builder.setView(view);
                builder.setPositiveButton("取消", null);
                builder.setNegativeButton("确定", new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        int checkedRadioButtonId = category.getCheckedRadioButtonId();
                        String newFileName = name.getText().toString().trim();
                        if (newFileName.equals("")) {
                            alert("未输入文件/文件夹的名字~");
                        } else {
                            switch (checkedRadioButtonId) {
                                case R.id.create_file:
                                    fileManage.createFile(newFileName,0);
                                    break;
                                case R.id.create_directory:
                                    fileManage.createFile(newFileName,1);
                                    break;
                            }
                            initFileList(fileManage.getCurrentDirectory());
                        }
                    }
                });
                builder.show();
                return true;
            case R.id.item1:
                return true;
            case R.id.about:
                return true;
            case R.id.update:
                ActivityCollector.finishAll();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK){
            if(fileManage.getCurrentDirectory().equals(Environment.getExternalStorageDirectory())){
                initFileList(fileManage.getCurrentDirectory());
            }else{
                initFileList(fileManage.getCurrentDirectory().getParentFile());
            }
        }
        return true;
    }

    public void initFileList(File file) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        list = fileManage.populate_list(file);
        SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, list, R.layout.file_item, new String[] { "img", "text","ext" }, new int[] { R.id.fileImageView, R.id.fileTextView ,R.id.extTextView});
        fileListView.setAdapter(adapter);
        currentPathTextView.setText("\t\t" + fileManage.getCurrentDirectory().getPath());
    }

    private void alert(Object message) {
        final String text = message.toString();
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }
}
