/*
 * Reference: https://www.baeldung.com/java-zookeeper
 */

package com.spyfall.manager;

import org.apache.zookeeper.KeeperException;

import java.io.UnsupportedEncodingException;

public interface IManager {
    void create(String path, byte[] data) throws KeeperException, InterruptedException;
    Object getNodeData(String path, boolean watchFlag) throws KeeperException, InterruptedException, UnsupportedEncodingException;
    void update(String path, byte[] data) throws KeeperException, InterruptedException;
    Boolean exist(String path) throws KeeperException, InterruptedException;
    void list(String path) throws KeeperException, InterruptedException;
}
