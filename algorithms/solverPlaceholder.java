package algorithms;

import core.metodoOtimizacao;
import model.problemaLinear;
import model.solucao;
import model.statusSolucao;
import java.util.function.Consumer;

//Um solver de placeholder para outros métodos (ex: Pontos Interiores).
public class solverPlaceholder implements metodoOtimizacao {
    
    private String nome;
    
    public solverPlaceholder(String nome) {
        this.nome = nome;
    }

    @Override
    public String getNome() {
        return nome + " (Não implementado)";
    }

    @Override
    public solucao resolver(problemaLinear problema, Consumer<String> logCallback) {
        String msg = "Este método ainda não foi implementado.";
        logCallback.accept(msg + "\n");
        return new solucao(statusSolucao.NAO_RESOLVIDO, 0, null, msg);
    }
}