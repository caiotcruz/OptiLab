package core;

import model.problemaLinear;
import model.solucao;
import java.util.function.Consumer;

public interface metodoOtimizacao {
    
    //Retorna o nome do método para ser exibido na GUI.
    String getNome();

    // "problema" é O problema a ser resolvido. "logCallback" é Um "callback" para reportar o progresso (ex: imprimir iterações).
    solucao resolver(problemaLinear problema, Consumer<String> logCallback);
}