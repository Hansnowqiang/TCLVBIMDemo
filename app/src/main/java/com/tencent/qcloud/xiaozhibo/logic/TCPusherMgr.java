package com.tencent.qcloud.xiaozhibo.logic;

import com.tencent.qcloud.xiaozhibo.base.TCHttpEngine;

import org.json.JSONObject;


public class TCPusherMgr {

    public static final int TCLiveStatus_Online                = 0;
    public static final int TCLiveStatus_Offline               = 1;

    private static final String TAG = TCPusherMgr.class.getSimpleName();
    public PusherListener mPusherListener;

    private TCPusherMgr() {

    }

    private static class TCPusherMgrHolder {
        private static TCPusherMgr instance = new TCPusherMgr();
    }

    public static TCPusherMgr getInstance() {
        return TCPusherMgrHolder.instance;
    }

    public void setPusherListener(PusherListener pusherListener) {
        mPusherListener = pusherListener;
    }


    /**
     * 更改直播状态
     * @param userId 主播ID
     * @param status 状态 TCLiveStatus_Online = 0; TCLiveStatus_Offline = 1;

     */
    public void changeLiveStatus(String userId, int status) {
        try {
            JSONObject req = new JSONObject();
            req.put("Action", "ChangeStatus");
            req.put("userid", userId);
            req.put("status", status);

            TCHttpEngine.getInstance().post(req, new TCHttpEngine.Listener() {
                @Override
                public void onResponse(int retCode, String retMsg, JSONObject retData) {
                    if (null != mPusherListener) {
                        mPusherListener.onChangeLiveStatus(retCode);
                    }

                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            if (null != mPusherListener) {
                mPusherListener.onChangeLiveStatus(-1);
            }
        }
    }

    /**
     * 获取推流url
     * @param userId 主播ID
     * @param groupId 群组ID
     * @param title 直播标题
     * @param coverPic 直播封面
     * @param nickName 主播昵称
     * @param headPic 主播头像
     * @param location 主播地理位置
     */
    public void getPusherUrl(final String userId, final String groupId, final String title,
                                     final String coverPic, final String nickName, final String headPic, final String location) {
        try {
            JSONObject userInfo = new JSONObject();
            userInfo.put("nickname", nickName);
            userInfo.put("headpic", headPic);
            userInfo.put("frontcover", coverPic);
            userInfo.put("location", location);

            JSONObject req = new JSONObject();
            req.put("Action", "RequestLVBAddr");
            req.put("userid", userId);
            req.put("groupid", groupId);
            req.put("title", title);
            req.put("userinfo", userInfo);

            TCHttpEngine.getInstance().post(req, new TCHttpEngine.Listener() {
                @Override
                public void onResponse(int retCode, String retMsg, JSONObject retData) {
                    String pusherUrl = "";
                    String timestamp = "";
                    try {
                        if (retCode == 0) {
                            pusherUrl = retData.getString("pushurl");
                            timestamp = retData.getString("timestamp");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (null != mPusherListener)
                        mPusherListener.onGetPusherUrl(retCode, groupId, pusherUrl, timestamp);
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
            if (null != mPusherListener)
                mPusherListener.onGetPusherUrl(-1, groupId, null, null);
        }
    }


    public interface PusherListener {

        /**
        *   创建群组失败时，groupId，pusherUrl为null；
        *   创建群组成功，拉取推流URL失败时，groupId为正常值，pusherUrl为null或空string；
        **/
        void onGetPusherUrl(int errCode, String groupId, String pusherUrl, String timeStamp);

        void onChangeLiveStatus(int errCode);


    }


    /**
     * 获取连麦推流url
     * @param userId 主播ID
     * @param title 直播标题
     * @param coverPic 直播封面
     * @param nickName 主播昵称
     * @param headPic 主播头像
     * @param location 主播地理位置
     */
    public void getPusherUrlForLinkMic(final String userId, final String title,
                                       final String coverPic, final String nickName, final String headPic, final String location, final OnGetPusherUrlForLinkMic listener) {
        try {
            JSONObject userInfo = new JSONObject();
            userInfo.put("nickname", nickName);
            userInfo.put("headpic", headPic);
            userInfo.put("frontcover", coverPic);
            userInfo.put("location", location);

            JSONObject req = new JSONObject();
            req.put("Action", "RequestLVBAddrForLinkMic");
            req.put("userid", userId);
            req.put("title", title);
            req.put("userinfo", userInfo);

            TCHttpEngine.getInstance().post(req, new TCHttpEngine.Listener() {
                @Override
                public void onResponse(int retCode, String retMsg, JSONObject retData) {
                    String pusherUrl = "";
                    String timestamp = "";
                    String playUrl   = "";
                    try {
                        if (retCode == 0) {
                            pusherUrl = retData.getString("pushurl");
                            timestamp = retData.getString("timestamp");
                            playUrl   = retData.getString("playurl");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (null != listener)
                        listener.onGetPusherUrlForLinkMic(retCode, pusherUrl, playUrl, timestamp);
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
            if (null != listener)
                listener.onGetPusherUrlForLinkMic(-1, null, null, null);
        }
    }

    public interface OnGetPusherUrlForLinkMic {
        /**
         * 获取到连麦推流地址
         * @param errCode
         * @param pusherUrl
         * @param timeStamp
         */
        void onGetPusherUrlForLinkMic(int errCode, String pusherUrl, String playUrl, String timeStamp);
    }

}
