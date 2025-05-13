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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador para a visualização de produtos.
 * Gerencia a listagem, busca, adição, edição e remoção de produtos.
 */
public class ProdutController implements Initializable {

    // Componentes da tabela de produtos - Nomes corrigidos conforme o FXML
    @FXML private TableView<Produto> productsTable;
    @FXML private TableColumn<Produto, Integer> idColumn;
    @FXML private TableColumn<Produto, String> nameColumn;
    @FXML private TableColumn<Produto, String> colorColumn;
    @FXML private TableColumn<Produto, String> sizeColumn;
    @FXML private TableColumn<Produto, BigDecimal> priceColumn;
    @FXML private TableColumn<Produto, Integer> stockColumn;
    @FXML private TableColumn<Produto, String> inputColumn;
    @FXML private TableColumn<Produto, String> actionsColumn;
    @FXML private TableColumn<Produto, LocalDate> vencimentoColumn;
    @FXML private TableColumn<Produto, String> alertaColumn;
    
    // Componentes de controle da interface
    @FXML private Button newButton;
    @FXML private Button backButton;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Label totalProductsLabel;
    
    // Componentes do formulário
    @FXML private VBox formPanel;
    @FXML private TextField codigoField;
    @FXML private TextField nameField;
    @FXML private TextField colorField;
    @FXML private TextField sizeField;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private TextField minStockField;
    @FXML private DatePicker vencimentoDatePicker;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;
    
    // Listas e DAOs
    private ObservableList<Produto> produtos;
    private ProdutoDAO produtoDAO;
    private CategoriaDAO categoriaDAO;
    private MarcaDAO marcaDAO;
    private SubcategoriaDAO subcategoriaDAO;
    
    // Estado atual
    private boolean editMode = false;
    private Produto currentProduto = null;
    
    // Guarda a cena principal para voltar
    private Scene mainScene;
    private Stage primaryStage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Inicializar DAOs
        produtoDAO = ProdutoDAO.getInstance();
        categoriaDAO = CategoriaDAO.getInstance();
        marcaDAO = MarcaDAO.getInstance();
        subcategoriaDAO = SubcategoriaDAO.getInstance();
        
        // Configurar as colunas da tabela
        configurarColunas();
        
        // Inicializar a lista de produtos
        produtos = FXCollections.observableArrayList();
        productsTable.setItems(produtos);
        
        // Carregar os produtos
        carregarProdutos();
        
        // Configurar os botões
        configurarBotoes();
        
        // Inicialmente esconder o painel de formulário
        formPanel.setVisible(false);
        formPanel.setManaged(false);
    }
    
    /**
     * Configura as colunas da tabela
     */
    private void configurarColunas() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colorColumn.setCellValueFactory(new PropertyValueFactory<>("cor"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("tamanho"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("preco"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("estoqueAtual"));
        
        // Configurar coluna de data de entrada
        inputColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDataCadastro() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getDataCadastro().toLocalDate().format(formatter));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        
        // Configurar coluna de data de vencimento
        vencimentoColumn.setCellValueFactory(new PropertyValueFactory<>("dataVencimento"));
        
        // Configurar coluna de alerta de estoque
        alertaColumn.setCellValueFactory(cellData -> {
            Produto produto = cellData.getValue();
            boolean estoqueAbaixoDoMinimo = produto.getEstoqueAtual() < produto.getEstoqueMinimo();
            return new javafx.beans.property.SimpleStringProperty(estoqueAbaixoDoMinimo ? "⚠️" : "");
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
            List<Produto> lista = produtoDAO.findAllAtivos();
            produtos.addAll(lista);
            
            // Atualizar o contador de produtos
            atualizarContadorProdutos();
            
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
     * Configura os botões da tela
     */
    private void configurarBotoes() {
        // Botão Novo Produto
        newButton.setOnAction(event -> mostrarFormulario(null));
        
        // Botão Voltar
        backButton.setOnAction(event -> voltarParaTelaAnterior());
        
        // Botão Pesquisar
        searchButton.setOnAction(event -> pesquisarProdutos());
        
        // Configurar evento de pesquisa ao pressionar Enter
        searchField.setOnAction(event -> pesquisarProdutos());
        
        // Botões do formulário
        cancelButton.setOnAction(event -> esconderFormulario());
        saveButton.setOnAction(event -> salvarProduto());
        
        // Configurar evento de duplo clique na tabela
        productsTable.setRowFactory(tv -> {
            TableRow<Produto> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    mostrarFormulario(row.getItem());
                }
            });
            return row;
        });
    }
    
    /**
     * Mostra o formulário para adicionar ou editar um produto
     * 
     * @param produto O produto a ser editado, ou null para novo produto
     */
    private void mostrarFormulario(Produto produto) {
        editMode = (produto != null);
        currentProduto = produto;
        
        if (editMode) {
            // Preencher o formulário com os dados do produto
            codigoField.setText(produto.getCodigo());
            nameField.setText(produto.getNome());
            colorField.setText(produto.getCor());
            sizeField.setText(produto.getTamanho());
            priceField.setText(FormatUtil.formatarValor(produto.getPreco()));
            stockField.setText(String.valueOf(produto.getEstoqueAtual()));
            minStockField.setText(String.valueOf(produto.getEstoqueMinimo()));
            vencimentoDatePicker.setValue(produto.getDataVencimento());
        } else {
            // Limpar o formulário para um novo produto
            codigoField.clear();
            nameField.clear();
            colorField.clear();
            sizeField.clear();
            priceField.setText("0,00");
            stockField.setText("0");
            minStockField.setText("0");
            vencimentoDatePicker.setValue(LocalDate.now().plusMonths(6));
        }
        
        // Mostrar o painel de formulário
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }
    
    /**
     * Esconde o formulário
     */
    private void esconderFormulario() {
        formPanel.setVisible(false);
        formPanel.setManaged(false);
        editMode = false;
        currentProduto = null;
    }
    
    /**
     * Manipulador para o botão de cancelar
     */
    @FXML
    public void handleCancelButton(ActionEvent event) {
        esconderFormulario();
    }
    
    /**
     * Manipulador para o botão de salvar
     */
    @FXML
    public void handleSaveButton(ActionEvent event) {
        salvarProduto();
    }
    
    /**
     * Salva o produto com os dados do formulário
     */
    private void salvarProduto() {
        try {
            // Validar campos obrigatórios
            if (nameField.getText().trim().isEmpty()) {
                AlertUtil.showWarning("Campo obrigatório", "O nome do produto é obrigatório.");
                nameField.requestFocus();
                return;
            }
            
            // Criar um novo produto ou usar o existente em modo de edição
            Produto produto;
            if (editMode && currentProduto != null) {
                produto = currentProduto;
            } else {
                produto = new Produto();
            }
            
            // Preencher os dados do produto a partir do formulário
            produto.setCodigo(codigoField.getText().trim());
            produto.setNome(nameField.getText().trim());
            produto.setCor(colorField.getText().trim());
            produto.setTamanho(sizeField.getText().trim());
            
            try {
                String precoStr = priceField.getText().replace("R$", "").replace(".", "").replace(",", ".").trim();
                produto.setPreco(new BigDecimal(precoStr));
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Valor inválido", "Preço inválido. Use o formato 0,00.");
                priceField.requestFocus();
                return;
            }
            
            try {
                produto.setEstoqueAtual(Integer.parseInt(stockField.getText().trim()));
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Valor inválido", "Estoque inválido. Digite um número inteiro.");
                stockField.requestFocus();
                return;
            }
            
            try {
                produto.setEstoqueMinimo(Integer.parseInt(minStockField.getText().trim()));
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Valor inválido", "Estoque mínimo inválido. Digite um número inteiro.");
                minStockField.requestFocus();
                return;
            }
            
            produto.setDataVencimento(vencimentoDatePicker.getValue());
            produto.setAtivo(true);
            
            // Salvar o produto
            if (editMode) {
                produtoDAO.update(produto);
                AlertUtil.showInfo("Sucesso", "Produto atualizado com sucesso!");
            } else {
                produtoDAO.create(produto);
                AlertUtil.showInfo("Sucesso", "Produto cadastrado com sucesso!");
            }
            
            // Recarregar a lista e esconder o formulário
            carregarProdutos();
            esconderFormulario();
            
        } catch (SQLException e) {
            AlertUtil.showError("Erro ao salvar produto", "Ocorreu um erro ao salvar o produto: " + e.getMessage());
        }
    }
    
    /**
     * Pesquisa produtos pelo termo digitado
     */
    private void pesquisarProdutos() {
        String termo = searchField.getText().trim();
        
        try {
            // Limpar a lista atual
            produtos.clear();
            
            // Se o termo estiver vazio, carregar todos os produtos
            if (termo.isEmpty()) {
                carregarProdutos();
                return;
            }
            
            // Buscar produtos pelo termo de pesquisa
            List<Produto> resultados = produtoDAO.findByNome(termo);
            produtos.addAll(resultados);
            
            // Atualizar o contador
            atualizarContadorProdutos();
            
        } catch (SQLException e) {
            AlertUtil.showError("Erro na pesquisa", "Erro ao pesquisar produtos: " + e.getMessage());
        }
    }
    
    /**
     * Volta para a tela anterior (geralmente a tela principal)
     */
    private void voltarParaTelaAnterior() {
        if (primaryStage != null && mainScene != null) {
            primaryStage.setScene(mainScene);
            primaryStage.setTitle("PDV - Sistema de Ponto de Venda");
        } else {
            AlertUtil.showWarning("Erro", "Não foi possível voltar à tela anterior.");
        }
    }
    
    /**
     * Configura o Stage e a Scene para este controlador
     * 
     * @param stage O Stage a ser configurado
     * @param scene A Scene a ser configurada
     */
    public void setStage(Stage stage, Scene scene) {
        this.primaryStage = stage;
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