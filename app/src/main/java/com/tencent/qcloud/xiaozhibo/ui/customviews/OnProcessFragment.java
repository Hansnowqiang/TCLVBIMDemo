package com.tencent.qcloud.xiaozhibo.ui.customviews;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

import com.tencent.qcloud.xiaozhibo.R;

/**
 * Created by Administrator on 2016/10/9
 */

public class OnProcessFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(getActivity(), R.style.loading_dialog);
        dialog.setContentView(R.layout.fragment_loading);
        dialog.setCancelable(false);

        return dialog;
    }
}
