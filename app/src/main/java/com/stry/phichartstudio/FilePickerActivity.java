package com.stry.phichartstudio;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FilePickerActivity extends AppCompatActivity {

    private static final int SYSTEM_FILE_PICKER_REQUEST_CODE = 1001; // 系统文件选择器请求码
    private static final String DEFAULT_EXTENSION = ".phichart"; // 自定义文件扩展名
    private ListView fileListView;
    private TextView currentPathTextView;
    private List<File> fileList = new ArrayList<>();
    private File currentDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_picker);

        // 初始化视图
        fileListView = findViewById(R.id.file_list);
        currentPathTextView = findViewById(R.id.current_path);
        Button btnSystemPicker = findViewById(R.id.btn_system_picker); // 系统文件选择器按钮

        // 初始化默认路径
        currentDirectory = Environment.getExternalStorageDirectory();
        loadFiles(currentDirectory);

        // 自定义文件列表点击事件（保持原有逻辑）
        fileListView.setOnItemClickListener((parent, view, position, id) -> {
            File selectedFile = fileList.get(position);
            if (selectedFile.isDirectory()) {
                currentDirectory = selectedFile;
                loadFiles(currentDirectory);
            } else {
                if (selectedFile.getName().endsWith(DEFAULT_EXTENSION)) {
                    returnSelectedFile(selectedFile.getAbsolutePath());
                } else {
                    Toast.makeText(this, "请选择" + DEFAULT_EXTENSION + "格式的文件", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 系统文件选择器按钮点击事件
        btnSystemPicker.setOnClickListener(v -> openSystemFilePicker());

        // 长按路径栏返回上级目录（保持原有逻辑）
        currentPathTextView.setOnLongClickListener(v -> {
            if (currentDirectory.getParentFile() != null) {
                currentDirectory = currentDirectory.getParentFile();
                loadFiles(currentDirectory);
                return true;
            }
            return false;
        });
    }

    // 打开系统文件选择器
    private void openSystemFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // 指定MIME类型（文本类型，可根据需要修改）
        intent.setType("*/*");
        // 添加自定义文件扩展名过滤
        String[] mimeTypes = {"application/octet-stream", "text/plain"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        // 启动系统文件选择器
        startActivityForResult(intent, SYSTEM_FILE_PICKER_REQUEST_CODE);
    }

    // 处理系统文件选择器返回结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SYSTEM_FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    // 将Uri转换为路径（简化处理，实际项目需考虑不同存储类型）
                    String filePath = uri.getPath();
                    // 验证文件扩展名
                    if (filePath != null && filePath.endsWith(DEFAULT_EXTENSION)) {
                        returnSelectedFile(filePath);
                    } else {
                        Toast.makeText(this, "请选择" + DEFAULT_EXTENSION + "格式的文件", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    // 返回选中的文件路径给调用者
    private void returnSelectedFile(String filePath) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selected_file_path", filePath);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    // 加载目录下的文件（保持原有逻辑）
    private void loadFiles(File directory) {
        if (!directory.canRead()) {
            Toast.makeText(this, "无法访问该目录", Toast.LENGTH_SHORT).show();
            return;
        }

        fileList.clear();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() || file.getName().endsWith(DEFAULT_EXTENSION)) {
                    fileList.add(file);
                }
            }
        }

        // 排序：目录在前，文件在后
        Collections.sort(fileList, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        currentPathTextView.setText(directory.getAbsolutePath());

        // 更新列表
        List<String> fileNameList = new ArrayList<>();
        for (File file : fileList) {
            fileNameList.add(file.getName() + (file.isDirectory() ? "/" : ""));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                fileNameList
        );
        fileListView.setAdapter(adapter);
    }

    // 菜单相关（预留保存等功能）
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_picker_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            // 预留保存功能
            Toast.makeText(this, "保存功能待实现", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_create_new) {
            // 预留新建文件功能
            Toast.makeText(this, "新建文件功能待实现", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}