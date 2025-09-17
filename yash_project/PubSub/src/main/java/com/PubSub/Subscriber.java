package com.PubSub;

public interface Subscriber {
    void onMessage(Message message);
}
