package model;

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
