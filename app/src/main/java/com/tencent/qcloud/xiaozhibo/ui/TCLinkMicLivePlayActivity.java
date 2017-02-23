package com.tencent.qcloud.xiaozhibo.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.tencent.qcloud.xiaozhibo.R;
import com.tencent.qcloud.xiaozhibo.base.TCConstants;
import com.tencent.qcloud.xiaozhibo.base.TCUtils;
import com.tencent.qcloud.xiaozhibo.logic.TCLinkMicMgr;
import com.tencent.qcloud.xiaozhibo.logic.TCLoginMgr;
import com.tencent.qcloud.xiaozhibo.logic.TCPlayerMgr;
import com.tencent.qcloud.xiaozhibo.logic.TCPusherMgr;
import com.tencent.qcloud.xiaozhibo.logic.TCUserInfoMgr;
import com.tencent.rtmp.ITXLivePlayListener;
import com.tencent.rtmp.ITXLivePushListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayConfig;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.TXLivePushConfig;
import com.tencent.rtmp.TXLivePusher;
import com.tencent.rtmp.ui.TXCloudVideoView;

/**
 * Created by dennyfeng on 16/11/16.
 *
 * 连麦的请求端，连麦这或一般观众
 */
public class TCLinkMicLivePlayActivity extends TCLivePlayerActivity implements ITXLivePushListener, TCLinkMicMgr.TCLinkMicListener {

    private static final String TAG = TCLinkMicLivePlayActivity.class.getName();

    private boolean             mWaitingLinkMicResponse = false;
    private boolean             mIsBeingLinkMic         = false;

    private String              mSessionID;

    private Button              mBtnLinkMic;
    private Button              mBtnSwitchCamera;
    private TXCloudVideoView    mSmallVideoView;
    private ImageView           mLinkMicLoading;
    private FrameLayout         mLinkMicLoadingBg;

    private TXLivePusher        mTXLivePusherLink;
    private TXLivePushConfig    mTXLivePushConfigLink;
    private TXLivePlayer        mTXLivePlayerLink;
    private TXLivePlayConfig    mTXLivePlayConfigLink;

    private TCLinkMicMgr        mTCLinkMicMgr;

    private String              mLinkMicPlayUrl;
    private String              mLinkMicNotifyUrl;

    @Override
    protected void initLiveView() {
        super.initLiveView();

        if (mIsLivePlay == false || TCLinkMicMgr.supportLinkMic() == false) {
            return;
        }

        mSmallVideoView = (TXCloudVideoView) findViewById(R.id.small_video_view);
        mSmallVideoView.disableLog(false);

        mBtnLinkMic = (Button) findViewById(R.id.btn_linkmic);
        mBtnLinkMic.setVisibility(View.VISIBLE);
        mBtnLinkMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsBeingLinkMic == false) {
                    startLinkMic();
                } else {
                    stopLinkMic();
                    startPlay();
                    stopLoading();
                }
            }
        });

        mBtnSwitchCamera = (Button) findViewById(R.id.btn_switch_cam);
        mBtnSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsBeingLinkMic) {
                    mTXLivePusherLink.switchCamera();
                }
            }
        });

        mLinkMicLoading = (ImageView) findViewById(R.id.linkmic_loading);
        mLinkMicLoadingBg = (FrameLayout) findViewById(R.id.linkmic_loading_bg);

        initLinkMic();
    }

    private void initLinkMic() {
        mTXLivePusherLink = new TXLivePusher(this);
        mTXLivePushConfigLink = new TXLivePushConfig();

        //连麦者切后台推流图片
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pause_publish,options);
        //设置暂停图片
        mTXLivePushConfigLink.setPauseImg(bitmap);
        //设置暂停flag
        mTXLivePushConfigLink.setPauseFlag(TXLiveConstants.PAUSE_FLAG_PAUSE_VIDEO | TXLiveConstants.PAUSE_FLAG_PAUSE_AUDIO);
        mTXLivePushConfigLink.enableAEC(true);
        mTXLivePushConfigLink.setHardwareAcceleration(true);
        mTXLivePushConfigLink.setAudioSampleRate(48000);
        mTXLivePushConfigLink.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_320_480);
        mTXLivePushConfigLink.setMinVideoBitrate(100);
        mTXLivePushConfigLink.setMaxVideoBitrate(300);
        mTXLivePushConfigLink.setAutoAdjustBitrate(true);
        mTXLivePushConfigLink.setBeautyFilter(TCUtils.filtNumber(9, 100, 50), TCUtils.filtNumber(3, 100, 0));

        mTXLivePusherLink.setPushListener(this);
        mTXLivePusherLink.setConfig(mTXLivePushConfigLink);
        mTXLivePusherLink.setMicVolume(2.0f);

        mTXLivePlayerLink = new TXLivePlayer(this);
        mTXLivePlayConfigLink = new TXLivePlayConfig();
        mTXLivePlayConfigLink.setAutoAdjustCacheTime(true);
        mTXLivePlayConfigLink.setMinAutoAdjustCacheTime(0.2f);
        mTXLivePlayConfigLink.setMaxAutoAdjustCacheTime(0.2f);
        mTXLivePlayConfigLink.enableAEC(true);
        mTXLivePlayerLink.setConfig(mTXLivePlayConfigLink);
        mTXLivePlayerLink.setPlayListener(new TXLivePlayListener());
        mTXLivePlayerLink.enableHardwareDecode(true);

        mTCLinkMicMgr = TCLinkMicMgr.getInstance();
        mTCLinkMicMgr.setLinkMicListener(this);

        mLinkMicPlayUrl = mPlayUrl;

        mTXCloudVideoView.disableLog(!mShowLog);
        mSmallVideoView.disableLog(!mShowLog);
    }

    @Override
    protected void onResume() {
        if (mSmallVideoView != null) {
            mSmallVideoView.onResume();
        }
        if (mIsBeingLinkMic) {
            mTXLivePusherLink.resumePusher();
            startLinkPlay();
            mPausing = false;
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mSmallVideoView != null) {
            mSmallVideoView.onPause();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mIsBeingLinkMic) {
            mTXLivePusherLink.pausePusher();
            stopLinkPlay();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mSmallVideoView != null) {
            mSmallVideoView.onDestroy();
        }

        if (mIsLivePlay) {
            stopLinkMic();

            if (mTCLinkMicMgr != null) {
                mTCLinkMicMgr.setLinkMicListener(null);
                mTCLinkMicMgr = null;
            }
        }
    }

    @Override
    public void onGroupDelete() {
        stopLinkMic();
        super.onGroupDelete();
    }

    private Runnable mRunnableLinkMicTimeOut = new Runnable() {
        @Override
        public void run() {
            if (mWaitingLinkMicResponse) {
                mWaitingLinkMicResponse = false;
                mBtnLinkMic.setEnabled(true);
                mBtnLinkMic.setBackgroundResource(R.drawable.linkmic_on);
                Toast.makeText(getApplicationContext(), "连麦请求超时，主播没有做出回应", Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * 请求主播连麦
     */
    private void startLinkMic() {
        if (mIsBeingLinkMic || mWaitingLinkMicResponse) {
            return;
        }
        //发送IM消息连麦请求
        mTCLinkMicMgr.sendLinkMicRequest(mPusherId);

        mWaitingLinkMicResponse = true;

        mBtnLinkMic.setEnabled(false);
        mBtnLinkMic.setBackgroundResource(R.drawable.linkmic_off);
        //主播响应请求超时处理
        mHandler.removeCallbacks(mRunnableLinkMicTimeOut);
        mHandler.postDelayed(mRunnableLinkMicTimeOut, 10000);   //10秒超时
    }

    /**
     * 停止连麦
     */
    private synchronized void stopLinkMic() {
        if (mIsBeingLinkMic) {
            mIsBeingLinkMic = false;
            mTCLinkMicMgr.sendMemberExitNotify(mPusherId, TCLoginMgr.getInstance().getLastUserInfo().identifier);
        }

        if (mWaitingLinkMicResponse) {
            mWaitingLinkMicResponse = false;
            mHandler.removeCallbacks(mRunnableLinkMicTimeOut);
        }

        if (mBtnLinkMic != null) {
            mBtnLinkMic.setEnabled(true);
            mBtnLinkMic.setBackgroundResource(R.drawable.linkmic_on);
        }

        if (mBtnSwitchCamera != null) {
            mBtnSwitchCamera.setVisibility(View.INVISIBLE);
        }

        stopLinkPlay();
        stopLinkPush();
    }

    private void startLinkPush(String pusherUrl) {
        //启动推流
        if (mTXLivePusherLink != null) {
            mTXCloudVideoView.setVisibility(View.VISIBLE);
            mTXCloudVideoView.clearLog();
            mTXLivePusherLink.startCameraPreview(mTXCloudVideoView);
            mTXLivePusherLink.startPusher(pusherUrl);
        }

        mBtnSwitchCamera.setVisibility(View.VISIBLE);
        startLoading();
    }

    private void stopLinkPush() {
        if (mTXLivePusherLink != null) {
            mTXLivePusherLink.stopCameraPreview(true);
            mTXLivePusherLink.stopPusher();
        }
    }

    private void startLinkPlay() {
        if (mTXLivePlayerLink != null) {
            startLoading();
            mTXLivePlayerLink.setPlayerView(mSmallVideoView);
            mSmallVideoView.clearLog();
            mTXLivePlayerLink.startPlay(mLinkMicPlayUrl, TXLivePlayer.PLAY_TYPE_LIVE_RTMP_ACC);
        }
    }

    private void stopLinkPlay() {
        if (mTXLivePlayerLink != null) {
            stopLoading();
            mTXLivePlayerLink.stopPlay(true);
        }
    }

    private void handleLinkMicFailed(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        //结束连麦
        stopLinkMic();

        //重新从CDN拉流播放
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mPausing && !mIsBeingLinkMic) { //退到后台不用启动拉流
                    startPlay();
                }
            }
        });

        //结束loading
        stopLoading();
    }

    @Override
    public void onReceiveLinkMicRequest(final String strUserId, final String strNickName) {
    }

    /**
     * 请求连麦后，主播对请求的响应
     * @param strUserId
     * @param responseType 是否同意连麦
     * @param strParams 同意：合流用的sessionId，不同意：拒绝的原因
     */
    @Override
    public void onReceiveLinkMicResponse(final String strUserId, final int responseType, final String strParams) {
        if (mWaitingLinkMicResponse == false) {
            return;
        }

        mWaitingLinkMicResponse = false;
        mBtnLinkMic.setEnabled(true);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (TCConstants.LINKMIC_RESPONSE_TYPE_ACCEPT == responseType) {
                    mSessionID = strParams;
                    if (mSessionID == null || mSessionID.length() == 0) {
                        return;
                    }

                    mIsBeingLinkMic = true;
                    mBtnLinkMic.setBackgroundResource(R.drawable.linkmic_off);
                    Toast.makeText(getApplicationContext(), "主播接受了您的连麦请求，开始连麦", Toast.LENGTH_SHORT).show();
                    //请求服务器获取连麦推流地址
                    TCPusherMgr.getInstance().getPusherUrlForLinkMic(TCUserInfoMgr.getInstance().getUserId(),
                            "连麦",
                            TCUserInfoMgr.getInstance().getCoverPic(),
                            TCUserInfoMgr.getInstance().getNickname(),
                            TCUserInfoMgr.getInstance().getHeadPic(),
                            TCUserInfoMgr.getInstance().getLocation(),
                            new TCPusherMgr.OnGetPusherUrlForLinkMic() {
                                @Override
                                public void onGetPusherUrlForLinkMic(int errCode, String pusherUrl, String playUrl, String timeStamp) {
                                    if (errCode == 0) {
                                        //构建推流地址
                                        pusherUrl = new StringBuilder(pusherUrl).append(String.format("&mix=layer:s;t_id:1;session_id:%s", mSessionID)).toString();
                                        //要给主播端的拉取上麦这视频流的地址
                                        mLinkMicNotifyUrl = playUrl;

                                        //结束从CDN拉流
                                        stopPlay(true);
                                        startLinkPush(pusherUrl);
                                    } else {
                                        Toast.makeText(getApplicationContext(), "拉取连麦推流地址失败，error=" + errCode, Toast.LENGTH_SHORT).show();
                                        mIsBeingLinkMic = false;
                                        mWaitingLinkMicResponse = false;
                                        mBtnLinkMic.setBackgroundResource(R.drawable.linkmic_on);
                                    }
                                }
                            }
                    );
                } else if (TCConstants.LINKMIC_RESPONSE_TYPE_REJECT == responseType) {
                    String reason = strParams;
                    if (reason != null && reason.length() > 0) {
                        Toast.makeText(getApplicationContext(), reason, Toast.LENGTH_SHORT).show();
                    }
                    mIsBeingLinkMic = false;
                    mBtnLinkMic.setBackgroundResource(R.drawable.linkmic_on);
                }
            }
        });
    }

    @Override
    public void onReceiveKickedOutNotify() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                stopLoading();
                Toast.makeText(getApplicationContext(), "不好意思，您被主播踢开", Toast.LENGTH_SHORT).show();
                //结束连麦
                stopLinkMic();
                //重新从CDN拉流播放
                startPlay();
            }
        });
    }

    @Override
    public void onReceiveMemberJoinNotify(String strUserId, String strPlayUrl) {
    }

    @Override
    public void onReceiveMemberExitNotify(String strUserId) {

    }

    private void startLoading() {
        mLinkMicLoadingBg.setVisibility(View.VISIBLE);
        mLinkMicLoading.setVisibility(View.VISIBLE);
        mLinkMicLoading.setImageResource(R.drawable.linkmic_loading);
        AnimationDrawable ad = (AnimationDrawable) mLinkMicLoading.getDrawable();
        if (ad != null) {
            ad.start();
        }
    }

    private void stopLoading() {
        mLinkMicLoadingBg.setVisibility(View.GONE);
        mLinkMicLoading.setVisibility(View.GONE);
        AnimationDrawable ad = (AnimationDrawable) mLinkMicLoading.getDrawable();
        if (ad != null) {
            ad.stop();
        }
    }

    /**
     * 开始推流后收到推流事件的回调
     * @param event 时间
     * @param bundle
     */
    @Override
    public void onPushEvent(int event, Bundle bundle) {
        if (event == TXLiveConstants.PUSH_EVT_PUSH_BEGIN && mIsBeingLinkMic) {                     //开始推流事件通知
            //向主播发送加入连麦的通知
            mTCLinkMicMgr.sendMemberJoinNotify(mPusherId, TCLoginMgr.getInstance().getLastUserInfo().identifier, mLinkMicNotifyUrl);

            mTCPlayerMgr.getPlayUrlWithSignature(mUserId, mPlayUrl, new TCPlayerMgr.OnGetPlayUrlWithSignature() {
                @Override
                public void onGetPlayUrlWithSignature(int errCode, String strPlayUrl) {
                    if (errCode == 0 && strPlayUrl != null && strPlayUrl.length() > 0) {
                        //从低时延服务器拉流
                        mLinkMicPlayUrl = (strPlayUrl + "&session_id=" + mSessionID);
                        if (mIsBeingLinkMic && !mPausing) {
                            startLinkPlay();
                        }
                    } else {
                        handleLinkMicFailed("获取防盗链key失败，结束连麦");
                    }
                }
            });
        }
        else if (event == TXLiveConstants.PUSH_ERR_NET_DISCONNECT) {            //推流失败事件通知
            handleLinkMicFailed("推流失败，结束连麦");
        }  else if (event == TXLiveConstants.PUSH_ERR_OPEN_CAMERA_FAIL) {//未获得摄像头权限
            handleLinkMicFailed("未获得摄像头权限，结束连麦");
        } else if (event == TXLiveConstants.PUSH_ERR_OPEN_MIC_FAIL) { //未获得麦克风权限
            if (mIsBeingLinkMic) {
                handleLinkMicFailed("未获得麦克风权限，结束连麦");
            }
        }
    }

    private class TXLivePlayListener implements ITXLivePlayListener {
        public void onPlayEvent(final int event, final Bundle param) {
            if (mSmallVideoView != null) {
                mSmallVideoView.setLogText(null,param,event);
            }
            if (event == TXLiveConstants.PLAY_EVT_PLAY_BEGIN) {
                //开始拉流，或者拉流失败，结束loading
                stopLoading();
            } else if (event == TXLiveConstants.PLAY_ERR_NET_DISCONNECT || event == TXLiveConstants.PLAY_EVT_PLAY_END) {
                handleLinkMicFailed("主播的流拉取失败，结束连麦");
            }
        }

        public void onNetStatus(final Bundle status) {
            if (mSmallVideoView != null) {
                mSmallVideoView.setLogText(status,null,0);
            }
        }
    }

    protected void showLog() {
        super.showLog();
        if (mSmallVideoView != null) {
            mSmallVideoView.disableLog(!mShowLog);
        }
    }
}
