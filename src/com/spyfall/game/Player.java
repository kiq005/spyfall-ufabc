package com.spyfall.game;

import com.spyfall.monitor.Message;

import java.io.Serializable;
import java.util.Random;

public class Player implements Serializable {

    public enum PlayerState {
        NotReady,
        Ready,
        Playing,
    }

    private String playerName;
    private int playerId;
    private int roomId;
    public PlayerState state;

    public boolean accused;
    public boolean spy;
    private boolean hasToken;
    private boolean lastToken;

    public Player(String playerName){
        this.playerName = playerName;
        this.playerId = new Random().nextInt(Integer.MAX_VALUE);
        this.roomId = -1;
        this.spy = false;
        this.hasToken = false;
        this.accused = false;
    }

    public Player(String playerName, int playerId, int roomId){
        this.playerName = playerName;
        this.playerId = playerId;
        this.roomId = roomId;
        this.spy = false;
        this.hasToken = false;
        this.accused = false;
    }

    public byte[] message(Message.MessageType type){
        return new Message(this, roomId, type).getBytes();
    }

    public byte[] message(Message.MessageType type, String message){
        return new Message(this, roomId, type, message).getBytes();
    }

    public void connectToRoom(int roomId){
        this.roomId = roomId;
        this.state = PlayerState.NotReady;
        this.spy = false;
        this.hasToken = false;
        this.lastToken = false;
        this.accused = false;
    }

    public void disconnectFromRoom(){
        this.roomId = -1;
        this.state = PlayerState.NotReady;
        this.spy = false;
        this.hasToken = false;
        this.lastToken = false;
        this.accused = false;
    }

    public String getPlayerName(){
        return this.playerName;
    }

    public int getPlayerId(){
        return this.playerId;
    }

    public void passToken(boolean hasToken){
        this.lastToken = this.hasToken;
        this.hasToken = hasToken;
    }

    public boolean hasToken(){
        return hasToken;
    }

    public void setAccused(boolean did){
        accused = did;
    }
}
