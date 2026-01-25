package com.snuabar.mycomfy.common;

import java.io.IOException;

public class Callbacks {

    @FunctionalInterface
    public interface Callback {
        void apply();
    }

    @FunctionalInterface
    public interface CallbackT<T> {
        void apply(T obj);
    }

    @FunctionalInterface
    public interface CallbackR<R> {
        R apply() throws IOException;
    }

    @FunctionalInterface
    public interface CallbackTR<T, R> {
        R apply(T obj);
    }

    @FunctionalInterface
    public interface Callback2T<T1, T2> {
        void apply(T1 obj1, T2 obj2);
    }

    @FunctionalInterface
    public interface Callback3T<T1, T2, T3> {
        void apply(T1 obj1, T2 obj2, T3 obj3);
    }

    @FunctionalInterface
    public interface Callback3TR<T1, T2, T3, R> {
        R apply(T1 obj1, T2 obj2, T3 obj3);
    }

    @FunctionalInterface
    public interface Callback4T<T1, T2, T3, T4> {
        void apply(T1 obj1, T2 obj2, T3 obj3, T4 obj4);
    }

    @FunctionalInterface
    public interface Callback4TR<T1, T2, T3, T4, R> {
        R apply(T1 obj1, T2 obj2, T3 obj3, T4 obj4);
    }

    @FunctionalInterface
    public interface Callback5T<T1, T2, T3, T4, T5> {
        void apply(T1 obj1, T2 obj2, T3 obj3, T4 obj4, T5 obj5);
    }

    @FunctionalInterface
    public interface Callback5TR<T1, T2, T3, T4, T5, R> {
        R apply(T1 obj1, T2 obj2, T3 obj3, T4 obj4, T5 obj5);
    }
}
