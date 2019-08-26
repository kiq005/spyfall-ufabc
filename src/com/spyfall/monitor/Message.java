package com.spyfall.monitor;

import com.spyfall.game.Player;

import java.io.*;

public class Message implements Serializable {

    public Player player;
    public int roomId;
    public MessageType type;
    public String message;

    public enum MessageType {
        NullMessage,
        Connection,
        Disconnection,
        Ready,
        NotReady,
        StartingGame,
        CantStartGame,
        DefinePlace,
        DefineSpy,
        PassToken,
        SpyWin,
        SpyLose,
        Chat,
    }

    public Message(Player sender, int roomId, MessageType type, String message){
        this.player = sender;
        this.roomId = roomId;
        this.type = type;
        this.message = message;
    }

    public Message(Player sender, int roomId, MessageType type){
        this.player = sender;
        this.roomId = roomId;
        this.type = type;
        this.message = "";
    }

    public byte[] getBytes(){
        return Message.getBytes(this);
    }

    public static Message fromBytes(byte[] data) {
        Message m = null;
        try {
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
            m = (Message) in.readObject();
            in.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return m;
    }

    public static byte[] getBytes(Message m){
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(m);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bos.toByteArray();
    }
}
