package Jogo;

import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import XML.XMLManagerPlayers;

/**
 * Implementação do jogo GoBang (5 em linha) para dois jogadores.
 * 
 * <p>O GoBang é um jogo de estratégia tradicional jogado num tabuleiro 15x15.
 * Os jogadores alternam colocando pedras pretas (X) e brancas (O) com o objetivo
 * de formar uma linha ininterrupta de 5 pedras na horizontal, vertical ou diagonal.</p>
 * 
 * <p>Funcionalidades incluídas:
 * <ul>
 *   <li>Tabuleiro 15x15 com validação de jogadas</li>
 *   <li>Sistema de turnos alternados</li>
 *   <li>Detecção automática de vitória</li>
 *   <li>Controlo de tempo por jogador</li>
 *   <li>Modo singleplayer (jogador vs jogador no mesmo computador)</li>
 *   <li>Modo multiplayer (jogador/servidor vs jogador em diferentes computadores)</li>
 * </ul>
 * </p>
 * 
 * <p>Regras:
 * <ul>
 *   <li>Pedras colocadas não podem ser movidas ou removidas</li>
 *   <li>O primeiro a formar uma linha de 5 pedras vence</li>
 *   <li>Se o tabuleiro encher sem vencedor, é declarado empate</li>
 * </ul>
 * </p>
 */
public class GoBang {
    // Constantes do jogo
    private static final int TAMANHO = 15;          // Tamanho do tabuleiro (15x15)
    private static final char VAZIO = '.';         // Representação de espaço vazio
    private static final char PRETA = 'X';          // Representação da pedra preta
    private static final char BRANCA = 'O';         // Representação da pedra branca
    
    // Variáveis de estado do jogo
    private char[][] tabuleiro;                    // Matriz que representa o tabuleiro
    private boolean vezPretas;                     // Indica se é a vez das pedras pretas
    private boolean jogoTerminado;                 // Flag para controlo do término do jogo
    
    // Variáveis para controle de tempo
    private long inicioJogo;                       // Timestamp de início do jogo
    private long tempoTotalPretas;                 // Tempo acumulado das pretas (ms)
    private long tempoTotalBrancas;                // Tempo acumulado das brancas (ms)
    private long ultimoTempoTroca;                 // Última troca de turno (ms)
    
    private String arquivoXML = "jogadores_test.xml";
    private XMLManagerPlayers xmlManager;
    private String jogador1, jogador2;
    
    private Scanner scanner;
    private Random rnd;

    /**
     * Construtor - Inicializa o tabuleiro e variáveis de estado
     */
    public GoBang() {
        tabuleiro = new char[TAMANHO][TAMANHO];
        // Preenche o tabuleiro com espaços vazios
        for (int i = 0; i < TAMANHO; i++) {
            for (int j = 0; j < TAMANHO; j++) {
                tabuleiro[i][j] = VAZIO;
            }
        }
        vezPretas = true;  
        jogoTerminado = false;
        
        // Inicializa temporizadores
        tempoTotalPretas = 0;
        tempoTotalBrancas = 0;
        xmlManager  = new XMLManagerPlayers(arquivoXML);
        scanner = new Scanner(System.in);
        rnd = new Random();
    }

    /**
     * Método principal que controla o fluxo do jogo
     */
    public void jogar() {
    	
        System.out.println("Bem-vindo ao GoBang!");
        System.out.println();

        // Login/registo do jogador 1
        String resposta = perguntarRegistro("Jogador 1");
        processarJogador(resposta);

        // Login/registo do jogador 2
        resposta = perguntarRegistro("Jogador 2");
        processarJogador(resposta);
        

    	System.out.println("Para esta partida as peças ficam: ");
    	System.out.println("Peças pretas >> " + jogador1);
    	System.out.println("Peças brancas >> " + jogador2);
    	System.out.println();
        if (rnd.nextFloat() < 0.5f) {
        	vezPretas = false;
        }
        
        // Inicia os temporizadores
        inicioJogo = System.currentTimeMillis();
        ultimoTempoTroca = inicioJogo;
        
        // Loop principal do jogo
        while (!jogoTerminado) {
            imprimirTabuleiro();
            imprimirTempos();
            
            char jogadorAtual = vezPretas ? PRETA : BRANCA;
            System.out.print("É a vez das " + (vezPretas ? "pretas (X)" : "brancas (O)") + ": ");
            
            try {
                // Lê as coordenadas da jogada
                int linha = scanner.nextInt() - 1;  
                int coluna = scanner.nextInt() - 1;
                
                // Valida as coordenadas
                if (linha < 0 || linha >= TAMANHO || coluna < 0 || coluna >= TAMANHO) {
                    System.out.println("Coordenadas inválidas! Usa valores entre 1 e 15.");
                    continue;
                }
                
                // Verifica se a posição está vazia
                if (tabuleiro[linha][coluna] != VAZIO) {
                    System.out.println("Posição já ocupada! Escolhe outra.");
                    continue;
                }
                
                // Atualiza os temporizadores antes de processar a jogada
                atualizarTempos();
                
                // Faz a jogada
                tabuleiro[linha][coluna] = jogadorAtual;
                
                // Verifica condições de término
                if (verificarVitoria(linha, coluna)) {
                    atualizarTempos();
                    imprimirTabuleiro();
                    imprimirTempos();
                    System.out.println("Parabéns! As " + (vezPretas ? "pretas (X)" : "brancas (O)") + " venceram!");
                    jogoTerminado = true;
                } else if (tabuleiroCheio()) {
                    atualizarTempos();
                    imprimirTabuleiro();
                    imprimirTempos();
                    System.out.println("Empate! O tabuleiro está cheio.");
                    jogoTerminado = true;
                } else {
                    // Passa a vez para o outro jogador
                    vezPretas = !vezPretas;
                    ultimoTempoTroca = System.currentTimeMillis();
                }
            } catch (Exception e) {
                System.out.println("Entrada inválida! Digita as coordenadas no formato 'linha coluna' (ex: 7 7).");
                scanner.nextLine(); // Limpa o buffer do scanner
            }
        }
        scanner.close();
    }
    
    private String perguntarRegistro(String jogador) {
        System.out.printf("%s, tens um perfil de jogador já registado? (sim/não)%n", jogador);
        return scanner.nextLine().toLowerCase();
    }

    private void processarJogador(String resposta) {
        boolean loginSucesso = false;
        while (!loginSucesso) {
        	switch (resposta) {
            case "sim":
                loginSucesso = processarLogin();
                break; // Importante adicionar o break!
            case "não": // Fall-through
            case "nao":
                loginSucesso = processarRegisto();
                break; // Importante adicionar o break!
            default:
                System.out.println("Resposta inválida. Por favor responde 'sim' ou 'não'.");
        	}
        }
    }
    
    private boolean processarLogin() {
        System.out.println("Qual é o teu nickname? ");
        String nickname = scanner.nextLine();
        if (jogador1 == null) {
        	jogador1 = nickname;
        }
        else {
        	jogador2 = nickname;
        }
        
        System.out.println("Introduz a tua password: ");
        String password = scanner.nextLine();
        
        if (xmlManager.verifyPlayer(nickname, password) != null) {
            System.out.println("Muito bem " + nickname + ". Bem-vindo de volta!");
            System.out.println();
            return true;
        }
        
        System.out.println("Parece que o teu nickname ou password estão errados.");
        return false;
    }

    private boolean processarRegisto() {
        System.out.println("Qual é o nickname que queres atribuir ao teu perfil? ");
        String nickname = scanner.nextLine();
        if (jogador1 == null) {
        	jogador1 = nickname;
        }
        else {
        	jogador2 = nickname;
        }
        
        System.out.println("De que país és? ");
        String nacionalidade = scanner.nextLine();
        
        System.out.println("Que idade tens? ");
        int idade = Integer.parseInt(scanner.nextLine());
        
        System.out.println("Indica a password de acesso: ");
        String password = scanner.nextLine();
        
        while(xmlManager.addPlayer(nickname, nacionalidade, idade, password, null, null) != 1) {
        	nickname = scanner.nextLine();
            if (jogador1 == null) {
            	jogador1 = nickname;
            }
            else {
            	jogador2 = nickname;
            }
        }
        return true;
    }

    /**
     * Atualiza os temporizadores dos jogadores
     */
    public void atualizarTempos() {
        long agora = System.currentTimeMillis();
        if (vezPretas) {
            tempoTotalPretas += agora - ultimoTempoTroca;
        } else {
            tempoTotalBrancas += agora - ultimoTempoTroca;
        }
        ultimoTempoTroca = agora;
    }
    public long[] atualizarTempos(boolean isBlack, long pretas, long brancas, long ultimo) {
        long agora = System.currentTimeMillis();
        if (isBlack) {
            pretas += agora - ultimo;
        } else {
            brancas += agora - ultimo;
        }
        ultimo = agora;
        long[] retorno = {pretas, brancas, ultimo};
        return retorno;
    }

    /**
     * Imprime os tempos atuais durante o jogo
     */
    private void imprimirTempos() {
        System.out.println("\nTempos:");
        System.out.printf("Pretas (X): %s\n", formatarTempo(tempoTotalPretas));
        System.out.printf("Brancas (O): %s\n", formatarTempo(tempoTotalBrancas));
        System.out.printf("Tempo total: %s\n", formatarTempo(System.currentTimeMillis() - inicioJogo));
        System.out.println();
    }

    /**
     * Formata um tempo em milissegundos para o formato MM:SS
     * @param millis tempo em milissegundos
     * @return String formatada
     */
    public String formatarTempo(long millis) {
        long minutos = TimeUnit.MILLISECONDS.toMinutes(millis);
        long segundos = TimeUnit.MILLISECONDS.toSeconds(millis) - 
                        TimeUnit.MINUTES.toSeconds(minutos);
        return String.format("%02d:%02d", minutos, segundos);
    }

    /**
     * Verifica se a última jogada resultou em vitória
     * @param linha linha da última jogada
     * @param coluna coluna da última jogada
     * @return true se houve vitória, false caso contrário
     */
    private boolean verificarVitoria(int linha, int coluna) {
        char jogador = tabuleiro[linha][coluna];
        
        // Verifica todas as direções possíveis
        return contarSequencia(linha, coluna, 0, 1, jogador) >= 5 ||  // Horizontal
               contarSequencia(linha, coluna, 1, 0, jogador) >= 5 ||  // Vertical
               contarSequencia(linha, coluna, 1, 1, jogador) >= 5 ||  // Diagonal principal
               contarSequencia(linha, coluna, 1, -1, jogador) >= 5;  // Diagonal secundária
    }

    /**
     * Conta a sequência de pedras iguais em ambas as direções a partir de um ponto
     * @param linha linha inicial
     * @param coluna coluna inicial
     * @param deltaLinha incremento para linha
     * @param deltaColuna incremento para coluna
     * @param jogador tipo de pedra a verificar
     * @return total de pedras em sequência (incluindo a posição inicial)
     */
    private int contarSequencia(int linha, int coluna, int deltaLinha, int deltaColuna, char jogador) {
        int contagem = 1; // Conta a própria posição
        
        // Conta em ambas direções
        contagem += contarNaDirecao(linha, coluna, deltaLinha, deltaColuna, jogador);
        contagem += contarNaDirecao(linha, coluna, -deltaLinha, -deltaColuna, jogador);
        
        return contagem;
    }

    /**
     * Conta pedras iguais numa direção específica
     */
    private int contarNaDirecao(int linha, int coluna, int deltaLinha, int deltaColuna, char jogador) {
        int contagem = 0;
        int l = linha + deltaLinha;
        int c = coluna + deltaColuna;
        
        // Percorre enquanto estiver dentro do tabuleiro e encontrar pedras iguais
        while (l >= 0 && l < TAMANHO && c >= 0 && c < TAMANHO && tabuleiro[l][c] == jogador) {
            contagem++;
            l += deltaLinha;
            c += deltaColuna;
        }
        
        return contagem;
    }

    /**
     * Verifica se o tabuleiro está completamente preenchido
     * @return true se o tabuleiro está cheio, false caso contrário
     */
    private boolean tabuleiroCheio() {
        for (int i = 0; i < TAMANHO; i++) {
            for (int j = 0; j < TAMANHO; j++) {
                if (tabuleiro[i][j] == VAZIO) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Imprime o tabuleiro atual na consola
     */
    public void imprimirTabuleiro() {
        // Imprime o cabeçalho com os números das colunas
        System.out.print("   ");
        for (int i = 1; i <= TAMANHO; i++) {
            System.out.printf("%2d ", i);
        }
        System.out.println();
        
        // Imprime cada linha do tabuleiro com o seu número
        for (int i = 0; i < TAMANHO; i++) {
            System.out.printf("%2d ", i + 1);
            for (int j = 0; j < TAMANHO; j++) {
                System.out.print(" " + tabuleiro[i][j] + " ");
            }
            System.out.println();
        }
    }
    
    /**
     * Verifica se o jogo terminou
     * @return true se o jogo terminou (por vitória ou empate), false caso contrário
     */
    public boolean isGameOver() {
        return jogoTerminado;
    }

    /**
     * Verifica de quem é a vez atual
     * @return true se é a vez das pedras pretas (X), false se é a vez das brancas (O)
     */
    public boolean isBlackTurn() {
        return vezPretas;
    }
    public void setVezPretas(boolean x) {
    	vezPretas = x;
    }

    /**
     * Verifica se o jogo tem um vencedor
     * @return true se o jogo terminou com um vencedor, false se terminou em empate ou não terminou
     */
    public boolean hasWinner() {
        return jogoTerminado && !tabuleiroCheio();
    }

    /**
     * Verifica se as pedras pretas (X) são as vencedoras
     * @return true se o jogo terminou e as pretas venceram, false caso contrário
     */
    public boolean isBlackWinner() {
        if (!jogoTerminado || tabuleiroCheio()) {
            return false;
        }
        
        // Encontra a última jogada das pretas
        for (int i = 0; i < TAMANHO; i++) {
            for (int j = 0; j < TAMANHO; j++) {
                if (tabuleiro[i][j] == PRETA && verificarVitoria(i, j)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Verifica se uma jogada é válida
     * @param row linha da jogada 
     * @param col coluna da jogada 
     * @return true se a posição está dentro do tabuleiro e vazia, false caso contrário
     */
    public boolean isValidMove(int row, int col) {
        return row >= 0 && row < TAMANHO && col >= 0 && col < TAMANHO && tabuleiro[row][col] == VAZIO;
    }

    /**
     * Realiza uma jogada no tabuleiro
     * @param row linha da jogada 
     * @param col coluna da jogada 
     * @param isBlack true se for jogada das pretas (X), false se for das brancas (O)
     */
    public void makeMove(int row, int col, boolean isBlack) {
        tabuleiro[row][col] = isBlack ? PRETA : BRANCA;
        vezPretas = !isBlack;
        jogoTerminado = verificarVitoria(row, col) || tabuleiroCheio();
    }

    /**
     * Método main - ponto de entrada do programa
     */
    public static void main(String[] args) {
        GoBang jogo = new GoBang();
        jogo.jogar();
    }
}