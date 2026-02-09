package com.snuabar.mycomfy.main.data.livedata;

public class MessageModelState {
    public static int STATE_NONE = -1;
    public static int STATE_CHANGED = 0;
    public static int STATE_ADDED = 1;
    public static int STATE_DELETED = 2;

    public final int state;
    public final int index;

    public MessageModelState(int index, int state) {
        this.index = index;
        this.state = state;
    }

    public static MessageModelState added(int index) {
        return new MessageModelState(index, STATE_ADDED);
    }

    public static MessageModelState changed(int index) {
        return new MessageModelState(index, STATE_CHANGED);
    }

    public static MessageModelState deleted(int index) {
        return new MessageModelState(index, STATE_DELETED);
    }
}
