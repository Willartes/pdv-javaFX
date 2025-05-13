package br.com.pdv.controller;

import br.com.pdv.controller.*;
import br.com.pdv.dao.ProdutoDAO;
import br.com.pdv.model.Produto;
import br.com.pdv.util.AlertUtil;
import br.com.pdv.util.FormatUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Controlador para a tela de entrada de produtos (adição de estoque)
 */
public class EntradaProdutoController implements Initializable {

    @FXML private TextField produtoField;
    @FXML private TextField quantidadeField;
    @FXML private TextField valorUnitarioField;
    @FXML private TextField valorCustoField;
    @FXML private TextField markupField;
    @FXML private Label infoLabel;
    @FXML private Button salvarButton;
    @FXML private Button closeButton;
    
    private Produto produto;
    private ProdutoDAO produtoDAO;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        produtoDAO = ProdutoDAO.getInstance();
        
        // Configurar tooltip para o ícone de informação
        Tooltip infoTooltip = new Tooltip("Quantidade a ser adicionada ao estoque atual do produto.");
        Tooltip.install(infoLabel, infoTooltip);
        
        // Configurar formatação para campos numéricos
        FormatUtil.formatTextFieldAsInteger(quantidadeField);
        FormatUtil.formatTextFieldAsCurrency(valorUnitarioField);
        FormatUtil.formatTextFieldAsCurrency(valorCustoField);
        
        // Definir valores padrão
        quantidadeField.setText("1");
        
        // Adicionar listener para recalcular o markup quando o custo for alterado
        valorCustoField.textProperty().addListener((observable, oldValue, newValue) -> {
            calcularMarkup();
        });
        
        // Adicionar listener para recalcular o markup quando o valor unitário for alterado
        valorUnitarioField.textProperty().addListener((observable, oldValue, newValue) -> {
            calcularMarkup();
        });
    }
    
    /**
     * Calcula o markup com base nos valores atuais dos campos
     */
    private void calcularMarkup() {
        try {
            String custoStr = valorCustoField.getText().replace("R$", "").replace(".", "").replace(",", ".").trim();
            String valorStr = valorUnitarioField.getText().replace("R$", "").replace(".", "").replace(",", ".").trim();
            
            if (custoStr.isEmpty() || valorStr.isEmpty()) {
                return;
            }
            
            BigDecimal custo = new BigDecimal(custoStr);
            BigDecimal valor = new BigDecimal(valorStr);
            
            // Evitar divisão por zero
            if (custo.compareTo(BigDecimal.ZERO) <= 0) {
                markupField.setText("0");
                return;
            }
            
            // Calcular o markup - Fórmula: markup = (valor / custo - 1) * 100
            BigDecimal markup = valor.divide(custo, 4, RoundingMode.HALF_EVEN)
                             .subtract(BigDecimal.ONE)
                             .multiply(new BigDecimal("100"))
                             .setScale(2, RoundingMode.HALF_EVEN);
            
            // Atualizar o campo de markup
            markupField.setText(markup.toString().replace(".", ","));
        } catch (Exception e) {
            // Ignorar erros durante a digitação
        }
    }
   
    /**
     * Define o produto para entrada
     * @param produto O produto selecionado
    public void setProduto(Produto produto) {
        try {
            this.produto = produto;
            
            // Log para verificação
            System.out.println("Produto carregado: " + produto.getNome());
            System.out.println("Custo original: " + produto.getCusto());
            System.out.println("Preço original: " + produto.getPreco());
            
            // Preencher os campos com dados do produto exatamente como estão no banco
            produtoField.setText(produto.getNome());
            valorUnitarioField.setText(FormatUtil.formatarValor(produto.getPreco()));
            
            // Usar EXATAMENTE o custo cadastrado, sem recalcular
            valorCustoField.setText(FormatUtil.formatarValor(produto.getCusto()));
            
            // Verificar se é possível calcular o markup
            if (produto.getCusto() != null && produto.getCusto().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal custo = produto.getCusto();
                BigDecimal preco = produto.getPreco();
                
                // Calcular markup exato com base no custo e preço cadastrados
                BigDecimal markup = preco.divide(custo, 4, RoundingMode.HALF_EVEN)
                                 .subtract(BigDecimal.ONE)
                                 .multiply(new BigDecimal("100"))
                                 .setScale(2, RoundingMode.HALF_EVEN);
                
                markupField.setText(markup.toString().replace(".", ","));
                System.out.println("Markup calculado: " + markup);
            } else {
                // Se custo for zero, definir markup como zero
                markupField.setText("0");
                valorCustoField.setStyle("-fx-background-color: lightyellow;");
            }
        } catch (Exception e) {
            System.err.println("Erro ao configurar produto: " + e.getMessage());
            e.printStackTrace();
        }
    }
    **/
    
    
    /**
     * Define o produto para entrada
     * @param produto O produto selecionado
     */
    public void setProduto(Produto produto) {
        try {
            this.produto = produto;
            
            // Verificar se recebemos o produto corretamente
            System.out.println("Produto carregado: " + produto.getNome());
            System.out.println("Custo cadastrado: " + produto.getCusto());
            System.out.println("Preço cadastrado: " + produto.getPreco());
            
            // Preencher os campos com dados do produto
            produtoField.setText(produto.getNome());
            valorUnitarioField.setText(FormatUtil.formatarValor(produto.getPreco()));
            
            // Verificar se o custo está zerado
            if (produto.getCusto() == null || produto.getCusto().compareTo(BigDecimal.ZERO) <= 0) {
                // Buscar o último valor de custo usado para este produto, se existir
                BigDecimal ultimoCusto = buscarUltimoCustoProduto(produto.getId());
                
                if (ultimoCusto != null && ultimoCusto.compareTo(BigDecimal.ZERO) > 0) {
                    // Usar o último custo registrado
                    valorCustoField.setText(FormatUtil.formatarValor(ultimoCusto));
                    
                    // Calcular markup com base no último custo
                    BigDecimal preco = produto.getPreco();
                    BigDecimal markup = preco.divide(ultimoCusto, 4, RoundingMode.HALF_EVEN)
                                     .subtract(BigDecimal.ONE)
                                     .multiply(new BigDecimal("100"))
                                     .setScale(2, RoundingMode.HALF_EVEN);
                    
                    markupField.setText(markup.toString().replace(".", ","));
                } else {
                    // Sugerir um valor de custo padrão (por exemplo, 60% do preço de venda)
                    BigDecimal precoVenda = produto.getPreco();
                    BigDecimal custoSugerido = precoVenda.multiply(new BigDecimal("0.6")).setScale(2, RoundingMode.HALF_EVEN);
                    
                    valorCustoField.setText(FormatUtil.formatarValor(custoSugerido));
                    
                    // Markup padrão para esse caso seria 66,67% (100/60 - 1)*100
                    markupField.setText("66,67");
                }
                
                // Destacar o campo para indicar que é um valor sugerido
                valorCustoField.setStyle("-fx-background-color: lightyellow;");
                
                // Opcionalmente, mostrar um alerta para o usuário
                AlertUtil.showInfo("Valor de Custo", 
                    "O produto não possui um valor de custo cadastrado.\n" +
                    "Um valor sugerido foi preenchido. Por favor, ajuste conforme necessário.");
            } else {
                // Usar o custo cadastrado
                valorCustoField.setText(FormatUtil.formatarValor(produto.getCusto()));
                
                // Calcular markup
                BigDecimal custo = produto.getCusto();
                BigDecimal preco = produto.getPreco();
                
                BigDecimal markup = preco.divide(custo, 4, RoundingMode.HALF_EVEN)
                                 .subtract(BigDecimal.ONE)
                                 .multiply(new BigDecimal("100"))
                                 .setScale(2, RoundingMode.HALF_EVEN);
                
                markupField.setText(markup.toString().replace(".", ","));
                System.out.println("Markup calculado: " + markup);
            }
        } catch (Exception e) {
            System.err.println("Erro ao configurar produto: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Busca o último valor de custo usado para este produto
     * @param produtoId ID do produto
     * @return Último valor de custo ou null se não encontrado
     */
    private BigDecimal buscarUltimoCustoProduto(Integer produtoId) {
    	BigDecimal preco = produto.getPreco();
        return preco.multiply(new BigDecimal("0.6")).setScale(2, RoundingMode.HALF_EVEN);
    }
    /**
     * Salva a entrada de produtos no banco de dados
     */
    @FXML
    private void salvarEntrada() {
        try {
            // Validar quantidade
            if (quantidadeField.getText().trim().isEmpty()) {
                AlertUtil.showWarning("Campo obrigatório", "A quantidade é obrigatória.");
                quantidadeField.requestFocus();
                return;
            }
            
            int quantidade = Integer.parseInt(quantidadeField.getText().trim());
            if (quantidade <= 0) {
                AlertUtil.showWarning("Quantidade inválida", "A quantidade deve ser maior que zero.");
                quantidadeField.requestFocus();
                return;
            }
            
            // Atualizar custo do produto
            String custoStr = valorCustoField.getText().replace("R$", "").replace(".", "").replace(",", ".").trim();
            BigDecimal novoCusto = new BigDecimal(custoStr);
            
            // Validar se o custo é maior que zero
            if (novoCusto.compareTo(BigDecimal.ZERO) <= 0) {
                // Perguntar ao usuário se deseja continuar com custo zero
                boolean confirmar = AlertUtil.showConfirmation(
                    "Custo Zerado", 
                    "O custo do produto está zerado ou negativo, o que resultará em um markup incorreto.\n" +
                    "Deseja informar um valor de custo válido?");
                    
                if (confirmar) {
                    valorCustoField.requestFocus();
                    return;
                }
                // Se o usuário optar por continuar, usa um valor mínimo para evitar divisão por zero
                novoCusto = new BigDecimal("0.01");
            }
            
            produto.setCusto(novoCusto);
            
            // Atualizar preço de venda
            String valorStr = valorUnitarioField.getText().replace("R$", "").replace(".", "").replace(",", ".").trim();
            BigDecimal novoPreco = new BigDecimal(valorStr);
            produto.setPreco(novoPreco);
            
            // Atualizar estoque
            int estoqueAtual = produto.getEstoqueAtual();
            produto.setEstoqueAtual(estoqueAtual + quantidade);
            
            // Salvar as alterações
            produtoDAO.update(produto);
            
            // Exibir mensagem de sucesso
            AlertUtil.showInfo("Entrada realizada", 
                String.format("Adicionados %d itens ao estoque do produto '%s'.", 
                              quantidade, produto.getNome()));
            
            // Fechar a janela
            fecharJanela();
            
        } catch (NumberFormatException e) {
            AlertUtil.showError("Valor inválido", "Por favor, verifique os valores informados.");
        } catch (SQLException e) {
            AlertUtil.showError("Erro ao salvar", 
                "Ocorreu um erro ao salvar a entrada de produtos: " + e.getMessage());
        }
    }
    
    /**
     * Fecha a janela atual
     */
    @FXML
    private void fecharJanela() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}