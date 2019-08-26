/*
 * Reference: https://www.baeldung.com/java-zookeeper
 */
package com.spyfall.manager;

import com.spyfall.connection.Connection;
import com.spyfall.monitor.DataMonitor;
import com.spyfall.monitor.DataMonitorListener;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Manager implements IManager {
    private static ZooKeeper zoo;
    private static Connection connection;
    private static DataMonitor monitor;

    private final int SYNC_TIMER = 2; // TODO: Remover se possível

    public Manager() {
        initialize();
    }

    @Override
    public void create(String path, byte[] data) throws KeeperException, InterruptedException {
        // TODO: Alterar para flags corretas
        zoo.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    @Override
    public void delete(String path) throws KeeperException, InterruptedException {
        zoo.delete(path, zoo.exists(path, false).getVersion());
    }

    @Override
    public Object getNodeData(String path, boolean watchFlag) throws KeeperException, InterruptedException {
        return new String(zoo.getData(path, watchFlag, null), StandardCharsets.UTF_8);
    }

    @Override
    public void update(String path, byte[] data) throws KeeperException, InterruptedException {
        zoo.setData(path, data, zoo.exists(path, false).getVersion());
        TimeUnit.SECONDS.sleep(SYNC_TIMER);
    }

    @Override
    public Boolean exist(String path) throws KeeperException, InterruptedException {
        return zoo.exists(path, false) != null;
    }

    @Override
    public void list(String path) throws KeeperException, InterruptedException {
        StringBuilder nodes = new StringBuilder("Salas: ");
        for (String node : zoo.getChildren(path, false)) {
            if(!node.endsWith("-players")) nodes.append("["+node+"] ");
        }
        // TODO: Formatar para imprimir no máximo 5 salas por linha
        System.out.println(nodes);
    }

    @Override
    public List<String> getNodes(String path) throws KeeperException, InterruptedException {
        return zoo.getChildren(path, false);
    }

    @Override
    public void watch(String path, DataMonitorListener listener) throws KeeperException, InterruptedException {
        if(monitor != null) stopWatching();
        monitor = new DataMonitor(zoo, path, null, listener);
    }

    @Override
    public void stopWatching() {
        if(monitor != null) {
            monitor.kill();
            monitor = null;
        }
    }

    private void initialize(){
        connection = new Connection();
        try {
            zoo = connection.connect("localhost"); // TODO: Receber um IP
        } catch (IOException e) {
            // TODO: Tratar exceção
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO: Tratar exceção
            e.printStackTrace();
        }
    }
}
