package com.example.cloudmusicdemo.core.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class GradientBackgroundView extends View {

    private Paint paint;
    private ValueAnimator animator;
    private float animatedValue = 0f;

    // 定义三种颜色
    private int[] colors1 = {0xFF1a1a2e, 0xFF16213e, 0xFF0f3460};
    private int[] colors2 = {0xFF0f3460, 0xFF16213e, 0xFF1a1a2e};
    private int[] colors3 = {0xFF1a1a2e, 0xFF0f3460, 0xFF16213e};

    public GradientBackgroundView(Context context) {
        this(context, null);
    }

    public GradientBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 创建动画，15秒一个周期
        animator = ValueAnimator.ofFloat(0f, 3f);
        animator.setDuration(15000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.addUpdateListener(animation -> {
            animatedValue = (float) animation.getAnimatedValue();
            invalidate(); // 重绘
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateGradient(w, h);
    }

    private void updateGradient(int width, int height) {
        float fraction = animatedValue % 1f;
        int currentIndex = (int) animatedValue % 3;

        int[] startColors, endColors;

        switch (currentIndex) {
            case 0:
                startColors = colors1;
                endColors = colors2;
                break;
            case 1:
                startColors = colors2;
                endColors = colors3;
                break;
            default:
                startColors = colors3;
                endColors = colors1;
                break;
        }

        // 插值计算当前颜色
        int[] currentColors = new int[3];
        for (int i = 0; i < 3; i++) {
            currentColors[i] = interpolateColor(startColors[i], endColors[i], fraction);
        }

        // 创建线性渐变
        LinearGradient gradient = new LinearGradient(
            0, 0, width, height,
            currentColors,
            null,
            Shader.TileMode.CLAMP
        );

        paint.setShader(gradient);
    }

    private int interpolateColor(int color1, int color2, float fraction) {
        int a = (int) (Color.alpha(color1) + (Color.alpha(color2) - Color.alpha(color1)) * fraction);
        int r = (int) (Color.red(color1) + (Color.red(color2) - Color.red(color1)) * fraction);
        int g = (int) (Color.green(color1) + (Color.green(color2) - Color.green(color1)) * fraction);
        int b = (int) (Color.blue(color1) + (Color.blue(color2) - Color.blue(color1)) * fraction);
        return Color.argb(a, r, g, b);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        updateGradient(getWidth(), getHeight());
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }

    public void startAnimation() {
        if (animator != null && !animator.isStarted()) {
            animator.start();
        }
    }

    public void stopAnimation() {
        if (animator != null) {
            animator.cancel();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }
}