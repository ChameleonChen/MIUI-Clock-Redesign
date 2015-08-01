package com.chameleonchen.miui_clock.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.chameleonchen.miui_clock.R;

import java.util.logging.LogRecord;

/**
 * Created on 2015/7/30.
 * @author ChameleonChen
 */
public class ElasticAndAlignBottomLayout extends RelativeLayout{

    /** the mix height value */
    private float mixHeight;
    /** the max height value */
    private float maxHeight;
    /** the mix distance to the top.
     *  this view is elastic in vertical direction, the essence of elastic
     *  is changing the value of this view's distance to top from mixDistance to maxDistance.
     */
    private int mixDistanceToTop;
    /** the max distance to the top. */
    private int maxDistanceToTop;
    /** the left position. It's a final value. */
    private int originalLeft;
    /** the right position. it's a final value. */
    private int originalRight;
    /** the top position. it's a final value. */
    private int originalTop;
    /** the bottom position. it's a final value. */
    private int originalBottom;

    public ElasticAndAlignBottomLayout(Context context) {
        super(context);
    }

    public ElasticAndAlignBottomLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ElasticAndAlignBottomLayout);
        mixHeight = a.getDimension(R.styleable.ElasticAndAlignBottomLayout_mixHeight, -1);
        maxHeight = a.getDimension(R.styleable.ElasticAndAlignBottomLayout_maxHeight, -1);
        if (mixHeight < 0 || maxHeight < 0) {
            StringBuilder errorInfo = new StringBuilder();
            errorInfo.append("mixHeight(").append(mixHeight).append(") and maxHeight(").append(maxHeight).append(") must larger than zero,")
                    .append("you must set up mixHeight and maxHeight for ElasticAndAlignBottomLayout");
            throw new IllegalStateException(errorInfo.toString());
        }
        a.recycle();

    }

    public ElasticAndAlignBottomLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private boolean isInitOriginalPositionData = false;
    private void initOriginalPositionData() {
        if (isInitOriginalPositionData)
            return ;
        originalTop = getTop();
        originalBottom = getBottom();
        originalLeft = getLeft();
        originalRight = getRight();

        maxDistanceToTop = originalTop;
        int height = getHeight();
        int newHeight = (int) (height * (maxHeight / mixHeight));
        mixDistanceToTop = originalBottom - newHeight;

        isInitOriginalPositionData = true;
    }

    private boolean isTouchDown = false;
    private float lastY = 0;
    private float dY;
    private float offset = 0;
    private int firstTop;

    private void resetValue() {
        if (!isTouchDown)
            return ;
        // reset values here.
        offset = 0;
        lastY = 0;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // in this method, this only accept ACTION_DOWN even.
        // it must decide whether to intercept the even.

        final int action = MotionEventCompat.getActionMasked(ev);
        if (action == MotionEvent.ACTION_DOWN) {
            isTouchDown = true;
            startMotion(ev);
        }

        // if the elastic view group move to top limit, did not intercept the even.
        if (firstTop == mixDistanceToTop) {
            ElasticListView child = getChildView();
            if (child != null) {
                if (child.isListViewAtTop()) {
                    if (!isTouchDown) {
                        isTouchDown = true;
                        startMotion(ev);
                    } else {
                        dealMotionEven(ev);
                    }
                }
            }
            return false;
        }
        else
            // return true to intercept the even, and did not pass on to child view.
            // The even will pass on to it's own onTouchEven() method.
            // see more details: http://developer.android.com/intl/zh-cn/reference/android/view/ViewGroup.html#onInterceptTouchEvent(android.view.MotionEvent)
            return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // If it's own onInterceptTouchEven() return true,
        // the even will pass on here.
        // If the even pass on here, means the even will not pass on to child view

        dealMotionEven(ev);

        // it must return true.
        return true;
    }

    private void startMotion(MotionEvent ev) {
        initOriginalPositionData();
        lastY = ev.getRawY();
        firstTop = getTop();
    }

    private void dealMotionEven(MotionEvent ev) {

        final int action = MotionEventCompat.getActionMasked(ev);

        if (action == MotionEvent.ACTION_UP) {
            int temp = (int) (firstTop + offset);
            int length = maxDistanceToTop - mixDistanceToTop;
            if (temp < mixDistanceToTop || temp < mixDistanceToTop + length/2) {
//                layout(originalLeft, mixDistanceToTop, originalRight, originalBottom);
                movingLayoutSlowly(originalLeft, originalRight, originalBottom, temp, mixDistanceToTop);
            } else if (temp > maxDistanceToTop || temp > maxDistanceToTop - length/2) {
//                layout(originalLeft, maxDistanceToTop, originalRight, originalBottom);
                movingLayoutSlowly(originalLeft, originalRight, originalBottom, temp, maxDistanceToTop);
            }

            resetValue();
            isTouchDown = false;
        }
        else if (action == MotionEvent.ACTION_MOVE) {
            dY = ev.getRawY() - lastY;
            int temp = (int) (firstTop + offset + dY);
            if (temp >= mixDistanceToTop && temp <= maxDistanceToTop) {
                offset = offset + dY;
                layout(originalLeft, temp, originalRight, originalBottom);
            } else if (temp < mixDistanceToTop) {
            }

            lastY = ev.getRawY();
        }
    }


    private Handler handler = new Handler();
    private MovingLayoutSlowlyRunnable mMovingLayoutSlowlyRunnable;
    private void movingLayoutSlowly(int originalLeft, int originalRight, int originalBottom, int fromTop, int toTop) {
        mMovingLayoutSlowlyRunnable = new MovingLayoutSlowlyRunnable(handler, 5, 20, originalLeft, originalRight, originalBottom, fromTop, toTop);
        handler.postDelayed(mMovingLayoutSlowlyRunnable, 200);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        ElasticListView childView = getChildView();
        childView.layout(0, 0, r - l, b - t);   // make view fill this view group.
    }

    private ElasticListView mElasticListView;

    private ElasticListView getChildView() {
        final int count = getChildCount();
        if (count == 0)
            return null;
        else if (count == 1) {
            // if this view group has a view, resize the view.
            // the view must fill this view group.

            mElasticListView = (ElasticListView) getChildAt(0);
            if (mElasticListView.getVisibility() != GONE) {
                return mElasticListView;
            }
            else
                return null;
        }
        else {
            throw new IllegalStateException("The ElasticAndAlignBottomLayout must only has zero or one view");
        }
    }

    class MovingLayoutSlowlyRunnable implements Runnable {
        private int originalLeft, originalRight, originalBottom, fromTop, toTop;
        private int unitTime;
        private int times;
        private Handler handler;
        private int unitDistance;
        private int i = 1;
        public MovingLayoutSlowlyRunnable(final Handler handler, int unitTime, int times,
                                          int originalLeft, int originalRight, int originalBottom, int fromTop, int toTop) {
            this.originalLeft = originalLeft;
            this.originalBottom = originalBottom;
            this.originalRight =originalRight;
            this.fromTop = fromTop;
            this.toTop = toTop;
            this.unitTime = unitTime;
            this.times = times;
            this.handler = handler;

            unitDistance = Math.abs(fromTop - toTop) / times;
        }
        @Override
        public void run() {
            if (i <= times) {
                if (fromTop < toTop)
                    ElasticAndAlignBottomLayout.this.layout(originalLeft, fromTop + i * unitDistance, originalRight, originalBottom);
                else
                    ElasticAndAlignBottomLayout.this.layout(originalLeft, fromTop - i * unitDistance, originalRight, originalBottom);

                i++;
                handler.postDelayed(this, unitTime);
            }
        }
    }
}
