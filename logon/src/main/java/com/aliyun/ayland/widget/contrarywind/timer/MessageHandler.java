package com.aliyun.ayland.widget.contrarywind.timer;

import android.os.Handler;
import android.os.Message;

import com.aliyun.ayland.widget.contrarywind.view.WheelView;


/**
 * Handler 消息类
 *
 * @author 小嵩
 * date: 2017-12-23 23:20:44
 */
public final class MessageHandler extends Handler {
    public static final int WHAT_INVALIDATE_LOOP_VIEW = 1000;
    public static final int WHAT_SMOOTH_SCROLL = 2000;
    public static final int WHAT_ITEM_SELECTED = 3000;

    private final WheelView mWheelView;

    public MessageHandler(WheelView WheelView) {
        this.mWheelView = WheelView;
    }

    @Override
    public final void handleMessage(Message msg) {
        switch (msg.what) {
            case WHAT_INVALIDATE_LOOP_VIEW:
                mWheelView.invalidate();
                break;

            case WHAT_SMOOTH_SCROLL:
                mWheelView.smoothScroll(WheelView.ACTION.FLING);
                break;

            case WHAT_ITEM_SELECTED:
                mWheelView.onItemSelected();
                break;
        }
    }

}
