package com.tencent.qcloud.xiaozhibo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.tencent.qcloud.xiaozhibo.R;
import com.tencent.qcloud.xiaozhibo.base.TCConstants;
import com.tencent.qcloud.xiaozhibo.logic.TCLiveInfo;
import com.tencent.qcloud.xiaozhibo.logic.TCLiveListMgr;
import com.tencent.qcloud.xiaozhibo.logic.TCLiveListAdapter;

import java.util.ArrayList;


/**
 * 直播列表页面，展示当前直播及回放视频
 * 界面展示使用：ListView+SwipeRefreshLayout
 * 列表数据Adapter：TCLiveListAdapter
 * 数据获取接口： TCLiveListMgr
 */
public class TCLiveListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{
    public static final int START_LIVE_PLAY = 100;
    private static final String TAG = "TCLiveListFragment";
    private ListView mVideoListView;
    private TCLiveListAdapter mVideoListViewAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    //避免连击
    private long mLastClickTime = 0;

    public TCLiveListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videolist, container, false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout_list);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mVideoListView = (ListView) view.findViewById(R.id.live_list);
        mVideoListViewAdapter = new TCLiveListAdapter(getActivity(), (ArrayList<TCLiveInfo>) TCLiveListMgr.getInstance().getLiveList().clone());
        mVideoListView.setAdapter(mVideoListViewAdapter);
        mVideoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    if (i >= mVideoListViewAdapter.getCount()) {
                        return;
                    }
                    if (0 == mLastClickTime || System.currentTimeMillis() - mLastClickTime > 1000) {
                        TCLiveInfo item = mVideoListViewAdapter.getItem(i);
                        if (item == null) {
                            Log.e(TAG, "live list item is null at position:" + i);
                            return;
                        }

                        startLivePlay(item);
                    }
                    mLastClickTime = System.currentTimeMillis();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        refreshListView();

        return view;
    }

    @Override
    public void onRefresh() {
        refreshListView();
    }

    /**
     * 刷新直播列表
     */
    private void refreshListView() {
        if (reloadLiveList()) {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode,data);

        try {
            if (START_LIVE_PLAY == requestCode) {
                if (0 != resultCode) {
                    //观看直播返回错误信息后，刷新列表，但是不显示动画
                    reloadLiveList();
                } else {
                    if (data == null) {
                        return;
                    }
                    //更新列表项的观看人数和点赞数
                    String userId = data.getStringExtra(TCConstants.PUSHER_ID);
                    for (int i = 0; i < mVideoListViewAdapter.getCount(); i++) {
                        TCLiveInfo info = mVideoListViewAdapter.getItem(i);
                        if (info != null && info.userid.equalsIgnoreCase(userId)) {
                            info.viewercount = (int) data.getLongExtra(TCConstants.MEMBER_COUNT, info.viewercount);
                            info.likecount = (int) data.getLongExtra(TCConstants.HEART_COUNT, info.likecount);
                            mVideoListViewAdapter.notifyDataSetChanged();
                            break;
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 重新加载直播列表
     */
    private boolean reloadLiveList() {
        return TCLiveListMgr.getInstance().reloadLiveList(new TCLiveListMgr.Listener() {
            @Override
            public void onLiveList(int retCode, final ArrayList<TCLiveInfo> result, boolean refresh) {
                if (retCode == 0 ) {
                    mVideoListViewAdapter.clear();
                    if (result != null) {
                        mVideoListViewAdapter.addAll((ArrayList<TCLiveInfo>)result.clone());
                    }
                    if (refresh) {
                        mVideoListViewAdapter.notifyDataSetChanged();
                    }
                } else {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "刷新列表失败", Toast.LENGTH_LONG).show();
                    }
                }
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    /**
     * 开始播放视频
     * @param item 视频数据
     */
    private void startLivePlay(final TCLiveInfo item) {
        Intent intent;
        if (TCConstants.TX_ENABLE_LINK_MIC) {
            intent = new Intent(getActivity(), TCLinkMicLivePlayActivity.class);
        }
        else {
            intent = new Intent(getActivity(), TCLivePlayerActivity.class);
        }
        intent.putExtra(TCConstants.PUSHER_ID, item.userid);
        intent.putExtra(TCConstants.PLAY_URL, item.playurl);
        intent.putExtra(TCConstants.PUSHER_NAME, item.userinfo.nickname == null ? item.userid : item.userinfo.nickname);
        intent.putExtra(TCConstants.PUSHER_AVATAR, item.userinfo.headpic);
        intent.putExtra(TCConstants.HEART_COUNT, "" + item.likecount);
        intent.putExtra(TCConstants.MEMBER_COUNT, "" + item.viewercount);
        intent.putExtra(TCConstants.GROUP_ID, item.groupid);
        intent.putExtra(TCConstants.PLAY_TYPE, item.type);
        intent.putExtra(TCConstants.FILE_ID, item.fileid);
        intent.putExtra(TCConstants.COVER_PIC, item.userinfo.frontcover);
        intent.putExtra(TCConstants.TIMESTAMP, item.timestamp);
        intent.putExtra(TCConstants.ROOM_TITLE, item.title);
        startActivityForResult(intent,START_LIVE_PLAY);
    }

}