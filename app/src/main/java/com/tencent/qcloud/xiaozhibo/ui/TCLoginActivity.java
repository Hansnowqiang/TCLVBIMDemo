package com.tencent.qcloud.xiaozhibo.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.ViewTarget;
import com.tencent.qcloud.xiaozhibo.R;
import com.tencent.qcloud.xiaozhibo.base.TCUtils;
import com.tencent.qcloud.xiaozhibo.logic.ITCUserInfoMgrListener;
import com.tencent.qcloud.xiaozhibo.logic.TCLoginMgr;
import com.tencent.qcloud.xiaozhibo.logic.TCUserInfoMgr;
import com.tencent.qcloud.xiaozhibo.ui.customviews.OnProcessFragment;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

/**
 * Created by RTMP on 2016/8/1
 */
public class TCLoginActivity extends Activity implements TCLoginMgr.TCLoginCallback {

    private static final String TAG = TCLoginActivity.class.getSimpleName();

    private TCLoginMgr mTCLoginMgr;

    //共用控件
    private RelativeLayout rootRelativeLayout;

    private ProgressBar progressBar;

    private EditText etPassword;

    private AutoCompleteTextView etLogin;

    private Button btnLogin;

    private TextView tvPhoneLogin;

    private TextInputLayout tilLogin, tilPassword;

    private TextView tvRegister;

    private TextView tvGuestLogin;

    //手机验证登陆控件
    private TextView tvVerifyCode;

    private boolean bIsGuest = false; //是不是游客登录

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        rootRelativeLayout = (RelativeLayout) findViewById(R.id.rl_login_root);

        mTCLoginMgr = TCLoginMgr.getInstance();

        if (null != rootRelativeLayout) {
            ViewTarget<RelativeLayout, GlideDrawable> viewTarget = new ViewTarget<RelativeLayout, GlideDrawable>(rootRelativeLayout) {
                @Override
                public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                    this.view.setBackgroundDrawable(resource.getCurrent());
                }
            };

            Glide.with(getApplicationContext()) // safer!
                    .load(R.drawable.bg_dark)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(viewTarget);
        }

        etLogin = (AutoCompleteTextView) findViewById(R.id.et_login);

        etPassword = (EditText) findViewById(R.id.et_password);

        tvRegister = (TextView) findViewById(R.id.btn_register);

        tvPhoneLogin = (TextView) findViewById(R.id.tv_phone_login);

        btnLogin = (Button) findViewById(R.id.btn_login);

        progressBar = (ProgressBar) findViewById(R.id.progressbar);

        tilLogin = (TextInputLayout) findViewById(R.id.til_login);

        tilPassword = (TextInputLayout) findViewById(R.id.til_password);

        tvVerifyCode = (TextView) findViewById(R.id.btn_verify_code);

        tvGuestLogin = (TextView) findViewById(R.id.btn_guest_login);

        userNameLoginViewInit();

        //检测是否存在缓存
        if (TCUtils.isNetworkAvailable(this)) {
            mTCLoginMgr.setTCLoginCallback(this);
            //返回true表示存在本地缓存，进行登录操作，显示loadingFragment
            if (TCLoginMgr.getInstance().checkCacheAndLogin()) {
                OnProcessFragment loadinfFragment = new OnProcessFragment();
                loadinfFragment.show(getFragmentManager(), "");
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        //设置登录回调,resume设置回调避免被registerActivity冲掉
        mTCLoginMgr.setTCLoginCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //删除登录回调
        mTCLoginMgr.removeTCLoginCallback();
    }

    /**
     * 短信登录界面init
     */
    public void phoneLoginViewinit() {

        tvVerifyCode.setVisibility(View.VISIBLE);

        AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        alphaAnimation.setDuration(250);
        tvVerifyCode.setAnimation(alphaAnimation);

        //设定点击优先级于最前（避免被EditText遮挡的情况）
        tvVerifyCode.bringToFront();

        etLogin.setInputType(EditorInfo.TYPE_CLASS_PHONE);

        tvPhoneLogin.setText(getString(R.string.activity_login_normal_login));

        tilLogin.setHint(getString(R.string.activity_login_phone_num));

        tilPassword.setHint(getString(R.string.activity_login_verify_code_edit));

        tvVerifyCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendLoginVerifyMessage(etLogin.getText().toString());
            }
        });

        tvPhoneLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //转换为用户名登录界面
                userNameLoginViewInit();
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //手机登录逻辑
                showOnLoading(true);
                attemptPhoneLogin(etLogin.getText().toString(), etPassword.getText().toString());
                bIsGuest = false;
            }
        });

    }

    /**
     * 用户名密码登录界面init
     */
    public void userNameLoginViewInit() {

        tvVerifyCode.setVisibility(View.GONE);

        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
        alphaAnimation.setDuration(250);
        tvVerifyCode.setAnimation(alphaAnimation);

        etLogin.setInputType(EditorInfo.TYPE_CLASS_TEXT);

        etLogin.setText("");
        etPassword.setText("");

        tvPhoneLogin.setText(getString(R.string.activity_login_phone_login));

        tilLogin.setHint(getString(R.string.activity_login_username));

        tilPassword.setHint(getString(R.string.activity_login_password));

        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //注册界面 phoneView 与 normalView跳转逻辑一致
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), TCRegisterActivity.class);
                startActivity(intent);
            }
        });

        tvPhoneLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //转换为手机登录界面
                phoneLoginViewinit();
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //调用normal登录逻辑
                showOnLoading(true);

                attemptNormalLogin(etLogin.getText().toString(), etPassword.getText().toString());
                bIsGuest = false;

            }
        });

        tvGuestLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOnLoading(true);
                mTCLoginMgr.guestLogin();
                bIsGuest = true;
            }
        });
    }

    /**
     * trigger loading模式
     *
     * @param active
     */
    public void showOnLoading(boolean active) {
        if (active) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.INVISIBLE);
            etLogin.setEnabled(false);
            etPassword.setEnabled(false);
            tvPhoneLogin.setClickable(false);
            tvPhoneLogin.setTextColor(getResources().getColor(R.color.colorLightTransparentGray));
            tvRegister.setClickable(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);
            etLogin.setEnabled(true);
            etPassword.setEnabled(true);
            tvPhoneLogin.setClickable(true);
            tvPhoneLogin.setTextColor(getResources().getColor(R.color.colorTransparentGray));
            tvRegister.setClickable(true);
            tvRegister.setTextColor(getResources().getColor(R.color.colorTransparentGray));
        }

    }

    public void showLoginError(String errorString) {
        etLogin.setError(errorString);
        showOnLoading(false);
    }

    public void showPasswordError(String errorString) {
        etPassword.setError(errorString);
        showOnLoading(false);
    }

    /**
     * 登录成功后被调用，跳转至TCMainActivity
     */
    public void jumpToHomeActivity() {
        Intent intent = new Intent(this, TCMainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * 请求后台发送验证码
     *
     * @param phoneNum 手机号(默认为+86)
     */
    public void sendLoginVerifyMessage(String phoneNum) {

        if (TCUtils.isPhoneNumValid(phoneNum)) {
            if (TCUtils.isNetworkAvailable(this)) {
                //发送请求 目前默认countryCode为86
                mTCLoginMgr.smsLoginAskCode(TCUtils.getWellFormatMobile("86", phoneNum), new TCLoginMgr.TCSmsCallback() {
                    @Override
                    public void onGetVerifyCode(int reaskDuration, int expireDuration) {
                        Log.d(TAG, "OnSmsLoginaskCodeSuccess");
                        Toast.makeText(getApplicationContext(), "注册短信下发,验证码" + expireDuration / 60 + "分钟内有效", Toast.LENGTH_SHORT).show();
                        TCUtils.startTimer(new WeakReference<>(tvVerifyCode), "验证码", reaskDuration, 1);
                        showOnLoading(false);
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "当前无网络连接", Toast.LENGTH_SHORT).show();
                showOnLoading(false);
            }
        } else {
            showLoginError("手机格式错误");
        }
    }

    /**
     * 获取验证码后进行验证
     *
     * @param phoneNum   手机号
     * @param verifyCode 验证码
     */
    public void attemptPhoneLogin(String phoneNum, String verifyCode) {
        if (TCUtils.isPhoneNumValid(phoneNum)) {
            if (TCUtils.isVerifyCodeValid(verifyCode)) {
                if (TCUtils.isNetworkAvailable(this)) {
                    //手机验证验证码登录
                    mTCLoginMgr.smsLoginVerifyCode(verifyCode);
                } else {
                    Toast.makeText(getApplicationContext(), "当前无网络连接", Toast.LENGTH_SHORT).show();
                    showOnLoading(false);
                }
            } else {
                showPasswordError("验证码错误");
            }
        } else {
            showLoginError("手机格式错误");
        }
    }

    /**
     * 用户名密码登录
     *
     * @param username 用户名
     * @param password 密码
     */
    public void attemptNormalLogin(String username, String password) {

        if (TCUtils.isUsernameVaild(username)) {
            if (TCUtils.isPasswordValid(password)) {
                if (TCUtils.isNetworkAvailable(this)) {
                    //调用LoginHelper进行普通登陆
                    mTCLoginMgr.pwdLogin(username, password);
                } else {
                    Toast.makeText(getApplicationContext(), "当前无网络连接", Toast.LENGTH_SHORT).show();
                    showOnLoading(false);
                }
            } else {
                showPasswordError("密码过短");
            }
        } else {
            showLoginError("用户名不符合规范");
        }
    }


    /**
     * IMSDK登录成功
     */
    @Override
    public void onSuccess() {
        TCUserInfoMgr.getInstance().setUserId(mTCLoginMgr.getLastUserInfo().identifier, new ITCUserInfoMgrListener() {
            @Override
            public void OnQueryUserInfo(int error, String errorMsg) {
                // TODO: 16/8/10
            }

            @Override
            public void OnSetUserInfo(int error, String errorMsg) {
                if (0 != error)
                    Toast.makeText(getApplicationContext(), "设置 User ID 失败" + errorMsg, Toast.LENGTH_LONG).show();
            }
        });

        Toast.makeText(getApplicationContext(), "登陆成功", Toast.LENGTH_SHORT).show();
        mTCLoginMgr.removeTCLoginCallback();
        showOnLoading(false);
        if (bIsGuest) {
            showEditNicknameDia();
        } else {
            jumpToHomeActivity();
        }
    }

    /**
     * 失败
     *
     * @param errCode errCode
     * @param msg     msg
     */
    @Override
    public void onFailure(int errCode, String msg) {
        Log.d(TAG, "Login Error errCode:" + errCode + " msg:" + msg);
        showOnLoading(false);
        //被踢下线后弹窗显示被踢
        if (6208 == errCode) {
            TCUtils.showKickOutDialog(this);
        }
        Toast.makeText(getApplicationContext(), "登录失败" + msg, Toast.LENGTH_SHORT).show();

    }

    private void showEditNicknameDia() {
        final EditText et = new EditText(this);
        et.setText("游客");
        et.setSelection(et.getText().length());
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.NickNameDialog)
                .setCancelable(true)
                .setTitle("修改昵称")
                .setView(et)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String input = et.getText().toString();
                        if (TextUtils.isEmpty(input.trim())) {
                            Toast.makeText(TCLoginActivity.this, "昵称不能为空！" + input, Toast.LENGTH_LONG).show();
                            try {
                                Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
                                field.setAccessible(true);
                                field.set(dialog, false);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            TCUserInfoMgr.getInstance().setUserNickName(input, new ITCUserInfoMgrListener() {
                                @Override
                                public void OnQueryUserInfo(int error, String errorMsg) {
                                    Toast.makeText(TCLoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void OnSetUserInfo(int error, String errorMsg) {
                                    if (0 != error) {
                                        //非法昵称检测
                                        Toast.makeText(TCLoginActivity.this, "昵称不合法，请更换 : " + errorMsg, Toast.LENGTH_LONG).show();
                                    } else {
                                        jumpToHomeActivity();
                                    }
                                }
                            });
                        }
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }
}
