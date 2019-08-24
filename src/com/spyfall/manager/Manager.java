/*
 * Reference: https://www.baeldung.com/java-zookeeper
 */
package com.spyfall.manager;

import com.spyfall.connection.Connection;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Manager implements IManager {
    private static ZooKeeper zoo;
    private static Connection connection;

    public Manager() {
        initialize();
    }

    @Override
    public void create(String path, byte[] data) throws KeeperException, InterruptedException {
        // TODO: Alterar para flags corretas
        zoo.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    @Override
    public Object getNodeData(String path, boolean watchFlag) throws KeeperException, InterruptedException {
        return new String(zoo.getData(path, null, null), StandardCharsets.UTF_8);
    }

    @Override
    public void update(String path, byte[] data) throws KeeperException, InterruptedException {
        zoo.setData(path, data, zoo.exists(path, true).getVersion());
    }

    @Override
    public Boolean exist(String path) throws KeeperException, InterruptedException {
        return zoo.exists(path, true) != null;
    }

    @Override
    public void list(String path) throws KeeperException, InterruptedException {
        StringBuilder nodes = new StringBuilder();
        for (String node : zoo.getChildren(path, false)) {
            nodes.append("["+node+"] ");
        }
        // TODO: Formatar para imprimir no máximo 5 salas por linha
        System.out.println(nodes);
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
