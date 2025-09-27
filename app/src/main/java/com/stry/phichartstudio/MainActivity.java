package com.stry.phichartstudio;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends BaseActivity implements TrackTimelineView.OnClipSelectedListener, TrackTimelineView.OnNoteSelectedListener {

    private TrackTimelineView timelineView;
    private EditText etTrack, etStartBeat, etDuration, etLabel;
    private Button btnAddClip;
    private Chart.Clip selectedClip;
    private Button btnOpenFile;
    private View dynamicView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏状态栏和导航栏，适配全面屏
        hideSystemBars();
        setContentView(R.layout.activity_main);

        // 初始化视图
        timelineView = findViewById(R.id.timelineView);
        // 设置监听器
        timelineView.setOnClipSelectedListener(this);
        timelineView.setOnNoteSelectedListener(this);

        // 预设一些示例片段
        initSampleClips();
        showDefaultWindow();
    }

    // 初始化示例片段
    private void initSampleClips() {
    }

    @Override
    public void onClipSelected() {
        showPopupWindow();
    }

    @Override
    public void onClipDeselected() {
        showDefaultWindow();
    }

    @Override
    public void onNoteSelected() {
        showPopupWindow();
    }

    @Override
    public void onNoteDeselected() {
        showDefaultWindow();
    }

    private void hideSystemBars() {
        // 设置状态栏透明（解决紫色背景残留）
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        // 针对 Android 11+（API 30+）的全面屏适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            // 延迟到视图初始化完成后获取InsetsController
            getWindow().getDecorView().post(() -> {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    // 隐藏状态栏
                    controller.hide(WindowInsets.Type.statusBars());
                    // 允许交互时临时显示状态栏
                    controller.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                }
            });
        }
        // 针对 Android 10 及以下（API 29 及以下）
        else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    public void onNewPressed(View view) {
        timelineView = findViewById(R.id.timelineView);
        timelineView.onNew();
        timelineView.invalidate();
    }

    public void onDeletePressed(View view) {
        timelineView = findViewById(R.id.timelineView);
        for (Chart.Clip clip: timelineView.selectedClips) {
            timelineView.line.getClips(clip.track).remove(clip);
        }
        for (Chart.Note note: timelineView.selectedNotes) {
            timelineView.line.notes.remove(note);
        }
        timelineView.selectedClips.clear();
        timelineView.selectedNotes.clear();
        timelineView.invalidate();
        showDefaultWindow();
    }


    // 显示悬浮小窗
    protected FrameLayout frameContainer;
    public void showPopupWindow() {
        LayoutInflater inflater = LayoutInflater.from(this);
        TrackTimelineView timelineView = (TrackTimelineView) findViewById(R.id.timelineView);
        frameContainer = (FrameLayout) findViewById(R.id.rightFrame);

        // 2. 清空容器（可选：避免重复加载）
        frameContainer.removeAllViews();

        if (!timelineView.selectedClips.isEmpty()) {
            dynamicView = inflater.inflate(R.layout.activity_clip_configure, frameContainer, false);

            if (timelineView.selectedClips.size() == 1) {
                Chart.Clip selectedClip = timelineView.selectedClips.get(0);

                EditText et1 = (EditText) dynamicView.findViewById(R.id.clipEt1);
                EditText et2 = (EditText) dynamicView.findViewById(R.id.clipEt2);
                EditText et3 = (EditText) dynamicView.findViewById(R.id.clipEt3);
                EditText et4 = (EditText) dynamicView.findViewById(R.id.clipEt4);
                EditText et5 = (EditText) dynamicView.findViewById(R.id.clipEt5);
                et1.setHint(String.valueOf(Chart.timeToBeat(selectedClip.st)));
                et2.setHint(String.valueOf(Chart.timeToBeat(selectedClip.et)));
                et3.setHint(String.valueOf(selectedClip.sv));
                et4.setHint(String.valueOf(selectedClip.ev));
                et5.setHint(String.valueOf(selectedClip.easing));
            }
        }
        else if (!timelineView.selectedNotes.isEmpty()) {
            dynamicView = inflater.inflate(R.layout.activity_note_configure, frameContainer, false);
        }
        else {
            return;
        }

        // 3. 将加载的布局添加到容器中
        frameContainer.addView(dynamicView);
    }
    public void removePopupWindow() {
        frameContainer = (FrameLayout) findViewById(R.id.rightFrame);
        frameContainer.removeAllViews();
    }

    public void showDefaultWindow() {
        frameContainer = (FrameLayout) findViewById(R.id.rightFrame);
        frameContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        View dynamicView = inflater.inflate(R.layout.default_menu, frameContainer, false);
        frameContainer.addView(dynamicView);
    }

    public void clipConfigApply(View v) {
        EditText et1 = (EditText) dynamicView.findViewById(R.id.clipEt1);
        EditText et2 = (EditText) dynamicView.findViewById(R.id.clipEt2);
        EditText et3 = (EditText) dynamicView.findViewById(R.id.clipEt3);
        EditText et4 = (EditText) dynamicView.findViewById(R.id.clipEt4);
        EditText et5 = (EditText) dynamicView.findViewById(R.id.clipEt5);

        for (Chart.Clip clip: timelineView.selectedClips) {
            String et1Get = et1.getText().toString();
            if (!et1Get.isEmpty()) {
                try {
                    float valueGotten = (Chart.beatToTime(et1Get));
                    clip.st = valueGotten;
                } catch (Exception e) {Toast.makeText(MainActivity.this, "起始时间错误！", Toast.LENGTH_SHORT).show(); break;}
            }


            String et2Get = et2.getText().toString();
            if (!et2Get.isEmpty()) {
                try {
                    float valueGotten = (Chart.beatToTime(et2Get));
                    clip.et = valueGotten;
                } catch (Exception e) {Toast.makeText(MainActivity.this, "结束时间错误！", Toast.LENGTH_SHORT).show(); break;}
            }


            String et3Get = et3.getText().toString();
            if (!et3Get.isEmpty()) {
                try {
                    clip.sv = Float.parseFloat(et3Get);
                } catch (Exception e) {Toast.makeText(MainActivity.this, "起始值错误！", Toast.LENGTH_SHORT).show(); break;}
            }

            String et4Get = et4.getText().toString();
            if (!et4Get.isEmpty()) {
                try {
                    clip.ev = Float.parseFloat(et4Get);
                } catch (Exception e) {Toast.makeText(MainActivity.this, "结束值错误！", Toast.LENGTH_SHORT).show(); break;}
            }

            String et5Get = et5.getText().toString();
            if (!et5Get.isEmpty()) {
                try {
                    clip.easing = Integer.parseInt(et5Get);
                } catch (Exception e) {Toast.makeText(MainActivity.this, "缓动类型错误！", Toast.LENGTH_SHORT).show(); break;}
            }
        }

        timelineView.invalidate();

    }
}