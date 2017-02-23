package com.tencent.qcloud.xiaozhibo.ui.customviews;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tencent.qcloud.xiaozhibo.R;

/**
 * Created by teckjiang on 2016/9/26
 */
public class BeautyDialogFragment extends DialogFragment {

    private static final String TAG = BeautyDialogFragment.class.getSimpleName();

    private SeekBarCallback mSeekBarCallback;
    private SeekBar mBeautySeekbar;
    private TextView mTVBeauty;
    private TextView mTVWhitening;
    public static final int STATE_BEAUTY = 0, STATE_WHITE = 1;
    private int mBeautyProgress = 100; //默认初始值为100
    private int mWhiteningProgress = 0;
    private int mState;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Dialog dialog = new Dialog(getActivity(), R.style.BottomDialog);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.fragment_beauty_area);
        dialog.setCanceledOnTouchOutside(true); // 外部点击取消

        Log.d(TAG, "create fragment");

        mBeautySeekbar = (SeekBar) dialog.findViewById(R.id.beauty_seekbar);
        mBeautySeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSeekBarCallback.onProgressChanged(progress, mState);
                if (mState == STATE_BEAUTY)
                    mBeautyProgress = progress;
                else
                    mWhiteningProgress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mBeautySeekbar.setProgress(mBeautyProgress);

        mTVBeauty = (TextView) dialog.findViewById(R.id.tv_face_beauty);
        mTVWhitening = (TextView) dialog.findViewById(R.id.tv_face_whitening);
        mTVBeauty.setSelected(true);
        mTVWhitening.setSelected(false);
        mState = STATE_BEAUTY;

        mTVBeauty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTVBeauty.setSelected(true);
                mTVWhitening.setSelected(false);

                //seek bar init
                mState = STATE_BEAUTY;
                mBeautySeekbar.setProgress(mBeautyProgress);
            }
        });

        mTVWhitening.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTVBeauty.setSelected(false);
                mTVWhitening.setSelected(true);

                //seek bar init
                mState = STATE_WHITE;
                mBeautySeekbar.setProgress(mWhiteningProgress);

            }
        });

        // 设置宽度为屏宽, 靠近屏幕底部。
        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.BOTTOM; // 紧贴底部
        lp.width = WindowManager.LayoutParams.MATCH_PARENT; // 宽度持平
        window.setAttributes(lp);

        //initView();

        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mSeekBarCallback = (SeekBarCallback) activity;
    }

    public interface SeekBarCallback {
        void onProgressChanged(int progress, int state);
    }
}
