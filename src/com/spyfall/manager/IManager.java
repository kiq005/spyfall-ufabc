/*
 * Reference: https://www.baeldung.com/java-zookeeper
 */

package com.spyfall.manager;

import com.spyfall.monitor.DataMonitorListener;
import org.apache.zookeeper.KeeperException;

import java.io.UnsupportedEncodingException;
import java.util.List;

public interface IManager {
    void create(String path, byte[] data) throws KeeperException, InterruptedException;
    void delete(String path) throws KeeperException, InterruptedException;
    Object getNodeData(String path, boolean watchFlag) throws KeeperException, InterruptedException, UnsupportedEncodingException;
    void update(String path, byte[] data) throws KeeperException, InterruptedException;
    Boolean exist(String path) throws KeeperException, InterruptedException;
    void list(String path) throws KeeperException, InterruptedException;
    List<String> getNodes(String path) throws KeeperException, InterruptedException;
    void watch(String path, DataMonitorListener listener) throws KeeperException, InterruptedException;
    void stopWatching();
}
