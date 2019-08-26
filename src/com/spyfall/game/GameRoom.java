package com.spyfall.game;

import com.spyfall.monitor.Message;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class GameRoom {

    private final String[] places = new String[]{"avião", "parque de diversões", "banco", "praia", "circo", "hotel", "base militar", "navio pirata", "estação espacial", "casino"};

    public String path;
    public String playersPath;
    public String place;

    private int roomId;
    private HashMap<Integer, Player> players;

    public GameRoom(String gamePath, int roomId){
        this.path = gamePath + "/" + roomId;
        this.playersPath = this.path + "-players/";
        this.roomId = roomId;
        this.players = new HashMap<>();
    }

    public boolean hasPlayer(int playerId){
        return players.containsKey(playerId);
    }

    public void addPlayer(Player player){
        if(players.containsKey(player.getPlayerId())) return;
        players.put(player.getPlayerId(), player);
    }

    public void addPlayer(Message playerMessage){
        if(players.containsKey(playerMessage.player.getPlayerId())){
            players.replace(playerMessage.player.getPlayerId(), playerMessage.player);
        }
        else{
            players.put(playerMessage.player.getPlayerId(), playerMessage.player);
        }
    }

    public void removePlayer(int playerId){
        if (players.containsKey(playerId)) players.remove(playerId);
    }

    public void setPlayerState(int playerId, Player.PlayerState state){
        if (players.containsKey(playerId)) players.get(playerId).state = state;
    }

    public int playersInRoom(){
        return players.size();
    }

    public int playersReadyInRoom(){
        int ready = 0;
        for(Player p : players.values()){
            if (p.state == Player.PlayerState.Ready) ++ready;
        }
        return ready;
    }

    public void listPlayers(){
        for(Player p : players.values()){
            System.out.println("(" + p.getPlayerName() + ")");
        }
    }

    public void setAllPlayersState(Player.PlayerState state){
        for(Player p : players.values()){
            p.state = state;
        }
    }

    public String generatePlace(){
        return places[new Random().nextInt(places.length)];
    }

    public String generateSpy(){
        int idx = new Random().nextInt(players.size());
        int playerId = (int) players.keySet().toArray()[idx];
        return String.valueOf(playerId);
    }

    public void setPlace(String place){
        this.place = place;
    }

    public void setSpy(int playerId){
        for (Player p : players.values()){
            p.spy = p.getPlayerId() == playerId;
        }
    }

    public void passToken(int playerId){
        for (Player p : players.values()){
            p.passToken(p.getPlayerId() == playerId);
        }
    }

    public Player getPlayerWithToken(){
        for (Player p : players.values()){
            if(p.hasToken()) return p;
        }
        return null;
    }

    public int mentionedAnotherPlayer(Player player, String message){
        for (Player p : players.values()){
            if(p.getPlayerId() == player.getPlayerId()) continue;
            if(message.contains(p.getPlayerName())) return p.getPlayerId();
        }
        return -1;
    }
}
