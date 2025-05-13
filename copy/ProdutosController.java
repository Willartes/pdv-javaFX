package br.com.pdv.controller;

import br.com.pdv.model.Produto;
import br.com.pdv.dao.ProdutoDAO;
import br.com.pdv.util.AlertUtil;
import br.com.pdv.util.FormatUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;
import javafx.stage.Modality;
import java.time.temporal.ChronoUnit;

public class ProdutosController implements Initializable {

    @FXML
    private TextField searchField;
    
    @FXML
    private Button searchButton;
    
    @FXML
    private Button newButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    private TableView<Produto> productsTable;
    
    @FXML
    private TableColumn<Produto, Integer> idColumn;
    
    @FXML
    private TableColumn<Produto, String> nameColumn;
    
    @FXML
    private TableColumn<Produto, String> colorColumn;
    
    @FXML
    private TableColumn<Produto, String> sizeColumn;
    
    @FXML
    private TableColumn<Produto, String> priceColumn;
    
    @FXML
    private TableColumn<Produto, String> stockColumn;
    
    @FXML
    private TableColumn<Produto, String> inputColumn;
    
    @FXML
    private TableColumn<Produto, Void> actionsColumn;
    
    @FXML
    private Label totalProductsLabel;
    
    @FXML
    private VBox formPanel;
    
    @FXML
    private TextField nameField;
    
    @FXML
    private TextField colorField;
    
    @FXML
    private TextField sizeField;
    
    @FXML
    private TextField priceField;
    
    @FXML
    private TextField stockField;
    
    @FXML
    private TextField minStockField;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private Button saveButton;
    
    @FXML
    private TextField codigoField;
    
    @FXML
    private TableColumn<Produto, String> vencimentoColumn;
    
    @FXML
    private TableColumn<Produto, String> alertaColumn;
    
    @FXML
    private DatePicker vencimentoDatePicker;
    
    private ProdutoDAO produtoDAO;
    private ObservableList<Produto> productsList;
    private Produto currentProduct;
    private Stage stage;
    private Scene mainScene;
    
   
	//private TextInputControl codigoField;
    
    @FXML
    private void handleCancelButton() {
    	// Verificar se existe um produto selecionado para exclusão
    	if (currentProduct != null && currentProduct.getId() != null) {
            // Confirmar com o usuário se realmente deseja excluir
            boolean confirmacao = AlertUtil.showConfirmation(
                "Excluir Produto", 
                "Tem certeza que deseja excluir o produto '" + currentProduct.getNome() + "'? Esta ação não pode ser desfeita."
            );
            
            // Se o usuário confirmou a exclusão
            if (confirmacao) {
                excluirProduto();
            } else {
                // Se não confirmou, apenas cancela a edição
                cancelarEdicao();
            }
        } else {
            // Se não tiver produto selecionado, apenas cancela a edição
            cancelarEdicao();
        }
    }

    @FXML
    private void handleSaveButton() {
        salvarProduto();
    }
    
    
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Inicializar DAOs e listas
        inicializarDados();
        
        // Configurar colunas da tabela
        configurarColunas();
        
        // Configurar formatação das linhas
        configurarEstilosLinhas();
        
        // Configurar eventos dos botões
        configurarEventosBotoes();
        
        // Carregar produtos
        carregarProdutos();
    }

    /**
     * Inicializa o DAO e a lista de produtos
     */
    private void inicializarDados() {
        produtoDAO = ProdutoDAO.getInstance(); 
        productsList = FXCollections.observableArrayList();
    }

    /**
     * Configura as colunas da tabela com seus formatadores
     */
    private void configurarColunas() {
        // Colunas básicas
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colorColumn.setCellValueFactory(new PropertyValueFactory<>("cor"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("tamanho"));
        
        configurarColunaPreco();
        configurarColunaEstoque();
        configurarColunaVencimento();
        configurarColunaAlerta();
        configurarColunaEntrada();
        configurarColunaAcoes();
    }

    /**
     * Configura a coluna de preço com formatação
     */
    private void configurarColunaPreco() {
        priceColumn.setCellValueFactory(cellData -> {
            BigDecimal preco = cellData.getValue().getPreco();
            return new SimpleStringProperty("R$ " + FormatUtil.formatarValor(preco));
        });
    }

    /**
     * Configura a coluna de estoque com formatação
     */
    private void configurarColunaEstoque() {
        stockColumn.setCellValueFactory(cellData -> {
            Integer estoque = cellData.getValue().getEstoqueAtual();
            String texto = (estoque <= 0) ? "0 - UN" : estoque + " - UN";
            return new SimpleStringProperty(texto);
        });
    }

    /**
     * Configura a coluna de vencimento com formatação e indicadores
     */
    private void configurarColunaVencimento() {
    	System.out.println("Configurando coluna de vencimento");
        vencimentoColumn.setCellValueFactory(cellData -> {
            LocalDate dataVencimento = cellData.getValue().getDataVencimento();
            if (dataVencimento == null) {
                return new SimpleStringProperty("N/A");
            }
            
            // Formatar a data
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String dataFormatada = dataVencimento.format(formatter);
            
            // Verificar se está perto do vencimento
            LocalDate hoje = LocalDate.now();
            long diasAteVencimento = ChronoUnit.DAYS.between(hoje, dataVencimento);
            
            if (dataVencimento.isBefore(hoje)) {
                // Produto vencido
                return new SimpleStringProperty(dataFormatada + " (Vencido)");
            } else if (diasAteVencimento <= 5) {
                // Próximo do vencimento
                return new SimpleStringProperty(dataFormatada + " (" + diasAteVencimento + " dias)");
            } else {
                return new SimpleStringProperty(dataFormatada);
            }
        });
    }

    /**
     * Configura a coluna de alerta com símbolos indicativos
     */
    private void configurarColunaAlerta() {
        alertaColumn.setCellValueFactory(cellData -> {
            LocalDate dataVencimento = cellData.getValue().getDataVencimento();
            if (dataVencimento == null) {
                return new SimpleStringProperty("");
            }
            
            LocalDate hoje = LocalDate.now();
            long diasAteVencimento = ChronoUnit.DAYS.between(hoje, dataVencimento);
            
            if (dataVencimento.isBefore(hoje)) {
                return new SimpleStringProperty("⚠️"); // Símbolo de alerta para vencido
            } else if (diasAteVencimento <= 5) {
                return new SimpleStringProperty("⏰"); // Símbolo de relógio para próximo do vencimento
            } else {
                return new SimpleStringProperty("");
            }
        });
    }
    
    /**
     * Método para excluir o produto atual do banco de dados
     */
    private void excluirProduto() {
        try {
            // Excluir o produto do banco de dados
            produtoDAO.delete(currentProduct.getId());
            
            // Mostrar mensagem de sucesso
            AlertUtil.showInfo("Sucesso", "Produto '" + currentProduct.getNome() + "' excluído com sucesso!");
            
            // Recarregar a lista de produtos
            carregarProdutos();
            
            // Esconder o formulário e limpar o produto atual
            mostrarFormulario(false);
            currentProduct = null;
        } catch (SQLException e) {
            // Tratar erros de banco de dados
            AlertUtil.showError("Erro ao excluir produto", 
                "Não foi possível excluir o produto: " + e.getMessage());
        } catch (Exception e) {
            // Tratar outros erros inesperados
            AlertUtil.showError("Erro inesperado", 
                "Ocorreu um erro ao excluir o produto: " + e.getMessage());
        }
    }
    
    /**
     * Configura a coluna de entrada com botão de adicionar estoque
     * Método atualizado para abrir a tela de entrada de produtos
     */
    private void configurarColunaEntrada() {
        inputColumn.setCellValueFactory(cellData -> new SimpleStringProperty("+"));
        inputColumn.setCellFactory(col -> {
            TableCell<Produto, String> cell = new TableCell<Produto, String>() {
                final Button btn = new Button("+");
                
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                        btn.setOnAction(event -> {
                            Produto produto = getTableView().getItems().get(getIndex());
                            adicionarEstoque(produto);
                        });
                        setGraphic(btn);
                    }
                }
            };
            return cell;
        });
    }

    /**
     * Configura a coluna de ações com botão de editar
     */
    private void configurarColunaAcoes() {
        actionsColumn.setCellFactory(col -> {
            TableCell<Produto, Void> cell = new TableCell<Produto, Void>() {
                final Button editBtn = new Button("✏️");
                
                {
                    editBtn.setStyle("-fx-background-color: transparent;");
                    editBtn.setOnAction(event -> {
                        Produto produto = getTableView().getItems().get(getIndex());
                        editarProduto(produto);
                    });
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(editBtn);
                    }
                }
            };
            return cell;
        });
    }

    /**
     * Configura os estilos das linhas da tabela com base no estoque e vencimento
     */
    private void configurarEstilosLinhas() {
        productsTable.setRowFactory(tv -> new TableRow<Produto>() {
            @Override
            protected void updateItem(Produto produto, boolean empty) {
                super.updateItem(produto, empty);
                if (produto == null || empty) {
                    setStyle("");
                } else {
                    aplicarEstiloProduto(this, produto);
                }
            }
        });
    }

    
    /**
     * Aplica o estilo apropriado à linha com base nas condições do produto
     */
    private void aplicarEstiloProduto(TableRow<Produto> row, Produto produto) {
        // Verificar estoque
        int estoque = produto.getEstoqueAtual();
        int estoqueMinimo = produto.getEstoqueMinimo();
        
        // Verificar data de vencimento
        LocalDate dataVencimento = produto.getDataVencimento();
        LocalDate hoje = LocalDate.now();
        
        // Aplicar estilos de acordo com as condições (prioridade: vencimento > estoque)
        if (dataVencimento != null) {
            long diasAteVencimento = ChronoUnit.DAYS.between(hoje, dataVencimento);
            
            if (dataVencimento.isBefore(hoje)) {
                // Produto já vencido - vermelho mais forte
                row.setStyle("-fx-background-color: #ff3333;");
                return;
            } else if (diasAteVencimento <= 5) {
                // Produto próximo do vencimento (5 dias ou menos) - vermelho
                row.setStyle("-fx-background-color: #ff6666;");
                return;
            }
        }
        
        // Se não tem problemas de vencimento, verifica estoque
        if (estoque <= 0) {
            // Produto sem estoque - vermelho claro
            row.setStyle("-fx-background-color: #ffcccc;");
        } else if (estoque <= estoqueMinimo) {
            // Estoque baixo - amarelo claro
            row.setStyle("-fx-background-color: #ffffcc;");
        } else {
            // Normal
            row.setStyle("");
        }
    }

    /**
     * Configura os eventos dos botões da interface
     */
    private void configurarEventosBotoes() {
        searchButton.setOnAction(event -> buscarProdutos());
        searchField.setOnAction(event -> buscarProdutos());
        newButton.setOnAction(event -> novoProduto());
        cancelButton.setOnAction(event -> handleCancelButton());
        saveButton.setOnAction(event -> salvarProduto());
        backButton.setOnAction(event -> voltarParaPDV());
    }

    
    // Método para limpar o formulário
    private void limparFormulario() {
        codigoField.setText("");
        nameField.setText("");
        colorField.setText("");
        sizeField.setText("");
        priceField.setText("");
        stockField.setText("");
        minStockField.setText("");
    }
    
    
    
    // Método para configurar o Stage e a cena principal
    public void setStage(Stage stage, Scene mainScene) {
        this.stage = stage;
        this.mainScene = mainScene;
    }
    
    
    
    // Método para carregar todos os produtos
    private void carregarProdutos() {
        try {
            List<Produto> produtos = produtoDAO.findAll();
            // Adicionar log para verificar datas de vencimento
        
            for (Produto p : produtos) {
                System.out.println("Produto: " + p.getNome() + 
                                   ", Vencimento: " + (p.getDataVencimento() != null ? 
                                                        p.getDataVencimento() : "Não definido"));
            }
            productsList.clear();
            productsList.addAll(produtos);
            productsTable.setItems(productsList);
            atualizarTotalProdutos();
        } catch (SQLException e) {
            AlertUtil.showError("Erro ao carregar produtos", e.getMessage());
        }
    }
    
    // Método para buscar produtos
    private void buscarProdutos() {
        String termo = searchField.getText().trim();
        try {
            List<Produto> produtos;
            if (termo.isEmpty()) {
                produtos = produtoDAO.findAll();
            } else {
                // Verifica se é um número (possível código) ou texto (possível nome)
                if (termo.matches("\\d+")) {
                    Produto produto = produtoDAO.findById(Integer.parseInt(termo));
                    produtos = produto != null ? List.of(produto) : List.of();
                } else {
                    produtos = produtoDAO.findByNome(termo);
                }
            }
            productsList.clear();
            productsList.addAll(produtos);
            productsTable.setItems(productsList);
            atualizarTotalProdutos();
        } catch (SQLException e) {
            AlertUtil.showError("Erro ao buscar produtos", e.getMessage());
        }
    }
    
    // Método para atualizar o contador de produtos
    private void atualizarTotalProdutos() {
        totalProductsLabel.setText("Total de produtos: " + productsList.size());
    }
    
    // Método para adicionar produto ao PDV
    /**
     * Método para adicionar produto ao PDV
     * Este método transfere o produto selecionado para a tela de PDV
     */
    private void adicionarProdutoPDV(Produto produto) {
        // Verificar estoque
        if (produto.getEstoqueAtual() <= 0) {
            AlertUtil.showWarning("Produto sem estoque", "Este produto está sem estoque disponível.");
            return;
        }
        
        // Verificar se temos acesso ao MainController
        MainController mainController = getMainController();
        if (mainController != null) {
            // Passar o produto para o controlador principal
            mainController.selecionarProduto(produto);
            AlertUtil.showInfo("Produto Adicionado", 
                              "Produto '" + produto.getNome() + "' adicionado ao pedido.");
        } else {
            // Fallback: apenas informar e voltar à tela principal
            AlertUtil.showInfo("Produto Selecionado", 
                              "Produto '" + produto.getNome() + "' selecionado.");
        }
        
        // Voltar para a tela de PDV
        voltarParaPDV();
    }

    /**
     * Método para voltar para a tela do PDV
     * Garante que a navegação entre telas ocorra corretamente
     */
    private void voltarParaPDV() {
        if (stage != null && mainScene != null) {
            try {
                // Restaurar a cena principal
                stage.setScene(mainScene);
                stage.setTitle("PDV - Sistema de Ponto de Venda");
                
                // Solicitar foco para o campo de produto na tela principal
                Platform.runLater(() -> {
                    Node node = mainScene.lookup("#productField");
                    if (node instanceof TextField) {
                        node.requestFocus();
                    }
                });
            } catch (Exception e) {
                AlertUtil.showError("Erro ao retornar", 
                                   "Ocorreu um erro ao voltar para a tela principal: " + e.getMessage());
            }
        } else {
            AlertUtil.showWarning("Navegação", 
                                 "Não foi possível retornar à tela principal. Stage ou Scene não configurados.");
        }
    }

    /**
     * Método auxiliar para obter a referência ao MainController
     * através da Scene principal armazenada
     */
    private MainController getMainController() {
        if (mainScene == null) {
            return null;
        }
        
        try {
            // Tentar obter o controlador da cena principal
            Parent root = mainScene.getRoot();
            if (root != null) {
                return (MainController) root.getUserData();
            }
        } catch (Exception e) {
            System.err.println("Erro ao obter o MainController: " + e.getMessage());
        }
        
        return null;
    }
    
    // Método para criar novo produto
    /**
     * Método para abrir a tela de cadastro de novo produto
     * Adicione este método à sua classe ProdutosController
     */
    private void novoProduto() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/NovoProdutoView.fxml"));
            Parent root = loader.load();
            
            NovoProdutoController controller = loader.getController();
            
            Stage stage = new Stage();
            stage.setTitle("Cadastro de Produto");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); // Bloqueia a janela principal
            stage.setResizable(true);
            
            // Mostrar a tela e aguardar que seja fechada
            stage.showAndWait();
            
            // Quando a tela for fechada, recarregar a lista de produtos
            carregarProdutos();
            
        } catch (IOException e) {
            AlertUtil.showError("Erro ao abrir tela de cadastro", e.getMessage());
        }
    }
    
    // Método para editar produto existente
    private void editarProduto(Produto produto) {
        currentProduct = produto;
        preencherFormulario();
        mostrarFormulario(true);
    }
    
 // Método para preencher o formulário com dados do produto
    private void preencherFormulario() {
        codigoField.setText(currentProduct.getCodigo());
        nameField.setText(currentProduct.getNome());
        colorField.setText(currentProduct.getCor());
        sizeField.setText(currentProduct.getTamanho());
        priceField.setText(FormatUtil.formatarValor(currentProduct.getPreco()));
        stockField.setText(String.valueOf(currentProduct.getEstoqueAtual()));
        minStockField.setText(String.valueOf(currentProduct.getEstoqueMinimo()));
        
        // Verificar se há um DatePicker para a data de vencimento no formulário
        if (vencimentoDatePicker != null && currentProduct.getDataVencimento() != null) {
            vencimentoDatePicker.setValue(currentProduct.getDataVencimento());
        }
    }
    
    
    
    // Método para mostrar ou esconder o formulário
    private void mostrarFormulario(boolean visivel) {
        formPanel.setVisible(visivel);
        formPanel.setManaged(visivel);
    }
    
    // Método para cancelar a edição
    private void cancelarEdicao() {
        mostrarFormulario(false);
        currentProduct = null;
    }
    
    
    
    
    /**
     * Gera um código único para o produto baseado no nome e timestamp atual
     */
    private String gerarCodigoUnico(String nome) {
        // Pega as 3 primeiras letras do nome (ou menos se o nome for mais curto)
        String prefixo = nome.length() > 3 ? nome.substring(0, 3).toUpperCase() : nome.toUpperCase();
        
        // Adiciona um timestamp para garantir a unicidade
        long timestamp = System.currentTimeMillis() % 10000; // Últimos 4 dígitos do timestamp
        
        // Formata o código: 3 letras + 4 dígitos
        return prefixo + String.format("%04d", timestamp);
    }
    
    
 // Método para salvar o produto
    private void salvarProduto() {
        try {
            // Validar campos obrigatórios
            if (nameField.getText().trim().isEmpty()) {
                AlertUtil.showWarning("Campo obrigatório", "O nome do produto é obrigatório.");
                nameField.requestFocus();
                return;
            }
            
            if (codigoField.getText().trim().isEmpty()) {
                AlertUtil.showWarning("Campo obrigatório", "O código do produto é obrigatório.");
                codigoField.requestFocus();
                return;
            }
            
         
            if (vencimentoDatePicker != null) {
                currentProduct.setDataVencimento(vencimentoDatePicker.getValue());
            }
            
            // Preencher o objeto produto com os dados do formulário
            currentProduct.setNome(nameField.getText().trim());
            currentProduct.setCodigo(codigoField.getText().trim());
            
            // Preencher os demais campos
            currentProduct.setCor(colorField.getText().trim());
            currentProduct.setTamanho(sizeField.getText().trim());
            
            try {
                String precoStr = priceField.getText().replace("R$", "").replace(".", "").replace(",", ".").trim();
                BigDecimal preco = new BigDecimal(precoStr);
                currentProduct.setPreco(preco);
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Valor inválido", "Preço inválido. Use o formato 00,00.");
                priceField.requestFocus();
                return;
            }
            
            try {
                int estoque = Integer.parseInt(stockField.getText().trim());
                currentProduct.setEstoqueAtual(estoque);
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Valor inválido", "Estoque inválido. Digite um número inteiro.");
                stockField.requestFocus();
                return;
            }
            
            try {
                int estoqueMinimo = Integer.parseInt(minStockField.getText().trim());
                currentProduct.setEstoqueMinimo(estoqueMinimo);
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Valor inválido", "Estoque mínimo inválido. Digite um número inteiro.");
                minStockField.requestFocus();
                return;
            }
            
            // Salvar o produto
            if (currentProduct.getId() == null) {
                produtoDAO.create(currentProduct);
                AlertUtil.showInfo("Sucesso", "Produto cadastrado com sucesso!");
            } else {
                produtoDAO.update(currentProduct);
                AlertUtil.showInfo("Sucesso", "Produto atualizado com sucesso!");
            }
            
            // Atualizar a tabela e esconder o formulário
            carregarProdutos();
            mostrarFormulario(false);
            currentProduct = null;
            
        } catch (Exception e) {
            AlertUtil.showError("Erro ao salvar produto", e.getMessage());
        }
    }
    
    /**
     * Método para abrir a tela de entrada de produtos (adição de estoque)
     * 
     * @param produto O produto selecionado para adicionar estoque
     */
    private void adicionarEstoque(Produto produto) {
        try {
            // Carregar o arquivo FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/EntradaProdutoView.fxml"));
            Parent root = loader.load();
            
            // Obter o controlador e passar o produto
            EntradaProdutoController controller = loader.getController();
            controller.setProduto(produto);
            
            // Criar e configurar uma nova janela
            Stage stage = new Stage();
            stage.setTitle("Entrada do Produto");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); // Bloqueia a janela principal
            stage.setResizable(false);
            
            // Mostrar a tela e aguardar que seja fechada
            stage.showAndWait();
            
            // Quando a tela for fechada, recarregar a lista de produtos
            carregarProdutos();
            
        } catch (IOException e) {
            AlertUtil.showError("Erro ao abrir tela de entrada", e.getMessage());
        }
    }

    
}