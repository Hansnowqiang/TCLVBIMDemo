package com.tencent.qcloud.xiaozhibo.logic;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.qcloud.xiaozhibo.base.TCHttpEngine;
import com.tencent.rtmp.TXLog;

import org.json.JSONArray;
import org.json.JSONObject;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class TCLiveListMgr {
    private static final String TAG = TCLiveListMgr.class.getSimpleName();
    private static final int PAGESIZE = 20;
    private static final int LIST_TYPE_LIVE = 1;
    private static final int LIST_TYPE_VOD  = 2;
    private static final int LIST_TYPE_ALL  = 3;
    private boolean mIsFetching;

    private ArrayList<TCLiveInfo> mLiveInfoList = new ArrayList<>();

    private TCLiveListMgr() {
        mIsFetching = false;
    }

    private static class TCLiveListMgrHolder {
        private static TCLiveListMgr instance = new TCLiveListMgr();
    }

    public static TCLiveListMgr getInstance() {
        return TCLiveListMgrHolder.instance;
    }

    /**
     * 获取内存中缓存的直播列表
     * @return 完整列表
     */
    public ArrayList<TCLiveInfo> getLiveList() {
        return mLiveInfoList;
    }

    /**
     * 分页获取完整直播列表
     * @param listener 列表回调，每获取到一页数据回调一次
     */
    public boolean reloadLiveList(Listener listener) {
        if (mIsFetching) {
            TXLog.w(TAG,"reloadLiveList ignore when fetching");
            return false;
        }
        TXLog.d(TAG,"fetchLiveList start");
        mLiveInfoList.clear();
        fetchLiveList(LIST_TYPE_ALL, 1, PAGESIZE, listener);
        mIsFetching = true;
        return true;
    }

    /**
     * 获取直播列表
     *
     * @param type     1:拉取在线直播列表 2:拉取7天内录播列表 3:拉取在线直播和7天内录播列表，直播列表在前，录播列表在后
     * @param pageNo   页数
     * @param pageSize 每页个数
     */
    private void fetchLiveList(final int type, final int pageNo, final int pageSize, final Listener listener) {
        JSONObject req = new JSONObject();
        try {
            req.put("flag", type);
            req.put("pageno", pageNo);
            req.put("pagesize", pageSize);
            req.put("Action","FetchList");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG,"fetchLiveList type:"+type+",pagNo:"+pageNo+",pageSize:"+pageSize);
        TCHttpEngine.getInstance().post(req, new TCHttpEngine.Listener() {
            @Override
            public void onResponse(int retCode, String retMsg, JSONObject retData) {
                if (retCode == 0 && retData != null) {
                    try {
                        int totalcount = retData.getInt("totalcount");
                        JSONArray record = retData.getJSONArray("pusherlist");
                        Type listType = new TypeToken<ArrayList<TCLiveInfo>>() {
                        }.getType();
                        ArrayList<TCLiveInfo> result = new Gson().fromJson(record.toString(), listType);
                        if (result != null && !result.isEmpty()) {
                            Log.d(TAG,"fetchLiveList result,totalCount:"+totalcount+",curCount:"+result.size());
                            mLiveInfoList.addAll(result);

                            if (mLiveInfoList.size() >= totalcount || pageNo*PAGESIZE >= totalcount) {
                                TXLog.d(TAG,"fetchLiveList finish count:"+totalcount);
                                mIsFetching = false;
                            } else {
                                fetchLiveList(type, pageNo+1, PAGESIZE, listener);
                            }
                            if (listener != null) {
                                listener.onLiveList(retCode,mLiveInfoList,pageNo == 1);
                            }
                            return;
                        } else {
                            TXLog.w(TAG,"fetchLiveList broken result,retCode:"+retCode+",retMsg:"+retMsg);
                            mIsFetching = false;
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (listener != null) {
                    listener.onLiveList(retCode,null,true);
                }
                mIsFetching = false;
            }
        });
    }

    /**
     * 视频列表获取结果回调
     */
    public interface Listener {
        /**
         * @param retCode 获取结果，0表示成功
         * @param result  列表数据
         * @param refresh 是否需要刷新界面，首页需要刷新
         */
        public void onLiveList(int retCode, final ArrayList<TCLiveInfo> result, boolean refresh);
    }
}

