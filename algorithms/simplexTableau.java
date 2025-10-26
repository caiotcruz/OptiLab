package algorithms;

import model.problemaLinear;
import model.solucao;
import model.statusSolucao;
import model.tipoRestricao;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class simplexTableau {

    public static final double EPSILON = 1e-9;

    private double[][] tableau;
    private int numRestricoes;  // m
    private int numVariaveis;   // n
    private int numFolga;
    private int numExcesso;
    private int numArtificiais;
    
    private int[] base; // Armazena o ÍNDICE DA COLUNA da var básica
    
    private int numColunasTableau;
    private int numLinhasTableau;
    private final int linhaZ = 0;
    private int colunaB;
    
    private problemaLinear problemaOriginal;
    private List<Integer> colunasArtificiais;
    
    // LOGGING
    private String[] nomesColunas;
    private String[] nomesLinhas; // Nomes das variáveis na base (Base)

    //Construtor para Fase 1 ou Fase 2-Direta.
    public simplexTableau(problemaLinear problema, boolean fase1) {
        this.problemaOriginal = problema;
        this.numRestricoes = problema.getNumRestricoes();
        this.numVariaveis = problema.getNumVariaveis();
        this.colunasArtificiais = new ArrayList<>();
        
        contarVariaveisAuxiliares(problema);

        this.numLinhasTableau = numRestricoes + 1;
        this.numColunasTableau = numVariaveis + numFolga + numExcesso + numArtificiais + 1;
        this.colunaB = numColunasTableau - 1;
        
        this.tableau = new double[numLinhasTableau][numColunasTableau];
        this.base = new int[numRestricoes]; 
        
        this.nomesColunas = new String[numColunasTableau];
        this.nomesLinhas = new String[numLinhasTableau];
        inicializarNomesCabecalho(problema, fase1);

        preencherTableau(problema, fase1);
        
        if (fase1) {
            canonizarFase1();
        }
    }
    
    //Construtor para Fase 2 (vinda da Fase 1).
    public simplexTableau(problemaLinear problemaOriginal, simplexTableau tabFase1) {
        this.problemaOriginal = problemaOriginal;
        this.numRestricoes = tabFase1.numRestricoes;
        this.numVariaveis = tabFase1.numVariaveis;
        this.numFolga = tabFase1.numFolga;
        this.numExcesso = tabFase1.numExcesso;
        this.numArtificiais = 0;
        
        this.numLinhasTableau = numRestricoes + 1;
        this.numColunasTableau = numVariaveis + numFolga + numExcesso + 1;
        this.colunaB = numColunasTableau - 1;
        
        this.tableau = new double[numLinhasTableau][numColunasTableau];
        this.colunasArtificiais = new ArrayList<>();
        
        this.nomesColunas = new String[numColunasTableau];
        this.nomesLinhas = new String[numLinhasTableau];
        this.nomesLinhas[0] = "Z";

        montarTableauFase2(problemaOriginal, tabFase1);
    }
    
    //Inicializa os arrays de nomes para as colunas e linhas.
    private void inicializarNomesCabecalho(problemaLinear problema, boolean fase1) {
        nomesLinhas[0] = fase1 ? "W" : "Z"; // Objetivo da Fase 1 é W
        
        int col = 0;
        String[] varNomes = problema.getNomesVariaveis();
        for (int j = 0; j < numVariaveis; j++) nomesColunas[col++] = varNomes[j];
        for (int j = 0; j < numFolga; j++) nomesColunas[col++] = "s" + (j + 1);
        for (int j = 0; j < numExcesso; j++) nomesColunas[col++] = "e" + (j + 1);
        for (int j = 0; j < numArtificiais; j++) nomesColunas[col++] = "a" + (j + 1);
        
        nomesColunas[col] = "b"; // Última coluna
    }
    
    
    private void contarVariaveisAuxiliares(problemaLinear problema) {
        numFolga = 0;
        numExcesso = 0;
        numArtificiais = 0;
        for (tipoRestricao r : problema.getTiposRestricoes()) {
            switch (r) {
                case MENOR_IGUAL: numFolga++; break;
                case MAIOR_IGUAL: numExcesso++; numArtificiais++; break;
                case IGUAL: numArtificiais++; break;
            }
        }
    }

    private void preencherTableau(problemaLinear problema, boolean fase1) {
        double[] c = problema.getC_objetivo();
        double[][] A = problema.getA_restricoes();
        double[] b = problema.getB_limites();
        tipoRestricao[] r = problema.getTiposRestricoes();

        if (fase1) {
            int colArtificalIdx = numVariaveis + numFolga + numExcesso;
            for (int i = 0; i < numRestricoes; i++) {
                if (r[i] == tipoRestricao.MAIOR_IGUAL || r[i] == tipoRestricao.IGUAL) {
                    tableau[linhaZ][colArtificalIdx] = 1; 
                    colunasArtificiais.add(colArtificalIdx);
                    colArtificalIdx++;
                }
            }
        } else {
            for (int j = 0; j < numVariaveis; j++) {
                tableau[linhaZ][j] = -c[j];
            }
        }

        int colFolga = numVariaveis;
        int colExcesso = numVariaveis + numFolga;
        int colArtificial = numVariaveis + numFolga + numExcesso;

        for (int i = 0; i < numRestricoes; i++) {
            int linha = i + 1;
            for (int j = 0; j < numVariaveis; j++) {
                tableau[linha][j] = A[i][j];
            }
            tableau[linha][colunaB] = b[i];

            switch (r[i]) {
                case MENOR_IGUAL:
                    tableau[linha][colFolga] = 1;
                    base[i] = colFolga;
                    nomesLinhas[linha] = nomesColunas[colFolga];
                    colFolga++;
                    break;
                case MAIOR_IGUAL:
                    tableau[linha][colExcesso] = -1;
                    tableau[linha][colArtificial] = 1;
                    base[i] = colArtificial;
                    nomesLinhas[linha] = nomesColunas[colArtificial]; 
                    colExcesso++;
                    colArtificial++;
                    break;
                case IGUAL:
                    tableau[linha][colArtificial] = 1;
                    base[i] = colArtificial;
                    nomesLinhas[linha] = nomesColunas[colArtificial];
                    colArtificial++;
                    break;
            }
        }
    }
    
    private void canonizarFase1() {
        for (int i = 0; i < numRestricoes; i++) {
            int linhaBase = i + 1;
            int colBase = base[i];
            if (Math.abs(tableau[linhaZ][colBase] - 1.0) < EPSILON) {
                subtrairLinha(linhaZ, linhaBase, 1.0);
            }
        }
    }

    private void montarTableauFase2(problemaLinear problemaOriginal, simplexTableau tabFase1) {
        double[][] tableauFase1 = tabFase1.getTableau();
        List<Integer> colsArtificiaisF1 = tabFase1.getColunasArtificiais();
        
        // Remapeamento de índices de colunas
        int[] remapeamento = new int[tabFase1.numColunasTableau];
        int colDest = 0;
        for (int j = 0; j < tabFase1.numColunasTableau; j++) {
            if (colsArtificiaisF1.contains(j)) {
                remapeamento[j] = -1; 
            } else {
                nomesColunas[colDest] = tabFase1.nomesColunas[j]; 
                remapeamento[j] = colDest; 
                colDest++;
            }
        }

        for (int i = 1; i < tabFase1.numLinhasTableau; i++) {
            colDest = 0;
            for (int j = 0; j < tabFase1.numColunasTableau; j++) {
                if (!colsArtificiaisF1.contains(j)) {
                    tableau[i][colDest++] = tableauFase1[i][j];
                }
            }
        }
        
        double[] c = problemaOriginal.getC_objetivo();
        for (int j = 0; j < numVariaveis; j++) {
            tableau[linhaZ][j] = -c[j];
        }
        tableau[linhaZ][colunaB] = 0.0;

        this.base = new int[numRestricoes];
        for(int i=0; i<numRestricoes; i++) {
            this.base[i] = remapeamento[tabFase1.base[i]]; // Converte índice da base
            nomesLinhas[i+1] = nomesColunas[this.base[i]]; // Define nome da linha
        }
        
        for (int i = 0; i < numRestricoes; i++) {
            int colBase = base[i];
            double coefZ = tableau[linhaZ][colBase];
            if (Math.abs(coefZ) > EPSILON) {
                subtrairLinha(linhaZ, i + 1, coefZ);
            }
        }
    }


    //Loop principal do Simplex 
    public solucao resolver(Consumer<String> logCallback) {
        int iteracoes = 0;
        
        // Log do estado inicial
        logCallback.accept(formatarTableauAtual(0, "Tableau Inicial"));
        
        while (iteracoes < 1000) {
            iteracoes++;

            int colunaEntrante = getColunaEntrante();
            if (colunaEntrante == -1) {
                logCallback.accept("\n--- Solução Ótima Encontrada ---\n");
                return construirSolucao(statusSolucao.OTIMO, "Ótimo encontrado em " + (iteracoes-1) + " iterações.");
            }

            int linhaSainte = getLinhaSainte(colunaEntrante);
            if (linhaSainte == -1) {
                String msg = "Problema ilimitado. (Coluna " + nomesColunas[colunaEntrante] + ")";
                logCallback.accept("\n--- " + msg + " ---\n");
                return new solucao(statusSolucao.ILIMITADO, 0, null, msg);
            }

            // Lógica de Log 
            String msg = "Pivô: " + nomesLinhas[linhaSainte] + " sai, " + nomesColunas[colunaEntrante] + " entra (L" + linhaSainte + ", C" + colunaEntrante + ")";
            
            // Pivoteamento
            pivoto(linhaSainte, colunaEntrante);
            
            // Atualizar base e nomes
            base[linhaSainte - 1] = colunaEntrante;
            nomesLinhas[linhaSainte] = nomesColunas[colunaEntrante];
            
            // Log
            logCallback.accept(formatarTableauAtual(iteracoes, msg));
        }
        
        return new solucao(statusSolucao.NAO_RESOLVIDO, 0, null, "Limite de iterações atingido.");
    }
    

    //Formata o tableau atual como uma String para exibição.
    private String formatarTableauAtual(int iteracao, String mensagem) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Iteração ").append(iteracao).append(" (").append(mensagem).append(") ---\n");
        
        // Cabeçalho das Colunas
        sb.append(String.format("%-6s |", "Base"));
        for (int j = 0; j < numColunasTableau; j++) {
            sb.append(String.format(" %8s", nomesColunas[j]));
        }
        sb.append("\n");
        sb.append(new String(new char[8 * (numColunasTableau + 1)]).replace("\0", "-"));
        sb.append("\n");

        // Linhas
        for (int i = 0; i < numLinhasTableau; i++) {
            sb.append(String.format("%-6s |", nomesLinhas[i])); // Nome da Linha (Base)
            for (int j = 0; j < numColunasTableau; j++) {
                sb.append(String.format(" %8.2f", tableau[i][j]));
            }
            sb.append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }
    
    
    private int getColunaEntrante() {
        double minVal = -EPSILON;
        int minIndex = -1;
        for (int j = 0; j < colunaB; j++) {
            if (tableau[linhaZ][j] < minVal) {
                minVal = tableau[linhaZ][j];
                minIndex = j;
            }
        }
        return minIndex;
    }

    private int getLinhaSainte(int colunaEntrante) {
        double minRazao = Double.MAX_VALUE;
        int minIndex = -1;

        for (int i = 1; i < numLinhasTableau; i++) {
            double a_ij = tableau[i][colunaEntrante];
            double b_i = tableau[i][colunaB];

            if (a_ij > EPSILON) {
                double razao = b_i / a_ij;
                if (razao < minRazao - EPSILON) {
                    minRazao = razao;
                    minIndex = i;
                } 
                else if (Math.abs(razao - minRazao) < EPSILON) {
                    int varSaindoAtual = base[i - 1];
                    int varSaindoCandidata = base[minIndex - 1];
                    if (varSaindoAtual < varSaindoCandidata) {
                        minIndex = i;
                    }
                }
            }
        }
        return minIndex;
    }
    
    private void pivoto(int linhaPivo, int colunaPivo) {
        double pivoVal = tableau[linhaPivo][colunaPivo];
        for (int j = 0; j < numColunasTableau; j++) {
            tableau[linhaPivo][j] /= pivoVal;
        }
        for (int i = 0; i < numLinhasTableau; i++) {
            if (i == linhaPivo) continue;
            double multiplicador = tableau[i][colunaPivo];
            if (Math.abs(multiplicador) > EPSILON) {
                subtrairLinha(i, linhaPivo, multiplicador);
            }
        }
    }
    
    private void subtrairLinha(int linha_i, int linha_j, double multiplicador) {
        for (int k = 0; k < numColunasTableau; k++) {
            tableau[linha_i][k] -= multiplicador * tableau[linha_j][k];
        }
    }

    private solucao construirSolucao(statusSolucao status, String msg) {
        double valorOtimo = tableau[linhaZ][colunaB];
        double[] valores = new double[this.numVariaveis];
        
        for (int i = 0; i < numRestricoes; i++) {
            int colVarBase = base[i];
            if (colVarBase < this.numVariaveis) {
                valores[colVarBase] = tableau[i + 1][colunaB];
            }
        }
        return new solucao(status, valorOtimo, valores, msg);
    }
    
    // Getters
    public double[][] getTableau() { return tableau; }
    public List<Integer> getColunasArtificiais() { return colunasArtificiais; }
    public int getNumLinhasTableau() { return numLinhasTableau; }
    public int getNumColunasTableau() { return numColunasTableau; }
    public String[] getNomesColunas() { return nomesColunas; }
    public int[] getBase() { return base; }
}