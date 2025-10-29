package algorithms;

import core.metodoOtimizacao;
import model.problemaLinear;
import model.solucao;
import model.statusSolucao;
import model.tipoOtimizacao;
import model.tipoRestricao;
import java.util.function.Consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class solverSimplex implements metodoOtimizacao {

    private Map<Integer, Integer> mapaVariaveisLivres;
    private String[] nomesOriginais;

    @Override
    public String getNome() {
        return "Simplex (Duas Fases)";
    }

    @Override
    public solucao resolver(problemaLinear problema, Consumer<String> logCallback) {
        
        this.nomesOriginais = problema.getNomesVariaveis();
        this.mapaVariaveisLivres = new HashMap<>();

        problemaLinear problemaTratado = prepararProblema(problema);

        solucao solucaoTransformada = null;

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
            solucaoTransformada = tableau.resolver(logCallback); // <-- logCallback repassado
            
        }

        return reconstruirSolucao(solucaoTransformada, problema.getTipo());
    }

    //Prepara o problema para o formato padrão do tableau.
    //1. Garante que todos os b_i >= 0 (multiplicando restrições por -1 se necessário).
    //2. Converte problemas de MIN para MAX (multiplicando c por -1).
    private problemaLinear prepararProblema(problemaLinear problema) {
        int m = problema.getNumRestricoes();
        int n_orig = problema.getNumVariaveis();
        Set<String> livres = problema.getNomesVariaveisLivres();

        // Clonar dados mutáveis
        double[] c_orig = problema.getC_objetivo().clone();
        double[] b = problema.getB_limites().clone();
        tipoRestricao[] r = problema.getTiposRestricoes().clone();
        double[][] A_orig = new double[m][];
        for (int i = 0; i < m; i++) {
            A_orig[i] = problema.getA_restricoes()[i].clone();
        }
        
        tipoOtimizacao tipo = problema.getTipo();

        for (int i = 0; i < m; i++) {
            if (b[i] < -simplexTableau.EPSILON) {
                b[i] *= -1;
                for (int j = 0; j < n_orig; j++) {
                    A_orig[i][j] *= -1;
                }
                if (r[i] == tipoRestricao.MENOR_IGUAL) {
                    r[i] = tipoRestricao.MAIOR_IGUAL;
                } else if (r[i] == tipoRestricao.MAIOR_IGUAL) {
                    r[i] = tipoRestricao.MENOR_IGUAL;
                }
            }
        }

        // 2. Converter MIN -> MAX
        if (tipo == tipoOtimizacao.MINIMIZAR) {
            tipo = tipoOtimizacao.MAXIMIZAR;
            for (int j = 0; j < n_orig; j++) {
                c_orig[j] *= -1;
            }
        }

        // 3. Substituir variáveis livres
        List<String> nomesTransformados = new ArrayList<>();
        List<Double> cTransformado = new ArrayList<>();
        List<ArrayList<Double>> aLinhasTransformadas = new ArrayList<>();
        for(int i=0; i < m; i++) aLinhasTransformadas.add(new ArrayList<Double>());
        
        mapaVariaveisLivres.clear(); // Limpa o mapa
        
        for (int j = 0; j < n_orig; j++) {
            String nomeVar = this.nomesOriginais[j];
            boolean isLivre = livres.contains(nomeVar);
            
            // Adiciona a parte positiva (x_j')
            nomesTransformados.add(isLivre ? nomeVar + "_p" : nomeVar);
            cTransformado.add(c_orig[j]);
            for(int i=0; i < m; i++) {
                aLinhasTransformadas.get(i).add(A_orig[i][j]);
            }
            
            if (isLivre) {
                // Adiciona a parte negativa (x_j'')
                int idx_p = nomesTransformados.size() - 1; // Índice do x_j' que acabamos de adicionar
                int idx_n = nomesTransformados.size();     // Índice do x_j'' que vamos adicionar
                
                nomesTransformados.add(nomeVar + "_n");
                cTransformado.add(-c_orig[j]); // Custo é negativo
                for(int i=0; i < m; i++) {
                    aLinhasTransformadas.get(i).add(-A_orig[i][j]); // Coeficiente é negativo
                }
                
                // Salva o mapeamento
                mapaVariaveisLivres.put(idx_p, idx_n);
            }
        }

        // Converter listas de volta para arrays
        String[] nomesFinais = nomesTransformados.toArray(new String[0]);
        double[] cFinal = cTransformado.stream().mapToDouble(d -> d).toArray();
        double[][] AFinal = new double[m][nomesFinais.length];
        for(int i=0; i<m; i++) {
            AFinal[i] = aLinhasTransformadas.get(i).stream().mapToDouble(d -> d).toArray();
        }
        
        // Passa os nomes das variáveis para o novo problema
        return new problemaLinear(tipo, cFinal, AFinal, b, r, nomesFinais, new HashSet<>());
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
    
    //Reconstrói a solução final a partir da solução transformada.
    //Combina x_j' e x_j'' de volta para x_j
    //Ajusta o valor Z se o problema original era MIN
    private solucao reconstruirSolucao(solucao solucaoTransformada, tipoOtimizacao tipoOriginal) {
        if (solucaoTransformada.getStatus() != statusSolucao.OTIMO) {
            // Se não for ótimo (ex: Ilimitado, Inviável), apenas retorna
            return solucaoTransformada;
        }

        double[] valoresTransformados = solucaoTransformada.getValoresVariaveis();
        double[] valoresFinais = new double[this.nomesOriginais.length];
        
        int idxTransformado = 0;
        for (int i = 0; i < valoresFinais.length; i++) {
            if (mapaVariaveisLivres.containsKey(idxTransformado)) {
                // Esta é uma variável livre
                int idx_p = idxTransformado;
                int idx_n = mapaVariaveisLivres.get(idx_p);
                
                double val_p = valoresTransformados[idx_p];
                double val_n = valoresTransformados[idx_n];
                
                valoresFinais[i] = val_p - val_n;
                
                idxTransformado += 2; // Pula as duas colunas (p e n)
            } else {
                // Esta é uma variável não-negativa padrão
                valoresFinais[i] = valoresTransformados[idxTransformado];
                idxTransformado += 1; // Pula apenas uma coluna
            }
        }
        
        double valorOtimo = solucaoTransformada.getValorOtimo();
        // Ajusta o valor de Z para problemas de MIN
        if (tipoOriginal == tipoOtimizacao.MINIMIZAR) {
            valorOtimo = -valorOtimo;
        }

        return new solucao(statusSolucao.OTIMO, valorOtimo, valoresFinais, solucaoTransformada.getMensagem());
    }

    private solucao ajustarSolucaoFinal(solucao solucao, tipoOtimizacao tipoOriginal) {
        if (tipoOriginal == tipoOtimizacao.MINIMIZAR && solucao.getStatus() == statusSolucao.OTIMO) {
            return new solucao(statusSolucao.OTIMO, -solucao.getValorOtimo(), solucao.getValoresVariaveis(), solucao.getMensagem());
        }
        return solucao;
    }
}