package XML;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XMLManagerScores {
	/* Documento DOM que representa o XML */
	private Document doc;
	/** Caminho para o ficheiro XML */
	private final String xmlFile;

	/** Objeto para sincronização de threads */
	private final Object lock = new Object();

	/**
	 * Construtor que inicializa o gestor de estatísticas.
	 * 
	 * @param xmlFile Caminho para o ficheiro XML (cria se não existir)
	 */
	public XMLManagerScores(String xmlFile) {
		this.xmlFile = xmlFile;
		initializeDocument();
	}

	/**
	 * Inicializa o documento XML, carregando existente ou criando novo.
	 */
	private void initializeDocument() {
		synchronized (lock) {
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();

				File file = new File(xmlFile);
				if (file.exists() && file.length() > 0) {
					doc = builder.parse(file);
				} else {
					// Cria novo documento com estrutura básica
					doc = builder.newDocument();
					Element root = doc.createElement("estatisticas");
					doc.appendChild(root);
					saveXML();
				}
			} catch (Exception e) {
				e.printStackTrace();
				try {
					// Fallback em caso de erro
					doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
					Element root = doc.createElement("estatisticas");
					doc.appendChild(root);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	/**
	 * Adiciona um novo resultado de jogo ao XML.
	 * 
	 * @param gameID       Identificador único do jogo
	 * @param opponentTime Tempo de jogo do adversário (formato HH:MM:SS)
	 * @param selfTime     Tempo de jogo do jogador (formato HH:MM:SS)
	 * @param resultado    Resultado do jogo (vitoria|derrota|empate)
	 * @param playerName   Nome do jogador a registar
	 * @return true se o registo foi bem sucedido, false caso contrário
	 */
	public boolean addScore(String gameID, String opponentTime, String selfTime, String resultado, String playerName) {
		synchronized (lock) {
			try {
				Element root = doc.getDocumentElement();
				if (root == null) {
					root = doc.createElement("estatisticas");
					doc.appendChild(root);
				}

				Element player = findPlayer(playerName);
				if (player == null) {
					// Cria novo jogador com estatísticas iniciais
					player = createNewPlayer(playerName, resultado);
					Element games = doc.createElement("jogos");
					Element game = createGameElement(gameID, selfTime, opponentTime, resultado);

					games.appendChild(game);
					player.appendChild(games);
					root.appendChild(player);
				} else {
					// Atualiza estatísticas existentes
					updatePlayerStats(player, resultado);
					Element jogos = (Element) player.getElementsByTagName("jogos").item(0);

					if (!gameExists(jogos, gameID)) {
						Element game = createGameElement(gameID, selfTime, opponentTime, resultado);
						jogos.appendChild(game);
					} else {
						return false; // Jogo já existe
					}
				}

				saveXML();
				System.out.println("Score adicionado com sucesso: " + gameID);
				return true;

			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	/**
	 * Cria um elemento XML representando um jogo.
	 * 
	 * @param gameID       Identificador do jogo
	 * @param selfTime     Tempo do jogador
	 * @param opponentTime Tempo do adversário
	 * @param result       Resultado do jogo
	 * @return Elemento XML criado
	 */
	private Element createGameElement(String gameID, String selfTime, String opponentTime, String result) {
		Element game = doc.createElement("jogo");
		game.setAttribute("id", gameID);
		addElement(game, "tempoPessoal", selfTime);
		addElement(game, "tempoAdversario", opponentTime);
		addElement(game, "resultado", result);
		return game;
	}

	/**
	 * Cria um novo elemento XML para um jogador.
	 * 
	 * @param playerName Nome do jogador
	 * @param result     Resultado do primeiro jogo
	 * @return Elemento XML criado
	 */
	private Element createNewPlayer(String playerName, String result) {
		Element player = doc.createElement("jogador");
		player.setAttribute("nickname", playerName);

		addElement(player, "totalJogos", "1");
		switch (result) {
		case "vitoria":
			addElement(player, "vitorias", "1");
			addElement(player, "derrotas", "0");
			addElement(player, "empates", "0");
			break;
		case "derrota":
			addElement(player, "vitorias", "0");
			addElement(player, "derrotas", "1");
			addElement(player, "empates", "0");
			break;
		default:
			addElement(player, "vitorias", "0");
			addElement(player, "derrotas", "0");
			addElement(player, "empates", "1");
		}
		return player;
	}

	/**
	 * Atualiza as estatísticas de um jogador existente.
	 * 
	 * @param player Elemento XML do jogador
	 * @param result Resultado do jogo a adicionar
	 */
	private void updatePlayerStats(Element player, String result) {
		String totalJogos = player.getElementsByTagName("totalJogos").item(0).getTextContent();
		updateElement(player, "totalJogos", String.valueOf(Integer.parseInt(totalJogos) + 1));

		switch (result) {
		case "vitoria":
			String vitorias = player.getElementsByTagName("vitorias").item(0).getTextContent();
			updateElement(player, "vitorias", String.valueOf(Integer.parseInt(vitorias) + 1));
			break;
		case "derrota":
			String derrotas = player.getElementsByTagName("derrotas").item(0).getTextContent();
			updateElement(player, "derrotas", String.valueOf(Integer.parseInt(derrotas) + 1));
			break;
		default:
			String empates = player.getElementsByTagName("empates").item(0).getTextContent();
			updateElement(player, "empates", String.valueOf(Integer.parseInt(empates) + 1));
		}
	}

	/**
	 * Verifica se um jogo já está registado para um jogador.
	 * 
	 * @param jogos  Elemento XML contendo os jogos
	 * @param gameID Identificador do jogo a verificar
	 * @return true se o jogo existe, false caso contrário
	 */
	private boolean gameExists(Element jogos, String gameID) {
		NodeList games = jogos.getElementsByTagName("jogo");
		for (int i = 0; i < games.getLength(); i++) {
			Element game = (Element) games.item(i);
			if (game.getAttribute("id").equals(gameID)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Obtém o total de jogos de um jogador.
	 * 
	 * @param playerName Nome do jogador
	 * @return Número total de jogos (0 se jogador não existir)
	 */
	public String getTotalGames(String playerName) {
		synchronized (lock) {
			try {
				Element player = findPlayer(playerName);
				if (player != null) {
					return player.getElementsByTagName("totalJogos").item(0).getTextContent();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "0";
		}
	}

	/**
	 * Obtém o total de vitórias de um jogador.
	 * 
	 * @param playerName Nome do jogador
	 * @return Número total de vitórias (0 se jogador não existir)
	 */
	public String getTotalWins(String playerName) {
		synchronized (lock) {
			try {
				Element player = findPlayer(playerName);
				if (player != null) {
					return player.getElementsByTagName("vitorias").item(0).getTextContent();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "0";
		}
	}

	/**
	 * Obtém o total de derrotas de um jogador.
	 * 
	 * @param playerName Nome do jogador
	 * @return Número total de derrotas (0 se jogador não existir)
	 */
	public String getTotalLosses(String playerName) {
		synchronized (lock) {
			try {
				Element player = findPlayer(playerName);
				if (player != null) {
					return player.getElementsByTagName("derrotas").item(0).getTextContent();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "0";
		}
	}

	/**
	 * Obtém o total de empates de um jogador.
	 * 
	 * @param playerName Nome do jogador
	 * @return Número total de empates (0 se jogador não existir)
	 */
	public String getTotalDraws(String playerName) {
		synchronized (lock) {
			try {
				Element player = findPlayer(playerName);
				if (player != null) {
					return player.getElementsByTagName("empates").item(0).getTextContent();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "0";
		}
	}

	/**
	 * Obtém o histórico completo de jogos de um jogador.
	 * 
	 * @param playerName Nome do jogador
	 * @return Lista de arrays com detalhes de cada jogo (id, tempo pessoal, tempo
	 *         adversário, resultado) ou null se o jogador não existir
	 */
	public ArrayList<String[]> getHistoryGames(String playerName) {
		synchronized (lock) {
			ArrayList<String[]> games = new ArrayList<>();
			try {
				Element player = findPlayer(playerName);
				if (player != null) {
					NodeList listJogo = ((Element) player.getElementsByTagName("jogos").item(0))
							.getElementsByTagName("jogo");
					for (int i = 0; i < listJogo.getLength(); i++) {
						Element jogo = (Element) listJogo.item(i);
						String id = jogo.getAttribute("id");
						String selfTime = "Tempo pessoal: "
								+ jogo.getElementsByTagName("tempoPessoal").item(0).getTextContent();
						String opponentTime = "Tempo do adversário "
								+ jogo.getElementsByTagName("tempoAdversario").item(0).getTextContent();
						String result = jogo.getElementsByTagName("resultado").item(0).getTextContent();
						String[] all = { id, selfTime, opponentTime, result };
						games.add(all);
					}
				} else {
					return null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return games;
		}
	}

	/**
	 * Procura um jogador pelo nickname.
	 * 
	 * @param nickname Nome do jogador a pesquisar
	 * @return Elemento XML do jogador ou null se não encontrado
	 */
	private Element findPlayer(String nickname) {
		NodeList players = doc.getElementsByTagName("jogador");
		for (int i = 0; i < players.getLength(); i++) {
			Element player = (Element) players.item(i);
			if (player.getAttribute("nickname").equals(nickname)) {
				return player;
			}
		}
		return null;
	}

	/**
	 * Adiciona um novo elemento ao XML.
	 * 
	 * @param parent Elemento pai
	 * @param name   Nome do novo elemento
	 * @param value  Valor do elemento
	 */
	private void addElement(Element parent, String name, String value) {
		Element element = doc.createElement(name);
		element.appendChild(doc.createTextNode(value));
		parent.appendChild(element);
	}

	/**
	 * Atualiza o valor de um elemento existente.
	 * 
	 * @param parent   Elemento pai
	 * @param name     Nome do elemento
	 * @param newValue Novo valor
	 */
	private void updateElement(Element parent, String name, String newValue) {
		parent.getElementsByTagName(name).item(0).setTextContent(newValue);
	}

	/**
	 * Guarda as alterações no ficheiro XML.
	 * 
	 * @throws TransformerException Se ocorrer erro ao escrever o ficheiro
	 */
	private void saveXML() throws TransformerException {
		try {
			File mainFile = new File(xmlFile);
			if (mainFile.exists()) {
				// Define o nome do ficheiro de backup
				File backupFile = new File(xmlFile + ".bak");

				// Converte para objetos Path para usar a API moderna de ficheiros
				Path mainPath = mainFile.toPath();
				Path backupPath = backupFile.toPath();

				// Copia o ficheiro atual para o ficheiro de backup, substituindo se já existir.
				// Esta é a operação de "backup".
				Files.copy(mainPath, backupPath, StandardCopyOption.REPLACE_EXISTING);

				System.out.println("Backup do quadro de honra criado com sucesso em: " + backupFile.getName());
			}
		} catch (IOException e) {
			// Se a cópia de segurança falhar, não é um erro fatal.
			// O mais importante é guardar o ficheiro principal.
			// Apenas imprime um aviso.
			System.err.println("AVISO: Falha ao criar a cópia de segurança do quadro de honra.");
			e.printStackTrace();
		}

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(xmlFile));
		transformer.transform(source, result);
	}

	/**
	 * Converte uma string de tempo no formato "MM:SS" para segundos.
	 * 
	 * @param timeString A string de tempo.
	 * @return O número total de segundos.
	 */
	private long parseTimeToSeconds(String timeString) {
		if (timeString == null || !timeString.contains(":")) {
			return 0;
		}
		try {
			String[] parts = timeString.split(":");
			long minutes = Long.parseLong(parts[0]);
			long seconds = Long.parseLong(parts[1]);
			return (minutes * 60) + seconds;
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			System.err.println("Erro ao converter tempo: " + timeString);
			return 0;
		}
	}

	/**
	 * Obtém os dados do leaderboard, ordenados por vitórias e depois por tempo
	 * médio.
	 * 
	 * @return Uma lista de PlayerStats com os jogadores ordenados.
	 */
	public synchronized List<PlayerStats> getLeaderboardData() {
		List<PlayerStats> statsList = new ArrayList<>();

		synchronized (lock) {
			if (doc == null) {
				return statsList;
			}

			NodeList players = doc.getElementsByTagName("jogador");
			for (int i = 0; i < players.getLength(); i++) {
				Element playerElement = (Element) players.item(i);

				String nickname = playerElement.getAttribute("nickname");
				int wins = Integer.parseInt(playerElement.getElementsByTagName("vitorias").item(0).getTextContent());

				// Calcular tempo médio
				long totalSeconds = 0;
				int gameCount = 0;
				NodeList games = playerElement.getElementsByTagName("jogo");
				for (int j = 0; j < games.getLength(); j++) {
					Element gameElement = (Element) games.item(j);
					String timeStr = gameElement.getElementsByTagName("tempoPessoal").item(0).getTextContent();
					totalSeconds += parseTimeToSeconds(timeStr);
					gameCount++;
				}

				double averageTime = (gameCount > 0) ? (double) totalSeconds / gameCount : 0.0;

				statsList.add(new PlayerStats(nickname, wins, averageTime));
			}

			Collections.sort(statsList);
		}

		// Retorna o top 10 (ou menos, se houver menos jogadores)
		return statsList.subList(0, Math.min(10, statsList.size()));
	}

	/**
	 * Imprime o conteúdo do XML no console (para debug).
	 */
	public void printXML() {
		synchronized (lock) {
			try {
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

				DOMSource source = new DOMSource(doc);
				StreamResult console = new StreamResult(System.out);
				transformer.transform(source, console);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}