package com.tencent.qcloud.xiaozhibo.logic;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.tencent.TIMGroupSettings;
import com.tencent.TIMManager;
import com.tencent.TIMUserStatusListener;
import com.tencent.qcloud.xiaozhibo.base.TCConstants;

/**
 * IMSDK init操作
 */
public class TCIMInitMgr {

    //标志位确定SDK是否初始化，避免客户SDK未初始化的情况，实现可重入的init操作
    private static boolean isSDKInit = false;

    /**
     * IMSDK init操作
     * @param context application context
     */
    public static void init(final Context context) {

        if (isSDKInit)
            return;

        //禁止服务器自动代替上报已读
        TIMManager.getInstance().disableAutoReport();
        //初始化imsdk
        TIMManager.getInstance().init(context);
        //初始化群设置
        TIMManager.getInstance().initGroupSettings(new TIMGroupSettings());
        //注册sig失效监听回调
        TIMManager.getInstance().setUserStatusListener(new TIMUserStatusListener() {
            @Override
            public void onForceOffline() {
                LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(new Intent(TCConstants.EXIT_APP));
            }

            @Override
            public void onUserSigExpired() {
                TCLoginMgr.getInstance().reLogin();
            }
        });

        //初始化登录模块
        TCLoginMgr.getInstance().init(context);
        //初始化注册模块
        TCRegisterMgr.getInstance().init(context);
        //禁用消息存储
        //TIMManager.getInstance().disableStorage();

        isSDKInit = true;
    }
}
