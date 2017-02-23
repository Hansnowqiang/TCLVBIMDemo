package com.tencent.qcloud.xiaozhibo.ui;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.tencent.qcloud.xiaozhibo.base.TCConstants;
import com.tencent.qcloud.xiaozhibo.base.TCUtils;
import com.tencent.qcloud.xiaozhibo.ui.customviews.ErrorDialogFragment;

/**
 * base activity to handle relogin info
 * Created by Administrator on 2016/9/20
 */
public class TCBaseActivity extends Activity {

    private static final String TAG = TCBaseActivity.class.getSimpleName();

    //错误消息弹窗
    private ErrorDialogFragment mErrDlgFragment;


    //被踢下线广播监听
    private LocalBroadcastManager mLocalBroadcatManager;
    private BroadcastReceiver mExitBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocalBroadcatManager = LocalBroadcastManager.getInstance(this);
        mExitBroadcastReceiver = new ExitBroadcastRecevier();
        mLocalBroadcatManager.registerReceiver(mExitBroadcastReceiver, new IntentFilter(TCConstants.EXIT_APP));

        mErrDlgFragment = new ErrorDialogFragment();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocalBroadcatManager.unregisterReceiver(mExitBroadcastReceiver);
    }

    public void onReceiveExitMsg() {
        TCUtils.showKickOutDialog(this);
    }

    public class ExitBroadcastRecevier extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TCConstants.EXIT_APP)) {
                //在被踢下线的情况下，执行退出前的处理操作：停止推流、关闭群组
                onReceiveExitMsg();
            }
        }
    }

    protected void showErrorAndQuit(String errorMsg) {

        if (!mErrDlgFragment.isAdded() && !this.isFinishing()) {
            Bundle args = new Bundle();
            args.putString("errorMsg", errorMsg);
            mErrDlgFragment.setArguments(args);
            mErrDlgFragment.setCancelable(false);

            //此处不使用用.show(...)的方式加载dialogfragment，避免IllegalStateException
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.add(mErrDlgFragment, "loading");
            transaction.commitAllowingStateLoss();
        }
    }
}
