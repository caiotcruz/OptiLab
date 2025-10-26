package ui;

import algorithms.solverPlaceholder;
import algorithms.solverSimplex;
import core.metodoOtimizacao;
import model.problemaLinear;
import model.solucao;
import model.statusSolucao;
import model.tipoOtimizacao;
import model.tipoRestricao;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class mainFrame extends JFrame {

    private JTextArea inputArea;
    private JComboBox<String> metodoComboBox;
    private JButton resolverButton;
    private JTextArea outputArea;
    private List<metodoOtimizacao> solvers;

    // Regex para encontrar termos como "3x1", "-5.2x2", "+ x3", "x4"
    private static final Pattern TERMO_PATTERN = Pattern.compile("([+-]?\\s*\\d*\\.?\\d*)\\s*([a-zA-Z]+\\d*)");
    // Regex para encontrar o operador da restrição
    private static final Pattern OP_PATTERN = Pattern.compile("\\s*(<=|>=|=)\\s*");


    public mainFrame() {
        setTitle("Resolvedor de Otimização Combinatória");
        setSize(1600, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Inicializa os Solvers
        solvers = new ArrayList<>();
        solvers.add(new solverSimplex());
        solvers.add(new solverPlaceholder("Pontos Interiores"));
        solvers.add(new solverPlaceholder("Branch and Bound"));

        // Painel de Controle (Topo)
        JPanel controlPanel = new JPanel();
        metodoComboBox = new JComboBox<>();
        for (metodoOtimizacao solver : solvers) {
            metodoComboBox.addItem(solver.getNome());
        }
        
        resolverButton = new JButton("Resolver");
        
        controlPanel.add(new JLabel("Método:"));
        controlPanel.add(metodoComboBox);
        controlPanel.add(resolverButton);
        add(controlPanel, BorderLayout.NORTH);
        
        // Painel Principal (Input e Output) 
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // Painel de Input
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Defina o Problema (Formato LP):"), BorderLayout.NORTH);
        inputArea = new JTextArea();
        inputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        inputArea.setText(getExemploProblema()); // Carrega um exemplo
        leftPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);

        // Painel de Output
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("Resultado:"), BorderLayout.NORTH);
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        outputArea.setEditable(false);
        rightPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        splitPane.setDividerLocation(400);
        
        add(splitPane, BorderLayout.CENTER);

        // Ação do Botão
        resolverButton.addActionListener(e -> {
            resolverProblema();
        });
    }

    private String getExemploProblema() {
        return "min: 2x1 + 3x2 - x3\n" +
               "st:\n" +
               "  x1 + 4x2 + 2x3 >= 8\n" +
               "  3x1 + 2x2 = 6\n" +
               "  x1 + x3 <= 2\n" +
               "end";
    }

    private void resolverProblema() {
        try {
            int selectedIndex = metodoComboBox.getSelectedIndex();
            metodoOtimizacao solverEscolhido = solvers.get(selectedIndex);

            String textoProblema = inputArea.getText();
            problemaLinear problema = parseProblema(textoProblema); 

            // Limpa a área de output
            outputArea.setText("Resolvendo com " + solverEscolhido.getNome() + "...\n\n");
            
            // Cria o "logger" (callback)
            Consumer<String> logger = (s) -> {
                outputArea.append(s);
                outputArea.setCaretPosition(outputArea.getDocument().getLength());
            };

            // Chamar o solver (passando o logger)
            solucao solucao = solverEscolhido.resolver(problema, logger);

            // Exibir o resultado final
            outputArea.append("\n--- RESULTADO FINAL ---\n");
            outputArea.append(formatarSolucao(solucao, problema));

        } catch (Exception ex) {
            outputArea.setText("Erro ao processar:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Parser do formato de texto LP. Converte o texto da inputArea em um objeto problemaLinear.
    private problemaLinear parseProblema(String texto) {
        // Limpa e divide as linhas
        String[] linhas = texto.toLowerCase().split("\\s*\n\\s*");
        
        tipoOtimizacao tipo = null;
        Map<String, Integer> varMap = new HashMap<>(); // "x1" -> 0, "x2" -> 1
        List<Double> cList = new ArrayList<>();
        List<double[]> aList = new ArrayList<>();
        List<Double> bList = new ArrayList<>();
        List<tipoRestricao> rList = new ArrayList<>();

        // Parsear Função Objetivo
        String linhaObj = "";
        for (String l : linhas) {
            l = l.trim();
            if (l.startsWith("max:") || l.startsWith("min:")) {
                linhaObj = l;
                break;
            }
        }
        
        if (linhaObj.startsWith("max:")) {
            tipo = tipoOtimizacao.MAXIMIZAR;
        } else if (linhaObj.startsWith("min:")) {
            tipo = tipoOtimizacao.MINIMIZAR;
        } else {
            throw new IllegalArgumentException("Função objetivo deve começar com 'max:' ou 'min:'");
        }
        
        String exprObj = linhaObj.substring(linhaObj.indexOf(":") + 1);
        Matcher m = TERMO_PATTERN.matcher(exprObj);
        while (m.find()) {
            double coef = parseCoeficiente(m.group(1));
            String varNome = m.group(2);
            if (!varMap.containsKey(varNome)) {
                varMap.put(varNome, varMap.size());
            }
            // Garante que a lista de custos tenha o tamanho certo
            while (cList.size() <= varMap.get(varNome)) {
                cList.add(0.0);
            }
            // Usar += para somar coeficientes (ex: 2x1 + 3x1)
            int index = varMap.get(varNome);
            cList.set(index, cList.get(index) + coef);
        }

        int numVariaveis = varMap.size();
        if (numVariaveis == 0) {
            throw new IllegalArgumentException("Nenhuma variável encontrada na função objetivo.");
        }

        // Parsear Restrições
        boolean emRestricoes = false;
        for (String linha : linhas) {
            linha = linha.trim();
            if (linha.equals("st:") || linha.equals("s.t.:")) {
                emRestricoes = true;
                continue;
            }
            if (!emRestricoes || linha.isEmpty() || linha.startsWith("max:") || linha.startsWith("min:")) {
                continue;
            }
            if (linha.equals("end")) break;
            
            // Ignora linhas de não-negatividade (já são padrão)
            if (linha.matches("[a-zA-Z]+\\d*\\s*(>=|>=?)\\s*0")) {
                continue;
            }

            Matcher opMatcher = OP_PATTERN.matcher(linha);
            if (!opMatcher.find()) {
                throw new IllegalArgumentException("Restrição sem operador (<=, >=, =): " + linha);
            }
            
            String lhs = linha.substring(0, opMatcher.start());
            String op = opMatcher.group(1);
            String rhs = linha.substring(opMatcher.end());
            
            double[] aRow = new double[numVariaveis];
            Matcher termoMatcher = TERMO_PATTERN.matcher(lhs);
            while (termoMatcher.find()) {
                double coef = parseCoeficiente(termoMatcher.group(1));
                String varNome = termoMatcher.group(2);
                
                if (!varMap.containsKey(varNome)) {
                    // Tenta adicionar a variável se ela não estiver no objetivo (coef 0)
                    varMap.put(varNome, numVariaveis++);
                    cList.add(0.0);
                    // Redimensiona todas as linhas 'a' anteriores
                    for(int i=0; i < aList.size(); i++) {
                        double[] oldRow = aList.get(i);
                        double[] newRow = new double[numVariaveis];
                        System.arraycopy(oldRow, 0, newRow, 0, oldRow.length);
                        aList.set(i, newRow);
                    }
                    aRow = new double[numVariaveis]; // Recria a linha atual
                }
                
                aRow[varMap.get(varNome)] += coef;
            }
            
            double b = Double.parseDouble(rhs.trim());
            tipoRestricao r;
            
            if (op.equals("<=")) {
                r = tipoRestricao.MENOR_IGUAL;
            } else if (op.equals(">=")) {
                r = tipoRestricao.MAIOR_IGUAL;
            } else if (op.equals("=")) {
                r = tipoRestricao.IGUAL;
            } else {
                 throw new IllegalArgumentException("Operador de restrição desconhecido: " + op);
            }
            
            aList.add(aRow);
            bList.add(b);
            rList.add(r);
        }
        
        // Montar o objeto problemaLinear 
        
        // Garante que 'c' tenha o tamanho de 'numVariaveis' caso novas variáveis tenham sido adicionadas
        while(cList.size() < numVariaveis) cList.add(0.0);
        
        double[] c = cList.stream().mapToDouble(d -> d).toArray();
        double[][] A = aList.toArray(new double[0][]);
        double[] b = bList.stream().mapToDouble(d -> d).toArray();
        tipoRestricao[] r = rList.toArray(new tipoRestricao[0]);
        
        String[] nomesVariaveis = new String[numVariaveis];
        for (Map.Entry<String, Integer> entry : varMap.entrySet()) {
            nomesVariaveis[entry.getValue()] = entry.getKey();
        }

        return new problemaLinear(tipo, c, A, b, r, nomesVariaveis);
    }
    
    // Helper para converter o coeficiente do regex (ex: "+ ", "-", " 3.5", "-2")
    private double parseCoeficiente(String coefStr) {
        coefStr = coefStr.replaceAll("\\s", "");
        if (coefStr.equals("") || coefStr.equals("+")) {
            return 1.0;
        }
        if (coefStr.equals("-")) {
            return -1.0;
        }
        return Double.parseDouble(coefStr);
    }

    private String formatarSolucao(solucao sol, problemaLinear p) {
        StringBuilder sb = new StringBuilder();
        sb.append("Status: ").append(sol.getStatus()).append("\n");
        sb.append("Mensagem: ").append(sol.getMensagem()).append("\n\n");

        if (sol.getStatus() == statusSolucao.OTIMO) {
            sb.append("Valor Ótimo (Z): ").append(String.format("%.4f", sol.getValorOtimo())).append("\n");
            sb.append("Valores das Variáveis:\n");
            String[] nomes = p.getNomesVariaveis();
            double[] valores = sol.getValoresVariaveis();
            
            if (valores != null) {
                for (int i = 0; i < valores.length; i++) {
                    sb.append(String.format("  %s: %.4f\n", nomes[i], valores[i]));
                }
            } else {
                 sb.append("  (Valores não retornados pelo solver stub)\n");
            }
        }
        return sb.toString();
    }
}