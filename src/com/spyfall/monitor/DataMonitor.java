/*
 * Reference: https://zookeeper.apache.org/doc/r3.1.2/javaExample.html
 */
package com.spyfall.monitor;

import java.util.Arrays;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

public class DataMonitor implements Watcher, AsyncCallback.StatCallback {
    ZooKeeper zoo;
    String path;
    Watcher chainedWatcher;
    boolean dead;

    DataMonitorListener listener;

    byte prevData[];

    public DataMonitor(ZooKeeper zoo, String path, Watcher chainedWatcher, DataMonitorListener listener){
        this.zoo = zoo;
        this.path = path;
        this.chainedWatcher = chainedWatcher;
        this.listener = listener;

        zoo.exists(path, true, this, null);
    }

    public void kill(){
        dead = true;
    }

    public void process(WatchedEvent event){
        String path = event.getPath();
        if (event.getType() == Event.EventType.None) {
            switch (event.getState()) {
                case SyncConnected:
                    break;
                case Expired:
                    dead = true;
                    listener.closing(KeeperException.Code.SESSIONEXPIRED);
                    break;
            }
        } else {
            if (path != null && path.equals(this.path)){
                zoo.exists(path, true, this, null);
            }
        }
        if (chainedWatcher != null){
            chainedWatcher.process(event);
        }
    }


    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        if (dead) return;// Esta linha deve executar no máximo uma vez!
        boolean exists;
        KeeperException.Code code = KeeperException.Code.get(rc);

        switch (KeeperException.Code.get(rc)){
            case OK:
                exists = true;
                break;
            case NONODE:
                exists = false;
                break;
            case SESSIONEXPIRED:
            case NOAUTH:
                dead = true;
                listener.closing(code);
                return;
            default:
                zoo.exists(path, true, this, null);
                return;
        }

        byte data[] = null;
        if (exists) {
            try {
                data = zoo.getData(path, false, null);
                zoo.exists(path, true, this, null);// TODO: Remover esta linha e adicionar um novo monitor para cada evento
            } catch (KeeperException e){
                e.printStackTrace();
            } catch (InterruptedException e){
                return;
            }
        }
        if ((data == null && data != prevData) || (data != null && !Arrays.equals(prevData, data))){
            try {
                listener.process(data);
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                // Do Nothing...
            }
            prevData = data;
        }
    }
}
