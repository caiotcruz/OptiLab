package model;

public class solucao {
    private statusSolucao status;
    private double valorOtimo;
    private double[] valoresVariaveis;
    private String mensagem;

    public solucao(statusSolucao status, double valorOtimo, double[] valoresVariaveis, String mensagem) {
        this.status = status;
        this.valorOtimo = valorOtimo;
        this.valoresVariaveis = valoresVariaveis;
        this.mensagem = mensagem;
    }

    // Getters
    public statusSolucao getStatus() { return status; }
    public double getValorOtimo() { return valorOtimo; }
    public double[] getValoresVariaveis() { return valoresVariaveis; }
    public String getMensagem() { return mensagem; }
}
