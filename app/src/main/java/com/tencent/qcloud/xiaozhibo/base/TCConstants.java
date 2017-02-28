package com.tencent.qcloud.xiaozhibo.base;

import com.tencent.cos.common.COSEndPoint;
/**
 * 静态函数
 */
public class TCConstants {

    //小直播相关配置请参考:https://www.qcloud.com/document/product/454/7999
    //************在腾讯云开通各项服务后，将您的配置替换到如下的几个定义中************
    //云通信服务相关配置
    public static final int IMSDK_ACCOUNT_TYPE = 6335;
    public static final int IMSDK_APPID = 1400012211;

    //COS存储服务相关配置
    public static final String COS_BUCKET = "";
    public static final String COS_APPID  = "0";
    //COS服务配置的机房区域，从COS的管理控制台https://console.qcloud.com/cos4/bucket进入Bucket列表后，选择您所创建的Bucket->基础配置->所属地区，查到所属地区后，根据如下
    //对应关系填入，如是“华南”请填写COSEndPoint.COS_GZ，“华北”请填写COSEndPoint.COS_TJ，“华东”请填写COSEndPoint.COS_SH
    public static final COSEndPoint COS_REGION = COSEndPoint.COS_SH;

    //业务Server的Http配置
    public static final String SVR_POST_URL = "http://192.168.31.128:8088/NewLiveServer/interface.php";

    //直播分享页面的跳转地址，分享到微信、手Q后点击观看将会跳转到这个地址，请参考https://www.qcloud.com/document/product/454/8046 文档部署html5的代码后，替换成相应的页面地址
    public static final String SVR_LivePlayShare_URL = "";
    //设置第三方平台的appid和appsecrect，大部分平台进行分享操作需要在第三方平台创建应用并提交审核，通过后拿到appid和appsecrect并填入这里，具体申请方式请参考http://dev.umeng.com/social/android/operation
    //有关友盟组件更多资料请参考这里：http://dev.umeng.com/social/android/quick-integration
    public static final String WEIXIN_SHARE_ID = "";
    public static final String WEIXIN_SHARE_SECRECT = "";

    public static final String SINA_WEIBO_SHARE_ID = "";
    public static final String SINA_WEIBO_SHARE_SECRECT = "";

    public static final String QQZONE_SHARE_ID = "";
    public static final String QQZONE_SHARE_SECRECT = "";


    //bugly组件Appid，bugly为腾讯提供的用于App Crash收集和分析的组件
    public static final String BUGLY_APPID = "0";
    //**********************************************************************

	
	/**
	* 常量字符串
	*/
    public static final String USER_INFO        = "user_info";
    public static final String USER_ID          = "user_id";
    public static final String USER_SIG         = "user_sig";
    public static final String USER_NICK        = "user_nick";
    public static final String USER_SIGN        = "user_sign";
    public static final String USER_HEADPIC     = "user_headpic";
    public static final String USER_COVER       = "user_cover";
    public static final String USER_LOC         = "user_location";
    public static final String SVR_RETURN_CODE  = "returnValue";
    public static final String SVR_RETURN_MSG   = "returnMsg";
    public static final String SVR_RETURN_DATA  = "returnData";

    //主播退出广播字段
    public static final String EXIT_APP         = "EXIT_APP";

    public static final int USER_INFO_MAXLEN    = 20;
    public static final int TV_TITLE_MAX_LEN    = 30;
    public static final int NICKNAME_MAX_LEN    = 20;

    //直播类型
    public static final int RECORD_TYPE_CAMERA = 991;
    public static final int RECORD_TYPE_SCREEN = 992;


    //码率
    public static final int BITRATE_SLOW = 900;
    public static final int BITRATE_NORMAL = 1200;
    public static final int BITRATE_FAST = 1600;

    //直播端右下角listview显示type
    public static final int TEXT_TYPE           = 0;
    public static final int MEMBER_ENTER        = 1;
    public static final int MEMBER_EXIT         = 2;
    public static final int PRAISE              = 3;

    public static final int LOCATION_PERMISSION_REQ_CODE = 1;
    public static final int WRITE_PERMISSION_REQ_CODE    = 2;

    public static final String PUBLISH_URL      = "publish_url";
    public static final String ROOM_ID          = "room_id";
    public static final String ROOM_TITLE       = "room_title";
    public static final String COVER_PIC        = "cover_pic";
    public static final String BITRATE          = "bitrate";
    public static final String GROUP_ID         = "group_id";
    public static final String PLAY_URL         = "play_url";
    public static final String PLAY_TYPE        = "play_type";
    public static final String PUSHER_AVATAR    = "pusher_avatar";
    public static final String PUSHER_ID        = "pusher_id";
    public static final String PUSHER_NAME        = "pusher_name";
    public static final String MEMBER_COUNT     = "member_count";
    public static final String HEART_COUNT      = "heart_count";
    public static final String FILE_ID          = "file_id";
    public static final String TIMESTAMP        = "timestamp";
    public static final String ACTIVITY_RESULT  = "activity_result";
    public static final String SHARE_PLATFORM   = "share_platform";

    public static final String CMD_KEY          = "userAction";
    public static final String DANMU_TEXT       = "actionParam";
	
	public static final String NOTIFY_QUERY_USERINFO_RESULT = "notify_query_userinfo_result";

    /**
	* IM 互动消息类型
	*/
    public static final int IMCMD_PAILN_TEXT    = 1;   // 文本消息
    public static final int IMCMD_ENTER_LIVE    = 2;   // 用户加入直播
    public static final int IMCMD_EXIT_LIVE     = 3;   // 用户退出直播
    public static final int IMCMD_PRAISE        = 4;   // 点赞消息
    public static final int IMCMD_DANMU         = 5;   // 弹幕消息


    //ERROR CODE TYPE
    public static final int ERROR_GROUP_NOT_EXIT             = 10010;
    public static final int ERROR_QALSDK_NOT_INIT             = 6013;
    public static final int ERROR_JOIN_GROUP_ERROR           = 10015;
    public static final int SERVER_NOT_RESPONSE_CREATE_ROOM  = 1002;
    public static final int NO_LOGIN_CACHE  = 1265;


	/**
	* 用户可见的错误提示语
	*/
    public static final String ERROR_MSG_NET_DISCONNECTED    = "网络异常，请检查网络";

    //直播端错误信息
    public static final String ERROR_MSG_CREATE_GROUP_FAILED = "创建直播房间失败,Error:";
    public static final String ERROR_MSG_GET_PUSH_URL_FAILED = "拉取直播推流地址失败,Error:";
    public static final String ERROR_MSG_OPEN_CAMERA_FAIL    = "无法打开摄像头，需要摄像头权限";
    public static final String ERROR_MSG_OPEN_MIC_FAIL       = "无法打开麦克风，需要麦克风权限";
    public static final String ERROR_MSG_RECORD_PERMISSION_FAIL   = "无法进行录屏,需要录屏权限";
    public static final String ERROR_MSG_NO_LOGIN_CACHE   = "您的帐号已在其它地方登陆";

    //播放端错误信息
    public static final String ERROR_MSG_GROUP_NOT_EXIT      = "直播已结束，加入失败";
    public static final String ERROR_MSG_JOIN_GROUP_FAILED   = "加入房间失败，Error:";
    public static final String ERROR_MSG_LIVE_STOPPED        = "直播已结束";
    public static final String ERROR_MSG_NOT_QCLOUD_LINK     = "非腾讯云链接，若要放开限制请联系腾讯云商务团队";
    public static final String ERROR_RTMP_PLAY_FAILED        = "视频流播放失败，Error:";

    public static final String TIPS_MSG_STOP_PUSH            = "当前正在直播，是否退出直播？";

    //网络类型
    public static final int NETTYPE_WIFI = 0;
    public static final int NETTYPE_NONE = 1;
    public static final int NETTYPE_2G   = 2;
    public static final int NETTYPE_3G   = 3;
    public static final int NETTYPE_4G   = 4;

    //连麦开关
    public static final boolean TX_ENABLE_LINK_MIC                          = true; //开启连麦标志位

    //连麦消息类型
    public static final int LINKMIC_CMD_REQUEST                             = 10001;
    public static final int LINKMIC_CMD_ACCEPT                              = 10002;
    public static final int LINKMIC_CMD_REJECT                              = 10003;
    public static final int LINKMIC_CMD_MEMBER_JOIN_NOTIFY                  = 10004;
    public static final int LINKMIC_CMD_MEMBER_EXIT_NOTIFY                  = 10005;
    public static final int LINKMIC_CMD_KICK_MEMBER                         = 10006;

    //连麦响应类型
    public static final int LINKMIC_RESPONSE_TYPE_ACCEPT                    = 1;    //主播接受连麦
    public static final int LINKMIC_RESPONSE_TYPE_REJECT                    = 2;    //主播拒绝连麦
}
