package com.tencent.qcloud.xiaozhibo.logic;

import android.os.Build;
import android.util.Log;

import com.tencent.TIMConversation;
import com.tencent.TIMConversationType;
import com.tencent.TIMElem;
import com.tencent.TIMElemType;
import com.tencent.TIMManager;
import com.tencent.TIMMessage;
import com.tencent.TIMMessageListener;
import com.tencent.TIMTextElem;
import com.tencent.TIMValueCallBack;
import com.tencent.qcloud.xiaozhibo.base.TCConstants;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.List;

public class TCLinkMicMgr implements TIMMessageListener{
    public static final String TAG = TCLinkMicMgr.class.getSimpleName();

    private TCLinkMicListener mLinkMicListener;

    private TCLinkMicMgr() {
        TIMManager.getInstance().addMessageListener(this);
    }

    private static class TCLinkMicMgrHolder {
        private static TCLinkMicMgr instance = new TCLinkMicMgr();
    }

    public static TCLinkMicMgr getInstance() {
       return TCLinkMicMgrHolder.instance;
    }

    public static boolean supportLinkMic() {
        return Build.VERSION.SDK_INT >= 18;
    }

    public void setLinkMicListener(TCLinkMicListener listener) {
        mLinkMicListener = listener;
    }

    public void sendLinkMicRequest(String strToUserId) {
        sendMessage(strToUserId, TCConstants.LINKMIC_CMD_REQUEST, "");
    }

    public void sendLinkMicResponse(String strToUserId, int responseType, String param) {
        try {
            int cmd = -1;
            JSONObject json = new JSONObject();
            switch (responseType) {
                case TCConstants.LINKMIC_RESPONSE_TYPE_ACCEPT:
                    cmd = TCConstants.LINKMIC_CMD_ACCEPT;
                    json.put("sessionID", param);
                    break;

                case TCConstants.LINKMIC_RESPONSE_TYPE_REJECT:
                    cmd = TCConstants.LINKMIC_CMD_REJECT;
                    json.put("reason", param);
                    break;

                default:
                    break;
            }

            if (cmd != -1) {
                sendMessage(strToUserId, cmd, json.toString());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMemberJoinNotify(String strToUserId, String strJoinerID,  String playUrl) {
        try {
            JSONObject json = new JSONObject();
            json.put("joinerID", strJoinerID);
            json.put("playUrl", playUrl);
            sendMessage(strToUserId, TCConstants.LINKMIC_CMD_MEMBER_JOIN_NOTIFY, json.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMemberExitNotify(String strToUserId, String strExiterID) {
        try {
            JSONObject json = new JSONObject();
            json.put("exiterID", strExiterID);
            sendMessage(strToUserId, TCConstants.LINKMIC_CMD_MEMBER_EXIT_NOTIFY, json.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void kickOutLinkMicMember(String strToUserId) {
        sendMessage(strToUserId, TCConstants.LINKMIC_CMD_KICK_MEMBER, "");
    }

    private void sendMessage(final String strUserId, final int cmd, String param) {
        sendMessage(strUserId, cmd, param, new TIMValueCallBack<TIMMessage>() {
            @Override
            public void onError(int i, String s) {
                Log.d(TAG, "sendMessage failed, cmd = " + cmd + " toUserId = " + strUserId);
            }

            @Override
            public void onSuccess(TIMMessage timMessage) {
                Log.d(TAG, "sendMessage success, cmd = " + cmd + " toUserId = " + strUserId);
            }
        });
    }

    private void sendMessage(String strUserId, int cmd, String param, TIMValueCallBack<TIMMessage> timValueCallBack) {
        JSONObject sendJson = new JSONObject();
        try {
            sendJson.put("userAction", cmd);
            sendJson.put("userId", TCLoginMgr.getInstance().getLastUserInfo().identifier);
            sendJson.put("nickName", TCUserInfoMgr.getInstance().getNickname());
            sendJson.put("param", param);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        TIMTextElem elem = new TIMTextElem();
        elem.setText(sendJson.toString());

        TIMMessage msg = new TIMMessage();
        msg.addElement(elem);

        sendTIMMessage(strUserId, msg, timValueCallBack);
    }

    /**
     * 发送消息
     * @param msg              TIM消息
     * @param timValueCallBack 发送消息回调类
     */
    private void sendTIMMessage(String strUserId, TIMMessage msg, TIMValueCallBack<TIMMessage> timValueCallBack) {
        TIMConversation conversion = TIMManager.getInstance().getConversation(TIMConversationType.C2C, strUserId);
        if (conversion != null) {
            conversion.sendMessage(msg, timValueCallBack);
        }
        else {
            Log.e(TAG, "TIMManager GetConversation failed");
        }
    }

    @Override
    public boolean onNewMessages(List<TIMMessage> list) {
        if (mLinkMicListener == null) {
            return false;
        }

        for (int i = list.size() - 1; i >= 0; i--) {
            TIMMessage currMsg = list.get(i);
            for (int j = 0; j < currMsg.getElementCount(); j++) {
                if (currMsg.getElement(j) == null)  {
                    continue;
                }

                TIMElem elem = currMsg.getElement(j);
                TIMElemType type = elem.getType();
                String sendId = currMsg.getSender();

                if (sendId.equals(TCLoginMgr.getInstance().getLastUserInfo().identifier)) {
                    Log.d(TAG, "recevie a self-msg type:" + type.name());
                    continue;
                }

                if (type != TIMElemType.Text) {
                    continue;
                }

                try {
                    String jsonString = ((TIMTextElem) elem).getText();
                    Log.i(TAG, "receive linkmic message: " + jsonString);

                    JSONTokener jsonParser = new JSONTokener(jsonString);
                    JSONObject json = (JSONObject) jsonParser.nextValue();
                    int action = (int) json.get("userAction");
                    String userId = (String) json.get("userId");
                    String nickname = (String) json.get("nickName");
                    String param = (String) json.get("param");

                    if (TCConstants.LINKMIC_CMD_REQUEST == action) {
                        //收到连麦请求
                        if (supportLinkMic()) {
                            mLinkMicListener.onReceiveLinkMicRequest(userId, nickname);
                        }
                        else {
                            sendLinkMicResponse(userId, TCConstants.LINKMIC_RESPONSE_TYPE_REJECT, "主播端不支持连麦");
                        }
                    }
                    else if (TCConstants.LINKMIC_CMD_ACCEPT == action){
                        //收到允许连麦的im消息，携带者sessionId
                        String strSessionID = "";
                        if (param != null && param.length() > 0) {
                            JSONTokener tokener = new JSONTokener(param);
                            JSONObject obj = (JSONObject) tokener.nextValue();
                            strSessionID = obj.getString("sessionID");
                        }
                        if (strSessionID != null && strSessionID.length() > 0) {
                            mLinkMicListener.onReceiveLinkMicResponse(userId, TCConstants.LINKMIC_RESPONSE_TYPE_ACCEPT, strSessionID);
                        }
                        else {
                            Log.e(TAG, "recvc linkmic accept response, invalid sessionID");
                        }
                    }
                    else if (TCConstants.LINKMIC_CMD_REJECT == action){
                        //连麦被拒绝，带有拒绝原因
                        String reason = "";
                        if (param != null && param.length() > 0) {
                            JSONTokener tokener = new JSONTokener(param);
                            JSONObject obj = (JSONObject) tokener.nextValue();
                            reason = obj.getString("reason");
                        }
                        mLinkMicListener.onReceiveLinkMicResponse(userId, TCConstants.LINKMIC_RESPONSE_TYPE_REJECT, reason);
                    }
                    else if (TCConstants.LINKMIC_CMD_MEMBER_JOIN_NOTIFY == action) {
                        //连麦者开始推流
                        //连麦者id
                        String strJoinerID    = "";
                        //连麦者视频的拉流地址
                        String strPlayUrl = "";
                        if (param != null && param.length() > 0) {
                            JSONTokener tokener = new JSONTokener(param);
                            JSONObject obj = (JSONObject) tokener.nextValue();
                            strJoinerID    = obj.getString("joinerID");
                            strPlayUrl = obj.getString("playUrl");
                        }
                        if (strJoinerID.length() > 0 && strPlayUrl.length() > 0) {
                            mLinkMicListener.onReceiveMemberJoinNotify(strJoinerID, strPlayUrl);
                        }
                    }
                    else if (TCConstants.LINKMIC_CMD_MEMBER_EXIT_NOTIFY == action) {
                        //连麦者申请退出连麦
                        String strExiterID = "";
                        if (param != null && param.length() > 0) {
                            JSONTokener tokener = new JSONTokener(param);
                            JSONObject obj = (JSONObject) tokener.nextValue();
                            strExiterID = obj.getString("exiterID");
                        }
                        if (strExiterID.length() > 0) {
                            mLinkMicListener.onReceiveMemberExitNotify(strExiterID);
                        }
                    }
                    else if (TCConstants.LINKMIC_CMD_KICK_MEMBER == action) {
                        //被主播剔出连麦
                        mLinkMicListener.onReceiveKickedOutNotify();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public String getStreamIDByStreamUrl(String strStreamUrl) {
        if (strStreamUrl == null || strStreamUrl.length() == 0) {
            return null;
        }

        strStreamUrl = strStreamUrl.toLowerCase();

        //推流地址格式：rtmp://8888.livepush.myqcloud.com/live/8888_test_12345_test?txSecret=aaaa&txTime=bbbb
        //拉流地址格式：rtmp://8888.liveplay.myqcloud.com/live/8888_test_12345_test
        //            http://8888.liveplay.myqcloud.com/live/8888_test_12345_test.flv
        //            http://8888.liveplay.myqcloud.com/live/8888_test_12345_test.m3u8

        String strLive = "/live/";
        int index = strStreamUrl.indexOf(strLive);
        if (index == -1) {
            return null;
        }

        String strSubString = strStreamUrl.substring(index + strLive.length());
        String [] strArrays = strSubString.split("[?.]");
        if (strArrays.length > 0) {
            return strArrays[0];
        }

        return null;
    }

    public interface TCLinkMicListener {

        /**
         * 收到连麦请求
         * @param strUserId
         * @param strNickName
         */
        void onReceiveLinkMicRequest(final String strUserId, final String strNickName);

        /**
         * 收到连麦响应
         * @param strUserId
         * @param responseType
         * @param strParams
         */
        void onReceiveLinkMicResponse(final String strUserId, final int responseType, final String strParams);

        /**
         * 收到新成员加入连麦的通知
         */
        void onReceiveMemberJoinNotify(final String strUserId, final String strPlayUrl);

        /**
         * 收到成员退出连麦的通知
         */
        void onReceiveMemberExitNotify(final String strUserId);

        /**
         * 被主播踢出连麦
         */
        void onReceiveKickedOutNotify();
    }
}
