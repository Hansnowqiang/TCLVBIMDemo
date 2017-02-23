package com.tencent.qcloud.xiaozhibo.logic;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.qcloud.xiaozhibo.base.TCHttpEngine;
import com.tencent.rtmp.TXLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.List;


/**
 * 播放端后台Mgr
 */
public class TCPlayerMgr {
    private static final String TAG = TCPlayerMgr.class.getSimpleName();

    public static final int TCLiveListItemType_Live = 0;
    public static final int TCLiveListItemType_Record = 1;
    public PlayerListener mPlayerListener;

    private TCPlayerMgr() {
    }

    private static class TCPlayerMgrHolder {
        private static TCPlayerMgr instance = new TCPlayerMgr();
    }

    public static TCPlayerMgr getInstance() {
        return TCPlayerMgrHolder.instance;
    }

    public void setPlayerListener(PlayerListener playerListener) {
        mPlayerListener = playerListener;
    }


    /**
     * 发送点赞信息
     * @param mUserId 主播ID
     */
    public void addHeartCount(String mUserId) {
        internalSendRequest(mUserId, 1, 0, 0, null);
    }

    /**
     * 后台发送请求接口
     * @param userId 用户id
     * @param type:0：修改观看数量 1：修改点赞数量
     * @param optype 0：增加 1：减少
     * @param flag 0：直播 1：点播
     * @param fileId 文件id，在点播情况下使用，直播情况填null
     */
    private void internalSendRequest(String userId, int type, int optype, int flag, String fileId) {
        try {
            final JSONObject req = new JSONObject();
            req.put("Action", "ChangeCount");
            req.put("userid", userId);
            req.put("type", type);
            req.put("optype", optype);
            req.put("flag", flag);
            req.put("fileid", fileId == null ? "" : fileId);

            if (type == 0 && optype == 1) {
                TCHttpEngine.getInstance().post(req, new TCHttpEngine.Listener() {
                    @Override
                    public void onResponse(int retCode, String retMsg, JSONObject retData) {
                        if (mPlayerListener != null) {
                            mPlayerListener.onRequestCallback(retCode);
                        }
                        mPlayerListener = null;
                    }
                });
            } else {
                TCHttpEngine.getInstance().post(req, new TCHttpEngine.Listener() {
                    @Override
                    public void onResponse(int retCode, String retMsg, JSONObject retData) {
                        if (mPlayerListener != null) {
                            mPlayerListener.onRequestCallback(retCode);
                        }
                    }
                });
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 举报接口
     * @param userId 举报人id
     * @param hostId 被举报人id
     */
    public void reportUser(String userId, String hostId) {
        try {
            JSONObject req = new JSONObject();
            req.put("Action", "ReportUser");
            req.put("userid", userId);
            req.put("hostuserid", hostId);

            TCHttpEngine.getInstance().post(req, new TCHttpEngine.Listener() {
                @Override
                public void onResponse(int retCode, String retMsg, JSONObject retData) {
                    TXLog.d(TAG, "ReportUser: retCode --" + retCode + "|" + retMsg);
                    if (mPlayerListener != null)
                        mPlayerListener.onRequestCallback(retCode);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param userId 用户ID
     * @param liveUserId 主播ID
     * @param groupId 群组ID
     * @param nickname 昵称
     * @param headPic 头像链接
     * @param flag 直播/点播 直播--0 点播--1
     */
    public void enterGroup(String userId ,String liveUserId ,String groupId ,
                           String nickname ,String headPic, int flag) {
        try {
            JSONObject req = new JSONObject();
            req.put("Action", "EnterGroup");
            req.put("userid", userId);
            req.put("liveuserid", liveUserId);
            req.put("groupid", groupId);
            req.put("nickname", nickname);
            req.put("headpic", headPic);
            req.put("flag", flag);

            TCHttpEngine.getInstance().post(req, new TCHttpEngine.Listener() {
                @Override
                public void onResponse(int retCode, String retMsg, JSONObject retData) {
                    TXLog.d(TAG, "EnterGroup: retCode --" + retCode + "|" + retMsg);
                    if (mPlayerListener != null) {
                        mPlayerListener.onRequestCallback(retCode);
                    }
                    mPlayerListener = null;
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 退出群组
     * @param userId 用户ID
     * @param liveUserId 主播ID
     * @param groupId 群组ID
     * @param flag 直播/点播 直播--0 点播--1
     */
    public void quitGroup(String userId, String liveUserId, String groupId, int flag) {
        try {
            JSONObject req = new JSONObject();
            req.put("Action", "QuitGroup");
            req.put("userid", userId);
            req.put("flag", flag);
            req.put("liveuserid", liveUserId);
            req.put("groupid", groupId);

            TCHttpEngine.getInstance().post(req, new TCHttpEngine.Listener() {
                @Override
                public void onResponse(int retCode, String retMsg, JSONObject retData) {
                    TXLog.d(TAG, "QuitGroup: retCode --" + retCode + "|" + retMsg);
                    if (mPlayerListener != null)
                        mPlayerListener.onRequestCallback(retCode);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 拉取观众列表
     * @param liveUserId 主播ID
     * @param groupId 群组ID
     * @param pageNo 页码，从1开始
     * @param pageSize 页的大小
     * @param listener 回调
     */
    public void fetchGroupMembersList(String liveUserId, String groupId, int pageNo, int pageSize, final OnGetMembersListener listener) {
        try {
            JSONObject req = new JSONObject();
            req.put("Action", "FetchGroupMemberList");
            req.put("liveuserid", liveUserId);
            req.put("groupid", groupId);
            req.put("pageno", pageNo);
            req.put("pagesize", pageSize);

            TCHttpEngine.getInstance().post(req, new TCHttpEngine.Listener() {
                @Override
                public void onResponse(int retCode, String retMsg, JSONObject retData) {
                    if (retCode == 0 && retData != null) {
                        try {
                            int totalcount = retData.getInt("totalcount");
                            JSONArray record = retData.getJSONArray("memberlist");
                            Type listType = new TypeToken<List<TCSimpleUserInfo>>() {}.getType();
                            List<TCSimpleUserInfo> infos = new Gson().fromJson(record.toString(), listType);
                            if (listener != null) {
                                listener.onGetMembersList(retCode, totalcount, infos);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (listener != null) {
                            listener.onGetMembersList(retCode, -1, null);
                        }
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取观看用户列表
     */
    public interface OnGetMembersListener {

        void onGetMembersList(int retCode, int totalCount, List<TCSimpleUserInfo> membersList);
    }

    /**
     * request回调
     */
    public interface PlayerListener {

        void onRequestCallback(int errCode);

    }

    /**
     * 获取带防盗链签名的拉流地址：主播和连麦观众在拉取对方的视频流时，必须带上防盗链签名
     * @param userId 用户ID
     * @param strPlayUrl 原始拉流地址（未带防盗链签名）
     */
    public void getPlayUrlWithSignature(final String userId, final String strPlayUrl, final OnGetPlayUrlWithSignature listener) {
        try {
            JSONObject req = new JSONObject();
            req.put("Action", "RequestPlayUrlWithSignForLinkMic");
            req.put("userid", userId);
            req.put("originStreamUrl", strPlayUrl);

            TCHttpEngine.getInstance().post(req, new TCHttpEngine.Listener() {
                @Override
                public void onResponse(int retCode, String retMsg, JSONObject retData) {
                    String playUrl = "";
                    try {
                        if (retCode == 0) {
                            playUrl = retData.getString("streamUrlWithSignature");
    }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (null != listener)
                        listener.onGetPlayUrlWithSignature(retCode,playUrl);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (null != listener)
                listener.onGetPlayUrlWithSignature(-1, null);
        }
    }

    public interface OnGetPlayUrlWithSignature {
        /**
         * 连麦专用：得到带防盗链签名的拉流地址
         * @param strPlayUrl 带防盗链签名的拉流地址
         */
        void onGetPlayUrlWithSignature(int errCode, String strPlayUrl);
}

}
