package TCP;

import Jogo.GoBang;
import ponte_TCP_WEB.IClientHandler;

public class GameSession {
	/* Identificador único da sessão de jogo */
	private String gameId;
	/** Gestor do jogador que usa as peças pretas (X) */
	private IClientHandler player1;

	/** Gestor do jogador que usa as peças brancas (O) */
	private IClientHandler player2;

	/** Referência para o servidor principal */
	private Server server;

	/** Lógica do jogo GoBang */
	private GoBang game;

	/** Timestamp de início do jogo (milissegundos) */
	private long gameStart;

	/** Timestamp da última jogada (milissegundos) */
	private long lastChangeTime;

	/** Tempo total acumulado do jogador com peças pretas (ms) */
	private long totalTimeBlack;

	/** Tempo total acumulado do jogador com peças brancas (ms) */
	private long totalTimeWhite;

	/**
	 * Construtor que inicializa uma nova sessão de jogo.
	 * 
	 * @param gameId  Identificador único do jogo
	 * @param player1 Gestor do primeiro jogador (peças pretas/X)
	 * @param player2 Gestor do segundo jogador (peças brancas/O)
	 * @param server  Referência ao servidor principal
	 */
	public GameSession(String gameId, IClientHandler player1, IClientHandler player2, Server server) {
		this.gameId = gameId;
		this.player1 = player1;
		this.player2 = player2;
		this.server = server;
		this.game = new GoBang();
		gameStart = System.currentTimeMillis();
		lastChangeTime = gameStart;
		totalTimeBlack = 0;
		totalTimeWhite = 0;
	}

	/**
	 * Processa e encaminha mensagens recebidas de um dos jogadores.
	 * 
	 * @param sender  Gestor do cliente que enviou a mensagem
	 * @param message Mensagem recebida (pode ser jogada, desistência, etc.)
	 */
	public void relayMessage(IClientHandler sender, String message) {
		if (message.startsWith("MOVE")) {
			processMove(sender, message);
		} else if (message.equals("QUIT")) {
			endGame(sender);
		}
	}

	/**
	 * Processa uma jogada recebida de um jogador.
	 * 
	 * @param sender Gestor do cliente que enviou a jogada
	 * @param move   Mensagem de jogada no formato "MOVE linha coluna"
	 */
	private void processMove(IClientHandler sender, String move) {
		try {
			String[] parts = move.split(" ");
			int row = Integer.parseInt(parts[1]) - 1; // Converte para índice 0-based
			int col = Integer.parseInt(parts[2]) - 1;

			boolean isBlack = (sender == player1);
			uploadTime(isBlack);

			if (game.isValidMove(row, col)) {
				game.makeMove(row, col, isBlack);

				// Prepara mensagem para enviar a ambos os jogadores
				String moveMessage = "MOVE " + (row + 1) + " " + (col + 1) + " " + (isBlack ? "X" : "O");

				lastChangeTime = System.currentTimeMillis();
				printTime(player1);
				printTime(player2);

				// Verifica se a jogada terminou o jogo
				if (game.isGameOver()) {
					uploadTime(isBlack);
					endGame(row, col, isBlack);
				} else {
					// Encaminha a jogada válida para ambos
					player1.sendMessage(moveMessage);
					player2.sendMessage(moveMessage);
				}

			} else {
				sender.sendMessage("JOGADA_INVALIDA");
			}
		} catch (Exception e) {
			sender.sendMessage("ERRO_FORMATO");
		}
	}

	/**
	 * Envia a informação atualizada dos tempos para um jogador.
	 * 
	 * @param player Gestor do cliente que receberá a informação
	 */
	private void printTime(IClientHandler player) {
		player.sendMessage("TEMPOS_#Tempos:_#Pretas (X): " + game.formatarTempo(totalTimeBlack) + "_#Brancas (0): "
				+ game.formatarTempo(totalTimeWhite) + "_#Tempo total: "
				+ game.formatarTempo(System.currentTimeMillis() - gameStart));
	}

	/**
	 * Atualiza os tempos acumulados dos jogadores.
	 * 
	 * @param isBlack Indica se o jogador atual é o das peças pretas
	 */
	private void uploadTime(boolean isBlack) {
		long[] tempos = game.atualizarTempos(isBlack, totalTimeBlack, totalTimeWhite, lastChangeTime);
		totalTimeBlack = tempos[0];
		totalTimeWhite = tempos[1];
		lastChangeTime = tempos[2];
	}

	/**
	 * Termina o jogo com base no estado atual do tabuleiro.
	 * 
	 * @param row     Última linha jogada
	 * @param col     Última coluna jogada
	 * @param isBlack Indica se o último jogador usava peças pretas
	 */
	private void endGame(int row, int col, boolean isBlack) {
		if (game.hasWinner()) {
			String winner = game.isBlackWinner() ? player1.getPlayerName() : player2.getPlayerName();
			printTime(player1);
			printTime(player2);
			if (winner.equals(player1.getPlayerName())) {
				server.xmlManagerScores.addScore(gameId, String.valueOf(game.formatarTempo(totalTimeWhite)),
						String.valueOf(game.formatarTempo(totalTimeBlack)), "vitoria", player1.getPlayerName());
				server.xmlManagerScores.addScore(gameId, String.valueOf(game.formatarTempo(totalTimeBlack)),
						String.valueOf(game.formatarTempo(totalTimeWhite)), "derrota", player2.getPlayerName());
			} else {
				server.xmlManagerScores.addScore(gameId, String.valueOf(game.formatarTempo(totalTimeBlack)),
						String.valueOf(game.formatarTempo(totalTimeWhite)), "vitoria", player2.getPlayerName());
				server.xmlManagerScores.addScore(gameId, String.valueOf(game.formatarTempo(totalTimeWhite)),
						String.valueOf(game.formatarTempo(totalTimeBlack)), "derrota", player1.getPlayerName());
			}
			player1.sendMessage("FIM_JOGO VENCEDOR " + winner + " " + row + " " + col + " " + isBlack);
			player2.sendMessage("FIM_JOGO VENCEDOR " + winner + " " + row + " " + col + " " + isBlack);
		} else {
			printTime(player1);
			printTime(player2);

			server.xmlManagerScores.addScore(gameId, String.valueOf(game.formatarTempo(totalTimeWhite)),
					String.valueOf(game.formatarTempo(totalTimeBlack)), "empate", player1.getPlayerName());
			server.xmlManagerScores.addScore(gameId, String.valueOf(game.formatarTempo(totalTimeBlack)),
					String.valueOf(game.formatarTempo(totalTimeWhite)), "empate", player2.getPlayerName());
			player1.sendMessage("FIM_JOGO EMPATE");
			player2.sendMessage("FIM_JOGO EMPATE");
		}

		server.removeGame(gameId, player1, player2);
	}

	/**
	 * Termina o jogo por desistência de um jogador.
	 * 
	 * @param sender Gestor do cliente que desistiu
	 */
	private void endGame(IClientHandler sender) {
		if (sender.getPlayerName().equals(player1.getPlayerName())) {
			server.xmlManagerScores.addScore(gameId, String.valueOf(game.formatarTempo(totalTimeBlack)),
					String.valueOf(game.formatarTempo(totalTimeWhite)), "vitoria", player2.getPlayerName());
			player2.sendMessage("FIM_JOGO DESISTENCIA");
			printTime(player2);
		} else {
			server.xmlManagerScores.addScore(gameId, String.valueOf(game.formatarTempo(totalTimeWhite)),
					String.valueOf(game.formatarTempo(totalTimeBlack)), "vitoria", player1.getPlayerName());
			player1.sendMessage("FIM_JOGO DESISTENCIA");
			printTime(player1);
		}

		server.removeGame(gameId, player1, player2);
	}
}