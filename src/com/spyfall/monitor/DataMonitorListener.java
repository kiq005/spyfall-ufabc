package com.spyfall.monitor;

import org.apache.zookeeper.KeeperException;

public interface DataMonitorListener {
    void process(byte data[]) throws KeeperException, InterruptedException;
    void closing(KeeperException.Code rc);
}
