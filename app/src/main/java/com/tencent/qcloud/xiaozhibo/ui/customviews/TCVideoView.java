package com.tencent.qcloud.xiaozhibo.ui.customviews;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.tencent.rtmp.ui.TXCloudVideoView;

/**
 * TCVideoView在TXCloudVideoView上面叠加一个logview,用于显示sdk 内部状态及事件
 */
public class TCVideoView extends TXCloudVideoView {

    private TCLogView           mTXLogView;

    public TCVideoView(Context context) {
		this(context,null);
	}

	public TCVideoView(Context context, AttributeSet attrs) {
		super(context,attrs);
        mTXLogView = new TCLogView(context);
        addView(mTXLogView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        mTXLogView.setVisibility(GONE);
	}

    //--------------------------下面的代码用于在视频浮层显示Log和事件------------------------
    public void disableLog(boolean disable) {
        mTXLogView.disableLog(disable);
        if (disable) {
            mTXLogView.setVisibility(GONE);
        } else {
            mTXLogView.setVisibility(VISIBLE);
        }
    }

    public void clearLog() {
        mTXLogView.clearLog();
    }

    public void setLogText(Bundle status, Bundle event, int eventId) {
        mTXLogView.setLogText(status, event, eventId);
    }

}
