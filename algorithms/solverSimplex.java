package algorithms;

import core.metodoOtimizacao;
import model.problemaLinear;
import model.solucao;
import model.statusSolucao;
import model.tipoOtimizacao;
import model.tipoRestricao;
import java.util.function.Consumer;

public class solverSimplex implements metodoOtimizacao {

    @Override
    public String getNome() {
        return "Simplex (Duas Fases)";
    }

    @Override
    public solucao resolver(problemaLinear problema, Consumer<String> logCallback) {
        problemaLinear problemaTratado = prepararProblema(problema);

        // Verificar se a Fase 1 é necessária
        if (precisaFase1(problemaTratado)) {
            
            // Criar e resolver o problema da Fase 1
            logCallback.accept("--- INICIANDO FASE 1 ---\n");
            simplexTableau tableauFase1 = new simplexTableau(problemaTratado, true);
            solucao solucaoFase1 = tableauFase1.resolver(logCallback); // <-- logCallback repassado

            // Verificar viabilidade
            if (solucaoFase1.getStatus() != statusSolucao.OTIMO || solucaoFase1.getValorOtimo() < -simplexTableau.EPSILON) {
                String msg = "Problema inviável (Fase 1 terminou com W > 0). Valor de W: " + (-solucaoFase1.getValorOtimo());
                logCallback.accept("\n" + msg + "\n");
                return new solucao(statusSolucao.INVIAVEL, 0, null, msg);
            }

            // Se viável, montar o tableau da Fase 2
            logCallback.accept("\n--- INICIANDO FASE 2 ---\n");
            simplexTableau tableauFase2 = new simplexTableau(problemaTratado, tableauFase1);
            solucao solucaoFinal = tableauFase2.resolver(logCallback); // <-- logCallback repassado

            return ajustarSolucaoFinal(solucaoFinal, problema.getTipo());

        } else {
            // Se a origem for viável, ir direto para a Fase 2
            logCallback.accept("--- INICIANDO FASE 2 (Direta) ---\n");
            simplexTableau tableau = new simplexTableau(problemaTratado, false);
            solucao solucaoFinal = tableau.resolver(logCallback); // <-- logCallback repassado
            
            return ajustarSolucaoFinal(solucaoFinal, problema.getTipo());
        }
    }

    //Prepara o problema para o formato padrão do tableau.
    //1. Garante que todos os b_i >= 0 (multiplicando restrições por -1 se necessário).
    //2. Converte problemas de MIN para MAX (multiplicando c por -1).
    private problemaLinear prepararProblema(problemaLinear problema) {
        int m = problema.getNumRestricoes();
        int n = problema.getNumVariaveis();

        double[] c = problema.getC_objetivo().clone();
        double[] b = problema.getB_limites().clone();
        tipoRestricao[] r = problema.getTiposRestricoes().clone();
        double[][] A = new double[m][];
        for (int i = 0; i < m; i++) {
            A[i] = problema.getA_restricoes()[i].clone();
        }
        
        tipoOtimizacao tipo = problema.getTipo();

        for (int i = 0; i < m; i++) {
            if (b[i] < -simplexTableau.EPSILON) {
                b[i] *= -1;
                for (int j = 0; j < n; j++) {
                    A[i][j] *= -1;
                }
                if (r[i] == tipoRestricao.MENOR_IGUAL) {
                    r[i] = tipoRestricao.MAIOR_IGUAL;
                } else if (r[i] == tipoRestricao.MAIOR_IGUAL) {
                    r[i] = tipoRestricao.MENOR_IGUAL;
                }
            }
        }

        if (tipo == tipoOtimizacao.MINIMIZAR) {
            tipo = tipoOtimizacao.MAXIMIZAR;
            for (int j = 0; j < n; j++) {
                c[j] *= -1;
            }
        }
        
        // Passa os nomes das variáveis para o novo problema
        return new problemaLinear(tipo, c, A, b, r, problema.getNomesVariaveis());
    }

    //Verifica se a Fase 1 é necessária.
    //(O problema já deve ter sido preparado com prepararProblema())
    //return true se houver restrições >= ou ==, false caso contrário.
    private boolean precisaFase1(problemaLinear problema) {
        for (tipoRestricao tr : problema.getTiposRestricoes()) {
            if (tr == tipoRestricao.MAIOR_IGUAL || tr == tipoRestricao.IGUAL) {
                return true;
            }
        }
        return false;
    }
    
    private solucao ajustarSolucaoFinal(solucao solucao, tipoOtimizacao tipoOriginal) {
        if (tipoOriginal == tipoOtimizacao.MINIMIZAR && solucao.getStatus() == statusSolucao.OTIMO) {
            return new solucao(statusSolucao.OTIMO, -solucao.getValorOtimo(), solucao.getValoresVariaveis(), solucao.getMensagem());
        }
        return solucao;
    }
}