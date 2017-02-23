package com.tencent.qcloud.xiaozhibo.logic;

/**
 * Created by Administrator on 2016/8/14.
 */
public class TCGroupMemberInfo{
    private String userId;
    private String nickname;
    private String faceUrl;

    public TCGroupMemberInfo(String userId, String nickname, String faceUrl) {
        this.userId = userId;
        this.nickname = nickname;
        this.faceUrl = faceUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getFaceUrl() {
        return faceUrl;
    }

    public void setFaceUrl(String faceUrl) {
        this.faceUrl = faceUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TCGroupMemberInfo)) return false;

        TCGroupMemberInfo that = (TCGroupMemberInfo) o;

        if (getUserId() != null ? !getUserId().equals(that.getUserId()) : that.getUserId() != null)
            return false;
        if (getNickname() != null ? !getNickname().equals(that.getNickname()) : that.getNickname() != null)
            return false;
        return getFaceUrl() != null ? getFaceUrl().equals(that.getFaceUrl()) : that.getFaceUrl() == null;

    }

    @Override
    public int hashCode() {
        int result = getUserId() != null ? getUserId().hashCode() : 0;
        result = 31 * result + (getNickname() != null ? getNickname().hashCode() : 0);
        result = 31 * result + (getFaceUrl() != null ? getFaceUrl().hashCode() : 0);
        return result;
    }
}