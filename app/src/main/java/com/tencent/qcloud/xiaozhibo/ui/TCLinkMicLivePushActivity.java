package com.tencent.qcloud.xiaozhibo.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.tencent.qcloud.xiaozhibo.R;
import com.tencent.qcloud.xiaozhibo.base.TCConstants;
import com.tencent.qcloud.xiaozhibo.logic.TCLinkMicMgr;
import com.tencent.qcloud.xiaozhibo.logic.TCPlayerMgr;
import com.tencent.rtmp.ITXLivePlayListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayConfig;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by dennyfeng on 16/11/16.
 *
 *
 * 连麦的接受端，主播端
 */
public class TCLinkMicLivePushActivity extends TCLivePublisherActivity implements TCLinkMicMgr.TCLinkMicListener{

    private static final String TAG = TCLinkMicLivePushActivity.class.getName();

    private static final int  MAX_LINKMIC_MEMBER_SUPPORT = 1;
    private class TCPlayItem {
        public boolean              mPending = false;
        public String               mUserID = "";
        public String               mPlayUrl = "";
        public TXCloudVideoView     mVideoView;
        public ImageView            mLinkMicLoading;
        public FrameLayout          mLinkMicLoadingBg;
        public Button               mBtnKickout;

        public TXLivePlayer         mTXLivePlayer;
        public TXLivePlayConfig     mTXLivePlayConfig = new TXLivePlayConfig();

        public void empty() {
            mPending = false;
            mUserID = "";
            mPlayUrl = "";
        }
    }

    class TCLinkMicTimeoutRunnable implements Runnable {
        private String strUserID = "";

        public void setUserID(String userID) {
            strUserID = userID;
        }

        @Override
        public void run() {
            TCPlayItem item = getPlayItemByUserID(strUserID);
            if (item != null && item.mPending == true) {
                if (item.mPlayUrl != null) {
                    item.mTXLivePlayer.stopPlay(true);
                }
                mTCLinkMicMgr.kickOutLinkMicMember(strUserID);
                stopLoading(item, false);
                item.empty();
                item.mBtnKickout.setVisibility(View.INVISIBLE);

                mMapLinkMicMember.remove(strUserID);
//                if (mMapLinkMicMember.size() == 0) {
//                    //如果没有人连麦了，关闭AEC
//                    enablePublishAEC(false);
//                }

                Toast.makeText(getApplicationContext(), "连麦超时", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String                      mSessionID;
    private boolean                     mHasPendingRequest = false;
    //连麦单元信息的集合，示例容量是1个
    private Vector<TCPlayItem>          mVecPlayItems = new Vector<>();
    private TCLinkMicTimeoutRunnable    mLinkMicTimeOutRunnable = new TCLinkMicTimeoutRunnable();
    //连麦者id集合
    private Map<String, String>         mMapLinkMicMember = new HashMap<>();

    private TCLinkMicMgr                mTCLinkMicMgr;



    @Override
    protected void initView() {
        super.initView();

        mTCLinkMicMgr = TCLinkMicMgr.getInstance();
        mTCLinkMicMgr.setLinkMicListener(this);

        if (TCLinkMicMgr.supportLinkMic() == false) {
            return;
        }

        mSessionID = getLinkMicSessionID();

        TCPlayItem playItem1 = new TCPlayItem();
        playItem1.mVideoView = (TXCloudVideoView) findViewById(R.id.play_video_view1);
        playItem1.mVideoView.disableLog(!mShowLog);
        playItem1.mLinkMicLoading = (ImageView) findViewById(R.id.linkmic_loading);
        playItem1.mLinkMicLoadingBg = (FrameLayout) findViewById(R.id.linkmic_loading_bg);
        playItem1.mBtnKickout = (Button) findViewById(R.id.btn_kick_out1);
        playItem1.mTXLivePlayer = new TXLivePlayer(this);
        playItem1.mTXLivePlayer.setPlayListener(new TXLivePlayListener(playItem1));
        playItem1.mTXLivePlayer.setPlayerView(playItem1.mVideoView);
        playItem1.mTXLivePlayer.enableHardwareDecode(true);

        playItem1.mTXLivePlayConfig = new TXLivePlayConfig();
        playItem1.mTXLivePlayConfig.enableAEC(true);
        playItem1.mTXLivePlayConfig.setAutoAdjustCacheTime(true);
        playItem1.mTXLivePlayConfig.setMinAutoAdjustCacheTime(0.2f);
        playItem1.mTXLivePlayConfig.setMaxAutoAdjustCacheTime(0.2f);
        playItem1.mTXLivePlayer.setConfig(playItem1.mTXLivePlayConfig);

        mVecPlayItems.add(playItem1);
    }

    @Override
    protected void onResume() {
        super.onResume();

        for (int i = 0; i < mVecPlayItems.size(); ++i) {
            TXCloudVideoView videoView = mVecPlayItems.get(i).mVideoView;
            if (videoView != null) {
                videoView.onResume();
            }
        }

        for (TCPlayItem item: mVecPlayItems) {
            if (!TextUtils.isEmpty(item.mPlayUrl) && !TextUtils.isEmpty(item.mUserID)) {
                startLoading(item);
                item.mTXLivePlayer.startPlay(item.mPlayUrl, TXLivePlayer.PLAY_TYPE_LIVE_RTMP_ACC);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        for (int i = 0; i < mVecPlayItems.size(); ++i) {
            TXCloudVideoView videoView = mVecPlayItems.get(i).mVideoView;
            if (videoView != null) {
                videoView.onPause();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        for (TCPlayItem item: mVecPlayItems) {
            if (!TextUtils.isEmpty(item.mPlayUrl)) {
                stopLoading(item, false);
                item.mTXLivePlayer.stopPlay(true);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (int i = 0; i < mVecPlayItems.size(); ++i) {
            TXCloudVideoView videoView = mVecPlayItems.get(i).mVideoView;
            if (videoView != null) {
                videoView.onDestroy();
            }
        }

        mVecPlayItems.clear();

        if (mTCLinkMicMgr != null) {
            mTCLinkMicMgr.setLinkMicListener(null);
            mTCLinkMicMgr = null;
        }
    }

    @Override
    protected void startPublish() {
        //开启连麦，设置连麦参数
        if (!mPushUrl.contains("&mix=layer")) {
            mPushUrl += String.format("&mix=layer:b;t_id:1;session_id:%s", mSessionID);
        }

        mTXPushConfig.enableAEC(true);
        mTXPushConfig.setAutoAdjustBitrate(true);
        mTXPushConfig.setAutoAdjustStrategy(TXLiveConstants.AUTO_ADJUST_BITRATE_STRATEGY_2);
        if (Build.VERSION.SDK_INT < 18) {
            mTXPushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_360_640);
            mTXPushConfig.setMinVideoBitrate(400);
            mTXPushConfig.setMaxVideoBitrate(1000);
            mTXPushConfig.setHardwareAcceleration(false);
        } else {
            mTXPushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_540_960);
            mTXPushConfig.setMinVideoBitrate(800);
            mTXPushConfig.setMaxVideoBitrate(1200);
            mTXPushConfig.setHardwareAcceleration(true);
        }
        super.startPublish();
    }

    @Override
    protected void stopPublish() {
        super.stopPublish();
        for (TCPlayItem item: mVecPlayItems) {
            if (!TextUtils.isEmpty(item.mPlayUrl)) {
                item.mTXLivePlayer.stopPlay(false);
            }
        }
    }

    private void handleLinkMicFailed(TCPlayItem item, String message) {
        if (item == null) {
            return;
        }

        if (item.mPending == true) {
            mHandler.removeCallbacks(mLinkMicTimeOutRunnable);
        }
        if (item.mPlayUrl != null) {
            item.mTXLivePlayer.stopPlay(true);
        }

        mTCLinkMicMgr.kickOutLinkMicMember(item.mUserID);
        mMapLinkMicMember.remove(item.mUserID);
//        if (mMapLinkMicMember.size() == 0) {
//            //如果没有人连麦了，关闭AEC
//            enablePublishAEC(false);
//        }
        stopLoading(item, false);
        item.empty();
        item.mBtnKickout.setVisibility(View.INVISIBLE);

        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 收到连麦请求
     * @param strUserId 连麦id
     * @param strNickName 连麦nickname
     */
    @Override
    public void onReceiveLinkMicRequest(final String strUserId, final String strNickName) {
        //连麦人数限制，正在处理连麦请求
        if (mMapLinkMicMember.size() >= MAX_LINKMIC_MEMBER_SUPPORT || mHasPendingRequest == true) {
            mTCLinkMicMgr.sendLinkMicResponse(strUserId, TCConstants.LINKMIC_RESPONSE_TYPE_REJECT, "主播端连麦人数超过最大限制");
            return;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("提示")
                .setMessage(strNickName + "向您发起连麦请求")
                .setPositiveButton("接受", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //连麦者id，同意连麦，后台合并视频流用的sessionid（本地获取）
                        mTCLinkMicMgr.sendLinkMicResponse(strUserId, TCConstants.LINKMIC_RESPONSE_TYPE_ACCEPT, mSessionID);

                        for (TCPlayItem item: mVecPlayItems) {
                            if (item.mUserID == null || item.mUserID.length() == 0) {
                                item.mUserID = strUserId;
                                item.mPending = true;
                                startLoading(item);

                                //设置超时逻辑，15秒连麦者未上麦自动移出连麦状态
                                mLinkMicTimeOutRunnable.setUserID(strUserId);
                                mHandler.removeCallbacks(mLinkMicTimeOutRunnable);
                                mHandler.postDelayed(mLinkMicTimeOutRunnable, 15000);

                                //加入连麦成员列表
                                mMapLinkMicMember.put(strUserId, "");

//                                //连麦情况下需要开启AEC
//                                enablePublishAEC(true);

                                break;
                            }
                        }

                        dialog.dismiss();
                        mHasPendingRequest = false;
                    }
                })
                .setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //连麦者id，拒绝连麦，拒绝原因
                        mTCLinkMicMgr.sendLinkMicResponse(strUserId, TCConstants.LINKMIC_RESPONSE_TYPE_REJECT, "主播拒绝了您的连麦请求");
                        dialog.dismiss();
                        mHasPendingRequest = false;
                    }
                });
        /**
         * 展示dialog
         */
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mHasPendingRequest = true;

                final AlertDialog alertDialog = builder.create();
                alertDialog.setCancelable(false);
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.show();

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        alertDialog.dismiss();
                        mHasPendingRequest = false;
                    }
                }, 10000);
            }
        });
    }

    @Override
    public void onReceiveLinkMicResponse(final String strUserId, final int result, final String strSessionId) {
    }

    @Override
    public void onReceiveKickedOutNotify() {
    }

    /**
     * 收到连麦者开始推流的通知
     * @param strUserID
     * @param strPlayUrl
     */
    @Override
    public void onReceiveMemberJoinNotify(final String strUserID, final String strPlayUrl) {
        if (TextUtils.isEmpty(strUserID) || TextUtils.isEmpty(strPlayUrl)) {
            return;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mMapLinkMicMember.containsKey(strUserID)) {
                    return;
                }

                TCPlayItem item = getPlayItemByUserID(strUserID);
                if (item == null) {
                    return;
                }

                mMapLinkMicMember.put(strUserID, strPlayUrl);
                if (item.mPlayUrl == null || item.mPlayUrl.length() == 0) {
                    //获取连麦者带有防盗链的拉流地址
                    TCPlayerMgr.getInstance().getPlayUrlWithSignature(mUserId, strPlayUrl, new TCPlayerMgr.OnGetPlayUrlWithSignature() {
                        @Override
                        public void onGetPlayUrlWithSignature(int errCode, String playUrl) {
                            TCPlayItem playItem = getPlayItemByUserID(strUserID);
                            if (playItem != null) {
                                if (errCode == 0 && !TextUtils.isEmpty(playUrl)) {
                                    playItem.mPlayUrl = new StringBuilder(playUrl).append("&session_id=").append(mSessionID).toString();
                                    if (!mPasuing) {
                                        if (playItem.mVideoView != null) {
                                            playItem.mVideoView.clearLog();
                                        }
                                        playItem.mTXLivePlayer.startPlay(playItem.mPlayUrl, TXLivePlayer.PLAY_TYPE_LIVE_RTMP_ACC);
                                    }
                                } else {
                                    handleLinkMicFailed(playItem, "获取拉流防盗链key失败");
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 连麦者退出连麦
     * @param strUserID 退出连麦的userid
     */
    @Override
    public void onReceiveMemberExitNotify(final String strUserID) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                //获取到连麦者的播放单元信息
                TCPlayItem item = getPlayItemByUserID(strUserID);
                if (item != null) {
                    if (item.mPending == true) {
                        //这在加载中连麦，移除定时任务
                        mHandler.removeCallbacks(mLinkMicTimeOutRunnable);
                    }

                    if (item.mPlayUrl != null && item.mPlayUrl.length() != 0) {
                        //连麦单元停止播放
                        item.mTXLivePlayer.stopPlay(true);
                    }
                    //停止loading
                    stopLoading(item, false);
                    //清空单元信息
                    item.empty();
                    item.mBtnKickout.setVisibility(View.INVISIBLE);
                }
                //移除连麦者id
                mMapLinkMicMember.remove(strUserID);
//                if (mMapLinkMicMember.size() == 0) {
//                    //如果没有人连麦了，关闭AEC
//                    enablePublishAEC(false);
//                }
            }
        });
    }

    private String getLinkMicSessionID() {
        //说明：
        //1.sessionID是混流依据，sessionID相同的流，后台混流Server会混为一路视频流；因此，sessionID必须全局唯一

        //2.直播码频道ID理论上是全局唯一的，使用直播码作为sessionID是最为合适的
        //String strSessionID = TCLinkMicMgr.getInstance().getStreamIDByStreamUrl(mPushUrl)

        //3.直播码是字符串，混流Server目前只支持64位数字表示的sessionID，暂时按照下面这种方式生成sessionID
        //  待混流Server改造完成后，再使用直播码作为sessionID

        long timeStamp = System.currentTimeMillis();
        long sessionID = (((long)3891 << 48) | timeStamp);      // 3891是bizid, timeStamp是当前毫秒值
        return String.valueOf(sessionID);
    }

    private TCPlayItem getPlayItemByUserID(String strUserID) {
        if (strUserID == null || strUserID.length() == 0) {
            return null;
        }

        for (TCPlayItem item: mVecPlayItems) {
            if (strUserID.equalsIgnoreCase(item.mUserID)) {
                return item;
            }
        }

        return null;
    }

    private void startLoading(TCPlayItem item) {
        item.mBtnKickout.setVisibility(View.INVISIBLE);
        item.mLinkMicLoadingBg.setVisibility(View.VISIBLE);
        item.mLinkMicLoading.setVisibility(View.VISIBLE);
        item.mLinkMicLoading.setImageResource(R.drawable.linkmic_loading);
        AnimationDrawable ad = (AnimationDrawable) item.mLinkMicLoading.getDrawable();
        ad.start();
    }

    private void stopLoading(TCPlayItem item, boolean showKickout) {
        item.mBtnKickout.setVisibility(showKickout ? View.VISIBLE : View.GONE);
        item.mLinkMicLoadingBg.setVisibility(View.GONE);
        item.mLinkMicLoading.setVisibility(View.GONE);
        AnimationDrawable ad = (AnimationDrawable) item.mLinkMicLoading.getDrawable();
        if (ad != null) {
            ad.stop();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_kick_out1:
                for (int i = 0; i < mVecPlayItems.size(); ++i) {
                    TCPlayItem item = mVecPlayItems.get(i);
                    if (item.mBtnKickout.getId() == v.getId()) {
                        item.mTXLivePlayer.stopPlay(true);
                        stopLoading(item, false);
                        mTCLinkMicMgr.kickOutLinkMicMember(item.mUserID);
                        mMapLinkMicMember.remove(item.mUserID);
//                        if (mMapLinkMicMember.size() == 0) {
//                            //如果没有人连麦了，关闭AEC
//                            enablePublishAEC(false);
//                        }
                        item.empty();
                    }
                }
                break;

            default:
                super.onClick(v);
        }

    }

    private class TXLivePlayListener implements ITXLivePlayListener {
        private TCPlayItem item;

        public TXLivePlayListener(TCPlayItem item) {
            this.item = item;
        }

        public void onPlayEvent(final int event, final Bundle param) {
            TXCloudVideoView videoView = item.mVideoView;
            if (videoView != null) {
                videoView.setLogText(null,param,event);
            }
            if (event == TXLiveConstants.PLAY_ERR_NET_DISCONNECT || event == TXLiveConstants.PLAY_EVT_PLAY_END) {
                if (item.mPending == true) {
                    handleLinkMicFailed(item, "拉流失败，结束连麦");
                }
                else {
                    handleLinkMicFailed(item, "连麦观众视频断流，结束连麦");
                }
            }
            else if (event == TXLiveConstants.PLAY_EVT_PLAY_BEGIN) {
                if (item.mPending == true) {
                    item.mPending = false;
                }
                stopLoading(item, true);
            }
        }

        public void onNetStatus(final Bundle status) {
            TXCloudVideoView videoView = item.mVideoView;
            if (videoView != null) {
                videoView.setLogText(status,null,0);
            }
        }
    }

    /**
     * 有观众连麦时开启AEC，否则不需要开启AEC
     * @param bEnable
     */
    private void enablePublishAEC(boolean bEnable) {
        mTXPushConfig.enableAEC(bEnable);
        mTXLivePusher.setConfig(mTXPushConfig);
    }

    protected void showLog() {
        super.showLog();
        for (int i = 0; i < mVecPlayItems.size(); ++i) {
            TXCloudVideoView videoView = mVecPlayItems.get(i).mVideoView;
            if (videoView != null) {
                videoView.disableLog(!mShowLog);
            }
        }
    }
}
