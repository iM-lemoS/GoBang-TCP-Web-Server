package XML;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLManagerNationality {
	/* Documento DOM que representa o XML */
	private Document doc;
	/** Caminho para o ficheiro XML */
// private final String xmlFile; // Não é mais necessário se o File for passado diretamente

	/** Objeto para sincronização da inicialização do documento */
	private final Object docInitializationLock = new Object(); // Lock específico para inicialização

	private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}");

	/**
	 * Construtor que inicializa o gestor de XML.
	 *
	 * @param xmlFilePath Caminho para o ficheiro XML
	 * @throws RuntimeException se houver erro ao carregar ou parsear o XML.
	 */
	public XMLManagerNationality(String xmlFilePath) {
		// this.xmlFile = xmlFilePath; // Se precisar do caminho depois
		initializeDocument(xmlFilePath);
	}

	/**
	 * Inicializa o documento XML. Este método é thread-safe para a inicialização.
	 * 
	 * @param xmlFilePath Caminho para o arquivo XML
	 */
	private void initializeDocument(String xmlFilePath) {
		synchronized (docInitializationLock) {
			// Evita re-inicialização se já carregado, embora no construtor isso não seja um
			// problema
			if (this.doc != null) {
				return;
			}
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				File file = new File(xmlFilePath);
				if (!file.exists()) {
					throw new IOException("Arquivo XML não encontrado em: " + xmlFilePath);
				}
				this.doc = builder.parse(file);
				this.doc.getDocumentElement().normalize(); // Boa prática
			} catch (ParserConfigurationException | SAXException | IOException e) {
				// Logar o erro com um logger de verdade em uma aplicação real
				e.printStackTrace(); // Para depuração
				// Lançar uma exceção para indicar que a inicialização falhou
				throw new RuntimeException("Falha ao inicializar o documento XML de nacionalidades: " + e.getMessage(),
						e);
			}
		}
	}

	/**
	 * Verifica se o país existe no XML. Esta operação é de leitura, assumindo que
	 * 'doc' não muda após a inicialização.
	 *
	 * @param pais País identificado pelo utilizador para se registar
	 * @return true se o país existir, false se não
	 */
	public boolean verifyNationalityValid(String pais) {
		if (this.doc == null) {
			// Ou lançar exceção se o doc não foi carregado corretamente.
			// A exceção no construtor já deve prevenir isso.
			System.err.println("Documento XML não inicializado.");
			return false;
		}
		return findCountryElement(pais) != null;
	}

	/**
	 * Procura um país pelo nome e retorna o elemento XML <nacionalidade>. A
	 * comparação é feita de forma case-insensitive e ignorando acentos.
	 *
	 * @param paisToSearch País a pesquisar
	 * @return Elemento XML da nacionalidade ou null se não encontrado
	 */
	public Element findCountryElement(String paisToSearch) {
		if (this.doc == null) {
			System.err.println("Documento XML não inicializado.");
			return null;
		}

		String normalizedPaisToSearch = normalizeName(paisToSearch);
		NodeList nacionalidadesNodes = doc.getElementsByTagName("nacionalidade");

		for (int i = 0; i < nacionalidadesNodes.getLength(); i++) {
			Element nacionalidadeElement = (Element) nacionalidadesNodes.item(i);
			NodeList paisNodes = nacionalidadeElement.getElementsByTagName("pais");

			if (paisNodes.getLength() > 0) {
				String nomePaisFromXml = paisNodes.item(0).getTextContent();
				String normalizedNomePaisFromXml = normalizeName(nomePaisFromXml);

				if (normalizedNomePaisFromXml.equals(normalizedPaisToSearch)) {
					return nacionalidadeElement;
				}
			}
		}
		return null; // Não encontrado
	}

	/**
	 * Normaliza o nome, removendo acentos e convertendo para maiúsculas.
	 *
	 * @param name String a normalizar
	 * @return String com o nome normalizado
	 */
	public static String normalizeName(String name) {
		if (name == null) {
			return "";
		}
		// 1. Converte para maiúsculas
		String upperCaseName = name.toUpperCase();

		// 2. Normalizar (NFD) para separar acentos dos caracteres base
		String normalizedName = Normalizer.normalize(upperCaseName, Normalizer.Form.NFD);

		// 3. Remover os diacríticos (acentos) usando Regex
		return DIACRITICS_PATTERN.matcher(normalizedName).replaceAll("");
	}

	/**
	 * Obtém o caminho para a imagem da bandeira de um determinado país.
	 *
	 * @param pais O nome do país.
	 * @return O caminho do ficheiro da bandeira, ou null se não for encontrado.
	 */
	public String getFlagPath(String pais) {
		if (this.doc == null || pais == null || pais.isEmpty()) {
			return null;
		}

		Element nacionalidadeElement = findCountryElement(pais);
		if (nacionalidadeElement != null) {
			NodeList flagNodes = nacionalidadeElement.getElementsByTagName("bandeira");
			if (flagNodes.getLength() > 0) {
				return flagNodes.item(0).getTextContent();
			}
		}
		return null;
	}
}