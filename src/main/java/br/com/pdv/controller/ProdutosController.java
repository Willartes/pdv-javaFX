package br.com.pdv.controller;

import br.com.pdv.dao.CategoriaDAO;
import br.com.pdv.dao.MarcaDAO;
import br.com.pdv.dao.ProdutoDAO;
import br.com.pdv.dao.SubcategoriaDAO;
import br.com.pdv.model.Categoria;
import br.com.pdv.model.Marca;
import br.com.pdv.model.Produto;
import br.com.pdv.model.Subcategoria;
import br.com.pdv.util.AlertUtil;
import br.com.pdv.util.FormatUtil;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador para a visualização de produtos.
 * Gerencia a listagem, busca, adição, edição e remoção de produtos.
 */
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
    private TableColumn<Produto, String> vencimentoColumn;
    
    @FXML
    private TableColumn<Produto, String> alertaColumn;
    
    @FXML
    private Label totalProductsLabel;
    
    @FXML
    private VBox formPanel;
    
    @FXML
    private TextField codigoField;
    
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
    private DatePicker vencimentoDatePicker;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private Button saveButton;
    
    // DAOs e listas
    private ProdutoDAO produtoDAO;
    private CategoriaDAO categoriaDAO;
    private MarcaDAO marcaDAO;
    private SubcategoriaDAO subcategoriaDAO;
    
    // Lista para a tabela
    private ObservableList<Produto> produtos;
    
    // Estado atual
    private boolean editMode = false;
    private Produto currentProduct = null;
    
    // Referências para navegação
    private Stage stage;
    private Scene mainScene;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Inicializar DAOs
        inicializarDados();
        
        // Configurar as colunas da tabela
        configurarColunas();
        
        // Configurar a formatação das linhas com base no estoque/vencimento
        configurarEstilosLinhas();
        
        // Configurar os botões
        configurarEventosBotoes();
        
        // Carregar os produtos
        carregarProdutos();
        
        // Inicialmente esconder o painel de formulário
        mostrarFormulario(false);
        
        Platform.runLater(() -> productsTable.refresh());
    }
    
    /**
     * Inicializa o DAO e a lista de produtos
     */
    private void inicializarDados() {
        produtoDAO = ProdutoDAO.getInstance();
        categoriaDAO = CategoriaDAO.getInstance();
        marcaDAO = MarcaDAO.getInstance();
        subcategoriaDAO = SubcategoriaDAO.getInstance();
        
        produtos = FXCollections.observableArrayList();
        productsTable.setItems(produtos);
    }
    
    /**
     * Configura as colunas da tabela
     */
    private void configurarColunas() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colorColumn.setCellValueFactory(new PropertyValueFactory<>("cor"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("tamanho"));
        
        configurarColunaPreco();
        configurarColunaEstoque();
        configurarColunaEntrada();
        configurarColunaAcoes();
        configurarColunaVencimento();
        configurarColunaAlerta();
    }
   
    /**
     * Método de diagnóstico para verificar os arquivos FXML disponíveis
     */
    private void verificarArquivosFXML() {
        System.out.println("Verificando arquivos FXML disponíveis:");
        
        String[] caminhos = {
            "/br/com/pdv/gui/views/NovoProduto.fxml",
            "/br/com/pdv/gui/views/NovoProdutoView.fxml",
            "/br/com/pdv/gui/views/EntradaProdutoView.fxml"
        };
        
        for (String caminho : caminhos) {
            URL url = getClass().getResource(caminho);
            System.out.println("  - " + caminho + ": " + (url != null ? "ENCONTRADO" : "NÃO ENCONTRADO"));
        }
        
        // Verificar o diretório pai
        URL diretorioUrl = getClass().getResource("/br/com/pdv/gui/views/");
        System.out.println("  - Diretório /views/: " + (diretorioUrl != null ? "ENCONTRADO" : "NÃO ENCONTRADO"));
    }
    
    /**
     * Abre a tela de edição de produto quando o botão de ações é clicado
     * 
     * @param produto O produto selecionado para edição
     */
    private void abrirTelaEdicaoProduto(Produto produto) {
        System.out.println("Abrindo tela externa para edição do produto: " + produto.getNome());
        
        verificarArquivosFXML();
        
        try {
            // Carregar o arquivo FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/NovoProdutoView.fxml"));
            Parent root = loader.load();
            
            // Obter o controlador e configurar para edição
            NovoProdutoController controller = loader.getController();
            controller.editarProduto(produto);
            
            // Criar e configurar uma nova janela
            Stage stage = new Stage();
            stage.setTitle("Editar Produto: " + produto.getNome());
            stage.setScene(new Scene(root));
            stage.initOwner(productsTable.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            
            // Mostrar e aguardar
            stage.showAndWait();
            
            // Após fechar a tela, atualizar a lista de produtos
            carregarProdutos();
            
            // Garantir que a tabela seja atualizada na thread da UI
            Platform.runLater(() -> productsTable.refresh());
            
        } catch (IOException e) {
            AlertUtil.showError("Erro ao abrir tela", "Não foi possível abrir a tela de edição: " + e.getMessage());
            System.out.println("ERRO ao abrir tela: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
    /**
     * Configura a coluna de ações com um único botão de editar
     */
    private void configurarColunaAcoes() {
        actionsColumn.setCellFactory(col -> {
            TableCell<Produto, Void> cell = new TableCell<Produto, Void>() {
                private final Button editBtn = new Button("✏️");
                
                {
                    // Aplicar estilo visível e claro
                    editBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand;");
                    editBtn.setOnAction(event -> {
                        // Verificar se a célula não está vazia e tem um índice válido
                        if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                            Produto produto = getTableView().getItems().get(getIndex());
                            // Garantir que o produto não seja nulo antes de chamar o método
                            if (produto != null) {
                                System.out.println("Botão de edição clicado para produto: " + produto.getNome());
                                abrirTelaEdicaoProduto(produto);
                            }
                        }
                     
                    });
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    // Importante: sempre definir um conteúdo gráfico mesmo quando não vazio
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        // Definir explicitamente o botão toda vez
                        setGraphic(editBtn);
                        setText(null);
                    }
                }
            };
            
            // Ajustar o alinhamento da célula
            cell.setAlignment(javafx.geometry.Pos.CENTER);
            return cell;
        });
        
        // Garantir que a coluna tenha uma largura adequada
        actionsColumn.setPrefWidth(60);
        actionsColumn.setMinWidth(60);
        actionsColumn.setStyle("-fx-alignment: CENTER;");
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
     * Configura a coluna de entrada com botão de adicionar estoque
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
            cell.setAlignment(javafx.geometry.Pos.CENTER);
            return cell;
        });
    }

    
    
    /**
     * Configura os estilos das linhas da tabela com base no estoque e vencimento
     
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
    **/
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
        saveButton.setOnAction(event -> handleSaveButton());
        backButton.setOnAction(event -> voltarParaPDV());
    }

    private void configurarEstilosLinhas() {
        productsTable.setRowFactory(tv -> {
            TableRow<Produto> row = new TableRow<Produto>() {
                @Override
                protected void updateItem(Produto produto, boolean empty) {
                    super.updateItem(produto, empty);
                    if (produto == null || empty) {
                        setStyle("");
                    } else {
                        aplicarEstiloProduto(this, produto);
                    }
                }
            };
            
            // Adicionar o evento de duplo clique aqui
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editarProduto(row.getItem());
                }
            });
            
            return row;
        });
    }
    
    /**
     * Carrega os produtos do banco de dados
     */
    private void carregarProdutos() {
        try {
            // Limpar a lista existente
            produtos.clear();
            
            // Carregar produtos ativos do banco
            List<Produto> lista = produtoDAO.findAll();
            produtos.addAll(lista);
            
            // Atualizar o contador de produtos
            atualizarContadorProdutos();
            
            // Garantir que a tabela seja atualizada na thread da UI
            Platform.runLater(() -> productsTable.refresh());
            
        } catch (SQLException e) {
            AlertUtil.showError("Erro ao carregar produtos", e.getMessage());
        }
    }
    
    /**
     * Atualiza o contador de produtos no rodapé
     */
    private void atualizarContadorProdutos() {
        totalProductsLabel.setText("Total de produtos: " + produtos.size());
    }
    
    /**
     * Pesquisa produtos pelo termo digitado
     */
    private void buscarProdutos() {
        String termo = searchField.getText().trim();
        
        try {
            // Limpar a lista atual
            produtos.clear();
            
            List<Produto> resultados;
            
            // Se o termo estiver vazio, carregar todos os produtos
            if (termo.isEmpty()) {
                resultados = produtoDAO.findAll();
            } else {
                // Verifica se é um número (possível código) ou texto (possível nome)
                if (termo.matches("\\d+")) {
                    Produto produto = produtoDAO.findById(Integer.parseInt(termo));
                    resultados = produto != null ? List.of(produto) : new ArrayList<>();
                } else {
                    // Pode ser um código não numérico
                    Produto produtoPorCodigo = produtoDAO.findByCodigo(termo);
                    if (produtoPorCodigo != null) {
                        resultados = List.of(produtoPorCodigo);
                    } else {
                        resultados = produtoDAO.findByNome(termo);
                    }
                }
            }
            
            produtos.addAll(resultados);
            
            // Atualizar o contador
            atualizarContadorProdutos();
            
            // Garantir que a tabela seja atualizada na thread da UI
            Platform.runLater(() -> productsTable.refresh());
            
        } catch (SQLException e) {
            AlertUtil.showError("Erro na pesquisa", "Erro ao pesquisar produtos: " + e.getMessage());
        }
    }
    
    /**
     * Abre a tela de cadastro de novo produto
     */
    private void novoProduto() {
        try {
            // Carregar o arquivo FXML para novo produto
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/NovoProdutoView.fxml"));
            Parent root = loader.load();
            
            // Obter o controlador e configurar para novo produto
            NovoProdutoController controller = loader.getController();
            controller.novoProdutoView();
            
            // Criar e configurar uma nova janela
            Stage stage = new Stage();
            stage.setTitle("Novo Produto");
            stage.setScene(new Scene(root));
            stage.initOwner(productsTable.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            
            // Mostrar e aguardar
            stage.showAndWait();
            
            stage.setOnShown(e -> {
                System.out.println("Tela aberta com sucesso!");
            });

            stage.setOnHidden(e -> {
                System.out.println("Tela fechada!");
                // Garantir que a tabela seja atualizada após fechar a janela
                Platform.runLater(() -> {
                    carregarProdutos();
                    productsTable.refresh();
                });
            });
            
        } catch (IOException e) {
            AlertUtil.showError("Erro ao abrir tela", "Não foi possível abrir a tela de cadastro: " + e.getMessage());
        }
    }
    
    /**
     * Editar um produto existente
     */
    private void editarProduto(Produto produto) {
        System.out.println("Editando produto: " + produto.getNome());
        currentProduct = produto;
        preencherFormulario();
        System.out.println("Mostrando formulário");
        mostrarFormulario(true);
    }
    
    /**
     * Preenche o formulário com dados do produto
     */
    private void preencherFormulario() {
        codigoField.setText(currentProduct.getCodigo());
        nameField.setText(currentProduct.getNome());
        colorField.setText(currentProduct.getCor());
        sizeField.setText(currentProduct.getTamanho());
        priceField.setText(FormatUtil.formatarValor(currentProduct.getPreco()));
        stockField.setText(String.valueOf(currentProduct.getEstoqueAtual()));
        minStockField.setText(String.valueOf(currentProduct.getEstoqueMinimo()));
        
        // Configurar data de vencimento
        if (vencimentoDatePicker != null) {
            vencimentoDatePicker.setValue(currentProduct.getDataVencimento());
        }
    }
    
    /**
     * Mostra ou esconde o painel de formulário
     */
    private void mostrarFormulario(boolean visivel) {
        formPanel.setVisible(visivel);
        formPanel.setManaged(visivel);
    }
    
    /**
     * Manipulador para o botão de cancelar
     */
    @FXML
    public void handleCancelButton() {
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
    
    /**
     * Manipulador para o botão de salvar
     */
    @FXML
    public void handleSaveButton() {
        salvarProduto();
    }
    
    /**
     * Método para limpar o formulário e cancelar a edição
     */
    private void cancelarEdicao() {
        mostrarFormulario(false);
        currentProduct = null;
    }
    
    /**
     * Excluir o produto selecionado
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
            
            // Garantir que a tabela seja atualizada na thread da UI
            Platform.runLater(() -> productsTable.refresh());
            
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
     * Salva o produto com os dados do formulário
     */
    private void salvarProduto() {
        try {
            // Verificar se os campos de texto estão disponíveis
            if (nameField == null || codigoField == null || colorField == null || 
                sizeField == null || priceField == null || stockField == null || 
                minStockField == null) {
                AlertUtil.showError("Erro de interface", "Alguns campos de formulário não estão disponíveis.");
                return;
            }
            
            // Obter valores com verificação de nulidade
            String nome = nameField.getText() != null ? nameField.getText().trim() : "";
            String codigo = codigoField.getText() != null ? codigoField.getText().trim() : "";
            String cor = colorField.getText() != null ? colorField.getText().trim() : "";
            String tamanho = sizeField.getText() != null ? sizeField.getText().trim() : "";
            String precoStr = priceField.getText() != null ? priceField.getText() : "0";
            String estoqueStr = stockField.getText() != null ? stockField.getText().trim() : "0";
            String estoqueMinimoStr = minStockField.getText() != null ? minStockField.getText().trim() : "0";
            
            // Validar campos obrigatórios
            if (nome.isEmpty()) {
                AlertUtil.showWarning("Campo obrigatório", "O nome do produto é obrigatório.");
                nameField.requestFocus();
                return;
            }
            
            // Cria um novo produto se não estiver em modo de edição
            if (currentProduct == null) {
                currentProduct = new Produto();
                
                // Gerar código único se não foi preenchido
                if (codigo.isEmpty()) {
                    codigo = gerarCodigoUnico(nome);
                }
            }
            
            // Preencher o objeto produto com os dados do formulário
            currentProduct.setNome(nome);
            currentProduct.setCodigo(codigo);
            currentProduct.setCor(cor);
            currentProduct.setTamanho(tamanho);
            
            // Converter e validar preço
            try {
                precoStr = precoStr.replace("R$", "").replace(".", "").replace(",", ".").trim();
                BigDecimal preco = new BigDecimal(precoStr.isEmpty() ? "0" : precoStr);
                currentProduct.setPreco(preco);
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Valor inválido", "Preço inválido. Use o formato 00,00.");
                priceField.requestFocus();
                return;
            }
            
            // Converter e validar estoque
            try {
                int estoque = Integer.parseInt(estoqueStr.isEmpty() ? "0" : estoqueStr);
                currentProduct.setEstoqueAtual(estoque);
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Valor inválido", "Estoque inválido. Digite um número inteiro.");
                stockField.requestFocus();
                return;
            }
            
            // Converter e validar estoque mínimo
            try {
                int estoqueMinimo = Integer.parseInt(estoqueMinimoStr.isEmpty() ? "0" : estoqueMinimoStr);
                currentProduct.setEstoqueMinimo(estoqueMinimo);
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Valor inválido", "Estoque mínimo inválido. Digite um número inteiro.");
                minStockField.requestFocus();
                return;
            }
            
            // Configura a data de vencimento
            if (vencimentoDatePicker != null) {
                currentProduct.setDataVencimento(vencimentoDatePicker.getValue());
            }
            
            // Definir produto como ativo
            currentProduct.setAtivo(true);
            
            // Salvar o produto
            if (currentProduct.getId() == null) {
                produtoDAO.create(currentProduct);
                AlertUtil.showInfo("Sucesso", "Produto cadastrado com sucesso!");
            } else {
                produtoDAO.update(currentProduct);
                AlertUtil.showInfo("Sucesso", "Produto atualizado com sucesso!");
            }
            
            // Recarregar a lista e esconder o formulário
            carregarProdutos();
            mostrarFormulario(false);
            currentProduct = null;
            
            // Garantir que a tabela seja atualizada na thread da UI
            Platform.runLater(() -> productsTable.refresh());
            
        } catch (SQLException e) {
            AlertUtil.showError("Erro ao salvar produto", "Ocorreu um erro ao salvar o produto: " + e.getMessage());
        } catch (Exception e) {
            AlertUtil.showError("Erro inesperado", "Ocorreu um erro inesperado: " + e.getMessage());
            e.printStackTrace();
        }
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
    
    /**
     * Método para adicionar estoque a um produto
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
            
            // Garantir que a tabela seja atualizada na thread da UI
            Platform.runLater(() -> productsTable.refresh());
            
        } catch (IOException e) {
            AlertUtil.showError("Erro ao abrir tela de entrada", e.getMessage());
        }
    }
    
    /**
     * Volta para a tela anterior (geralmente a tela principal)
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
     * Configura o Stage e a Scene para este controlador
     * 
     * @param stage O Stage a ser configurado
     * @param scene A Scene a ser configurada
     */
    public void setStage(Stage stage, Scene scene) {
        this.stage = stage;
        this.mainScene = scene;
    }
    
    /**
     * Busca um produto pelo código
     * 
     * @param codigo O código do produto
     * @return O produto encontrado ou null
     */
    public Produto findByCodigo(String codigo) {
        try {
            // Delegar para o DAO
            return produtoDAO.findByCodigo(codigo);
        } catch (SQLException e) {
            AlertUtil.showError("Erro ao buscar produto", e.getMessage());
            return null;
        }
    }
}