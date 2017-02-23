package com.tencent.qcloud.xiaozhibo.logic;

public class TCLiveInfo {
    public String   userid;
    public String   groupid;
    public int      timestamp;
    public int      type;
    public int      viewercount;
    public int      likecount;
    public String   title;
    public String   playurl;
    public String   fileid;

    //TCLiveUserInfo
    public TCLiveUserInfo userinfo;


    public class TCLiveUserInfo {
        public String nickname;
        public String headpic;
        public String frontcover;
        public String location;
    }
}
