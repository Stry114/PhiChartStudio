package com.stry.phichartstudio;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.EditText;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

public class TrackTimelineView extends View {

    private static final int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
    // 轨道配置
    protected static final int TRACK_COUNT = 5; // 普通轨道数量
    protected static final int NOTE_TRACK_INDEX = 0; // Note轨作为第一轨
    private static final int TRACK_SPACING = screenWidth<2400 ? 80 : 100;; // Note轨与普通轨道间距
    private static final float DEFAULT_PIXELS_PER_EIGHTH_BEAT = 20; // 每1/8拍像素高度
    private static final int TRACK_WIDTH = screenWidth<2400 ? 120 : 150;; // 普通轨道宽度
    private static final int NOTE_TRACK_WIDTH = screenWidth<2400 ? 600 : 750; // Note轨宽度（可自定义）
    private static final int TIMELINE_WIDTH = 0; // 时间轴宽度

    // 颜色配置
    private static final int TRACK_LINE_COLOR = 0xFFE0E0E0;
    private static final int BEAT_LINE_COLOR = 0xFFCCCCCC;
    private static final int STRONG_BEAT_LINE_COLOR = 0xFFAAAAAA;
    private static final int SELECTED_CLIP_COLOR = 0xFF00FA9A;
    private static final int NORMAL_CLIP_COLOR = 0xFF1e90ff;
    private static final int NOTE_COLOR = 0xFFFF9800;
    private static final int CLIP_BORDER_COLOR = Color.WHITE;
    private static final int SNAP_LINE_COLOR = 0xFFFF0000; // 吸附线颜色
    private static final int SNAP_LINE_WIDTH = 2;
    private static final int SELECTED_BORDER_WIDTH = 3;
    private static final int NORMAL_BORDER_WIDTH = 1;

    // 其他配置
    private static float ts = 2.0f; // 当前时间
    private static int xs = -2; // 正在编辑的轨道
    private Float tempSnapLineBeat = null; // 临时Y轴吸附线
    private Float tempSnapLineX = null; // 临时X轴吸附线
    private static final float X_SNAP_INTERVAL = 135; // X轴吸附间隔（像素）

    // 普通轨道片段数据类
//    public static class Clip {
//        int track;          // 轨道索引（0-4）
//        String label;       // 标签
//        int color;          // 颜色
//
//        public float startValue;
//        public float endValue;
//        public float startBeat;    // 起始1/8拍位置
//        public float duration;     // 持续1/8拍数
//
//        public Clip(int track, float startBeat, float duration, String label) {
//            this.track = track;
//            this.startBeat = startBeat;
//            this.duration = duration;
//            this.label = label;
//            this.color = NORMAL_CLIP_COLOR;
//        }
//
//        public Clip(int track, float startBeat, float duration) {
//            this(track, startBeat, duration, "");
//        }
//    }
//
//    // Note轨道数据类（支持X坐标和横向拖动）
//    public static class Note {
//        float startBeat;    // 起始1/8拍位置（Y轴）
//        float duration;     // 持续1/8拍数（Y轴长度）
//        float xPosition;    // X轴位置（Note轨内横向位置）
//        float width;
//        String name;
//
//        public Note(float startBeat, float duration, float posX) {
//            this.startBeat = startBeat;
//            this.duration = duration;
//            this.xPosition = posX; // 默认起始X位置
//            this.width = 10;
//            this.name = "Note";
//        }
//
//        private String generateNoteName(int pitch) {
//            String[] notes = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
//            int octave = (pitch / 12) - 1;
//            int noteIndex = pitch % 12;
//            return notes[noteIndex] + octave;
//        }
//    }

    // 数据集合
    protected Chart chart = Chart.newDefaultChart(160);
    protected Chart.Line line = chart.lines.get(0);
    protected ArrayList<Chart.Clip> selectedClips = new ArrayList<>();
    protected ArrayList<Chart.Note> selectedNotes = new ArrayList<>();

    // 触摸相关变量
    private float initialY, initialX;
    private float initialStartBeat, initialXPosition;
    private Chart.Track initialTrack;
    private float scrollY = 0;
    private float scaleFactor = screenWidth<2400 ? 1.8f : 2.4f;;
    private float minScale = 0.5f;
    private float maxScale = 5.0f;
    private float touchSlop;
    private boolean isScrolling = false;
    private boolean isDraggingClip = false;
    private boolean isDraggingNote = false;
    private boolean isScaling = false;
    private float lastY, lastX;
    private float initialDistance;
    private float initialScaleFactor;

    // 监听器接口
    public interface OnClipSelectedListener {
        void onClipSelected();
        void onClipDeselected();
    }

    public interface OnNoteSelectedListener {
        void onNoteSelected();
        void onNoteDeselected();
    }

    private OnClipSelectedListener clipSelectedListener;
    private OnNoteSelectedListener noteSelectedListener;

    // 画笔
    private Paint trackLinePaint;
    private Paint beatLinePaint;
    private Paint strongBeatLinePaint;
    private Paint tsLinePaint;
    private Paint clipPaint;
    private Paint clipBorderPaint;
    private Paint notePaint;
    private Paint textPaint;
    private Paint timelineTextPaint;
    private Paint snapLinePaint;

    // 构造函数
    public TrackTimelineView(Context context) {
        super(context);
        init();
    }

    public TrackTimelineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TrackTimelineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 初始化触摸阈值
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        // 初始化画笔
        trackLinePaint = new Paint();
        trackLinePaint.setColor(TRACK_LINE_COLOR);
        trackLinePaint.setStrokeWidth(1);

        beatLinePaint = new Paint();
        beatLinePaint.setColor(BEAT_LINE_COLOR);
        beatLinePaint.setStrokeWidth(1);

        tsLinePaint = new Paint();
        tsLinePaint.setColor(STRONG_BEAT_LINE_COLOR);
        tsLinePaint.setStrokeWidth(5);

        strongBeatLinePaint = new Paint();
        strongBeatLinePaint.setColor(STRONG_BEAT_LINE_COLOR);
        strongBeatLinePaint.setStrokeWidth(2);

        clipPaint = new Paint();
        clipPaint.setColor(NORMAL_CLIP_COLOR);
        clipPaint.setStyle(Paint.Style.FILL);
        clipPaint.setAntiAlias(true);

        notePaint = new Paint();
        notePaint.setColor(NOTE_COLOR);
        notePaint.setStyle(Paint.Style.FILL);
        notePaint.setAntiAlias(true);

        clipBorderPaint = new Paint();
        clipBorderPaint.setColor(CLIP_BORDER_COLOR);
        clipBorderPaint.setStyle(Paint.Style.STROKE);
        clipBorderPaint.setAntiAlias(true);

        snapLinePaint = new Paint();
        snapLinePaint.setColor(SNAP_LINE_COLOR);
        snapLinePaint.setStyle(Paint.Style.STROKE);
        snapLinePaint.setStrokeWidth(SNAP_LINE_WIDTH);
        snapLinePaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        textPaint.setStrokeWidth(5);

        timelineTextPaint = new Paint();
        timelineTextPaint.setColor(Color.BLACK);
        timelineTextPaint.setTextSize(32);
        timelineTextPaint.setTextAlign(Paint.Align.CENTER);
        timelineTextPaint.setAntiAlias(true);
    }

    // 监听器设置
    public void setOnClipSelectedListener(OnClipSelectedListener listener) {
        this.clipSelectedListener = listener;
    }

    public void setOnNoteSelectedListener(OnNoteSelectedListener listener) {
        this.noteSelectedListener = listener;
    }

    // 获取选中元素
    public Chart.Clip getSelectedClip() {
        return selectedClips.isEmpty() ? null : selectedClips.get(0);
    }

    public Chart.Note getSelectedNote() {
        return selectedNotes.isEmpty() ? null : selectedNotes.get(0);
    }

//    public void removeNote(Note note) {
//        notes.remove(note);
//        if (selectedNotes.contains(note)) {
//            selectedNotes.remove(note);
//            noteSelectedListener.onNoteDeselected();
//        }
//        invalidate();
//    }

    // 坐标转换工具
    private float getPixelsPerEighthBeat() {
        return DEFAULT_PIXELS_PER_EIGHTH_BEAT * scaleFactor;
    }

    private float beatToY(float beat) {
        return getHeight() - (beat * getPixelsPerEighthBeat() + scrollY);
    }

    private float yToBeat(float y) {
        return (getHeight() - y - scrollY) / getPixelsPerEighthBeat();
    }

    // 吸附逻辑（Y轴吸附到1/8拍，X轴吸附到指定间隔）
    private float snapToBeat(float beat) {
        return Math.round(beat) * 1.0f;
    }

    private float snapToX(float x) {
        return Math.round(x / X_SNAP_INTERVAL) * X_SNAP_INTERVAL;
    }

    // 测量布局
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = TIMELINE_WIDTH + NOTE_TRACK_WIDTH + TRACK_SPACING + TRACK_COUNT * TRACK_WIDTH;
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    // 绘制逻辑
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        // 绘制轨道分隔线
        canvas.drawLine(TIMELINE_WIDTH, 0, TIMELINE_WIDTH, height, trackLinePaint); // 时间轴线
        float noteTrackRight = TIMELINE_WIDTH + NOTE_TRACK_WIDTH;
        canvas.drawLine(noteTrackRight, 0, noteTrackRight, height, trackLinePaint); // Note轨右边界
        float spacingRight = noteTrackRight + TRACK_SPACING;
        canvas.drawLine(spacingRight, 0, spacingRight, height, trackLinePaint); // 间隔线
        for (int i = 0; i <= TRACK_COUNT; i++) {
            float x = spacingRight + i * TRACK_WIDTH;
            canvas.drawLine(x, 0, x, height, trackLinePaint); // 普通轨道分隔线
        }
        for (float i = 0; i <= NOTE_TRACK_WIDTH; i+= (float) NOTE_TRACK_WIDTH /10) {
            int x = Math.round(TIMELINE_WIDTH + i);
            canvas.drawLine(x, 0, x, height, trackLinePaint); // 普通轨道分隔线
        }

        // 绘制节拍线和标签
        float visibleStartBeat = yToBeat(height);
        float visibleEndBeat = yToBeat(0);
        float startBeat = Math.max(0, (float) Math.floor(visibleStartBeat / 8) * 8);
        float endBeat = (float) Math.ceil(visibleEndBeat / 8) * 8;

        for (float beat = startBeat; beat <= endBeat; beat += 1) {
            float y = beatToY(beat);
            Paint paint = (beat % 8 == 0) ? strongBeatLinePaint : beatLinePaint;
            canvas.drawLine(TIMELINE_WIDTH, y, width, y, paint);

            if (beat % 8 == 0) {
                String beatText = String.valueOf((int) (beat / 8));
                canvas.drawText(beatText, TIMELINE_WIDTH + NOTE_TRACK_WIDTH + TRACK_SPACING/2 , y-5, timelineTextPaint);
            }
        }

        // 绘制当前时间线
        float tsY = beatToY(ts);
        canvas.drawLine(TIMELINE_WIDTH, tsY, width, tsY, tsLinePaint);
        // 绘制编辑的轴
        int xsX;
        if (xs > 0) {
            xsX = (int) (spacingRight + (xs - 0.5) * TRACK_WIDTH);
        } else {
            xsX = (int) (TIMELINE_WIDTH + (-xs) * NOTE_TRACK_WIDTH/10);
        }
        canvas.drawLine(xsX, 0, xsX, height, tsLinePaint);

        // 绘制吸附线（Y轴和X轴）
//        if (tempSnapLineBeat != null) {
//            float snapY = beatToY(tempSnapLineBeat);
//            canvas.drawLine(TIMELINE_WIDTH, snapY, width, snapY, snapLinePaint);
//        }
//        if (tempSnapLineX != null) {
//            float snapX = TIMELINE_WIDTH + tempSnapLineX;
//            canvas.drawLine(snapX, 0, snapX, height, snapLinePaint);
//        }

        // 绘制片段（先未选中后选中）
        // 普通轨道片段
        for (Chart.Clip clip : line.movex) {if (!selectedClips.contains(clip)) drawClip(canvas, clip, false);}
        for (Chart.Clip clip : line.movey) {if (!selectedClips.contains(clip)) drawClip(canvas, clip, false);}
        for (Chart.Clip clip : line.speed) {if (!selectedClips.contains(clip)) drawClip(canvas, clip, false);}
        for (Chart.Clip clip : line.alpha) {if (!selectedClips.contains(clip)) drawClip(canvas, clip, false);}
        for (Chart.Clip clip : line.rotate) {if (!selectedClips.contains(clip)) drawClip(canvas, clip, false);}
        for (Chart.Note note : line.notes) {
            if (!selectedNotes.contains(note)) drawNote(canvas, note, false);
        }
        // 选中的片段
        for (Chart.Clip clip : selectedClips) {drawClip(canvas,clip, true);}
        for (Chart.Note note : selectedNotes) {drawNote(canvas, note, true);}

        // 重置临时吸附线
        tempSnapLineBeat = null;
        tempSnapLineX = null;
    }

    // 绘制普通片段
    private void drawClip(Canvas canvas, Chart.Clip clip, boolean isSelected) {
        int trackIndex = clip.track.ordinal();
        float trackOffset = TIMELINE_WIDTH + NOTE_TRACK_WIDTH + TRACK_SPACING;
        float left = trackOffset + trackIndex * TRACK_WIDTH + 20;
        float right = left + TRACK_WIDTH - 40;
        float startY = beatToY(clip.st);
        float endY = beatToY(clip.et);

        if (startY < 0 && endY < 0) return;
        if (startY > getHeight() && endY > getHeight()) return;

        clipPaint.setColor(isSelected ? SELECTED_CLIP_COLOR : NORMAL_CLIP_COLOR);
        canvas.drawRect(left, endY, right, startY, clipPaint);
        clipBorderPaint.setStrokeWidth(isSelected ? SELECTED_BORDER_WIDTH : NORMAL_BORDER_WIDTH);
        canvas.drawRect(left, endY, right, startY, clipBorderPaint);

        float x1 = (clip.sv - clip.minValue) / (clip.maxValue - clip.minValue) * (right - left) + left;
        float x2 = (clip.ev - clip.minValue) / (clip.maxValue - clip.minValue) * (right - left) + left;
        canvas.drawLine(x1, startY, x2, endY, textPaint);

        canvas.drawText(String.valueOf(clip.sv), (left + right) / 2, startY-5, textPaint);
        canvas.drawText(String.valueOf(clip.ev), (left + right) / 2, endY+35, textPaint);

    }

    // 绘制Note（支持X坐标）
    private void drawNote(Canvas canvas, Chart.Note note, boolean isSelected) {
        // Note轨内的X坐标计算（基于xPosition）
        float center = (note.posX + 675) / 1350 * NOTE_TRACK_WIDTH;
        float left = center-50; // 左内边距
        float right = center+50; // 右内边距（减去左右内边距总和）
        float startY = beatToY(note.time);
        float endY = beatToY(note.time + note.duration);

        if (note.duration == 0) {
            startY = beatToY(note.time) + 10;
            endY = beatToY(note.time) - 10;
        }

        if (startY < 0 && endY < 0) return;
        if (startY > getHeight() && endY > getHeight()) return;

        notePaint.setColor(isSelected ? SELECTED_CLIP_COLOR : NOTE_COLOR);
        canvas.drawRect(left, endY, right, startY, notePaint);
        clipBorderPaint.setStrokeWidth(isSelected ? SELECTED_BORDER_WIDTH : NORMAL_BORDER_WIDTH);
        canvas.drawRect(left, endY, right, startY, clipBorderPaint);
//
//        float textY = endY + (startY - endY) / 2 - ((textPaint.descent() + textPaint.ascent()) / 2);
//        canvas.drawText(note.name, (left + right) / 2, textY, textPaint);
    }

    // 碰撞检测
    private boolean isClipOverlapping(Chart.Track track, float startBeat, float duration, ArrayList<Chart.Clip> excludeClip) {
        float endBeat = startBeat + duration;
        for (Chart.Clip clip : line.getClips(track)) {
            if (excludeClip.contains(clip)) continue;
            if (!(endBeat <= clip.st || startBeat >= clip.et)) return true;
        }
        return false;
    }

    // Note碰撞检测（同时检查Y轴时间和X轴位置）
//    private boolean isNoteOverlapping(Note note, float newStartBeat, float newX, Note excludeNote) {
//        float newEndBeat = newStartBeat + note.duration;
//        float newRight = newX + note.width;
//
//        for (Note other : notes) {
//            if (other == excludeNote) continue;
//
//            // 时间轴（Y轴）重叠检查
//            float otherEndBeat = other.startBeat + other.duration;
//            boolean yOverlap = !(newEndBeat <= other.startBeat || newStartBeat >= otherEndBeat);
//
//            // X轴重叠检查
//            float otherRight = other.xPosition + other.width;
//            boolean xOverlap = !(newRight <= other.xPosition || newX >= otherRight);
//
//            if (yOverlap && xOverlap) return true; // 同时重叠才视为碰撞
//        }
//        return false;
//    }

    // 触摸事件处理（核心：Note的X/Y轴拖动）
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float currentY = event.getY();
        float currentX = event.getX();

        ts = snapToBeat(yToBeat(currentY));
        xs = screenX2Xs(currentX);

        // 多点触控缩放
        if (event.getPointerCount() == 2) {
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                initialDistance = getDistance(event);
                initialScaleFactor = scaleFactor;
                isScaling = true;
                return true;
            } else if (action == MotionEvent.ACTION_MOVE && isScaling) {
                float newDistance = getDistance(event);
                scaleFactor = initialScaleFactor * (newDistance / initialDistance);
                scaleFactor = Math.max(minScale, Math.min(scaleFactor, maxScale));
                invalidate();
                return true;
            } else if (action == MotionEvent.ACTION_POINTER_UP) {
                isScaling = false;
                return true;
            }
        }

        // 单点触控
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastY = currentY;
                lastX = currentX;
                initialY = currentY;
                initialX = currentX;
                tempSnapLineBeat = null;
                tempSnapLineX = null;

                // 优先检查Note点击
                Chart.Note clickedNote = findNoteAtPosition(currentX, currentY);
                if (clickedNote != null) {
                    selectNote(clickedNote);
                    isDraggingNote = true;
                    initialStartBeat = clickedNote.time;
                    initialXPosition = clickedNote.posX; // 记录初始X位置
                    return true;
                }

                // 检查普通片段点击
                Chart.Clip clickedClip = findClipAtPosition(currentX, currentY);
                if (clickedClip != null) {
                    selectClip(clickedClip);
                    isDraggingClip = true;
                    initialStartBeat = clickedClip.st;
                    initialTrack = clickedClip.track;
                    return true;
                } else {
                    deselectClip();
                    deselectNote();
                    isScrolling = true;
                    return true;
                }

            case MotionEvent.ACTION_MOVE:
                if (isDraggingNote) {
                    // 普通片段Y轴拖动（不变）
                    float deltaY = lastY - currentY;
                    float beatDelta = deltaY / getPixelsPerEighthBeat();
                    for (Chart.Note note : selectedNotes) {
                        float newStartBeat = note.time + beatDelta;
                        newStartBeat = Math.max(0, newStartBeat);
                        note.time = newStartBeat;
                        tempSnapLineBeat = snapToBeat(newStartBeat);
                        invalidate();
                    }
                    lastY = currentY;

                    // note的横向拖动
                    float deltaX = lastX - currentX;
                    float deltaPosX = deltaX / NOTE_TRACK_WIDTH * 1350;
                    for (Chart.Note note : selectedNotes) {
                        float newStartX = note.posX - deltaPosX;
                        newStartX = Math.max(-675, newStartX);
                        newStartX = Math.min(675, newStartX);
                        note.posX = newStartX;
                        tempSnapLineX = snapToBeat(newStartX);
                        invalidate();
                    }
                    lastX = currentX;
                    return true;

                } else if (isDraggingClip) {
                    // 普通片段Y轴拖动（不变）
                    float deltaY = lastY - currentY;
                    float beatDelta = deltaY / getPixelsPerEighthBeat();
                    boolean canMove = true;
                    for (Chart.Clip clip : selectedClips) {
                        float newStartBeat = clip.st + beatDelta;
                        newStartBeat = Math.max(0, newStartBeat);
                        if (isClipOverlapping(clip.track, snapToBeat(newStartBeat), clip.et-clip.st, selectedClips)) {
                            canMove = false;
                        }
                    }
                    if (canMove) {
                        for (Chart.Clip clip : selectedClips) {
                            float newStartBeat = clip.st + beatDelta;
                            newStartBeat = Math.max(0, newStartBeat);
                            clip.et = newStartBeat + (clip.et - clip.st);
                            clip.st = newStartBeat;
                            tempSnapLineBeat = snapToBeat(newStartBeat);
                            invalidate();
                        }
                    }

                    lastY = currentY;
                    return true;

                } else if (isScrolling) {
                    // 滚动逻辑
                    float deltaY = currentY - lastY;
                    scrollY -= deltaY;
                    scrollY = Math.min(scrollY, 0);
                    lastY = currentY;
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDraggingNote) {
                    // Note松开时执行最终吸附
                    for (Chart.Note note : selectedNotes) {
                        float snappedBeat = snapToBeat(note.time);
                        float snappedX = snapToX(note.posX);
                        note.time = snappedBeat;
                        note.posX = snappedX;
                    }
                    noteSelectedListener.onNoteSelected();
                } else if (isDraggingClip) {
                    // 普通片段吸附
                    for (Chart.Clip clip : selectedClips) {
                        clip.st = snapToBeat(clip.st);
                        clip.et = snapToBeat(clip.et);
                    }
                }
                isDraggingClip = false;
                isDraggingNote = false;
                isScrolling = false;
                isScaling = false;
                invalidate();
                break;
        }

        invalidate();
        return true;
    }

    // 辅助方法：计算两点距离（缩放用）
    private float getDistance(MotionEvent event) {
        float x1 = event.getX(0);
        float y1 = event.getY(0);
        float x2 = event.getX(1);
        float y2 = event.getY(1);
        return (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    // 查找Note（基于X和Y坐标）
    private Chart.Note findNoteAtPosition(float x, float y) {
        float center = (x - TIMELINE_WIDTH) / NOTE_TRACK_WIDTH * 1350 - 675;
        float left = center-75;
        float right = center+75;
        float beat = yToBeat(y);

        // 从后往前检查，顶层优先
        for (int i = line.notes.size() - 1; i >= 0; i--) {
            Chart.Note note = line.notes.get(i);
            // 检查Y轴范围
            boolean inYRange = beat >= note.time && beat <= note.time + note.duration;
            if (note.duration == 0) {
                inYRange = Math.abs(note.time - beat) / scaleFactor <= 1;
            }
            // 检查X轴范围
            boolean inXRange = left <= note.posX && note.posX <= right;
            if (inYRange && inXRange) return note;
        }
        return null;
    }

    // 查找普通片段（不变）
    private Chart.Clip findClipAtPosition(float x, float y) {
        float trackOffset = TIMELINE_WIDTH + NOTE_TRACK_WIDTH + TRACK_SPACING;
        if (x < trackOffset) return null;

        int trackIndex = (int) ((x - trackOffset) / TRACK_WIDTH);
        if (trackIndex < 0 || trackIndex >= TRACK_COUNT) return null;
        Chart.Track track = Chart.Track.getByIndex(trackIndex);
        ArrayList<Chart.Clip> clips = line.getClips(track);

        float beat = yToBeat(y);
        for (int i = clips.size() - 1; i >= 0; i--) {
            Chart.Clip clip = clips.get(i);
            if (clip.track == track && beat >= clip.st && beat <= clip.et) {
                return clip;
            }
        }
        return null;
    }

    // 选中/取消选中逻辑
    private void selectClip(Chart.Clip clip) {
        if (selectedClips.contains(clip)) return;
        deselectNote();
        selectedNotes.clear();
        selectedClips.add(clip);
        invalidate();
        clipSelectedListener.onClipSelected();
    }

    private void selectNote(Chart.Note note) {
        if (selectedNotes.contains(note)) return;
        deselectClip();
        selectedClips.clear();
        selectedNotes.add(note);
        invalidate();
        noteSelectedListener.onNoteSelected();
    }

    private void deselectClip() {
        if (!selectedClips.isEmpty()) {
            selectedClips.clear();
            invalidate();
            clipSelectedListener.onClipDeselected();
        }
    }

    private void deselectNote() {
        if (!selectedNotes.isEmpty()) {
            selectedNotes.clear();
            invalidate();
            noteSelectedListener.onNoteDeselected();
        }
    }

    protected int screenX2Xs(float x) {
        if (x >= TIMELINE_WIDTH + NOTE_TRACK_WIDTH + TRACK_SPACING) {
            return (int) ((x - (TIMELINE_WIDTH + NOTE_TRACK_WIDTH + TRACK_SPACING)) / TRACK_WIDTH) + 1;
        } else {
            return Math.max(Math.round((x - TIMELINE_WIDTH) / NOTE_TRACK_WIDTH * -10), -10);
        }
    }

    protected void onNew() {
        if (xs > 0) {
            float lastClipTime = 0;
            float lastClipValue = 0;
            Chart.Track track = Chart.Track.getByIndex(xs-1);
            for (Chart.Clip clip: line.getClips(track)) {
                if (lastClipTime < clip.et && clip.et < ts) {
                    lastClipTime = clip.et;
                    lastClipValue = clip.ev;
                }
            }
            if (lastClipTime < ts) {
                line.addClip(track, lastClipTime, ts, lastClipValue, lastClipValue);
            }
        } else {
            line.notes.add(new Chart.Note(0, ts, (float) ((-5 - xs) * 675) /5));
        }
    }


}