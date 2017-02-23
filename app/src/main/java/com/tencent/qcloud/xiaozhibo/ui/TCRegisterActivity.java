package com.tencent.qcloud.xiaozhibo.ui;

import android.animation.LayoutTransition;
import android.app.Activity;
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
import com.tencent.qcloud.xiaozhibo.logic.TCLoginMgr;
import com.tencent.qcloud.xiaozhibo.logic.TCRegisterMgr;
import com.tencent.qcloud.xiaozhibo.logic.TCUserInfoMgr;
import com.tencent.rtmp.TXLog;

import java.lang.ref.WeakReference;


/**
 * Created by RTMP on 2016/8/1
 */
public class TCRegisterActivity extends Activity implements TCRegisterMgr.TCRegisterCallback {

    public static final String TAG = TCRegisterActivity.class.getSimpleName();

    private TCRegisterMgr mTCRegisterMgr;

    private String mPassword;

    //共用控件
    private RelativeLayout relativeLayout;

    private ProgressBar progressBar;

    private EditText etPassword;

    private EditText etPasswordVerify;

    private AutoCompleteTextView etRegister;

    private TextView tvPhoneRegister;

    private TextInputLayout tilRegister, tilPassword, tilPasswordVerify;

    private Button btnRegister;

    private TextView tvBackBtn;

    //手机验证注册控件
    private TextView tvVerifyCode;

    //动画
    AlphaAnimation fadeInAnimation, fadeOutAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        relativeLayout = (RelativeLayout) findViewById(R.id.rl_register_root);

        if(null != relativeLayout) {
            ViewTarget<RelativeLayout, GlideDrawable> viewTarget = new ViewTarget<RelativeLayout, GlideDrawable>(relativeLayout) {
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

        etRegister = (AutoCompleteTextView) findViewById(R.id.et_register);

        etPassword = (EditText) findViewById(R.id.et_password);

        etPasswordVerify = (EditText) findViewById(R.id.et_password_verify);

        tilPasswordVerify = (TextInputLayout) findViewById(R.id.til_password_verify);

        tvPhoneRegister = (TextView) findViewById(R.id.tv_phone_register);

        btnRegister = (Button) findViewById(R.id.btn_register);

        progressBar = (ProgressBar) findViewById(R.id.progressbar);

        tilRegister = (TextInputLayout) findViewById(R.id.til_register);

        tilPassword = (TextInputLayout) findViewById(R.id.til_password);

        tvVerifyCode = (TextView) findViewById(R.id.btn_verify_code);

        tvBackBtn = (TextView) findViewById(R.id.tv_back);

        mTCRegisterMgr = TCRegisterMgr.getInstance();
        mTCRegisterMgr.setTCRegisterCallback(this);

        fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);
        fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
        fadeInAnimation.setDuration(250);
        fadeOutAnimation.setDuration(250);

        LayoutTransition layoutTransition = new LayoutTransition();
        relativeLayout.setLayoutTransition(layoutTransition);

    }

    @Override
    protected void onResume() {
        super.onResume();
        userNameRegisterViewInit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void showOnLoading(boolean active) {
        if (active) {
            progressBar.setVisibility(View.VISIBLE);
            btnRegister.setVisibility(View.INVISIBLE);
            etPassword.setEnabled(false);
            etPasswordVerify.setEnabled(false);
            etRegister.setEnabled(false);
            tvPhoneRegister.setClickable(false);
            tvPhoneRegister.setTextColor(getResources().getColor(R.color.colorLightTransparentGray));
            btnRegister.setEnabled(false);
            //tvRegister.setTextColor(getResources().getColor(R.color.colorBlue));
        } else {
            progressBar.setVisibility(View.GONE);
            btnRegister.setVisibility(View.VISIBLE);
            etPassword.setEnabled(true);
            etPasswordVerify.setEnabled(true);
            etRegister.setEnabled(true);
            tvPhoneRegister.setClickable(true);
            tvPhoneRegister.setTextColor(getResources().getColor(R.color.colorTransparentGray));
            btnRegister.setEnabled(true);
            //tvRegister.setTextColor(getResources().getColor(R.color.colorAccent));
        }

    }

    private void phoneRegistViewinit() {

        etRegister.setText("");

        etRegister.setInputType(EditorInfo.TYPE_CLASS_PHONE);

        etPassword.setText("");

        etPasswordVerify.setText("");

        tvPhoneRegister.setText(getString(R.string.activity_register_normal_register));

        tilRegister.setHint(getString(R.string.activity_login_phone_num));
        tilPassword.setHint(getString(R.string.activity_register_verify_code));

//        tilPasswordVerify.startAnimation(fadeOutAnimation);
        tilPasswordVerify.setVisibility(View.GONE);

        tvVerifyCode.setVisibility(View.VISIBLE);
//        tvVerifyCode.setAnimation(fadeInAnimation);
        tvVerifyCode.bringToFront();

        tvVerifyCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendRegistVerifyCode(etRegister.getText().toString());
            }
        });

        tvPhoneRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //转换为用户名注册界面
                userNameRegisterViewInit();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //手机注册逻辑
                attemptPhoneRegist(etRegister.getText().toString(), etPassword.getText().toString());
            }
        });
    }

    private void userNameRegisterViewInit() {

        etRegister.setText("");

        etRegister.setInputType(EditorInfo.TYPE_CLASS_TEXT);

        etPassword.setText("");

        etPasswordVerify.setText("");

        tvVerifyCode.setVisibility(View.GONE);
//        tvVerifyCode.startAnimation(fadeOutAnimation);

        tilPasswordVerify.setVisibility(View.VISIBLE);
//        tilPasswordVerify.startAnimation(fadeInAnimation);

        tvPhoneRegister.setText(getString(R.string.activity_register_phone_register));

        tilRegister.setHint(getString(R.string.activity_register_username));

        tilPassword.setHint(getString(R.string.activity_register_password));

        tilPasswordVerify.setHint(getString(R.string.activity_register_password_verify));

        tvPhoneRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //转换为手机注册界面
                phoneRegistViewinit();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //调用normal注册逻辑
                attemptNormalRegist(etRegister.getText().toString(),
                        etPassword.getText().toString(), etPasswordVerify.getText().toString());
            }
        });

        tvBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void showRegistError(String errorString) {
        etRegister.setError(errorString);
        showOnLoading(false);
    }

    private void showPasswordError(String errorString) {
        etPassword.setError(errorString);
        showOnLoading(false);
    }

    private void showPasswordVerifyError(String errorString) {
        etPassword.setError(errorString);
        showOnLoading(false);
    }

    private void sendRegistVerifyCode(String phoneNum) {
        if (TCUtils.isPhoneNumValid(phoneNum)) {
            if(TCUtils.isNetworkAvailable(this)) {
                mTCRegisterMgr.smsRegAskCode(getWellFormatMobile("86", phoneNum), new TCRegisterMgr.TCSmsRegCallback() {
                    @Override
                    public void onGetVerifyCode(int reaskDuration, int expireDuration) {
                        Log.d(TAG, "OnSmsRegReaskCodeSuccess");
                        Toast.makeText(getApplicationContext(), "注册短信下发,验证码" + expireDuration / 60 + "分钟内有效", Toast.LENGTH_SHORT).show();
                        TCUtils.startTimer(new WeakReference<>(tvVerifyCode), "验证码", reaskDuration, 1);
                        showOnLoading(false);
                    }
                });
                showOnLoading(true);
            } else {
                Toast.makeText(getApplicationContext(), "当前无网络连接", Toast.LENGTH_SHORT).show();
            }
        } else {
            showRegistError("手机格式错误");
        }
    }

    private void attemptNormalRegist(String username, String password, String passwordVerify) {

        if (TCUtils.isUsernameVaild(username)) {
            if (TCUtils.isPasswordValid(password)) {
                if (password.equals(passwordVerify)) {
                    if(TCUtils.isNetworkAvailable(this)) {
                        mPassword = password;
                        mTCRegisterMgr.pwdRegist(username, password);
                        showOnLoading(true);
                    } else {
                        Toast.makeText(getApplicationContext(), "当前无网络连接", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showPasswordVerifyError("两次输入密码不一致");
                }
            } else {
                showPasswordVerifyError("密码长度应为8-16位");
            }
        } else {
            showRegistError("用户名不符合规范");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTCRegisterMgr.removeTCRegisterCallback();
    }

    private void attemptPhoneRegist(String phoneNum, String verifyCode) {
        if (TCUtils.isPhoneNumValid(phoneNum)) {
            if (TCUtils.isVerifyCodeValid(verifyCode)) {
                if(TCUtils.isNetworkAvailable(this)) {
                    mPassword = null;
                    mTCRegisterMgr.smsRegVerifyCode(verifyCode);
                    showOnLoading(true);
                } else {
                    Toast.makeText(getApplicationContext(), "当前无网络连接", Toast.LENGTH_SHORT).show();
                }
            } else {
                showPasswordError("验证码格式错误");
            }
        } else {
            showRegistError("手机号码不符合规范");
        }
    }

    private String getWellFormatMobile(String countryCode, String phoneNumber) {
        return countryCode + "-" + phoneNumber;
    }

    private void jumpToHomeActivity () {
        Intent intent = new Intent(this, TCMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * 注册成功
     * 成功后直接登录
     * @param identifier id
     */
    @Override
    public void onSuccess(final String identifier) {
        //自动登录逻辑
        final TCLoginMgr tcLoginMgr = TCLoginMgr.getInstance();
        tcLoginMgr.setTCLoginCallback(new TCLoginMgr.TCLoginCallback() {
            @Override
            public void onSuccess() {
                tcLoginMgr.removeTCLoginCallback();
                TCUserInfoMgr.getInstance().setUserId(identifier, null);
                Log.d(TAG, "login after regist success");
                Toast.makeText(getApplicationContext(), "自动登录成功", Toast.LENGTH_SHORT).show();
                jumpToHomeActivity();
            }

            @Override
            public void onFailure(int code, String msg) {
                tcLoginMgr.removeTCLoginCallback();
                TXLog.d(TAG, "login after regist fail, code:" + code + " msg:" + msg);
                Toast.makeText(getApplicationContext(), "自动登录失败", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        //根据mPassword判断登录类型采取不同的登录方式（sms or password）
        if (!TextUtils.isEmpty(mPassword))
            tcLoginMgr.pwdLogin(identifier, mPassword);
        else
            tcLoginMgr.smsLogin(identifier);
        Toast.makeText(getApplicationContext(), "成功注册" + identifier, Toast.LENGTH_SHORT).show();
        mTCRegisterMgr.removeTCRegisterCallback();
    }

    /**
     * 注册失败
     *
     * @param code 错误码
     * @param msg  错误信息
     */
    @Override
    public void onFailure(int code, String msg) {
        Log.d(TAG, "regist fail, code:" + code + " msg:" + msg);
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        showOnLoading(false);
    }
}
