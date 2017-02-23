package com.tencent.qcloud.xiaozhibo.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;

import com.tencent.TIMManager;
import com.tencent.qcloud.xiaozhibo.R;
import com.tencent.qcloud.xiaozhibo.base.TCConstants;
import com.tencent.qcloud.xiaozhibo.base.TCUtils;
import com.tencent.qcloud.xiaozhibo.logic.TCLoginMgr;
import com.tencent.qcloud.xiaozhibo.logic.TCUserInfoMgr;

import tencent.tls.platform.TLSUserInfo;

/**
 * 主界面，包括直播列表，用户信息页
 * UI使用FragmentTabHost+Fragment
 * 直播列表：TCLiveListFragment
 * 个人信息页：TCUserInfoFragment
 */
public class TCMainActivity extends FragmentActivity {
    private static final String TAG = TCMainActivity.class.getSimpleName();

    //被踢下线广播监听
    private LocalBroadcastManager mLocalBroadcatManager;
    private BroadcastReceiver mExitBroadcastReceiver;

    private FragmentTabHost mTabHost;
    private LayoutInflater mLayoutInflater;
    private final Class mFragmentArray[] = {TCLiveListFragment.class, TCLiveListFragment.class, TCUserInfoFragment.class};
    private int mImageViewArray[] = {R.drawable.tab_video, R.drawable.play_click, R.drawable.tab_user};
    private String mTextviewArray[] = {"video", "publish", "user"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        mLayoutInflater = LayoutInflater.from(this);
        mTabHost.setup(this, getSupportFragmentManager(), R.id.contentPanel);

        int fragmentCount = mFragmentArray.length;
        for (int i = 0; i < fragmentCount; i++) {
            TabHost.TabSpec tabSpec = mTabHost.newTabSpec(mTextviewArray[i]).setIndicator(getTabItemView(i));
            mTabHost.addTab(tabSpec, mFragmentArray[i], null);
            mTabHost.getTabWidget().setDividerDrawable(null);
        }
        mTabHost.getTabWidget().getChildTabViewAt(1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TCMainActivity.this, TCPublishSettingActivity.class));
            }
        });

        mLocalBroadcatManager = LocalBroadcastManager.getInstance(this);
        mExitBroadcastReceiver = new ExitBroadcastRecevier();
        mLocalBroadcatManager.registerReceiver(mExitBroadcastReceiver, new IntentFilter(TCConstants.EXIT_APP));

        Log.w("TCLog","mainactivity oncreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.w("TCLog","mainactivity onstart");
        if (TextUtils.isEmpty(TIMManager.getInstance().getLoginUser())) {
            //relogin
            final TCLoginMgr tcLoginMgr = TCLoginMgr.getInstance();
            final TLSUserInfo userInfo = TCLoginMgr.getInstance().getLastUserInfo();
            tcLoginMgr.setTCLoginCallback(new TCLoginMgr.TCLoginCallback() {
                @Override
                public void onSuccess() {
                    tcLoginMgr.removeTCLoginCallback();
                    TCUserInfoMgr.getInstance().setUserId(userInfo.identifier, null);
                }

                @Override
                public void onFailure(int code, String msg) {
                    tcLoginMgr.removeTCLoginCallback();
                }
            });

            tcLoginMgr.checkCacheAndLogin();
            Log.w("TCLog","mainactivity onstart relogin");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocalBroadcatManager.unregisterReceiver(mExitBroadcastReceiver);
    }

    public class ExitBroadcastRecevier extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TCConstants.EXIT_APP)) {
                onReceiveExitMsg();
            }
        }
    }

    public void onReceiveExitMsg() {
        TCUtils.showKickOutDialog(this);
    }

    /**
     * 动态获取tabicon
     * @param index tab index
     * @return
     */
    private View getTabItemView(int index) {
        View view;
        if (index % 2 == 0) {
            view = mLayoutInflater.inflate(R.layout.tab_button1, null);
        } else {
            view = mLayoutInflater.inflate(R.layout.tab_button, null);
        }
        ImageView icon = (ImageView) view.findViewById(R.id.tab_icon);
        icon.setImageResource(mImageViewArray[index]);
        return view;
    }

}
