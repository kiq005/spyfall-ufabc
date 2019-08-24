package com.spyfall.game;

import com.spyfall.manager.IManager;
import com.spyfall.manager.Manager;
import org.apache.zookeeper.KeeperException;

import java.util.Arrays;
import java.util.Scanner;

public class Game {

    enum GameState {
        MainMenu,
        ConnectingToRoom,
        WaitingPlayers,
        Playing,
        Quit

        }
    private final String gamePath = "/Spyfall";
    private String gameRoom;

    private IManager manager;
    private GameState gameState;
    private Scanner keyboard;

    public Game(){
        initialize();
    }

    private void initialize(){
        // Inicializa as variáveis principais
        gameState = GameState.MainMenu;
        manager = new Manager();
        keyboard = new Scanner(System.in);

        // Lógica de fluxo de jogo
        try {
            prepare();
            // TODO: Obter e salvar nome do jogador
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
        // TODO: Criar uma classe "Message", com informações de origem e dados, para padronizar a troca de mensagens
    }

    // Máquina de estados do jogo
    private void gameLoop() throws KeeperException, InterruptedException {
        switch (gameState){
            case MainMenu: selectRoom(); break;
            case ConnectingToRoom: connectToRoom(); break;
            case WaitingPlayers: waitingRoom(); break;
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
                gameRoom = "/" + roomNumber;
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
        if(!manager.exist(gamePath+gameRoom)){
            manager.create(gamePath+gameRoom, "init".getBytes());
            System.out.println("Sala criada!");
        }
        // Envia mensagem de conecção
        manager.update(gamePath+gameRoom, "Player connected!".getBytes());
        gameState = GameState.WaitingPlayers;
        System.out.println("Conectado!");
    }

    private void waitingRoom() throws KeeperException, InterruptedException {
        // Recebe comando do usuário
        String command = keyboard.nextLine().toLowerCase();

        if(Arrays.asList("sair", "quit", "exit").contains(command)) {
            // Desconecta da sala de espera
            manager.update(gamePath+gameRoom, "Player disconnected :(".getBytes());
            gameState = GameState.MainMenu;
            // TODO: Ao sair, se a sala estiver vazia, deve remover a sala
        }
        // TODO: Permitir troca de mensagens
        // TODO: Notificar conecção de outros players
        // TODO: Mudar de estado do player (waiting, ready)
        // TODO: Quando 4 ou mais jogadores estiverem prontos, permitir iniciar o jogo
    }
}
