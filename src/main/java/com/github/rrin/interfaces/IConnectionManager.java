package com.github.rrin.interfaces;

import java.io.IOException;
import java.net.Socket;

public interface IConnectionManager<T> {
    Long register(T connObj);
    T get(Long id);
    void remove(Long id);
    void closeAll();
    boolean isConnected(Long id);
}
