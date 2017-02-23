package com.tencent.qcloud.xiaozhibo.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;

import java.lang.ref.WeakReference;

/**
 * Created by RTMP on 2016/8/1
 */
public class TCSplashActivity extends Activity /*implements TCLoginMgr.TCLoginCallback*/ {

    private static final String TAG = TCSplashActivity.class.getSimpleName();

    private static final int START_LOGIN = 2873;
    private final MyHandler mHandler = new MyHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isTaskRoot()
                && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
                && getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_MAIN)) {

            finish();
            return;
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Message msg = Message.obtain();
        msg.arg1 = START_LOGIN;
        mHandler.sendMessageDelayed(msg, 1000);
        //TCLoginMgr.getInstance().setTCLoginCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //login();
    }

    @Override
    public void onBackPressed() {
        //splashActivity下不允许back键退出
        //super.onBackPressed();
    }

//    public void login() {
//        //判断网络环境
//        if(TCUtils.isNetworkAvailable(this)) {
//            TCLoginMgr.getInstance().checkCacheAndLogin();
//        } else {
//            //无网状态下转入登录界面
//            TCLoginMgr.getInstance().removeTCLoginCallback();
//            jumpToLoginActivity();
//        }
//    }

    private void jumpToLoginActivity() {
        Intent intent = new Intent(this, TCLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private static class MyHandler extends Handler {
        private final WeakReference<TCSplashActivity> mActivity;

        public MyHandler(TCSplashActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            TCSplashActivity activity = mActivity.get();
            if (activity != null) {
                activity.jumpToLoginActivity();
            }
        }
    }

    //为了便于阅读，将登录态检测的方法置于TCLoginActivity
//    private void jumpToMainActivity() {
//        Intent intent = new Intent(this, TCMainActivity.class);
//        startActivity(intent);
//        finish();
//    }

//    /**
//     * IMSDK登录成功
//     */
//    @Override
//    public void onSuccess() {
//        //登录成功跳转至主界面
//        Log.d(TAG, "already has cache, jump into login activity");
//        TCUserInfoMgr.getInstance().setUserId(TCLoginMgr.getInstance().getLastUserInfo().identifier, null);
//        TCLoginMgr.getInstance().removeTCLoginCallback();
//        jumpToMainActivity();
//    }
//
//    /**
//     * IMSDK登录失败
//     *
//     * @param code 错误码
//     * @param msg  错误信息
//     */
//    @Override
//    public void onFailure(int code, String msg) {
//        Log.d(TAG, "already has cache, but imsdk login fail");
//        //登录失败，跳转至login界面
//        TCLoginMgr.getInstance().removeTCLoginCallback();
//        jumpToLoginActivity();
//    }
}
