package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class problemaLinear {
    
    private double[] c_objetivo;
    private double[][] A_restricoes;
    private double[] b_limites;
    private String[] nomesVariaveis; 
    private tipoOtimizacao tipo;
    private tipoRestricao[] tiposRestricoes;
    private Set<String> nomesVariaveisLivres;

    // Construtor principal
    public problemaLinear(tipoOtimizacao tipo, double[] c_objetivo, double[][] A_restricoes, double[] b_limites, tipoRestricao[] tiposRestricoes, String[] nomesVariaveis, Set<String> nomesVariaveisLivres) {
        this.tipo = tipo;
        this.c_objetivo = c_objetivo;
        this.A_restricoes = A_restricoes;
        this.b_limites = b_limites;
        this.tiposRestricoes = tiposRestricoes;
        this.nomesVariaveis = nomesVariaveis;
        this.nomesVariaveisLivres = nomesVariaveisLivres;
        
        // Validação simples
        if (c_objetivo.length != nomesVariaveis.length) {
            System.err.println("Aviso: Contagem de coeficientes do objetivo não bate com a contagem de nomes de variáveis.");
        }
    }

    public problemaLinear gerarDual() {
        int m = this.getNumRestricoes(); // m (linhas) do primal -> n (vars) do dual
        int n = this.getNumVariaveis(); // n (vars) do primal -> m (linhas) do dual

        // 1. Tipo: Inverte
        tipoOtimizacao tipoDual = (this.tipo == tipoOtimizacao.MINIMIZAR) ? tipoOtimizacao.MAXIMIZAR : tipoOtimizacao.MINIMIZAR;

        // 2. Custo Dual (c_dual) é o Limite Primal (b_primal)
        double[] cDual = this.b_limites.clone();

        // 3. Limite Dual (b_dual) é o Custo Primal (c_primal)
        double[] bDual = this.c_objetivo.clone();

        // 4. Nomes das Variáveis Duais (y1, y2, ..., ym)
        String[] nomesDual = new String[m];
        for (int i = 0; i < m; i++) {
            nomesDual[i] = "y" + (i + 1);
        }

        // 5. Matriz Dual (A_dual) é a Transposta da Primal (A_primal^T)
        double[][] ADual = new double[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                ADual[i][j] = this.A_restricoes[j][i];
            }
        }

        // 6. Mapeamento (A parte mais complexa)

        tipoRestricao[] rDual = new tipoRestricao[n];
        Set<String> livresDual = new HashSet<>();
        
        boolean isMin = (this.tipo == tipoOtimizacao.MINIMIZAR);

        // Mapeia Variáveis Primal -> Restrições Dual
        for (int j = 0; j < n; j++) {
            String nomeVarPrimal = this.nomesVariaveis[j];
            if (this.nomesVariaveisLivres.contains(nomeVarPrimal)) {
                // Var Primal LIVRE -> Restrição Dual de IGUALDADE
                rDual[j] = tipoRestricao.IGUAL;
            } else {
                // Var Primal (>= 0) -> Restrição Dual (<= para MAX, >= para MIN)
                rDual[j] = (tipoDual == tipoOtimizacao.MAXIMIZAR) ? tipoRestricao.MENOR_IGUAL : tipoRestricao.MAIOR_IGUAL;
            }
        }

        // Mapeia Restrições Primal -> Variáveis Dual
        // Lista de restrições a adicionar para vars y_i <= 0
        List<double[]> A_extra = new ArrayList<>();
        List<Double> b_extra = new ArrayList<>();
        List<tipoRestricao> r_extra = new ArrayList<>();

        for (int i = 0; i < m; i++) {
            tipoRestricao rPrimal = this.tiposRestricoes[i];
            String nomeVarDual = nomesDual[i];

            if (rPrimal == tipoRestricao.IGUAL) {
                // Restr Primal (=) -> Var Dual LIVRE
                livresDual.add(nomeVarDual);
            } else if ((isMin && rPrimal == tipoRestricao.MENOR_IGUAL) || 
                    (!isMin && rPrimal == tipoRestricao.MAIOR_IGUAL)) {
                
                // Nosso solver só entende y>=0 ou y livre.
                // Solução: Fazemos y_i ser LIVRE e adicionamos uma nova restrição: y_i <= 0
                
                livresDual.add(nomeVarDual);
                
                // Cria a restrição "y_i <= 0"
                double[] novaLinhaA = new double[m];
                novaLinhaA[i] = 1.0; // 1.0 * y_i
                
                A_extra.add(novaLinhaA);
                b_extra.add(0.0); 
                r_extra.add(tipoRestricao.MENOR_IGUAL);
            }
            // O caso padrão (MIN e >=) ou (MAX e <=) resulta em y_i >= 0,
        }

        // Adiciona as restrições extras (para y_i <= 0) ao final do problema dual
        if (!A_extra.isEmpty()) {
            double[][] ADualNovo = new double[n + A_extra.size()][m];
            double[] bDualNovo = new double[n + b_extra.size()];
            tipoRestricao[] rDualNovo = new tipoRestricao[n + r_extra.size()];
            
            // Copia dados antigos
            for(int i=0; i<n; i++) ADualNovo[i] = ADual[i];
            System.arraycopy(bDual, 0, bDualNovo, 0, n);
            System.arraycopy(rDual, 0, rDualNovo, 0, n);

            // Adiciona dados novos
            for(int i=0; i<A_extra.size(); i++) {
                ADualNovo[n+i] = A_extra.get(i);
                bDualNovo[n+i] = b_extra.get(i);
                rDualNovo[n+i] = r_extra.get(i);
            }
            
            // Retorna o problema com as restrições extras
            return new problemaLinear(tipoDual, cDual, ADualNovo, bDualNovo, rDualNovo, nomesDual, livresDual);
        }

        // Retorna o problema dual simples (sem restrições extras)
        return new problemaLinear(tipoDual, cDual, ADual, bDual, rDual, nomesDual, livresDual);
    }
    
    // Getters
    public double[] getC_objetivo() { return c_objetivo; }
    public double[][] getA_restricoes() { return A_restricoes; }
    public double[] getB_limites() { return b_limites; }
    public tipoOtimizacao getTipo() { return tipo; }
    public tipoRestricao[] getTiposRestricoes() { return tiposRestricoes; }
    public int getNumVariaveis() { return c_objetivo.length; }
    public int getNumRestricoes() { return A_restricoes.length; }
    public String[] getNomesVariaveis() { return nomesVariaveis; }
    public Set<String> getNomesVariaveisLivres() { return nomesVariaveisLivres; }
}
