/*
 * Reference: https://www.baeldung.com/java-zookeeper
 */
package com.spyfall.connection;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class Connection {
    private ZooKeeper zoo;
    private CountDownLatch connectionLatch = new CountDownLatch(1);

    public ZooKeeper connect(String host) throws IOException, InterruptedException {
        return connect(host, 2000);
    }

    public ZooKeeper connect(String host, int timeout) throws IOException, InterruptedException {
        zoo = new ZooKeeper(host, timeout, WatchedEvent -> {
            if (WatchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                connectionLatch.countDown();
            }
            else {
                throw new RuntimeException("Error connecting to zookeeper");
            }
        });

        connectionLatch.await();
        return zoo;
    }

    public void close() throws InterruptedException {
        zoo.close();
    }
}
