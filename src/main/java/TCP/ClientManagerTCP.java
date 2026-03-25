package TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import ponte_TCP_WEB.AbstractClientManager;
import ponte_TCP_WEB.IClientHandler;
import ponte_TCP_WEB.MessageProcessor;

public class ClientManagerTCP extends AbstractClientManager implements Runnable, IClientHandler {
	/* Socket de comunicação com o cliente */
	private Socket socket;
	/** Referência ao servidor principal */
	private Server server;

	/** Stream de saída para enviar mensagens ao cliente */
	private PrintWriter out;

	/** Stream de entrada para receber mensagens do cliente */
	private BufferedReader in;

	/**
	 * Construtor que inicializa a gestão de um cliente.
	 * 
	 * @param socket Socket TCP do cliente
	 * @param server Referência ao servidor principal
	 */
	public ClientManagerTCP(Socket socket, Server server) {
		this.socket = socket;
		this.server = server;
		try {
			this.out = new PrintWriter(socket.getOutputStream(), true);
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			System.err.println("Erro ao criar handler: " + e.getMessage());
		}
	}

	/**
	 * Método principal executado na thread do cliente. Responsável por receber e
	 * processar mensagens continuamente.
	 */
	@Override
	public void run() {
		try {
			receiveMessages();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				System.err.println("Erro ao fechar socket: " + e.getMessage());
			}
		}
	}

	/**
	 * Envia uma mensagem para o cliente.
	 * 
	 * @param message Mensagem a ser enviada
	 */
	@Override
	public void sendMessage(String message) {
		out.println(message);
	}

	/**
	 * Processa mensagens recebidas do cliente continuamente.
	 * 
	 * <p>
	 * Comandos suportados:
	 * </p>
	 * <ul>
	 * <li>LOGIN - Autenticação do jogador</li>
	 * <li>REGISTO - Criação de novo perfil</li>
	 * <li>CREATE_GAME - Criar novo jogo</li>
	 * <li>LIST_GAMES - Listar jogos disponíveis</li>
	 * <li>JOIN_GAME - Juntar-se a um jogo</li>
	 * <li>MOVE - Realizar jogada</li>
	 * <li>QUIT - Sair do sistema</li>
	 * </ul>
	 */
	private void receiveMessages() {
		try {
			String message;
			while ((message = in.readLine()) != null) {
				MessageProcessor.process(this, message, server);
			}
		} catch (IOException e) {
			System.err.println("Erro ao receber mensagens: " + e.getMessage());
			server.getLoggedPlayers().remove(playerName);
			if (currentGame != null) {
				currentGame.relayMessage(this, "QUIT");
			}
		}
	}

	@Override
	public void closeConnection() {
		try {
			if (socket != null && !socket.isClosed()) {
				socket.close();
			}
		} catch (IOException e) {
			System.err.println("Erro ao fechar socket do cliente TCP: " + e.getMessage());
		}

	}
}