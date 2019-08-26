package com.spyfall.game;

import com.spyfall.manager.IManager;
import com.spyfall.manager.Manager;
import com.spyfall.monitor.DataMonitorListener;
import com.spyfall.monitor.Message;
import org.apache.zookeeper.KeeperException;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static org.apache.zookeeper.KeeperException.*;

public class Game implements DataMonitorListener {

    enum GameState {
        MainMenu,
        ConnectingToRoom,
        WaitingPlayers,
        StartingGame,
        Playing,
        Quit,
        }

    enum PlayingState {
        Begining,
        Answering,
        Questioning,
        SpyGuess,
        PlaceGuess,
        Tribunal,
        End,
        }

    private final String gamePath = "/Spyfall";
    private final int MAX_QUESTIONS = 4;// TODO: Mudar para 64 ou 128
    private final int MIN_PLAYERS = 2; // TODO: Mudar para 4 ou 8

    private IManager manager;
    private GameState gameState;
    private Scanner keyboard;
    private Player player;
    private GameRoom gameRoom;
    private PlayingState playingState;

    private int counter;

    public Game(){
        initialize();
    }

    private void initialize(){
        // Inicializa as variáveis principais
        gameState = GameState.MainMenu;
        manager = new Manager();
        keyboard = new Scanner(System.in);

        // Inicializa o jogador
        System.out.println("Entre com seu nome: ");
        player = new Player(keyboard.nextLine());

        // Lógica de fluxo de jogo
        try {
            prepare();
            while(gameState != GameState.Quit){
                gameLoop();
            }
        } catch (KeeperException e) {
            e.printStackTrace();// TODO: Imprimir no log, não em std.out
            gameState = GameState.Quit;
            System.err.println("Zookeeper Fail!");
        } catch (InterruptedException e) {
            gameState = GameState.Quit;
            System.err.println("Execution interrupted!");
        }
    }

    // Cria um nó "Spyfall", que armazena todas as salas de jogo
    private void prepare() throws KeeperException, InterruptedException {
        if(!manager.exist(gamePath)) manager.create(gamePath, "init".getBytes());
        // TODO: Adicionar logs
    }

    // Máquina de estados do jogo
    private void gameLoop() throws KeeperException, InterruptedException {
        switch (gameState){
            case MainMenu: selectRoom(); break;
            case ConnectingToRoom: connectToRoom(); break;
            case WaitingPlayers: waitingRoom(); break;
            case StartingGame: startingGame(); break;
            case Playing: playing(); break;
        }
    }

    // Dá ao jogador a chance de escolher ou criar uma sala, ou sair do jogo
    private void selectRoom() throws KeeperException, InterruptedException {
        // Imprime informações
        System.out.println("Entre com o número de uma sala, ou escreva qualquer outra coisa para sair: ");
        listRooms();

        // Recebe comando do usuário
        String command = keyboard.nextLine().toLowerCase();

        try {
            int roomNumber = Integer.parseInt(command);
            if ( roomNumber < 0 || roomNumber > 9999){
                System.out.println("Entre um número entre 0 e 9999");
            }
            else{
                player.connectToRoom(roomNumber);
                gameRoom = new GameRoom(gamePath, roomNumber);
                gameState = GameState.ConnectingToRoom;
            }
        } catch (NumberFormatException | NullPointerException e){
            gameState = GameState.Quit;
        }
    }

    // Lista as salas existentes
    private void listRooms() throws KeeperException, InterruptedException {
        manager.list(gamePath);
    }

    // Conecta numa sala de jogo
    private void connectToRoom() throws KeeperException, InterruptedException {
        // Cria a sala, caso ela não exista
        if(!manager.exist(gameRoom.path)){
            manager.create(gameRoom.path, "init".getBytes());
            manager.create(gameRoom.path + "-players", "init".getBytes());
            System.out.println("Sala criada!");
        }
        // Cria o player
        manager.create(gameRoom.playersPath + player.getPlayerId(), player.message(Message.MessageType.Connection));
        // Envia mensagem de conecção
        manager.watch(gameRoom.path, this);
        manager.update(gameRoom.path, player.message(Message.MessageType.Connection));
        //
        gameState = GameState.WaitingPlayers;
    }

    private void waitingRoom() throws KeeperException, InterruptedException {
        // Recebe comando do usuário
        String command = keyboard.nextLine().toLowerCase();

        if (command.isEmpty()){
            return;
        }
        else if(Arrays.asList("sair", "quit", "exit").contains(command)) {
            // Desconecta da sala de espera
            manager.update(gameRoom.path, player.message(Message.MessageType.Disconnection));
            manager.delete(gameRoom.playersPath + player.getPlayerId());
            manager.stopWatching();
            gameState = GameState.MainMenu;
        }
        else if(Arrays.asList("list", "lista", "ls").contains(command)){
            gameRoom.listPlayers();
        }
        else if(Arrays.asList("pronto", "preparado", "ready").contains(command)){
            manager.update(gameRoom.path, player.message(Message.MessageType.Ready));
        }
        else if(Arrays.asList("not-ready").contains(command)){
            manager.update(gameRoom.path, player.message(Message.MessageType.NotReady));
        }
        else if(Arrays.asList("start", "iniciar", "começar").contains(command)){
            int readyPlayers = gameRoom.playersReadyInRoom();
            if(readyPlayers >= MIN_PLAYERS) {
                if (gameState == GameState.WaitingPlayers){
                    gameState = GameState.StartingGame;
                    manager.update(gameRoom.path, player.message(Message.MessageType.StartingGame));
                    manager.update(gameRoom.path, player.message(Message.MessageType.DefinePlace, gameRoom.generatePlace()));
                    manager.update(gameRoom.path, player.message(Message.MessageType.DefineSpy, gameRoom.generateSpy()));
                    manager.update(gameRoom.path, player.message(Message.MessageType.PassToken));
                }
            }
            else if(readyPlayers == 0){
                System.out.println("Nenhum jogador está pronto para jogar.");
            }
            else{
                System.out.println(readyPlayers + "/" + gameRoom.playersInRoom() + " jogadores estão prontos para jogar, espere por "+MIN_PLAYERS+".");
            }
        }
        else{
            // Chat Padrão
            manager.update(gameRoom.path, player.message(Message.MessageType.Chat, command));
        }
    }

    private void startingGame() throws InterruptedException {
        System.out.println("Aguarde: " + counter);
        --counter;
        if(counter <= 0){
            playingState = PlayingState.Begining;
            gameState = GameState.Playing;
            counter = MAX_QUESTIONS;
        }
        else{
            TimeUnit.SECONDS.sleep(2);
        }
    }

    private void playing() throws KeeperException, InterruptedException {
        switch (playingState){
            case Begining: playing_begining(); break;
            case Answering: playing_answering(); break;
            case Questioning: playing_questioning(); break;
            case SpyGuess: playing_spyguess(); break;
            case PlaceGuess: playing_placeguess(); break;
            case Tribunal: playing_tribunal(); break;
            case End: playing_end(); break;
        }
    }

    private void playing_begining(){
        if(player.spy){
            System.out.println("Você é o espião!!!");
            System.out.println("Tente descobrir o local antes que te descubram!");
        }
        else{
            System.out.println("O local é: " + gameRoom.place);
            System.out.println("Tente descobrir o espião, sem deixar ele descobrir o local!");
        }

        Player holder = gameRoom.getPlayerWithToken();
        System.out.println(holder.getPlayerName() + " faz a pergunta.");

        if(holder.getPlayerId() == player.getPlayerId()){
            playingState = PlayingState.Questioning;
        }
        else{
            playingState = PlayingState.Answering;
        }
    }

    private void playing_answering() throws KeeperException, InterruptedException {
        // Recebe comando do usuário
        String command = keyboard.nextLine().toLowerCase();
        if(player.hasToken()){
            manager.update(gameRoom.path, player.message(Message.MessageType.Chat, command));
            playingState = PlayingState.Questioning;
        }
        else{
            System.out.println("[Não é sua vez, os outros jogadores não receberão sua mensagem.]");
        }
    }

    private void playing_questioning() throws KeeperException, InterruptedException {
        // Recebe comando do usuário
        String command = keyboard.nextLine().toLowerCase();
        int mentioned = gameRoom.mentionedAnotherPlayer(player, command);

        if(!player.accused && Arrays.asList("acusar").contains(command.split(" ")[0]) && mentioned >= 0){
            playingState = PlayingState.SpyGuess;
        }
        else if(player.spy && Arrays.asList("local").contains(command)){
            playingState = PlayingState.PlaceGuess;
        }
        else if(mentioned >= 0){
            manager.update(gameRoom.path, player.message(Message.MessageType.PassToken, String.valueOf(mentioned)));
            playingState = PlayingState.Answering;
        }
        else{
            System.out.println("[Faça uma pergunta direcionada a um outro jogador...]");
        }
    }

    private void playing_spyguess(){
        // TODO: Implement
    }

    private void playing_placeguess() throws KeeperException, InterruptedException {
        // Recebe comando do usuário
        String command = keyboard.nextLine().toLowerCase();
        if(gameRoom.place.equals(command)){
            manager.update(gameRoom.path, player.message(Message.MessageType.SpyWin));
        }
        else{
            manager.update(gameRoom.path, player.message(Message.MessageType.SpyLose));
        }
    }

    private void playing_tribunal(){
        // TODO: Implement
    }

    private void playing_end(){
        player.state = Player.PlayerState.NotReady;
        gameState = GameState.WaitingPlayers;
    }

    private void updateGameRoom() throws KeeperException, InterruptedException {
        System.out.println("(Updating room info)");
        try {
            for (String node : manager.getNodes(gameRoom.playersPath)) {
                Message m = (Message) manager.getNodeData(node, false);
                gameRoom.addPlayer(m);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(byte[] data) throws KeeperException, InterruptedException {
        Message m = Message.fromBytes(data);

        switch (m.type){
            case NullMessage:
                System.err.println("Received NULL message");
                break;
            case Connection:
                if(!gameRoom.hasPlayer(m.player.getPlayerId())) {
                    System.out.println(m.player.getPlayerName() + " se juntou a sala!");
                    gameRoom.addPlayer(m);
                    updateGameRoom();
                }
                break;
            case Disconnection:
                if(gameRoom.hasPlayer(m.player.getPlayerId())) {
                    System.out.println(m.player.getPlayerName() + " saiu da sala :(");
                    gameRoom.removePlayer(m.player.getPlayerId());
                    player.disconnectFromRoom();
                    if (gameRoom.playersInRoom() <= 0) {
                        // TODO: Destruir sala de forma segura
                        // Destroi a sala quando ela esvazia
                        //manager.delete(gameRoom.path);
                    }
                }
                break;
            case Ready:
                System.out.println(m.player.getPlayerName() + " está pronto!");
                gameRoom.setPlayerState(m.player.getPlayerId(), Player.PlayerState.Ready);
                if(gameRoom.playersReadyInRoom() >= 4)
                    System.out.println("Jogadores suficientes já estão prontos, você já pode iniciar a partida!");
                break;
            case NotReady:
                System.out.println(m.player.getPlayerName() + " não está pronto...");
                gameRoom.setPlayerState(m.player.getPlayerId(), Player.PlayerState.NotReady);
                break;
            case Chat:
                System.out.println(m.player.getPlayerName() + ": " + m.message);
                break;
            case StartingGame:
                if(gameState != GameState.Playing && gameState != GameState.StartingGame){
                    System.out.println("O jogo vai começar!");
                    //if( player.state == Player.PlayerState.Ready ){
                        gameState = GameState.StartingGame;
                        player.state = Player.PlayerState.Playing;
                        gameRoom.setAllPlayersState(Player.PlayerState.Playing);
                        counter = 5;
                        System.out.println("[Aperte ENTER para iniciar a contagem!]");
                    //}
                    //else {
                        // TODO: Corrigir o cancelamento do jogo em caso de falha
                        // gameState = GameState.WaitingPlayers;
                        // player.state = Player.PlayerState.NotReady;
                        // manager.update(gameRoom.path, player.message(Message.MessageType.CantStartGame));
                    //}
                }
                break;
            case CantStartGame:
                System.out.println("O jogo não pôde ser iniciado...");
                gameState = GameState.WaitingPlayers;
                gameRoom.setAllPlayersState(Player.PlayerState.NotReady);
                break;
            case DefinePlace:
                gameRoom.setPlace(m.message);
                break;
            case DefineSpy:
                gameRoom.setSpy(Integer.parseInt(m.message));
                break;
            case PassToken:
                if(m.message.isEmpty()){
                    // Dá o token ao player
                    gameRoom.passToken(m.player.getPlayerId());
                }
                else{
                    // Dá o token ao player mencionado
                    gameRoom.passToken(Integer.parseInt(m.message));
                    System.out.println(m.player.getPlayerName() + " faz a pergunta.");
                }
                break;
            case SpyWin:
                System.out.println("O espião descobriu o lugar!");
                playingState = PlayingState.End;
                break;
            case SpyLose:
                System.out.println("O espião foi descoberto!");
                playingState = PlayingState.End;
                break;
        }
    }

    @Override
    public void closing(Code rc) {
        // TODO: Check disconnect player
        try {
            manager.update(gameRoom.path, player.message(Message.MessageType.Disconnection));
            manager.delete(gameRoom.playersPath + player.getPlayerId());
            manager.stopWatching();
            gameState = GameState.MainMenu;
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
