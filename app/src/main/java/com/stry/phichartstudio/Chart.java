package com.stry.phichartstudio;

import android.annotation.SuppressLint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Chart {

    ArrayList<Line> lines;
    float bpm;

    Chart(float bpm, int numOfLines) {
        this.bpm = bpm;
        this.lines = new ArrayList<>();
        for (int i=0; i<numOfLines; i++) {this.lines.add(new Line());}
    }

    public static class Note {
        public int type;
        public int isFake;
        public int isAbove;
        public float posX;
        public float time;
        public float speed;
        public float alpha;
        public float duration;

        Note(int type, float time, float posX) {
            this.type = type;
            this.isFake = 0;
            this.isAbove = 1;
            this.posX = posX;
            this.time = time;
            this.speed = 1.0f;
            this.alpha = 255;
            this.duration = 0;
        }
    }

    public static class Clip {
        public float st;  // start Time 起始时间
        public float et;  // end Time 结束时间
        public float sv;  // start value 起始值
        public float ev;  // end value 结束值
        public int easing;
        public Track track;

        public float minValue;  // 供参考的最小值
        public float maxValue;  // 供参考的最大值

        Clip(float st, float et, float sv, float ev, Track track) {
            this.st = st;
            this.et = et;
            this.sv = sv;
            this.ev = ev;
            this.easing = 0;
            this.track = track;
        }
    }

    public enum Track {
        MOVEX, MOVEY, ROTATE, ALPHA, SPEED;

        public static Track getByIndex(int index) {
            switch (index) {
                case 0: return MOVEX;
                case 1: return MOVEY;
                case 2: return ROTATE;
                case 3: return ALPHA;
                case 4: return SPEED;
                default: return null;
            }
        }
    }

    public static class Line {

        ArrayList<Note> notes = new ArrayList<>();
        ArrayList<Clip> movex = new ArrayList<>();
        ArrayList<Clip> movey = new ArrayList<>();
        ArrayList<Clip> speed = new ArrayList<>();
        ArrayList<Clip> alpha = new ArrayList<>();
        ArrayList<Clip> rotate = new ArrayList<>();

        public void addNote(Note note) {
            notes.add(note);
        }
        public void removeNote(Note note) {
            notes.remove(note);
        }
        public ArrayList<Clip> getClips(Track track) {
            switch (track) {
                case MOVEX: return this.movex;
                case MOVEY: return this.movey;
                case ALPHA: return this.alpha;
                case SPEED: return this.speed;
                case ROTATE: return this.rotate;
            }
            // 不可能触发的
            return null;
        }

        public float getValue(Track track, float t){
            ArrayList<Clip> clips = getClips(track);
            return Chart.getValueByTime(clips, t);
        }

        public void addClip(Track track, float st, float et, float sv, float ev) {
            float minValue;
            float maxValue;
            if (track == Track.MOVEX) {
                minValue = -675;
                maxValue = 675;
            } else if (track == Track.MOVEY) {
                minValue = -450;
                maxValue = 450;
            } else if (track == Track.SPEED) {
                minValue = 0;
                maxValue = 20;
            } else if (track == Track.ALPHA) {
                minValue = 0;
                maxValue = 255;
            } else if (track == Track.ROTATE) {
                minValue = -360;
                maxValue = 360;
            } else {
                minValue = 0;
                maxValue = 0;
            }

            Clip newClip = new Clip(st, et, sv, ev, track);
            this.getClips(track).add(newClip);
            newClip.minValue = minValue;
            newClip.maxValue = maxValue;
        }
    }

    // 按起始时间升序排序（二分法前提）
    public static void sortClipsByStartTime(ArrayList<Clip> clips) {
        clips.sort(new Comparator<Clip>() {
            @Override
            public int compare(Clip clip1, Clip clip2) {
                return Float.compare(clip1.st, clip2.st);
            }
        });
    }

    // 二分法查找并返回对应值（处理不连续场景）
    public static float getValueByTime(ArrayList<Clip> clips, float t) {
        if (clips == null || clips.isEmpty()) {
            return 0f; // 空列表返回0
        }
        sortClipsByStartTime(clips);

        int left = 0;
        int right = clips.size() - 1;
        Clip targetClip = null;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            Clip midClip = clips.get(mid);
            if (t < midClip.st) {
                right = mid - 1;
            } else if (t > midClip.et) {
                left = mid + 1;
            } else {
                targetClip = midClip;
                break;
            }
        }

        // 情况1：找到包含t的片段 → 线性插值
        if (targetClip != null) {
            float ratio = (t - targetClip.st) / (targetClip.et - targetClip.st);
            return targetClip.sv + ratio * (targetClip.ev - targetClip.sv);
        }

        // 情况2：未找到包含t的片段（t在片段之间或左侧）
        // left是t应该插入的位置（所有0~left-1的片段都在t左侧）
        if (left == 0) {
            // t在第一个片段左侧 → 返回0
            return 0f;
        } else {
            // t在两个片段之间 → 取上一个片段的ev
            return clips.get(left - 1).ev;
        }
    }

    public static Chart newDefaultChart(int bpm) {
        Chart chart =  new Chart(bpm, 24);
        for (Line line: chart.lines) {
            try {
                line.addClip(Track.MOVEX, 0, 16, -675, 675);
                line.addClip(Track.MOVEY, 0, 16, -450, 450);
                line.addClip(Track.ROTATE, 0, 16, -360, 360);
                line.addClip(Track.ALPHA, 0, 16, 0, 255);
                line.addClip(Track.SPEED, 0, 16, 0, 20);
            } catch (Exception e) {

            }
        }
        return chart;
    }

    public static float beatToTime(String input) {
        /*
         * 节拍转化为八分音符计数的时间
         * 输入形如“1 2 3”的字符串时，返回8*(1+2/3)的计算结果，
         * 输入形如“1.5”的字符串时，返回1.5*8的结果
         * */
        String trimInput = input.trim();

        // 分割字符串（按空格分割）
        String[] parts = trimInput.split("\\s+");

        // 处理输入格式
        if (parts.length == 1) {
            // 单个数：返回 数值 * 8
            try {
                double num = Double.parseDouble(parts[0]);
                return (float) num * 8;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("输入格式不合法，请输入数字或空格分隔的三个数字");
            }
        } else if (parts.length == 3) {
            // 三个数：返回 8*(a + b/c)
            try {
                double a = Double.parseDouble(parts[0]);
                double b = Double.parseDouble(parts[1]);
                double c = Double.parseDouble(parts[2]);

                // 避免除数为0
                if (c == 0) {
                    throw new ArithmeticException("除数不能为0");
                }

                return (float) (8 * (a + b / c));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("输入格式不合法，三个部分必须都是数字");
            }
        } else {
            // 输入格式错误（既不是1个也不是3个部分）
            throw new IllegalArgumentException("输入格式不合法，请输入单个数字或三个空格分隔的数字");
        }
    }

    @SuppressLint("DefaultLocale")
    public static String timeToBeat(double result) {
        // 先归一化：result = 8 * x → x = result / 8
        double x = result / 8.0;

        // 尝试分解x为 a + b/c（a、b为非负整数，c为1~8的正整数）
        // 取整部分作为a的候选（允许a为0）
        int a = (int) Math.floor(x);
        double remainder = x - a; // 剩余部分：b/c = remainder

        // 遍历可能的分母c（1~8）
        for (int c = 1; c <= 8; c++) {
            // 计算b = remainder * c（允许微小误差，避免浮点数精度问题）
            double bDouble = remainder * c;
            int b = (int) Math.round(bDouble);

            // 验证：b必须为非负整数，且b/c ≈ remainder（误差在1e-6内）
            if (b >= 0 && Math.abs(bDouble - b) < 1e-6) {
                // 找到合法分解，返回"a b c"
                return String.format("%d %d %d", a, b, c);
            }
        }

        // 无法分解为符合条件的a+b/c，返回单值形式
        return String.valueOf(x);
    }
}
