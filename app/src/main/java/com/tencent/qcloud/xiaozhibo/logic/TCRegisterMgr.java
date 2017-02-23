package com.tencent.qcloud.xiaozhibo.logic;

import android.content.Context;

import com.tencent.qcloud.xiaozhibo.base.TCConstants;

import tencent.tls.platform.TLSAccountHelper;
import tencent.tls.platform.TLSErrInfo;
import tencent.tls.platform.TLSSmsRegListener;
import tencent.tls.platform.TLSStrAccRegListener;
import tencent.tls.platform.TLSUserInfo;

/**
 * Created by Administrator on 2016/10/5
 */

public class TCRegisterMgr {
    public static final String TAG = TCLoginMgr.class.getSimpleName();

    private TCRegisterMgr() {
    }

    private static class TCRegisterMgrHolder {
        private static TCRegisterMgr instance = new TCRegisterMgr();
    }

    public static TCRegisterMgr getInstance() {
        return TCRegisterMgr.TCRegisterMgrHolder.instance;
    }

    private static TLSAccountHelper mTLSAccountHelper;

    private TCRegisterCallback mTCRegisterCallback;

    //手机ID缓存
    private String mMobileId;

    private TCSmsRegCallback mTCSmsRegCallback;

    /**
     * 初始化TLS SDK
     * @param context applicationContext
     */
    public void init(Context context) {

        mTLSAccountHelper = TLSAccountHelper.getInstance().init(context,
                TCConstants.IMSDK_APPID, TCConstants.IMSDK_ACCOUNT_TYPE, "1.0");
        mTLSAccountHelper.setCountry(Integer.parseInt("86"));
        mTLSAccountHelper.setTimeOut(8000);
        mTLSAccountHelper.setLocalId(2052);
        mTLSAccountHelper.setTestHost("", true);

    }

    public void setTCRegisterCallback(TCRegisterCallback tcRegisterCallback) {
        this.mTCRegisterCallback = tcRegisterCallback;
    }

    public void removeTCRegisterCallback() {
        this.mTCRegisterCallback = null;
        this.mTCSmsRegCallback = null;
    }

    //短信注册回调类
    private TLSSmsRegListener mTLSSmsRegListener = new TLSSmsRegListener() {

        /**
         * 短信注册验证码监听
         * @param reaskDuration reaskDuration时间内不可以重新请求下发短信
         * @param expireDuration 短信验证码失效时间
         */
        @Override
        public void OnSmsRegAskCodeSuccess(int reaskDuration, int expireDuration) {
            if(null != mTCSmsRegCallback)
                mTCSmsRegCallback.onGetVerifyCode(reaskDuration, expireDuration);
        }

        /**
         * 短信重新注册验证码监听
         * @param reaskDuration reaskDuration时间内不可以重新请求下发短信
         * @param expireDuration 短信验证码失效时间
         */
        @Override
        public void OnSmsRegReaskCodeSuccess(int reaskDuration, int expireDuration) {
            if(null != mTCSmsRegCallback)
                mTCSmsRegCallback.onGetVerifyCode(reaskDuration, expireDuration);
        }

        /**
         * 短信验证通过，下一步调用登录接口TLSSmsReg完成登录
         */
        @Override
        public void OnSmsRegVerifyCodeSuccess() {
            smsRegCommit(this);
        }

        /**
         * TLS注册成功
         * @param tlsUserInfo TLS用户信息
         */
        @Override
        public void OnSmsRegCommitSuccess(TLSUserInfo tlsUserInfo) {
            if(null != mTCRegisterCallback)
                mTCRegisterCallback.onSuccess(tlsUserInfo.identifier);
        }

        /**
         * 短信注册失败
         * @param tlsErrInfo 错误信息类
         */
        @Override
        public void OnSmsRegFail(TLSErrInfo tlsErrInfo) {
            if(null != mTCRegisterCallback)
                mTCRegisterCallback.onFailure(tlsErrInfo.ErrCode, tlsErrInfo.Msg);
        }

        /**
         * 短信注册超时
         * @param tlsErrInfo 错误信息类
         */
        @Override
        public void OnSmsRegTimeout(TLSErrInfo tlsErrInfo) {
            if(null != mTCRegisterCallback)
                mTCRegisterCallback.onFailure(tlsErrInfo.ErrCode, tlsErrInfo.Msg);
        }
    };

    /**
     * tlssms验证成功后调用完成注册流程
     * @param smsRegListener 注册结果监听
     */
    public void smsRegCommit(TLSSmsRegListener smsRegListener) {
        mTLSAccountHelper.TLSSmsRegCommit(smsRegListener);
    }

    /**
     * tlssmd登录 获取验证码
     * @param mobile 手机号（默认中国+86）
     */
    public void smsRegAskCode(String mobile, TCSmsRegCallback tcSmsCallback) {
        this.mMobileId = mobile;
        this.mTCSmsRegCallback = tcSmsCallback;
        mTLSAccountHelper.TLSSmsRegAskCode(mobile, mTLSSmsRegListener);
    }

    /**
     * tls用户名注册
     * @param username 用户名
     * @param password 密码
     */
    public void pwdRegist(final String username, final String password) {
        mTLSAccountHelper.TLSStrAccReg(username, password, new TLSStrAccRegListener() {


            /**
             * 用户名注册成功
             * @param tlsUserInfo TLS用户信息类
             */
            @Override
            public void OnStrAccRegSuccess(TLSUserInfo tlsUserInfo) {
                if(null != mTCRegisterCallback)
                    mTCRegisterCallback.onSuccess(tlsUserInfo.identifier);
            }

            /**
             * 用户名注册失败
             * @param tlsErrInfo TLS错误信息类
             */
            @Override
            public void OnStrAccRegFail(TLSErrInfo tlsErrInfo) {
                if(null != mTCRegisterCallback)
                    mTCRegisterCallback.onFailure(tlsErrInfo.ErrCode, tlsErrInfo.Msg);
            }

            /**
             * 用户名注册超时
             * @param tlsErrInfo TLS错误信息类
             */
            @Override
            public void OnStrAccRegTimeout(TLSErrInfo tlsErrInfo) {
                if(null != mTCRegisterCallback)
                    mTCRegisterCallback.onFailure(tlsErrInfo.ErrCode, tlsErrInfo.Msg);
            }
        });
    }


    /**
     * 短信注册验证码验证
     * @param verifyCode sms注册验证码
     */
    public void smsRegVerifyCode(String verifyCode) {
        mTLSAccountHelper.TLSSmsRegVerifyCode(verifyCode, mTLSSmsRegListener);
    }

    /**
     * 注册回调，封装IM与TLS
     */
    public interface TCRegisterCallback {

        /**
         * 注册成功
         */
        void onSuccess(String username);

        /**
         * 注册失败
         * @param code 错误码
         * @param msg 错误信息
         */
        void onFailure(int code, String msg);

    }

    /**
     * 获取验证码回调（失败将调用TCRegisterCallback#onFailure）
     */
    public interface TCSmsRegCallback {

        void onGetVerifyCode(int reaskDuration, int expireDuration);
    }

}
