package com.example.hellorpc;

import java.util.concurrent.Callable;

public interface CustomCallable<R> extends Callable<R> {
    void postExecute(R result);
    void preExecute();
}
