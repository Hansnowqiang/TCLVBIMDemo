package com.tencent.qcloud.xiaozhibo.ui;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.TIMElemType;
import com.tencent.TIMMessage;
import com.tencent.qcloud.xiaozhibo.R;
import com.tencent.qcloud.xiaozhibo.base.TCConstants;
import com.tencent.qcloud.xiaozhibo.base.TCUtils;
import com.tencent.qcloud.xiaozhibo.logic.TCChatEntity;
import com.tencent.qcloud.xiaozhibo.logic.TCChatMsgListAdapter;
import com.tencent.qcloud.xiaozhibo.logic.TCChatRoomMgr;
import com.tencent.qcloud.xiaozhibo.logic.TCDanmuMgr;
import com.tencent.qcloud.xiaozhibo.logic.TCLoginMgr;
import com.tencent.qcloud.xiaozhibo.logic.TCPusherMgr;
import com.tencent.qcloud.xiaozhibo.logic.TCScreenRecordService;
import com.tencent.qcloud.xiaozhibo.logic.TCSimpleUserInfo;
import com.tencent.qcloud.xiaozhibo.logic.TCUserAvatarListAdapter;
import com.tencent.qcloud.xiaozhibo.logic.TCUserInfoMgr;
import com.tencent.qcloud.xiaozhibo.ui.customviews.DetailDialogFragment;
import com.tencent.qcloud.xiaozhibo.ui.customviews.FloatingCameraView;
import com.tencent.qcloud.xiaozhibo.ui.customviews.FloatingView;
import com.tencent.qcloud.xiaozhibo.ui.customviews.TCHeartLayout;
import com.tencent.qcloud.xiaozhibo.ui.customviews.TCInputTextMsgDialog;
import com.tencent.rtmp.ITXLivePushListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePushConfig;
import com.tencent.rtmp.TXLivePusher;
import com.tencent.rtmp.TXLog;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.media.UMImage;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import master.flame.danmaku.controller.IDanmakuView;

/**
 * 屏幕录制Activity
 * 注：Android在API21+的版本才支持屏幕录制功能
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class TCScreenRecordActivity extends TCBaseActivity implements ITXLivePushListener, View.OnClickListener, TCPusherMgr.PusherListener, TCChatRoomMgr.TCChatRoomListener, TCInputTextMsgDialog.OnTextSendListener {

    private static final String TAG = TCScreenRecordActivity.class.getSimpleName();

    //悬浮摄像窗以及悬浮球
    private FloatingView mFloatingView;
    private FloatingCameraView mFloatingCameraView;
    private ImageView mPrivateBtn;
    private ImageView mCameraBtn;

    //主界面（主播信息 左下消息列表 右上头像列表）
    private RelativeLayout mPusherInfoLayout;
    private TextView mTVPrivateMode;
    private TCInputTextMsgDialog mInputTextMsgDialog;
    private ListView mListViewMsg;
    private ArrayList<TCChatEntity> mArrayListChatEntity = new ArrayList<>();
    private TCChatMsgListAdapter mChatMsgListAdapter;
    private TextView mMemberCount;
    private RecyclerView mUserAvatarList;
    private TCUserAvatarListAdapter mAvatarListAdapter;

    //隐私模式drawable(支持自定义大小)
    private Drawable mDrawableLockOn;
    private Drawable mDrawableLockOff;

    //推流信息
    private String mPushUrl;
    private String mUserId;
    private String mTitle;
    private String mCoverPicUrl;
    private String mHeadPicUrl;
    private String mNickName;
    private String mLocation;
    private int mBitrateType;

    //后台Mgr
    private TCChatRoomMgr mTCChatRoomMgr;
    private TCPusherMgr mTCPusherMgr;
    private TXLivePusher mTXLivePusher;
    private TXLivePushConfig mTXPushConfig;
    private boolean mInPrivacy = false;
    private boolean mInCamera = false;

    //飘心动画 以及手势控制
    private TCHeartLayout mHeartLayout;
    private RelativeLayout mControllLayer;
    private TCSwipeAnimationController mTCSwipeAnimationController;

    //弹幕
    private TCDanmuMgr mDanmuMgr;

    //主播信息 统计信息 小红点
    private Handler mHandler = new Handler();
    private ImageView mHeadIcon;
    private ImageView mRecordBall;
    private TextView mBroadcastTime;
    private Timer mBroadcastTimer;
    private BroadcastTimerTask mBroadcastTimerTask;
    private long mSecond = 0;
    private long lTotalMemberCount = 0;
    private long lMemberCount = 0;
    private long lHeartCount = 0;

    private Intent serviceIntent;

    //分享相关
    private UMImage mImage = null;
    private SHARE_MEDIA mShare_meidia = SHARE_MEDIA.WEIXIN;
    private String mShareUrl = TCConstants.SVR_LivePlayShare_URL;
    private boolean mSharedNotPublished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_record);

        //获取推流信息
        Intent intent = getIntent();
        mUserId = intent.getStringExtra(TCConstants.USER_ID);
        mTitle = intent.getStringExtra(TCConstants.ROOM_TITLE);
        mCoverPicUrl = intent.getStringExtra(TCConstants.COVER_PIC);
        mHeadPicUrl = intent.getStringExtra(TCConstants.USER_HEADPIC);
        mNickName = intent.getStringExtra(TCConstants.USER_NICK);
        mLocation = intent.getStringExtra(TCConstants.USER_LOC);
        mBitrateType = intent.getExtras().getInt(TCConstants.BITRATE);
        mShare_meidia = (SHARE_MEDIA)intent.getSerializableExtra(TCConstants.SHARE_PLATFORM);

        //推流 im 初始化
        mTXPushConfig = new TXLivePushConfig();

        mTCChatRoomMgr = TCChatRoomMgr.getInstance();
        mTCChatRoomMgr.setMessageListener(this);

        mTCPusherMgr = TCPusherMgr.getInstance();
        mTCPusherMgr.setPusherListener(this);

        mTCChatRoomMgr.createGroup();

        //启动后台拉活进程（处理强制下线消息）
        serviceIntent = new Intent();
        serviceIntent.setClassName(this, TCScreenRecordService.class.getName());

        startService(serviceIntent);
        //bindService(intentService, mServiceConn, BIND_AUTO_CREATE);

        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.rl_record_root);

        relativeLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mTCSwipeAnimationController.processEvent(event);
            }
        });

        //悬浮球界面
        mFloatingView = new FloatingView(getApplicationContext(), R.layout.view_floating_default);
        mFloatingView.setPopupWindow(R.layout.popup_layout);
        mFloatingCameraView = new FloatingCameraView(getApplicationContext());

        mPrivateBtn = (ImageView) mFloatingView.getPopupView().findViewById(R.id.btn_privacy);
        mCameraBtn = (ImageView) mFloatingView.getPopupView().findViewById(R.id.btn_camera);

        mFloatingView.setOnPopupItemClickListener(this);

        mDrawableLockOn = getResources().getDrawable(R.mipmap.lock_off);
        if (null != mDrawableLockOn) mDrawableLockOn .setBounds(0, 0, 40, 40);

        mDrawableLockOff = getResources().getDrawable(R.mipmap.lock_on);
        if (null != mDrawableLockOff) mDrawableLockOff.setBounds(0, 0, 40, 40);

        mTVPrivateMode = (TextView) findViewById(R.id.tv_private_mode);
        if (null != mTVPrivateMode)  mTVPrivateMode.setCompoundDrawables(mDrawableLockOff ,null, null, null);

        //推流主界面init
        //mScreenHeight = getResources().getDisplayMetrics().heightPixels;
        mPusherInfoLayout = (RelativeLayout) findViewById(R.id.layout_live_pusher_info);
        mPusherInfoLayout.setVisibility(View.INVISIBLE);
        mInputTextMsgDialog = new TCInputTextMsgDialog(this, R.style.InputDialog);
        mInputTextMsgDialog.setmOnTextSendListener(this);

        mBroadcastTime = (TextView) findViewById(R.id.tv_broadcasting_time);
        mBroadcastTime.setText(String.format(Locale.US,"%s","00:00:00"));
        mRecordBall = (ImageView) findViewById(R.id.iv_record_ball);

        mHeadIcon = (ImageView) findViewById(R.id.iv_head_icon);
        showHeadIcon(mHeadIcon, TCUserInfoMgr.getInstance().getHeadPic());
        mMemberCount = (TextView) findViewById(R.id.tv_member_counts);
        if (mMemberCount != null) mMemberCount.setText("0");

        mListViewMsg = (ListView) findViewById(R.id.im_msg_listview);
        mHeartLayout = (TCHeartLayout) findViewById(R.id.heart_layout);

        mChatMsgListAdapter = new TCChatMsgListAdapter(this, mListViewMsg, mArrayListChatEntity);
        mListViewMsg.setAdapter(mChatMsgListAdapter);

        IDanmakuView danmakuView = (IDanmakuView) findViewById(R.id.danmakuView);
        mDanmuMgr = new TCDanmuMgr(this);
        mDanmuMgr.setDanmakuView(danmakuView);

        mUserAvatarList = (RecyclerView) findViewById(R.id.rv_user_avatar);
        mUserAvatarList.setVisibility(View.GONE);//ui冲突，目前不显示
        mAvatarListAdapter = new TCUserAvatarListAdapter(this, TCLoginMgr.getInstance().getLastUserInfo().identifier  );
        mUserAvatarList.setAdapter(mAvatarListAdapter);

        mControllLayer = (RelativeLayout) findViewById(R.id.rl_controllLayer);
        mTCSwipeAnimationController = new TCSwipeAnimationController(this);
        mTCSwipeAnimationController.setAnimationView(mControllLayer);

    }

    /**
     * 退出房间
     * 包含后台退出与IMSDK房间退出操作
     */
    public void quitRoom() {
        mTCChatRoomMgr.deleteGroup();
        mTCPusherMgr.changeLiveStatus(mUserId, TCPusherMgr.TCLiveStatus_Offline);
    }

    /**
     * 开始录屏推流
     */
    protected void startPublish() {
        mSharedNotPublished = false;
        if (mTXLivePusher == null) {
            mTXLivePusher = new TXLivePusher(this);
        }

        int customModeType = 0;

        //根据用户选择确定码率
        switch (mBitrateType) {
            case TCConstants.BITRATE_SLOW:
                mTXPushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_360_640);

                mTXPushConfig.setAutoAdjustBitrate(false);
                mTXPushConfig.setVideoBitrate(mBitrateType);
                break;
            case TCConstants.BITRATE_NORMAL:
                mTXPushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_540_960);

                mTXPushConfig.setAutoAdjustBitrate(false);
                mTXPushConfig.setVideoBitrate(mBitrateType);
                break;
            case TCConstants.BITRATE_FAST:
                mTXPushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_720_1280);

                mTXPushConfig.setAutoAdjustBitrate(false);
                mTXPushConfig.setVideoBitrate(mBitrateType);
                break;
            default:
                //默认情况采用自动码率，正常情况下不会发生
                mTXPushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_360_640);

                mTXPushConfig.setAutoAdjustBitrate(false);
                mTXPushConfig.setVideoBitrate(1000);
                break;
        }


        mTXPushConfig.setCustomModeType(customModeType);
        //隐私模式图片自定义
        mTXPushConfig.setPauseImg(300, 10);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.recording_background_private_vertical,options);
        mTXPushConfig.setPauseImg(bitmap);
        mTXLivePusher.setConfig(mTXPushConfig);
        mTXLivePusher.setPushListener(this);
        //开启屏幕捕捉
        mTXLivePusher.startScreenCapture();
        mTXLivePusher.startPusher(mPushUrl);
        //mTXLivePusher.pausePusher(TXLiveConstants.PAUSE_WITH_IMAGE);
    }

    /**
     * 停止录屏推流
     */
    private void stopPublish() {
        if (mTXLivePusher != null) {
            mTXLivePusher.stopScreenCapture();
            mTXLivePusher.setPushListener(null);
            mTXLivePusher.stopPusher();
        }
    }

    /**
     * 显示确认消息
     * @param msg 消息内容
     * @param isError true错误消息（必须退出） false提示消息（可选择是否退出）
     */
    public void showComfirmDialog(String msg, Boolean isError) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(msg);

        if (!isError) {
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    stopPublish();
                    quitRoom();
                    stopRecordAnimation();
                    showDetailDialog();
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        } else {
            //当情况为错误的时候，直接停止推流
            stopPublish();
            quitRoom();
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    stopRecordAnimation();
                    showDetailDialog();
                }
            });
        }
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * 直播结束信息弹出dialog
     */
    private void showDetailDialog() {

        //显示时长、点赞信息、观看人数

        DetailDialogFragment dialogFragment = new DetailDialogFragment();
        Bundle args = new Bundle();
        args.putString("time", TCUtils.formattedTime(mSecond));
        args.putString("heartCount", String.format(Locale.CHINA, "%d", lHeartCount));
        args.putString("totalMemberCount", String.format(Locale.CHINA, "%d", lTotalMemberCount));
        dialogFragment.setArguments(args);
        dialogFragment.setCancelable(false);
        dialogFragment.show(getFragmentManager(), "");

    }

    /**
     * 显示错误消息（不可退出）
     * @param errorMsg 错误信息
     */
    @Override
    protected void showErrorAndQuit(String errorMsg) {

        quitRoom();
        stopRecordAnimation();

        super.showErrorAndQuit(errorMsg);
    }

    /**
     * 加载主播头像
     * @param view view
     * @param avatar 头像链接
     */
    private void showHeadIcon(ImageView view, String avatar) {
        TCUtils.showPicWithUrl(this, view, avatar, R.drawable.face);
    }


    ObjectAnimator mObjAnim;
    /**
     * 开启红点与计时动画
     */
    private void startRecordAnimation() {
        ObjectAnimator mObjAnim;
        mObjAnim = ObjectAnimator.ofFloat(mRecordBall, "alpha", 1f, 0f, 1f);
        mObjAnim.setDuration(1000);
        mObjAnim.setRepeatCount(-1);
        mObjAnim.start();

        //直播时间
        if(mBroadcastTimer == null) {
            mBroadcastTimer = new Timer(true);
            mBroadcastTimerTask = new BroadcastTimerTask();
            mBroadcastTimer.schedule(mBroadcastTimerTask, 1000, 1000);
        }
    }

    /**
     * 关闭红点与计时动画
     */
    private void stopRecordAnimation() {

        if (null != mObjAnim)
            mObjAnim.cancel();

        //直播时间
        if (null != mBroadcastTimer) {
            mBroadcastTimerTask.cancel();
        }
    }

    @Override
    public void onPushEvent(int event, Bundle bundle) {
        if (event < 0) {
            if (event == TXLiveConstants.PUSH_ERR_NET_DISCONNECT) {//网络断开，弹对话框强提醒，推流过程中直播中断需要显示直播信息后退出
                showComfirmDialog(TCConstants.ERROR_MSG_NET_DISCONNECTED, true);
            } else if (event == TXLiveConstants.PUSH_ERR_OPEN_CAMERA_FAIL) {//未获得摄像头权限，弹对话框强提醒，并退出
                showErrorAndQuit(TCConstants.ERROR_MSG_OPEN_CAMERA_FAIL);
            } else if (event == TXLiveConstants.PUSH_ERR_SCREEN_CAPTURE_START_FAILED) {
                showErrorAndQuit(TCConstants.ERROR_MSG_RECORD_PERMISSION_FAIL);//未获得录屏权限，弹对话框强提醒，并退出
            } else if (event == TXLiveConstants.PUSH_ERR_OPEN_MIC_FAIL) { //未获得麦克风权限，弹对话框强提醒，并退出
                Toast.makeText(getApplicationContext(), bundle.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
                showErrorAndQuit(TCConstants.ERROR_MSG_OPEN_MIC_FAIL);
            } else {
                //其他错误弹Toast弱提醒，并退出
                Toast.makeText(getApplicationContext(), bundle.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
                TCPusherMgr.getInstance().changeLiveStatus(mUserId, TCPusherMgr.TCLiveStatus_Offline);
                finish();
            }
        }

        if (event == TXLiveConstants.PUSH_EVT_PUSH_BEGIN) {
            TCPusherMgr.getInstance().changeLiveStatus(mUserId, TCPusherMgr.TCLiveStatus_Online);
        }
    }

    @Override
    public void onNetStatus(Bundle bundle) {

    }

    @Override
    public void onGetPusherUrl(int errCode, String groupId, String pusherUrl, String timeStamp) {
        if (errCode == 0) {
            mPushUrl = pusherUrl;
            startRecordAnimation();
            if (mShare_meidia == SHARE_MEDIA.MORE) {
                startPublish();
            } else {
                startShare(timeStamp);
                if ((mShare_meidia != SHARE_MEDIA.SINA && !UMShareAPI.get(this).isInstall(this, mShare_meidia)) ||
                        (mShare_meidia == SHARE_MEDIA.QZONE && !UMShareAPI.get(this).isInstall(this, SHARE_MEDIA.QQ))) {
                    startPublish();
                }
            }
        } else {
            if (null == groupId) {
                showErrorAndQuit(TCConstants.ERROR_MSG_CREATE_GROUP_FAILED + errCode);
            } else {
                showErrorAndQuit(TCConstants.ERROR_MSG_GET_PUSH_URL_FAILED + errCode);
            }
        }
    }

    private void startShare(String timeStamp) {
        ShareAction shareAction = new ShareAction(TCScreenRecordActivity.this);

        try {
            mShareUrl = mShareUrl + "?sdkappid=" + java.net.URLEncoder.encode(String.valueOf(TCConstants.IMSDK_APPID), "utf-8")
                    + "&acctype=" + java.net.URLEncoder.encode(String.valueOf(TCConstants.IMSDK_ACCOUNT_TYPE), "utf-8")
                    + "&userid=" + java.net.URLEncoder.encode(mUserId, "utf-8")
                    + "&type=0"
                    + "&ts=" + java.net.URLEncoder.encode(timeStamp, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (mCoverPicUrl.isEmpty()) {
            mImage = new UMImage(TCScreenRecordActivity.this.getApplicationContext(), R.drawable.bg);
        } else {
            mImage = new UMImage(TCScreenRecordActivity.this.getApplicationContext(), mCoverPicUrl);
        }
        shareAction.withMedia(mImage);
        shareAction.withTargetUrl(mShareUrl);
        shareAction.withTitle(mTitle);
        shareAction.withText(mNickName + "正在直播");
        shareAction.setCallback(umShareListener);

        shareAction.setPlatform(mShare_meidia).share();
        mSharedNotPublished = true;
    }

    private UMShareListener umShareListener = new UMShareListener() {
        @Override
        public void onResult(SHARE_MEDIA platform) {
            Log.d("plat","platform"+platform);
            Toast.makeText(TCScreenRecordActivity.this, platform + " 分享成功啦", Toast.LENGTH_SHORT).show();
            //开启推流
            TCScreenRecordActivity.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mSharedNotPublished)
                        startPublish();
                }
            });
        }

        @Override
        public void onError(SHARE_MEDIA platform, Throwable t) {
            Toast.makeText(TCScreenRecordActivity.this,platform + " 分享失败啦", Toast.LENGTH_SHORT).show();
            if(t!=null){
                Log.d("throw","throw:"+t.getMessage());
            }
            //开启推流
            TCScreenRecordActivity.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mSharedNotPublished)
                        startPublish();
                }
            });
        }

        @Override
        public void onCancel(SHARE_MEDIA platform) {
            Toast.makeText(TCScreenRecordActivity.this,platform + " 分享取消了", Toast.LENGTH_SHORT).show();
            //开启推流
            TCScreenRecordActivity.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mSharedNotPublished)
                        startPublish();
                }
            });
        }
    };

    @Override
    public void onChangeLiveStatus(int errCode) {
        Log.d(TAG, "onChangeLiveStatus:" + errCode);
    }

    @Override
    public void onJoinGroupCallback(int code, String msg) {
        if (0 == code) {
            //获取推流地址
            Log.d(TAG, "onJoin group success" + msg);
            mTCPusherMgr.getPusherUrl(mUserId, msg, mTitle, mCoverPicUrl, mNickName, mHeadPicUrl, mLocation);
        } else if (TCConstants.NO_LOGIN_CACHE == code) {
            TXLog.d(TAG, "onJoin group failed" + msg);
            showErrorAndQuit(TCConstants.ERROR_MSG_NO_LOGIN_CACHE);
        }  else {
            TXLog.d(TAG, "onJoin group failed" + msg);
            showErrorAndQuit(TCConstants.ERROR_MSG_JOIN_GROUP_FAILED + code);
        }
    }

    @Override
    public void onSendMsgCallback(int code, TIMMessage timMessage) {
        //消息发送成功后回显
        if (code == 0) {
            TIMElemType elemType = timMessage.getElement(0).getType();
            if (elemType == TIMElemType.Text) {
                //文本消息显示
                Log.d(TAG, "onSendTextMsgsuccess:" + code);

            } else if (elemType == TIMElemType.Custom) {
                //custom消息存在消息回调,此处显示成功失败
                Log.d(TAG, "onSendCustomMsgsuccess:" + code);
            }

        } else {
            Log.d(TAG, "onSendMsgfail:" + code + " msg:" + timMessage.getMsgId());
        }
    }

    @Override
    public void onReceiveMsg(int type, TCSimpleUserInfo userInfo, String content) {
        switch (type) {
            case TCConstants.IMCMD_ENTER_LIVE:
                handleMemberJoinMsg(userInfo);
                break;
            case TCConstants.IMCMD_EXIT_LIVE:
                handleMemberQuitMsg(userInfo);
                break;
            case TCConstants.IMCMD_PRAISE:
                handlePraiseMsg(userInfo);
                break;
            case TCConstants.IMCMD_PAILN_TEXT:
                handleTextMsg(userInfo, content);
                break;
            case TCConstants.IMCMD_DANMU:
                handleDanmuMsg(userInfo, content);
                break;
            default:
                break;
        }
    }

    /**
     * 处理自定义用户加入消息
     * @param userInfo 用户信息
     */
    public void handleMemberJoinMsg(TCSimpleUserInfo userInfo) {
        lTotalMemberCount++;
        lMemberCount++;
        mMemberCount.setText(String.format(Locale.CHINA,"%d",lMemberCount));

        mAvatarListAdapter.addItem(userInfo);

        TCChatEntity entity = new TCChatEntity();
        entity.setSenderName("通知");
        if(userInfo.nickname.equals(""))
            entity.setContext(userInfo.userid + "加入直播");
        else
            entity.setContext(userInfo.nickname + "加入直播");
        entity.setType(TCConstants.MEMBER_ENTER);
        notifyMsg(entity);
    }

    /**
     * 处理自定义用户退出消息
     * @param userInfo 用户信息
     */
    public void handleMemberQuitMsg(TCSimpleUserInfo userInfo) {
        if(lMemberCount > 0)
            lMemberCount--;
        else
            Log.d(TAG, "接受多次退出请求，目前人数为负数");
        mMemberCount.setText(String.format(Locale.CHINA,"%d",lMemberCount));

        mAvatarListAdapter.removeItem(userInfo.userid);

        TCChatEntity entity = new TCChatEntity();
        entity.setSenderName("通知");
        if(userInfo.nickname.equals(""))
            entity.setContext(userInfo.userid + "退出直播");
        else
            entity.setContext(userInfo.nickname + "退出直播");
        entity.setType(TCConstants.MEMBER_EXIT);
        notifyMsg(entity);
    }

    /**
     * 处理自定义用户点赞消息
     * @param userInfo 用户信息
     */
    public void handlePraiseMsg(TCSimpleUserInfo userInfo) {

        TCChatEntity entity = new TCChatEntity();
        entity.setSenderName("通知");
        if(userInfo.nickname.equals(""))
            entity.setContext(userInfo.userid + "点了个赞");
        else
            entity.setContext(userInfo.nickname + "点了个赞");

        if (mHeartLayout != null) {
            mHeartLayout.addFavor();
        }
        lHeartCount++;

        //todo：修改显示类型
        entity.setType(TCConstants.PRAISE);
        notifyMsg(entity);
    }

    /**
     * 处理自定义用户弹幕消息
     * @param userInfo 用户信息
     */
    public void handleDanmuMsg(TCSimpleUserInfo userInfo, String text) {

        TCChatEntity entity = new TCChatEntity();
        entity.setSenderName(userInfo.nickname);
        entity.setContext(text);
        entity.setType(TCConstants.TEXT_TYPE);
        notifyMsg(entity);

        if (mDanmuMgr != null) {
            mDanmuMgr.addDanmu(userInfo.headpic, userInfo.nickname, text);
        }
    }

    /**
     * 处理文本消息
     * @param userInfo 用户信息
     */
    public void handleTextMsg(TCSimpleUserInfo userInfo, String text) {
        TCChatEntity entity = new TCChatEntity();
        entity.setSenderName(userInfo.nickname);
        entity.setContext(text);
        entity.setType(TCConstants.TEXT_TYPE);

        notifyMsg(entity);
    }

    /**
     * 发消息弹出框
     */
    private void showInputMsgDialog() {
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams lp = mInputTextMsgDialog.getWindow().getAttributes();

        lp.width = (display.getWidth()); //设置宽度
        mInputTextMsgDialog.getWindow().setAttributes(lp);
        mInputTextMsgDialog.setCancelable(true);
        mInputTextMsgDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mInputTextMsgDialog.show();
    }


    @Override
    public void onGroupDelete() {
        //useless，主播端不需要该接口
    }

    @Override
    protected void onResume() {
        super.onResume();
        //关闭悬浮球与相机
//        if (mScrOrientation == TCConstants.ORIENTATION_LANDSCAPE) {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//            mRootRelativeLayout.setBackground(getDrawable(R.mipmap.recording_background_horizontal));
//        }

        if (mFloatingView.isShown()) {
            mFloatingView.dismiss();
        }
        if (null != mFloatingCameraView && mFloatingCameraView.isShown()) {
            mFloatingCameraView.dismiss();
            mCameraBtn.setImageResource(R.mipmap.camera_off);
            mInCamera = false;
        }
        if (mSharedNotPublished)
            startPublish();
    }

    @Override
    public void onTextSend(String msg, boolean danmuOpen) {
        if (msg.length() == 0)
            return;
        try {
            byte[] byte_num = msg.getBytes("utf8");
            if (byte_num.length > 160) {
                Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }

        TCChatEntity entity = new TCChatEntity();
        entity.setSenderName("我:");
        entity.setContext(msg);
        entity.setType(TCConstants.TEXT_TYPE);
        notifyMsg(entity);

        if (danmuOpen) {
            if (mDanmuMgr != null) {
                mDanmuMgr.addDanmu(TCUserInfoMgr.getInstance().getHeadPic(),TCUserInfoMgr.getInstance().getNickname(),msg);
            }
            mTCChatRoomMgr.sendDanmuMessage(msg);
        } else {
            mTCChatRoomMgr.sendTextMessage(msg);
        }

    }

    private void notifyMsg(final TCChatEntity entity) {

        mHandler.post(new Runnable() {
            @Override
            public void run() {
//                if(entity.getType() == TCConstants.PRAISE) {
//                    if(mArrayListChatEntity.contains(entity))
//                        return;
//                }
                mArrayListChatEntity.add(entity);
                mChatMsgListAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * 记时器
     */
    private class BroadcastTimerTask extends TimerTask {
        public void run() {
            Log.i(TAG, "timeTask ");
            ++mSecond;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(!mTCSwipeAnimationController.isMoving())
                        mBroadcastTime.setText(TCUtils.formattedTime(mSecond));
                }
            });
//            if (MySelfInfo.getInstance().getIdStatus() == TCConstants.HOST)
//                mHandler.sendEmptyMessage(UPDAT_WALL_TIME_TIMER_TASK);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!mFloatingView.isShown()) {
            if ((null != mTXLivePusher)) {
                if (mTXLivePusher.isPushing()) {
                    mFloatingView.show();
                    mFloatingView.setOnPopupItemClickListener(this);
                    //mTXLivePusher.resumePusher();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDanmuMgr != null) {
            mDanmuMgr.destroy();
            mDanmuMgr = null;
        }

        if (mFloatingView.isShown()) {
            mFloatingView.dismiss();
        }

        if (null != mFloatingCameraView) {
            if (mFloatingCameraView.isShown()) {
                mFloatingCameraView.dismiss();
            }
            mFloatingCameraView.release();
        }

        //unbindService(mServiceConn);
        stopService(serviceIntent);

        stopPublish();
        stopRecordAnimation();
        mTCChatRoomMgr.removeMsgListener();
        mTCPusherMgr.setPusherListener(null);
    }

    @Override
    public void onBackPressed() {
        showComfirmDialog(TCConstants.TIPS_MSG_STOP_PUSH, false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_message_input:
                showInputMsgDialog();
                break;
            case R.id.btn_return:
                //悬浮球返回主界面按钮
                Toast.makeText(getApplicationContext(), "返回主界面", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), TCScreenRecordActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(intent);
                break;
            case R.id.btn_close:
                //停止推流
                showComfirmDialog(TCConstants.TIPS_MSG_STOP_PUSH, false);
                break;
            case R.id.tv_private_mode:
            case R.id.btn_privacy:
                //隐私模式
                triggerPrivateMode();
                break;
            case R.id.btn_camera:
                //camera悬浮窗
                triggerFloatingCameraView();
                break;
            default:
                break;
        }
    }

    /**
     * 隐私模式切换
     */
    public void triggerPrivateMode() {
        if (mInPrivacy) {
            Toast.makeText(getApplicationContext(), getString(R.string.private_mode_off), Toast.LENGTH_SHORT).show();
            mTVPrivateMode.setText(getString(R.string.private_mode_off));
            mTVPrivateMode.setCompoundDrawables(mDrawableLockOn,null,null,null);
            mPrivateBtn.setImageResource(R.mipmap.lock_off);
            mTXLivePusher.resumePusher();
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.private_mode_on), Toast.LENGTH_SHORT).show();
            mTXLivePusher.pausePusher();
            mPrivateBtn.setImageResource(R.mipmap.lock_on);
            mTVPrivateMode.setText(getString(R.string.private_mode_on));
            mTVPrivateMode.setCompoundDrawables(mDrawableLockOff,null,null,null);
        }
        mInPrivacy = !mInPrivacy;
    }

    /**
     * 处理cameraview初始化、权限申请 以及 cameraview的显示与隐藏
     */
    public void triggerFloatingCameraView() {

        //trigger
        if (mInCamera) {
            Toast.makeText(getApplicationContext(), "关闭摄像头", Toast.LENGTH_SHORT).show();
            mCameraBtn.setImageResource(R.mipmap.camera_off);
            mFloatingCameraView.dismiss();
        } else {
            //show失败显示错误信息
            if (!mFloatingCameraView.show()) {
                Toast.makeText(getApplicationContext(), "打开摄像头权限失败,请在系统设置打开摄像头权限", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(getApplicationContext(), "打开摄像头", Toast.LENGTH_SHORT).show();
            mCameraBtn.setImageResource(R.mipmap.camera_on);
        }
        mInCamera = !mInCamera;
    }

    /**
     * 接受被挤下线消息后的操作
     */
    @Override
    public void onReceiveExitMsg() {
        super.onReceiveExitMsg();
        TXLog.d(TAG, "record activity broadcastReceiver receive exit app msg");
        //在被踢下线的情况下，执行退出前的处理操作：停止推流、关闭群组
        stopRecordAnimation();
        stopPublish();
        quitRoom();
    }

    /**
     * 强制下线消息显示
     * @param intent intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() != null && intent.getAction().equals(TCConstants.EXIT_APP)) {
            onReceiveExitMsg();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /** attention to this below ,must add this**/
        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
        com.umeng.socialize.utils.Log.d("result","onActivityResult");
    }
}
