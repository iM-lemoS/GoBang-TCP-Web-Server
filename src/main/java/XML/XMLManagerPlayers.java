package XML;

import javax.imageio.ImageIO;
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

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class XMLManagerPlayers {
	/* Documento DOM que representa o XML */
	private Document doc;
	/** Caminho para o ficheiro XML */
	private final String xmlFile;

	/** Objeto para sincronização de threads */
	private final Object lock = new Object();

	/** Caminho para o ficheiro XML com dados dos países */
	private String xmlFileNationality = "ListaDeNacionalidades.xml";

	/** Gestor para operações com o ficheiro XML dos países */
	public XMLManagerNationality xmlManagerNationality = new XMLManagerNationality(xmlFileNationality);

	/**
	 * Construtor que inicializa o gestor de XML.
	 * 
	 * @param xmlFile Caminho para o ficheiro XML (cria se não existir)
	 */
	public XMLManagerPlayers(String xmlFile) {
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
					Element root = doc.createElement("jogadores");
					doc.appendChild(root);
					saveXML();
				}
			} catch (Exception e) {
				e.printStackTrace();
				try {
					// Fallback em caso de erro
					doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
					Element root = doc.createElement("jogadores");
					doc.appendChild(root);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	/**
	 * Adiciona um novo jogador ao XML.
	 * 
	 * @param nickname    Nickname do jogador (deve ser único)
	 * @param nationality Nacionalidade do jogador
	 * @param age         Idade do jogador
	 * @param password    Password do jogador (armazenada em texto claro)
	 * @param photoPath   Caminho para a foto de perfil (opcional)
	 * @return 1 se o jogador for adicionado com sucesso, 2 se o nome já existe e 3
	 *         se o país for inválido
	 */
	public synchronized int addPlayer(String nickname, String nationality, int age, String password, String photoPath,
			String gender) {
		synchronized (lock) {
			try {
				Element root = doc.getDocumentElement();
				if (root == null) {
					root = doc.createElement("jogadores");
					doc.appendChild(root);
				}

				if (findPlayer(nickname) != null) {
					System.out.println("ERRO: Jogador já existente");
					return 2;
				}
				if (!xmlManagerNationality.verifyNationalityValid(nationality)) {
					System.out.println("ERRO: País não existe");
					return 3;
				}

				Element player = createPlayerElement(nickname, nationality, age, password, photoPath, gender);
				root.appendChild(player);
				saveXML();
				System.out.println("Perfil adicionado com sucesso: " + nickname);
				return 1;

			} catch (Exception e) {
				e.printStackTrace();
				return 2;
			}
		}
	}

	/**
	 * Cria um elemento XML representando um jogador.
	 * 
	 * @param nickname    Nickname do jogador
	 * @param nationality Nacionalidade
	 * @param age         Idade
	 * @param password    Password
	 * @param photoPath   Caminho para a foto
	 * @return Elemento XML criado
	 * @throws IOException Se ocorrer erro ao processar a foto
	 */
	private Element createPlayerElement(String nickname, String nationality, int age, String password, String photoPath,
			String gender) throws IOException {
		Element player = doc.createElement("jogador");
		player.setAttribute("nickname", nickname);

		addElement(player, "nacionalidade", nationality);
		addElement(player, "idade", String.valueOf(age));
		addElement(player, "password", password);

		String photoBase64 = ""; // Define uma string vazia por defeito
		if (photoPath != null && !photoPath.isEmpty()) {
			try {
				photoBase64 = encodeImageToBase64(photoPath);
			} catch (IOException e) {
				System.err.println(
						"Aviso: Falha ao codificar a imagem para o jogador " + nickname + ". Caminho: " + photoPath);
			}
		}

		addElement(player, "foto", photoBase64);

		addElement(player, "sexo", gender);

		return player;
	}

	/**
	 * Atualiza a foto de um jogador existente.
	 * 
	 * @param playerName Nickname do jogador
	 * @param photoPath  Novo caminho para a foto
	 */
	public synchronized void updatePhoto(String playerName, String photoPath) {
		synchronized (lock) {
			try {
				Element player = findPlayer(playerName);
				if (player != null) {
					updateElement(player, "foto", encodeImageToBase64(photoPath));
					saveXML();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Atualiza a password de um jogador existente.
	 * 
	 * @param playerName  Nickname do jogador
	 * @param newPassword Nova password
	 */
	public synchronized void updatePassword(String playerName, String newPassword) {
		synchronized (lock) {
			try {
				Element player = findPlayer(playerName);
				if (player != null) {
					updateElement(player, "password", newPassword);
					saveXML();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Verifica as credenciais de um jogador.
	 * 
	 * @param nickname Nickname do jogador
	 * @param password Password a verificar
	 * @return Elemento XML do jogador se credenciais válidas, null caso contrário
	 */
	public synchronized Element verifyPlayer(String nickname, String password) {
		synchronized (lock) {
			Element player = findPlayer(nickname);
			if (player != null) {
				NodeList passwordNodes = player.getElementsByTagName("password");
				if (passwordNodes.getLength() > 0 && password.equals(passwordNodes.item(0).getTextContent())) {
					return player;
				}
			}
			return null;
		}
	}

	/**
	 * Procura um jogador pelo nickname.
	 * 
	 * @param nickname Nickname a pesquisar
	 * @return Elemento XML do jogador ou null se não encontrado
	 */
	public synchronized Element findPlayer(String nickname) {
		synchronized (lock) {
			NodeList players = doc.getElementsByTagName("jogador");
			for (int i = 0; i < players.getLength(); i++) {
				Element player = (Element) players.item(i);
				if (player.getAttribute("nickname").equals(nickname)) {
					return player;
				}
			}
			return null;
		}
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
	 * Atualiza um elemento existente ou cria se não existir.
	 * 
	 * @param parent   Elemento pai
	 * @param name     Nome do elemento
	 * @param newValue Novo valor
	 */
	private void updateElement(Element parent, String name, String newValue) {
		NodeList nodes = parent.getElementsByTagName(name);
		if (nodes.getLength() > 0) {
			nodes.item(0).setTextContent(newValue);
		} else {
			addElement(parent, name, newValue);
		}
	}

	/**
	 * Codifica uma imagem para Base64.
	 * 
	 * @param path Caminho para o ficheiro de imagem
	 * @return String em Base64 representando a imagem
	 * @throws IOException Se ocorrer erro ao ler o ficheiro
	 */
	private String encodeImageToBase64(String path) throws IOException {
		byte[] imageBytes = Files.readAllBytes(Paths.get(path));
		return Base64.getEncoder().encodeToString(imageBytes);
	}

	/**
	 * Obtém a foto de perfil de um jogador.
	 * 
	 * @param nickname Nickname do jogador
	 * @return Objeto Image com a foto ou null se não existir
	 */
	public synchronized Image getPlayerPhoto(String nickname) {
		synchronized (lock) {
			try {
				Element player = findPlayer(nickname);
				if (player != null) {
					NodeList photoNodes = player.getElementsByTagName("foto");
					if (photoNodes.getLength() > 0) {
						String base64Image = photoNodes.item(0).getTextContent();
						return decodeBase64ToImage(base64Image);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * Obtém o sexo de um jogador.
	 * 
	 * @param nickname Nickname do jogador
	 * @return String String com o sexo
	 */
	public synchronized String getPlayerGender(String nickname) {
		synchronized (lock) {
			try {
				Element player = findPlayer(nickname);
				if (player != null) {
					NodeList photoNodes = player.getElementsByTagName("sexo");
					if (photoNodes.getLength() > 0) {
						return photoNodes.item(0).getTextContent();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * Decodifica uma imagem em Base64 para objeto Image.
	 * 
	 * @param base64String String Base64 contendo a imagem
	 * @return Objeto Image decodificado
	 * @throws IOException Se ocorrer erro na decodificação
	 */
	private Image decodeBase64ToImage(String base64String) throws IOException {
		if (base64String == null || base64String.isEmpty()) {
			return null;
		}

		try {
			String base64Image = base64String.split(",")[base64String.split(",").length - 1];
			byte[] imageBytes = Base64.getDecoder().decode(base64Image);
			ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
			return ImageIO.read(bis);
		} catch (Exception e) {
			throw new IOException("Erro ao decodificar imagem Base64", e);
		}
	}

	/**
	 * Obtém a foto de perfil de um jogador como uma string Base64.
	 * 
	 * @param nickname Nickname do jogador.
	 * @return A string Base64 da foto, ou uma string vazia se não existir.
	 */
	public synchronized String getPlayerPhotoBase64(String nickname) {
		synchronized (lock) {
			Element player = findPlayer(nickname);
			if (player != null) {
				NodeList photoNodes = player.getElementsByTagName("foto");
				if (photoNodes.getLength() > 0) {
					return photoNodes.item(0).getTextContent();
				}
			}
			return ""; // Retorna string vazia para evitar nulls
		}
	}

	/**
	 * Obtém a nacionalidade de um jogador a partir do seu nickname.
	 * 
	 * @param nickname Nickname do jogador.
	 * @return A nacionalidade do jogador, ou null se não for encontrado.
	 */
	public synchronized String getPlayerNationality(String nickname) {
		synchronized (lock) {
			Element player = findPlayer(nickname);
			if (player != null) {
				NodeList nationalityNodes = player.getElementsByTagName("nacionalidade");
				if (nationalityNodes.getLength() > 0) {
					return nationalityNodes.item(0).getTextContent();
				}
			}
			return null;
		}
	}

	/**
	 * Guarda as alterações no ficheiro XML.
	 * 
	 * @throws TransformerException Se ocorrer erro ao escrever o ficheiro
	 */
	private synchronized void saveXML() throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(xmlFile));
		transformer.transform(source, result);
	}

	/**
	 * Imprime o conteúdo do XML no console (para debug).
	 */
	public synchronized void printXML() {
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