package com.stry.phichartstudio;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏系统栏（导航栏+状态栏）
        hideSystemBars();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 页面恢复时重新隐藏
        hideSystemBars();
    }

    /**
     * 隐藏系统导航栏（小白条）和状态栏，修复Android 11+的空指针问题
     */
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 方案：延迟到DecorView初始化完成后执行
            getWindow().setDecorFitsSystemWindows(false);
            // 通过post()延迟执行，确保DecorView已创建
            getWindow().getDecorView().post(() -> {
                // 双重null检查，避免DecorView未初始化
                if (getWindow() == null || getWindow().getInsetsController() == null) {
                    return;
                }
                WindowInsetsController controller = getWindow().getInsetsController();
                // 隐藏导航栏和状态栏
                controller.hide(WindowInsets.Type.navigationBars() | WindowInsets.Type.statusBars());
                // 设置交互行为：滑动边缘临时显示
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            });
        } else {
            // Android 10及以下方案（保持不变）
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars(); // 重新获取焦点时再次隐藏
        }
    }
}