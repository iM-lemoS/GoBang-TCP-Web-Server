package TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import Jogo.GoBang;

public class ClientTCP {
	/* Endereço do servidor (localhost por padrão) */
	private static final String HOST = "localhost";
	/** Porta de conexão com o servidor */
	private static final int PORT = 5025;

	/** Socket de comunicação com o servidor */
	private Socket socket;

	/** Stream de saída para enviar mensagens */
	private PrintWriter out;

	/** Stream de entrada para receber mensagens */
	private BufferedReader in;

	/** Scanner para leitura de input do utilizador */
	private Scanner scanner;

	/** Instância do jogo GoBang (lógica do tabuleiro) */
	private GoBang game;

	/** Nome do jogador */
	private String playerName;

	/** Password do jogador */
	private String password;

	/** Nacionalidade do jogador */
	private String nationality;

	/** Sexo do jogador */
	private String gender;

	/** Foto/avatar do jogador */
	private String photo;

	/** Idade do jogador */
	private int age;

	/** Flag que indica se o login foi bem sucedido */
	private boolean loginSuccess;

	/** Flag que indica se um jogo está em curso */
	private boolean gameStart = false;

	/** Flag que indica se é a vez do jogador */
	private boolean myTurn = false;

	/** Peça atribuída ao jogador (X ou O) */
	private char myPiece;

	/** Número total de jogos feitos */
	int totalGames;

	/** Número total de jogos ganhos */
	int wins;

	/** Número total de jogos perdidos */
	int losses;

	/** Número total de jogos empatados */
	int draws;

	/**
	 * Método principal que inicia o cliente.
	 * 
	 * @param args Argumentos da linha de comandos (não utilizado)
	 */
	public static void main(String[] args) {
		new ClientTCP().start();
	}

	/**
	 * Inicia o cliente e estabelece conexão com o servidor. Responsável pelo fluxo
	 * principal da aplicação.
	 */
	public void start() {
		try {
			scanner = new Scanner(System.in);
			game = new GoBang();

			// Estabelece conexão com o servidor
			socket = new Socket(HOST, PORT);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			// Thread para receber mensagens do servidor assincronamente
			new Thread(this::receiveMessages).start();

			System.out.println("Bem-vind@ ao GoBang!");
			System.out.println();

			// Menu inicial de autenticação
			System.out.println("Tens um perfil de jogador já registado? (sim/não)");
			String response = scanner.nextLine().toLowerCase();

			boolean validResponse = false;
			while (!validResponse) {
				switch (response.toLowerCase()) {
				case "sim":
					processLogin();
					while (!loginSuccess) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						} // Espera pelo sucesso do login
					}
					validResponse = true;
					break;

				case "não":
				case "nao":
					processRegisto();
					while (!loginSuccess) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						} // Espera pelo sucesso do registo
					}
					validResponse = true;
					break;

				default:
					System.out.println("Resposta inválida. Por favor, responda 'sim' ou 'não'.");
					response = scanner.nextLine();
					break;
				}
			}

			// Mostra menu principal após autenticação
			showLobbyMenu();

			// Loop principal durante o jogo
			while (gameStart) {
				if (myTurn) {
					handlePlayerTurn();
				}

				// Pequena pausa para evitar consumo excessivo de CPU
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

		} catch (IOException e) {
			System.err.println("Erro no cliente: " + e.getMessage());
		}
	}

	/**
	 * Processa o login do jogador. Pede credenciais e envia para o servidor.
	 */
	private void processLogin() {
		System.out.println("Qual é o teu nickname? ");
		playerName = scanner.nextLine();

		System.out.println("Introduz a tua password: ");
		password = scanner.nextLine();

		sendMessage("LOGIN " + playerName + " " + password);
	}

	/**
	 * Processa o registo de um novo jogador. Pede informações detalhadas e valida
	 * os inputs.
	 */
	private void processRegisto() {
		System.out.println("Qual é o nickname que queres atribuir ao teu perfil? ");
		playerName = scanner.nextLine();

		System.out.println("De que país és? ");
		nationality = scanner.nextLine();

		System.out.println("Que idade tens? ");
		boolean validEntrance = false;
		while (!validEntrance) {
			String input = scanner.nextLine();

			try {
				age = Integer.parseInt(input);
				validEntrance = true;
			} catch (NumberFormatException e) {
				System.out.println("Digita apenas números inteiros (sem letras ou decimais): ");
			}
		}

		System.out.println("És do sexo masculino ou feminino? (caso nenhum dos dois indica o teu sexo biológico):");
		gender = scanner.nextLine();
		while (!gender.toUpperCase().equals("FEMININO") || !gender.toUpperCase().equals("MASCULINO")) {
			System.out.println("Parece que não introduzis-te um género válido. Tenta de novo: ");
			gender = scanner.nextLine();
		}

		System.out.println("Indica a password de acesso: ");
		password = scanner.nextLine();

		System.out.println("Indica o caminho para uma foto que queiras como foto de perfil: ");
		validEntrance = false;
		while (!validEntrance) {
			photo = scanner.nextLine().trim();

			if (isImageValid(photo)) {
				break;
			} else {
				System.out.println("Caminho inválido ou a imagem não foi encontrada. Tenta novamente: ");
			}
		}

		sendMessage("REGISTO_#" + playerName + "_#" + nationality + "_#" + age + "_#" + password + "_#" + photo);
	}

	/**
	 * Mostra o menu principal do lobby. Permite criar jogos, listar jogos
	 * disponíveis ou juntar-se a um jogo.
	 */
	private void showLobbyMenu() {
		System.out.println("\n=== MENU PRINCIPAL ===");
		System.out.println("1. Perfil de jogador");
		System.out.println("2. Criar um jogo novo");
		System.out.println("3. Lista de jogos disponíveis");
		System.out.println("4. Ligar a um jogo");
		System.out.println("5. Sair");
		System.out.println();
		System.out.print("Escolhe uma opção: ");

		boolean tryMenu = true;
		while (tryMenu) {
			String choiceMenu = scanner.nextLine();
			String gameAccessPassword = " ";

			switch (choiceMenu) {
			case "1":
				System.out.println();
				System.out.println("\n=== MENU DE PERFIL DO JOGADOR ===");
				System.out.println();
				sendMessage("ACCOUNT");
				tryMenu = false;
				break;
			case "2":
				System.out.println();
				System.out.println("1. Criar jogo PÚBLICO");
				System.out.println("2. Criar jogo PRIVADO");
				System.out.println();
				System.out.print("Escolhe uma opção: ");

				boolean tryPublicPrivate = true;
				while (tryPublicPrivate) {
					String choicePublicPrivate = scanner.nextLine();
					switch (choicePublicPrivate) {
					case "1":
						tryPublicPrivate = false;
						break;
					case "2":
						tryPublicPrivate = false;
						System.out.println();
						System.out.println("Define uma password de acesso ao teu jogo: ");
						gameAccessPassword = scanner.nextLine();
						break;
					default:
						System.out.println("Opção inválida, tenta outra vez: ");
					}
				}

				sendMessage("CREATE_GAME " + gameAccessPassword);
				System.out.println("Jogo criado com sucesso! Estás agora na lista de jogos disponíveis.");
				System.out.println("Aguarda que outro jogador se ligue ao teu jogo...");
				System.out.println();
				tryMenu = false;
				break;

			case "3":
				sendMessage("LIST_GAMES");
				tryMenu = false;
				break;

			case "4":
				System.out.print("Digite o nome do host do jogo ao qual se quer juntar: ");
				String gameName = scanner.nextLine();
				sendMessage("JOIN_GAME " + gameName);
				tryMenu = false;
				break;

			case "5":
				sendMessage("QUIT");
				System.out.println();
				System.out.println("Adeus!");
				System.exit(0);
				break;

			default:
				System.out.println("Opção inválida, tenta outra vez: ");
			}
		}
	}

	/**
	 * Mostra o menu de perfil do jogador com estatísticas e opções de gestão.
	 * 
	 * @param totalGames Total de jogos realizados
	 * @param wins       Número de vitórias
	 * @param losses     Número de derrotas
	 * @param draws      Número de empates
	 */
	private void showProfileMenu(int totalGames, int wins, int losses, int draws) {
		System.out.println("Total de jogos: " + totalGames);
		System.out.println();
		System.out.println("Vitórias: " + wins);
		System.out.println("Derrotas: " + losses);
		System.out.println("Empates: " + draws);
		System.out.println();
		System.out.println("1. Consultar histórico de partidas");
		System.out.println("2. Trocar password da conta");
		System.out.println("3. Trocar foto de perfil");
		System.out.println("4. Voltar ao menu principal");
		System.out.println();
		System.out.print("Escolhe uma opção: ");
		boolean tryMenuProfile = true;
		while (tryMenuProfile) {
			String choiceMenuProfile = scanner.nextLine();
			switch (choiceMenuProfile) {
			case "1":
				tryMenuProfile = false;
				System.out.println("Histórico de partidas: ");
				System.out.println();
				sendMessage("HISTORY");
				break;
			case "2":
				tryMenuProfile = false;
				System.out.println("Indica qual é a tua antiga password: ");
				String oldPassword = scanner.nextLine();
				sendMessage("VERIFY_OLD_PASSWORD " + oldPassword);
				break;
			case "3":
				tryMenuProfile = false;
				System.out.println("Indica o caminho para uma foto que queiras como foto de perfil: ");
				boolean validEntrance = false;
				while (!validEntrance) {
					photo = scanner.nextLine().trim();
					if (photo.isEmpty()) {
						System.out.println("Caminho vazio. Tenta novamente: ");
						continue;
					}

					if (isImageValid(photo)) {
						break;
					} else {
						System.out.println("Caminho inválido ou a imagem não foi encontrada. Tenta novamente: ");
					}
				}
				sendMessage("CHANGE_PHOTO_#" + photo);
				System.out.println();
				System.out.println("A tua foto foi atualizada.");
				System.out.println();
				sendMessage("ACCOUNT");
				break;
			case "4":
				tryMenuProfile = false;
				showLobbyMenu();
				break;
			default:
				System.out.println("Opção inválida, tenta outra vez: ");
			}
		}
	}

	/**
	 * Recebe e processa mensagens do servidor continuamente. Executado numa thread
	 * separada para não bloquear a interface.
	 */
	private void receiveMessages() {
		try {
			String message;
			while ((message = in.readLine()) != null) {
				String[] parts;
				String command;

				// Extrai o comando principal (pode usar separador "_#" ou espaço)
				String[] partsCustom = message.split("_#");
				if (partsCustom.length > 1) {
					command = partsCustom[0];
				} else {
					command = message.split(" ")[0];
				}

				switch (command) {
				case "LOGIN_FAIL":
					System.out.println("Parece que o teu nickname ou password estão errados.");
					System.out.println("Ou estás a tentar fazer login com uma conta que já está online.");
					System.out.println();
					processLogin();
					break;

				case "LOGIN_SUCCESS":
					parts = message.split(" ");
					gender = parts[1];
					if (gender.toUpperCase().equals("MASCULINO")) {
						System.out.println("Muito bem " + playerName + ". Bem-vindo de volta!");
					} else {
						System.out.println("Muito bem " + playerName + ". Bem-vinda de volta!");
					}
					loginSuccess = true;
					break;

				case "REGIST_FAIL":
					System.out.println("Nickname já existe. Escolhe outro: ");
					playerName = scanner.nextLine();
					sendMessage("REGISTO_#" + playerName + "_#" + nationality + "_#" + age + "_#" + password + "_#"
							+ photo);
					break;

				case "REGIST_FAIL_COUNTRY":
					System.out.println("O país que inseriste não é válido. Indica um existente: ");
					nationality = scanner.nextLine();
					sendMessage("REGISTO_#" + playerName + "_#" + nationality + "_#" + age + "_#" + password + "_#"
							+ photo);
					break;

				case "REGIST_SUCCESS":
					if (gender.toUpperCase().equals("MASCULINO")) {
						System.out.println("Foste registado " + playerName);
					} else {
						System.out.println("Foste registada " + playerName);
					}
					loginSuccess = true;
					break;

				case "LIST_GAMES":
					parts = message.split(" ");
					int numberOfGames = Integer.parseInt(parts[1]);
					System.out.println();
					System.out.println("Lista de jogos disponíveis:");
					System.out.println();
					if (numberOfGames < 1) {
						System.out.println("Não há jogos disponíveis de momento. Cria um novo.");
						System.out.println();
					}
					for (int i = 0; i < numberOfGames; i++) {
						System.out.println(String.valueOf(i + 1) + ". " + parts[i + 2]);
					}
					showLobbyMenu();
					break;

				case "MENU_PROFILE":
					parts = message.split(" ");
					totalGames = Integer.parseInt(parts[1]);
					wins = Integer.parseInt(parts[2]);
					losses = Integer.parseInt(parts[3]);
					draws = Integer.parseInt(parts[4]);
					showProfileMenu(totalGames, wins, losses, draws);
					break;

				case "INCORRECT_OLD_PASSWORD":
					System.out.println("A password que inseriste não corresponde à tua antiga.");
					System.out.println("Verifica e indica qual é a tua antiga password: ");
					String oldPassword = scanner.nextLine();
					sendMessage("VERIFY_OLD_PASSWORD " + oldPassword);
					break;
				case "CORRECT_OLD_PASSWORD":
					System.out.println("Muito bem, a password está correta.");
					System.out.println("Define uma nova password (sem espaços): ");
					String newPassword = scanner.nextLine();
					sendMessage("CHANGE_PASSWORD " + newPassword);
					System.out.println();
					System.out.println("A nova password foi definida com sucesso.");
					System.out.println();
					showProfileMenu(totalGames, wins, losses, draws);
					break;

				case "HISTORY":

					System.out.println(
							"==========================================================================================================================================");
					System.out.printf("%-12s | %-12s | %-20s | %-20s | %-12s | %-12s | %-10s%n", "Hora", "Data",
							"Jogador 1", "Jogador 2", "Tempo Pessoal", "Tempo do adversário", "Resultado");
					System.out.println(
							"==========================================================================================================================================");

					if (message.trim().equals("HISTORY")) {
						System.out.println("Nenhum histórico de jogos disponível.");
						System.out.println(
								"==========================================================================================================================================");
					} else {
						String historyData = message.substring("HISTORY_#".length());
						String[] games = historyData.split("\\]_#\\[");
						for (int i = 0; i < games.length; i++) {
							String game = (i == 0 ? games[i] + "]"
									: "[" + games[i] + (i == games.length - 1 ? "" : "]"));

							game = game.replace("[", "").replace("]", "");

							String[] components = game.split(", ");

							String[] mainInfo = components[0].split(" \\| ");

							String tempoPessoal = components[1].replace("Tempo pessoal: ", "");
							String tempoAdversario = components[2].replace("Tempo do adversário: ", "");
							String resultado = components[3];

							String hora = mainInfo[0].replace("Hora: ", "");
							String data = mainInfo[1].replace("Data: ", "");
							String player1 = mainInfo[2].replace("Player1: ", "");
							String player2 = mainInfo[3].replace("Player2: ", "");

							System.out.printf("%-12s | %-12s | %-20s | %-20s | %-12s | %-12s | %-10s%n", hora, data,
									player1, player2, tempoPessoal, tempoAdversario, resultado);
						}

						System.out.println(
								"==========================================================================================================================================");
					}

					System.out.println();
					showProfileMenu(totalGames, wins, losses, draws);
					break;

				case "ASK_PASSWORD":
					System.out.println("Insira a password: ");
					String passwordGame = scanner.nextLine();
					sendMessage("RECEIVED_PASSWORD " + passwordGame);
					break;

				case "PASSWORD_INCORRECT":
					System.out.println("Password incorreta. Tenta novamente: ");
					String passwordGameRepeat = scanner.nextLine();
					sendMessage("RECEIVED_PASSWORD " + passwordGameRepeat);
					break;

				case "PASSWORD_CHANGED_SUCCESS":
					System.out.println("A tua password foi trocada com sucesso.");
					break;

				case "GAME_NOT_FOUND":
					System.out.println();
					System.out.println("Jogo não encontrado. Tenta outro nome: ");
					String gameName = scanner.nextLine();
					sendMessage("JOIN_GAME " + gameName);
					break;

				case "GAME_FOUND":
					gameStart = true;
					break;

				case "OPONENTE_ENCONTRADO":
					parts = message.split(" ");
					String opponentName = parts[1];
					myPiece = parts[2].charAt(0);
					System.out.println("Oponente encontrado: " + opponentName);
					System.out.println("Vais jogar como: " + myPiece);
					myTurn = (myPiece == 'X');
					if (myTurn) {
						System.out.println("Começas tu!");
						System.out.println();
						game.imprimirTabuleiro();
						System.out.println();
						System.out.println("Insere a tua jogada (ex: 8 8): ");
						String move = scanner.nextLine();
						sendMessage("MOVE " + move);
					} else {
						System.out.println("Espera pela tua vez.");
					}
					break;

				case "MOVE":
					parts = message.split(" ");
					int row = Integer.parseInt(parts[1]) - 1;
					int col = Integer.parseInt(parts[2]) - 1;
					char piece = parts[3].charAt(0);

					game.makeMove(row, col, piece == 'X');
					game.imprimirTabuleiro();
					myTurn = (piece != myPiece);

					if (myTurn) {
						System.out.println();
						System.out.println("É a tua vez de jogar!");
						System.out.println("Insere a tua jogada (ex: 8 8): ");
						String move = scanner.nextLine();
						sendMessage("MOVE " + move);
					} else {
						System.out.println();
						System.out.println("Espera pela tua vez.");
					}
					break;

				case "JOGADA_INVALIDA":
					System.out.println();
					System.out.println("A tua jogada tem valores fora das posições do tabuleiro.");
					System.out.println("Tenta novamente: ");
					String move = scanner.nextLine();
					sendMessage("MOVE " + move);
					break;

				case "ERRO_FORMATO":
					System.out.println();
					System.out.println("A tua jogada está mal escrita.");
					System.out.println("Tenta novamente (ex: 8 8): ");
					String moveRepeat = scanner.nextLine();
					sendMessage("MOVE " + moveRepeat);
					break;

				case "TEMPOS":
					parts = message.split("_#");
					System.out.println();
					System.out.println(parts[1]);
					System.out.println(parts[2]);
					System.out.println(parts[3]);
					System.out.println(parts[4]);
					System.out.println();
					break;

				case "FIM_JOGO":
					myTurn = false;
					gameStart = false;
					if (message.contains("VENCEDOR")) {
						String winner = message.split(" ")[2];
						row = Integer.parseInt(message.split(" ")[3]);
						col = Integer.parseInt(message.split(" ")[4]);
						boolean isBlack = Boolean.parseBoolean(message.split(" ")[5]);
						game.makeMove(row, col, isBlack);
						game.imprimirTabuleiro();
						if (winner.equals(playerName)) {
							System.out.println();
							System.out.println();
							System.out.println("Fim de jogo. Venceste!");
						} else {
							System.out.println();
							System.out.println();
							System.out.println("Fim de jogo. Perdeste.");
						}
					} else if (message.contains("DESISTENCIA")) {
						System.out.println();
						System.out.println();
						System.out.println("Fim de jogo. O teu adversário desconectou-se. Venceste!");
						System.exit(0);
					} else {
						System.out.println();
						System.out.println();
						System.out.println("Fim de jogo! Empate!");
					}
					System.exit(0);
					break;
				}
			}
		} catch (IOException e) {
			System.err.println("Parece que o servidor teve algum problema. Serás desconectado.");
			System.err.println("Pedimos desculpa pelo inconveniente.");
			System.exit(0);
		}
	}

	/**
	 * Envia uma mensagem para o servidor.
	 * 
	 * @param message Mensagem a ser enviada
	 */
	private void sendMessage(String message) {
		out.println(message);
	}

	/**
	 * Processa a vez do jogador durante o jogo. Valida a jogada antes de enviar
	 * para o servidor.
	 */
	private void handlePlayerTurn() {
		boolean validMove = false;

		while (!validMove) {
			try {
				game.imprimirTabuleiro();
				System.out.print("Introduz a tua jogada (linha coluna, ex: 8 8): ");

				String input = scanner.nextLine();
				String[] parts = input.split(" ");

				if (parts.length != 2) {
					throw new IllegalArgumentException("Formato inválido! Usa: linha coluna (ex: 8 8)");
				}

				int row = Integer.parseInt(parts[0]) - 1;
				int col = Integer.parseInt(parts[1]) - 1;

				if (row < 0 || row > 14 || col < 0 || col > 14) {
					throw new IllegalArgumentException("Coordenadas inválidas! Usa valores entre 1 e 15, inclusive.");
				}

				sendMessage("MOVE " + (row + 1) + " " + (col + 1));
				myTurn = false;
				validMove = true;

			} catch (NumberFormatException e) {
				System.out.println("Erro: Deves inserir números inteiros!");
			} catch (IllegalArgumentException e) {
				System.out.println("Erro: " + e.getMessage());
			} catch (Exception e) {
				System.out.println("Erro inesperado: " + e.getMessage());
			}
		}
	}

	/**
	 * Verifica se um caminho de imagem é válido.
	 * 
	 * @param caminho Caminho para o ficheiro de imagem
	 * @return true se o ficheiro existe e é válido, false caso contrário
	 */
	private static boolean isImageValid(String caminho) {
		try {
			Path path = Paths.get(caminho);
			return Files.exists(path) && !Files.isDirectory(path);
		} catch (InvalidPathException e) {
			return false;
		}
	}
}