package com.tencent.qcloud.xiaozhibo.logic;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.TIMCallBack;
import com.tencent.TIMFriendGenderType;
import com.tencent.TIMFriendshipManager;
import com.tencent.TIMUserProfile;
import com.tencent.TIMValueCallBack;
import com.tencent.qcloud.xiaozhibo.base.TCConstants;

/**
 * 用户资料管理类
 */
public class TCUserInfoMgr {

    private String TAG = getClass().getName();

    public class TCUserInfo {

        public String userId;
        public String loginSig;
        public String cosSig;

        public String nickname;                 // 昵称
        public String headPic;                  // 头像
        public String coverPic;                 // 直播封面
        public TIMFriendGenderType sex;         // 性别
        public String selfSignature;            // 个性签名

        public String location;
        public double latitude;
        public double longitude;
    }

    private TCUserInfo mUserInfo;

    private TCUserInfoMgr() {
        mUserInfo = new TCUserInfo();
    }

    private static TCUserInfoMgr instance = new TCUserInfoMgr();

    public static TCUserInfoMgr getInstance() { return instance; }

    /**
     * 查询用户资料
     *
     * @param listener  查询结果的回调
     */
    public void queryUserInfo(final ITCUserInfoMgrListener listener){
        try {
            TIMFriendshipManager.getInstance().getSelfProfile(new TIMValueCallBack<TIMUserProfile>() {
                @Override
                public void onError(int i, String s) {
                    Log.e(TAG, "queryUserInfo  failed  , " + i + " : " + s);
                    if (listener != null) {
                        listener.OnQueryUserInfo(i,s);
                    }
                }

                @Override
                public void onSuccess(TIMUserProfile timUserProfile) {
                    Log.e(TAG, "queryUserInfo  success!");

                    mUserInfo.userId = timUserProfile.getIdentifier();
                    if (TextUtils.isEmpty(timUserProfile.getNickName())) {
                        mUserInfo.nickname = timUserProfile.getIdentifier();
                    } else {
                        mUserInfo.nickname = timUserProfile.getNickName();
                    }
                    mUserInfo.headPic = timUserProfile.getFaceUrl();
                    //// TODO: 16/8/9 暂时使用签名字段保存封面
                    mUserInfo.coverPic = timUserProfile.getSelfSignature();
                    mUserInfo.sex = timUserProfile.getGender();
                    mUserInfo.location = timUserProfile.getLocation();

                    if (listener != null) {
                        listener.OnQueryUserInfo(0, null);
                    }
                }
            });
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * 设置用户ID， 并使用ID向服务器查询用户信息
     *      setUserId一般在登录成功之后调用，用户信息需要提供给其他类使用，或者展示给用户，因此登录成功之后需要立即向服务器查询用户信息，
     *
     * @param userId
     * @param listener 设置结果回调
     */
    public void setUserId(final String userId, final ITCUserInfoMgrListener listener) {
        try {
            queryUserInfo(new ITCUserInfoMgrListener() {
                @Override
                public void OnQueryUserInfo(int error, String errorMsg) {
                    if (0 == error) {
                        mUserInfo.userId = userId;
                    } else {
                        Log.e(TAG,"setUserId failed:" + error + "," + errorMsg);
                    }
                    if (null != listener)
                        listener.OnSetUserInfo(error, errorMsg);
                }

                @Override
                public void OnSetUserInfo(int error, String errorMsg) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置昵称
     *
     * @param nickName 昵称
     * @param listener 设置结果回调
     */
    public void setUserNickName(final String nickName, final ITCUserInfoMgrListener listener){
        if (mUserInfo.nickname.equals(nickName)) {
            if (null != listener) listener.OnSetUserInfo(0, null);
            return;
        }

        TIMFriendshipManager.getInstance().setNickName(nickName, new TIMCallBack() {
            @Override
            public void onError(int i, String s) {
                Log.e(TAG,"setUserNickName failed:" + i + "," + s);
                if (null != listener)listener.OnSetUserInfo(i,s);
            }

            @Override
            public void onSuccess() {
                mUserInfo.nickname = nickName;
                if (null != listener) listener.OnSetUserInfo(0,null);
            }
        });
    }

    /**
     * 设置签名
     *
     * @param sign  签名
     * @param listener 设置结果回调
     */
    public void setUserSign(final String sign, final ITCUserInfoMgrListener listener){
        if (mUserInfo.selfSignature.equals(sign)) {
            if (null != listener) listener.OnSetUserInfo(0, null);
            return;
        }

        TIMFriendshipManager.getInstance().setSelfSignature(sign, new TIMCallBack() {
            @Override
            public void onError(int i, String s) {
                Log.e(TAG,"setUserSign failed:" + i + "," + s);
                if (null != listener) listener.OnSetUserInfo(i,s);
            }

            @Override
            public void onSuccess() {
                mUserInfo.selfSignature = sign;
                if (null != listener) listener.OnSetUserInfo(0,null);
            }
        });
    }

    /**
     * 设置性别
     *
     * @param sex 性别
     * @param listener 设置结果回调
     */
    public void setUserSex(final TIMFriendGenderType sex, final ITCUserInfoMgrListener listener){
        if (mUserInfo.sex == sex) {
            if (null != listener) listener.OnSetUserInfo(0,null);
            return;
        }

        TIMFriendshipManager.getInstance().setGender(sex, new TIMCallBack() {
            @Override
            public void onError(int i, String s) {
                Log.e(TAG,"setUserSex failed:" + i + "," + s);
                if (null != listener) listener.OnSetUserInfo(i,s);
            }

            @Override
            public void onSuccess() {
                mUserInfo.sex = sex;
                if (null != listener) listener.OnSetUserInfo(0,null);
            }
        });
    }

    /**
     * 设置头像
     *      设置头像前，首先会将该图片上传到服务器存储，之后服务器返回图片的存储URL，
     *    再调用setUserHeadPic将URL存储到服务器，以后查询头像就使用该URL到服务器下载。
     *
     * @param url 头像的存储URL
     * @param listener 设置结果回调
     */
    public void setUserHeadPic(final String url, final ITCUserInfoMgrListener listener){
        TIMFriendshipManager.getInstance().setFaceUrl(url, new TIMCallBack() {
            @Override
            public void onError(int i, String s) {
                Log.e(TAG,"setUserHeadPic failed:" + i + "," + s);
                if (null != listener) listener.OnSetUserInfo(i,s);
            }

            @Override
            public void onSuccess() {
                mUserInfo.headPic = url;
                if (null != listener) listener.OnSetUserInfo(0, null);
            }
        });
    }

    /**
     * 设置直播封面
     *      设置直播封面前，首先会将该图片上传到服务器存储，之后服务器返回图片的存储URL，
     *    再调用setUserCoverPic将URL存储到服务器，以后要查询直播封面就使用该URL到服务器下载
     *
     * @param url 直播封面的存储URL
     * @param listener 设置结果回调
     */
    public void setUserCoverPic(final String url, final ITCUserInfoMgrListener listener){
        TIMFriendshipManager.getInstance().setSelfSignature(url, new TIMCallBack() {
            @Override
            public void onError(int i, String s) {
                Log.e(TAG,"setUserCoverPic failed:" + i + "," + s);
                if (null != listener) listener.OnSetUserInfo(i,s);
            }

            @Override
            public void onSuccess() {
                mUserInfo.coverPic = url;
                if (null != listener) listener.OnSetUserInfo(0, null);
            }
        });
    }

    /**
     * 设置用户定位信息
     *
     * @param location 详细定位信息
     * @param latitude 纬度
     * @param longitude 经度
     * @param listener 设置结果回调
     */
    public void setLocation(@NonNull final String location, final double latitude, final double longitude, final ITCUserInfoMgrListener listener){
        if (mUserInfo.location !=null && mUserInfo.location.equals(location)) {
            if (null != listener) listener.OnSetUserInfo(0,null);
            return;
        }

        TIMFriendshipManager.getInstance().setLocation(location, new TIMCallBack() {
            @Override
            public void onError(int i, String s) {
                Log.e(TAG,"setLocation failed:" + i + "," + s);
                if (null != listener) listener.OnSetUserInfo(i,s);
            }

            @Override
            public void onSuccess() {
                mUserInfo.latitude = latitude;
                mUserInfo.longitude = longitude;
                mUserInfo.location = location;
                if (null != listener) listener.OnSetUserInfo(0, null);
            }
        });
    }

    public String getUserId() { return mUserInfo.userId; }

    public String getNickname() { return mUserInfo.nickname; }

    public String getHeadPic() { return mUserInfo.headPic; }

    public String getCoverPic() { return mUserInfo.coverPic; }

    public TIMFriendGenderType getSex() { return mUserInfo.sex; }

    public String getLocation() { return mUserInfo.location; }

}
