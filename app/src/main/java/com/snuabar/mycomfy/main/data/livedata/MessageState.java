package com.snuabar.mycomfy.main.data.livedata;

public class MessageState {
    public static int STATE_NONE = -1;
    public static int STATE_CHANGED = 0;
    public static int STATE_ADDED = 1;
    public static int STATE_DELETED = 2;

    public final int state;
    public final int index;
    public final Progress progress;

    public MessageState(int index, int state) {
        this.index = index;
        this.state = state;
        this.progress = null;
    }

    public MessageState(int index, int state, Progress progress) {
        this.index = index;
        this.state = state;
        this.progress = progress;
    }

    public static MessageState added(int index) {
        return new MessageState(index, STATE_ADDED);
    }

    public static MessageState changed(int index) {
        return new MessageState(index, STATE_CHANGED);
    }

    public static MessageState deleted(int index) {
        return new MessageState(index, STATE_DELETED);
    }

    public static MessageState progress(int index, long max, long cur) {
        return new MessageState(index, STATE_CHANGED, new Progress(max, cur));
    }

    public static class Progress {
        public final long max;
        public final long current;

        public Progress(long max, long current) {
            this.max = max;
            this.current = current;
        }
    }
}
