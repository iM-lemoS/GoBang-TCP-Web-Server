package TCP;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.websocket.server.WsSci;
import WEB.ClientManagerWEB;
import XML.XMLManagerPlayers;
import XML.XMLManagerScores;
import ponte_TCP_WEB.IClientHandler;

public class Server {
	/* Porta padrão onde o servidor escuta conexões TCP */
	private static final int TCP_PORT = 5025;
	/** Porta padrão onde o servidor escuta conexões WEB */
	private static final int WEB_PORT = 8080;

	/** Socket do servidor para aceitar conexões */
	private ServerSocket serverSocket;

	/** Mapa de jogadores à espera de oponente (jogador -> password do jogo) */
	private HashMap<IClientHandler, String> waitingGames = new HashMap<>();

	/** Lista de jogadores autenticados no sistema */
	private List<String> loggedPlayers = new ArrayList<>();

	/** Mapa de sessões de jogo ativas (ID do jogo -> sessão) */
	private Map<String, GameSession> activeGames = new HashMap<>();

	/** Caminho para o ficheiro XML com dados dos jogadores */
	private String xmlFilePlayers = "PerfilXML.xml";

	/** Caminho para o ficheiro XML com dados dos jogos */
	private String xmlFileScores = "XMLScores.xml";

	/** Gestor para operações com o ficheiro XML dos jogadores */
	private XMLManagerPlayers xmlManager = new XMLManagerPlayers(xmlFilePlayers);

	/** Gestor para operações com o ficheiro XML dos jogos */
	public XMLManagerScores xmlManagerScores = new XMLManagerScores(xmlFileScores);

	/**
	 * Método principal que inicia o servidor.
	 * 
	 * @param args Argumentos da linha de comandos (não utilizado)
	 */
	public static void main(String[] args) {
		new Server().start();
	}

	/**
	 * Inicia o servidor e fica à escuta de novas conexões. Cria uma nova thread
	 * para cada cliente conectado.
	 */
	public void start() {
		// Passa a instância do servidor para o handler de WebSocket
		ClientManagerWEB.setServer(this);

		// 1. Iniciar Servidor Web/WebSocket (em uma nova thread para não bloquear)
		new Thread(this::startWebServer).start();

		// 2. Iniciar Servidor TCP (na thread principal ou em outra)
		startTcpServer();
	}

	private void startWebServer() {
		Tomcat tomcat = new Tomcat();
		tomcat.setPort(WEB_PORT);
		tomcat.getConnector(); // Necessário para inicializar

		// Contexto para a aplicação web
		Context ctx = tomcat.addWebapp("", new File("src/main/webapp/").getAbsolutePath());
		ctx.addServletContainerInitializer(new WsSci(), null); // Habilita WebSockets

		try {
			System.out.println("Servidor Web/WebSocket iniciado na porta " + WEB_PORT);
			tomcat.start();
			tomcat.getServer().await(); // Mantém o servidor a correr
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void startTcpServer() {
		try {
			serverSocket = new ServerSocket(TCP_PORT);
			System.out.println("Servidor GoBang TCP iniciado na porta " + TCP_PORT);

			while (true) {
				Socket clientSocket = serverSocket.accept();
				System.out.println("Nova conexão TCP: " + clientSocket.getInetAddress());

				IClientHandler handler = new ClientManagerTCP(clientSocket, this);
				new Thread(handler::run).start();
			}
		} catch (IOException e) {
			System.err.println("Erro no servidor TCP: " + e.getMessage());
		}
	}

	/**
	 * Adiciona um jogador à lista de espera por um oponente.
	 * 
	 * @param player   Gestor do cliente que criou o jogo
	 * @param password Password definida para o jogo (pode ser vazia)
	 */
	public synchronized void addWaitingGame(IClientHandler player, String password) {
		getWaitingGames().put(player, password);
		System.out.println("Jogador " + player.getPlayerName() + " criou jogo com password: " + password);

		System.out.println("Lista de jogadores com login: " + getLoggedPlayers());
	}

	/**
	 * Cria uma nova sessão de jogo entre dois jogadores.
	 * 
	 * @param host            Jogador que criou o jogo (anfitrião)
	 * @param playerConnected Jogador que se juntou ao jogo
	 */
	public void createGameSession(IClientHandler host, IClientHandler playerConnected) {
		// Remove ambos os jogadores da lista de espera
		getWaitingGames().remove(host);
		getWaitingGames().remove(playerConnected);

		// Cria ID único para o jogo
		String gameId = createGameID(host.getPlayerName(), playerConnected.getPlayerName());

		// Decide aleatoriamente quem começa (X ou O)
		boolean startingPlayer = new Random().nextBoolean();
		GameSession game;

		if (startingPlayer) {
			game = new GameSession(gameId, host, playerConnected, this);
		} else {
			game = new GameSession(gameId, playerConnected, host, this);
		}

		activeGames.put(gameId, game);

		// Associa o jogo a ambos os jogadores
		host.setCurrentGame(game);
		playerConnected.setCurrentGame(game);

		// Notifica os jogadores sobre o oponente e o símbolo atribuído
		host.sendMessage("OPONENTE_ENCONTRADO " + playerConnected.getPlayerName() + " " + (startingPlayer ? "X" : "O"));
		playerConnected.sendMessage("OPONENTE_ENCONTRADO " + host.getPlayerName() + " " + (startingPlayer ? "O" : "X"));

		System.out.println("Jogo criado: " + gameId);
	}

	/**
	 * Remove uma sessão de jogo terminada.
	 * 
	 * @param gameId  ID do jogo a remover
	 * @param player1 Primeiro jogador da sessão
	 * @param player2 Segundo jogador da sessão
	 */
	public synchronized void removeGame(String gameId, IClientHandler player1, IClientHandler player2) {
		getLoggedPlayers().remove(player1.getPlayerName());
		getLoggedPlayers().remove(player2.getPlayerName());
		activeGames.remove(gameId);
	}

	/**
	 * Gera um ID único para uma sessão de jogo.
	 * 
	 * @param player1 Nome do primeiro jogador
	 * @param player2 Nome do segundo jogador
	 * @return String com ID formatado contendo hora, data e nomes dos jogadores
	 */
	private String createGameID(String player1, String player2) {
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		String timeFormatted = now.format(timeFormat);
		String dateFormatted = now.format(dateFormat);

		String gameID = String.format("Hora: %s | Data: %s | Player1: %s | Player2: %s", timeFormatted, dateFormatted,
				player1, player2);

		return gameID;
	}

	public XMLManagerPlayers getXmlManager() {
		return xmlManager;
	}

	public void setXmlManager(XMLManagerPlayers xmlManager) {
		this.xmlManager = xmlManager;
	}

	public List<String> getLoggedPlayers() {
		return loggedPlayers;
	}

	public void setLoggedPlayers(List<String> loggedPlayers) {
		this.loggedPlayers = loggedPlayers;
	}

	public HashMap<IClientHandler, String> getWaitingGames() {
		return waitingGames;
	}

	public void setWaitingGames(HashMap<IClientHandler, String> waitingGames) {
		this.waitingGames = waitingGames;
	}
}