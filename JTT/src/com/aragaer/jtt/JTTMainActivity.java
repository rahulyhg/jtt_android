package com.aragaer.jtt;

import android.app.ActivityGroup;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

public class JTTMainActivity extends ActivityGroup {
    private static final int btn_ids[] = { R.id.clockbtn, R.id.alarmbtn,
            R.id.settingsbtn };

    protected IJTTService api;
    protected Messenger srv;
    private ServiceConnection conn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("client", "Service connection established");
            api = IJTTService.Stub.asInterface(service);
            mHandler.postDelayed(mUpdateUITimerTask, 1000);
            srv = new Messenger(service);
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.i("client", "Service connection closed");
            srv = null;
        }
    };

    private final Runnable mUpdateUITimerTask = new Runnable() {
        public void run() {
            try {
                clock.setJTTHour(api.getHour());
            } catch (RemoteException e) {
                Log.d("jtt client", "service killed");
            }
            mHandler.postDelayed(mUpdateUITimerTask, 60 * 1000L);
        }
    };
    private final Handler mHandler = new Handler();

    private JTTClockView clock;
    private JTTPager pager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent service = new Intent(JTTService.class.getName());
        startService(service);

        setContentView(R.layout.main);

        Button tabs[] = new Button[btn_ids.length];
        for (int i = 0; i < btn_ids.length; i++)
            tabs[i] = (Button) findViewById(btn_ids[i]);

        clock = (JTTClockView) findViewById(R.id.hour);
        pager = (JTTPager) findViewById(R.id.tabcontent);
        if (savedInstanceState != null)
            pager.mCurrentScreen = savedInstanceState.getInt("Screen");
        pager.setTabs(tabs);

        final Window sw = getLocalActivityManager().startActivity("settings",
                new Intent(this, JTTSettingsActivity.class));
        pager.addView(sw.getDecorView());

        bindService(service, conn, 0);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("Screen", pager.mCurrentScreen);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            unbindService(conn);
        } catch (Throwable t) {
            Log.w("jtt client", "Failed to unbind from the service", t);
        }

        mHandler.removeCallbacks(mUpdateUITimerTask);
        Log.i("jtt client", "Activity destroyed");
    }

    public void onToggle(View view) {
        pager.btnToggle((Button) view);
    }

    static public class JTTPager extends ViewGroup {
        private static final int SNAP_VELOCITY = 1000;

        private boolean mFirstLayout = true;
        private VelocityTracker mVelocityTracker;
        private int mMaximumVelocity;
        private int mTouchSlop;

        protected int mCurrentScreen;

        private float mLastMotionX;
        // private float mLastMotionY;
        private int mScrollX;
        // private int mScrollY;

        private final static int TOUCH_STATE_REST = 0;
        private final static int TOUCH_STATE_SCROLLING = 1;
        private int mTouchState = TOUCH_STATE_REST;

        private Button[] tabs;

        public JTTPager(Context ctx, AttributeSet attrs) {
            super(ctx, attrs);

            setHapticFeedbackEnabled(false);

            final ViewConfiguration cfg = ViewConfiguration.get(getContext());
            mMaximumVelocity = cfg.getScaledMaximumFlingVelocity();
            mTouchSlop = cfg.getScaledTouchSlop();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            final int width = MeasureSpec.getSize(widthMeasureSpec);

            // The children are given the same width and height as the workspace
            final int count = getChildCount();
            for (int i = 0; i < count; i++)
                getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);

            if (width > 0 && mFirstLayout) {
                setHorizontalScrollBarEnabled(false);
                scrollTo(mCurrentScreen * width, 0);
                mFirstLayout = false;
            }
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right,
                int bottom) {
            int childLeft = 0;

            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                final int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, 0, childLeft + childWidth,
                        child.getMeasuredHeight());
                childLeft += childWidth;
            }
        }

        protected void setTabs(Button[] newTabs) {
            tabs = newTabs;
            tabs[mCurrentScreen].setSelected(true);
        }

        void show() {
            setVisibility(VISIBLE);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            final int action = ev.getAction();
            if (action == MotionEvent.ACTION_MOVE
                    && mTouchState != TOUCH_STATE_REST)
                return true;

            final float x = ev.getX();
            // final float y = ev.getY();
            switch (action) {
            case MotionEvent.ACTION_MOVE:
                final int xDiff = (int) Math.abs(x - mLastMotionX);
                // final int yDiff = (int) Math.abs(y - mLastMotionY);

                final int touchSlop = mTouchSlop;
                final boolean xMoved = xDiff > touchSlop;
                // final boolean yMoved = yDiff > touchSlop;

                if (xMoved)
                    mTouchState = TOUCH_STATE_SCROLLING;
                break;

            case MotionEvent.ACTION_DOWN:
                // Remember location of down touch
                mLastMotionX = x;
                // mLastMotionY = y;
                mTouchState = TOUCH_STATE_REST;
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // Release the drag
                mTouchState = TOUCH_STATE_REST;
                break;
            }

            /*
             * The only time we want to intercept motion events is if we are in
             * the drag mode.
             */
            return mTouchState != TOUCH_STATE_REST;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (mVelocityTracker == null)
                mVelocityTracker = VelocityTracker.obtain();
            mVelocityTracker.addMovement(ev);

            final int action = ev.getAction();
            final float x = ev.getX();

            switch (action) {
            case MotionEvent.ACTION_DOWN:
                // Remember where the motion event started
                mLastMotionX = x;
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = (int) (mLastMotionX - x);
                mTouchState = TOUCH_STATE_SCROLLING;
                // Scroll to follow the motion event
                mLastMotionX = x;

                final int maxWidth = (getChildCount() - 1) * getWidth();

                if (mScrollX + deltaX < 0)
                    deltaX = -mScrollX;
                else if (mScrollX + deltaX > maxWidth)
                    deltaX = maxWidth - mScrollX;

                scrollBy(deltaX, 0);
                break;
            case MotionEvent.ACTION_UP:
                if (mTouchState == TOUCH_STATE_SCROLLING) {
                    final int bump = getWidth() / 2 + 1;
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000,
                            mMaximumVelocity);
                    int velocityX = (int) velocityTracker.getXVelocity();

                    if (velocityX > SNAP_VELOCITY && mCurrentScreen > 0)
                        // Fling hard enough to move left
                        scrollBy(-bump, 0);
                    else if (velocityX < -SNAP_VELOCITY
                            && mCurrentScreen < getChildCount() - 1)
                        // Fling hard enough to move right
                        scrollBy(bump, 0);

                    snapToScreen(mCurrentScreen);

                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                mTouchState = TOUCH_STATE_REST;
                break;
            case MotionEvent.ACTION_CANCEL:
                mTouchState = TOUCH_STATE_REST;
            }

            return true;
        }

        @Override
        public void scrollBy(int scrollX, int scrollY) {
            mScrollX += scrollX;
            // mScrollY += scrollY;
            super.scrollBy(scrollX, scrollY);
            selectDestination();
        }

        @Override
        public void scrollTo(int scrollX, int scrollY) {
            mScrollX = scrollX;
            // mScrollY = scrollY;
            super.scrollTo(scrollX, scrollY);
            selectDestination();
        }

        private void selectDestination() {
            final int w = getWidth();
            if (w == 0)
                return;
            final int whichScreen = (int) (mScrollX + w / 2) / w;

            selectScreen(whichScreen);
        }

        private void selectScreen(int whichScreen) {
            tabs[mCurrentScreen].setSelected(false);
            mCurrentScreen = whichScreen;
            tabs[mCurrentScreen].setSelected(true);
        }

        public void snapToScreen(int whichScreen) {
            final int x = getWidth() * whichScreen;
            selectScreen(whichScreen);
            super.scrollTo(x, 0);
            mScrollX = x;
        }

        protected void btnToggle(Button btn) {
            if (btn == tabs[mCurrentScreen])
                return;
            for (int i = 0; i < tabs.length; i++)
                if (tabs[i] == btn) {
                    snapToScreen(i);
                    return;
                }
        }
    }
}
