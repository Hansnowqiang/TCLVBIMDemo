package com.tencent.qcloud.xiaozhibo.logic;

import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.tencent.qcloud.xiaozhibo.R;
import com.tencent.qcloud.xiaozhibo.base.TCUtils;
import java.util.ArrayList;

/**
 * 直播列表的Adapter
 * 列表项布局格式: R.layout.listview_video_item
 * 列表项数据格式: TCLiveInfo
 */
public class TCLiveListAdapter extends ArrayAdapter<TCLiveInfo> {
    private int resourceId;
    private Activity mActivity;
    private class ViewHolder{
        TextView tvTitle;
        TextView tvHost;
        TextView tvMembers;
        TextView tvAdmires;
        TextView tvLbs;
        ImageView ivCover;
        ImageView ivAvatar;
        ImageView ivLogo;
    }

    public TCLiveListAdapter(Activity activity, ArrayList<TCLiveInfo> objects) {
        super(activity, R.layout.listview_video_item, objects);
        resourceId = R.layout.listview_video_item;
        mActivity = activity;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView != null) {
            holder = (ViewHolder)convertView.getTag();
        } else {
            convertView = LayoutInflater.from(getContext()).inflate(resourceId, null);

            holder = new ViewHolder();
            holder.ivCover = (ImageView) convertView.findViewById(R.id.cover);
            holder.tvTitle = (TextView) convertView.findViewById(R.id.live_title);
            holder.tvHost = (TextView) convertView.findViewById(R.id.host_name);
            holder.tvMembers = (TextView) convertView.findViewById(R.id.live_members);
            holder.tvAdmires = (TextView) convertView.findViewById(R.id.praises);
            holder.tvLbs = (TextView) convertView.findViewById(R.id.live_lbs);
            holder.ivAvatar = (ImageView) convertView.findViewById(R.id.avatar);
            holder.ivLogo = (ImageView) convertView.findViewById(R.id.live_logo);

            convertView.setTag(holder);
        }

        TCLiveInfo data = getItem(position);

        //直播封面
        String cover = data.userinfo.frontcover;
        if (TextUtils.isEmpty(cover)){
            holder.ivCover.setImageResource(R.drawable.bg);
        }else{
            RequestManager req = Glide.with(mActivity);
            req.load(cover).placeholder(R.drawable.bg).into(holder.ivCover);
        }

        //主播头像
        TCUtils.showPicWithUrl(mActivity,holder.ivAvatar,data.userinfo.headpic,R.drawable.face);
        //主播昵称
        if (TextUtils.isEmpty(data.userinfo.nickname)){
            holder.tvHost.setText("@" + TCUtils.getLimitString(data.userid, 10));
        }else{
            holder.tvHost.setText("@" + TCUtils.getLimitString(data.userinfo.nickname, 10));
        }
        //主播地址
        if (TextUtils.isEmpty(data.userinfo.location)) {
            holder.tvLbs.setText(getContext().getString(R.string.live_unknown));
        }else{
            holder.tvLbs.setText(TCUtils.getLimitString(data.userinfo.location, 9));
        }

        //直播标题
        holder.tvTitle.setText(TCUtils.getLimitString(data.title, 10));
        //直播观看人数
        holder.tvMembers.setText(""+data.viewercount);
        //直播点赞数
        holder.tvAdmires.setText(""+data.likecount);
        //视频类型，直播或者回放
        if (data.type == 0) {
            holder.ivLogo.setImageResource(R.drawable.live);
        } else {
            holder.ivLogo.setImageResource(R.drawable.playback);
        }
        return convertView;
    }

}
