package br.com.pdv.controller;

import br.com.pdv.dao.ClienteDAO;
import br.com.pdv.model.Cliente;
import br.com.pdv.util.AlertUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ClienteController implements Initializable {

    @FXML
    private TextField searchField;
    
    @FXML
    private Button searchButton;
    
    @FXML
    private Button newButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    private TableView<Cliente> clientesTable;
    
    @FXML
    private TableColumn<Cliente, String> nomeColumn;
    
    @FXML
    private TableColumn<Cliente, String> celularColumn;
    
    @FXML
    private TableColumn<Cliente, String> emailColumn;
    
    @FXML
    private TableColumn<Cliente, String> cpfColumn;
    
    @FXML
    private TableColumn<Cliente, Void> actionsColumn;
    
    private ObservableList<Cliente> clientesList;
    private ClienteDAO clienteDAO;
    private Scene mainScene;
    private Stage stage;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Inicializar DAO e lista de clientes
        clienteDAO = ClienteDAO.getInstance();
        clientesList = FXCollections.observableArrayList();
        
        // Configurar colunas da tabela
        configurarColunas();
        
        // Configurar eventos dos botões
        configurarEventosBotoes();
        
        // Carregar dados iniciais
        carregarClientes();
        
        // Configurar evento de pesquisa no campo de busca
        searchField.setOnAction(event -> pesquisarClientes());
    }
    
    /**
     * Configura as colunas da tabela
     */
    private void configurarColunas() {
        // Configurar colunas básicas
        nomeColumn.setCellValueFactory(new PropertyValueFactory<>("nome"));
        
        celularColumn.setCellValueFactory(cellData -> {
            String telefone = cellData.getValue().getTelefone();
            return new SimpleStringProperty(telefone != null ? telefone : "");
        });
        
        emailColumn.setCellValueFactory(cellData -> {
            String email = cellData.getValue().getEmail();
            return new SimpleStringProperty(email != null ? email : "");
        });
        
        cpfColumn.setCellValueFactory(cellData -> {
            String cpfCnpj = cellData.getValue().getCpfCnpj();
            return new SimpleStringProperty(cpfCnpj != null ? cpfCnpj : "");
        });
        
        // Configurar coluna de ações
        configurarColunaAcoes();
    }
    
    /**
     * Configura a coluna de ações com botões de editar/visualizar
     */
    private void configurarColunaAcoes() {
        actionsColumn.setCellFactory(col -> {
            TableCell<Cliente, Void> cell = new TableCell<Cliente, Void>() {
                private final Button btn = new Button();
                
                {
                    btn.getStyleClass().add("edit-button");
                    btn.setText("✏️");
                    btn.setOnAction(event -> {
                        Cliente cliente = getTableView().getItems().get(getIndex());
                        editarCliente(cliente);
                    });
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(btn);
                    }
                }
            };
            return cell;
        });
    }
    
    /**
     * Configura os eventos dos botões
     */
    private void configurarEventosBotoes() {
        searchButton.setOnAction(event -> pesquisarClientes());
        newButton.setOnAction(event -> novoCliente());
        backButton.setOnAction(event -> voltarParaTelaPrincipal());
    }
    
    /**
     * Carrega todos os clientes do banco de dados
     */
    private void carregarClientes() {
        try {
            // Usando o método readAll() disponível na classe ClienteDAO
            List<Cliente> clientes = clienteDAO.readAll();
            clientesList.clear();
            clientesList.addAll(clientes);
            clientesTable.setItems(clientesList);
        } catch (SQLException e) {
            AlertUtil.showError("Erro ao carregar clientes", 
                "Ocorreu um erro ao carregar a lista de clientes: " + e.getMessage());
        }
    }
    
    /**
     * Pesquisa clientes com base no texto inserido no campo de busca
     */
    @FXML
    private void pesquisarClientes() {
        String termo = searchField.getText().trim();
        
        try {
            List<Cliente> resultados;
            
            if (termo.isEmpty()) {
                // Usando o método readAll() disponível na classe ClienteDAO
                resultados = clienteDAO.readAll();
            } else {
                // Verificar se é um CPF/CNPJ ou nome
                if (termo.matches("\\d+")) {
                    // Verificar se é um CPF/CNPJ
                    Cliente cliente = clienteDAO.findByCpfCnpj(termo);
                    resultados = cliente != null ? List.of(cliente) : new ArrayList<>();
                } else {
                    // Usar busca por nome parcial em vez de exato
                    // Verifique se o método findByNomeLike existe no ClienteDAO
                    resultados = clienteDAO.findByNomeLike(termo);
                    
                    // Se findByNomeLike não existir, use findByNome como fallback
                    if (resultados == null) {
                        Cliente cliente = clienteDAO.findByNome(termo);
                        resultados = cliente != null ? List.of(cliente) : new ArrayList<>();
                    }
                }
            }
            
            clientesList.clear();
            clientesList.addAll(resultados);
            
            // Atualizar a tabela explicitamente
            clientesTable.setItems(clientesList);
            
        } catch (SQLException e) {
            AlertUtil.showError("Erro na pesquisa", 
                "Ocorreu um erro ao pesquisar clientes: " + e.getMessage());
        }
    }
    
    /**
     * Abre o formulário para cadastrar um novo cliente
     */
    @FXML
    private void novoCliente() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/NovoClienteView.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Novo Cliente");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
            // Recarregar a lista de clientes após fechar o formulário
            carregarClientes();
            
        } catch (IOException e) {
            AlertUtil.showError("Erro ao abrir formulário", 
                "Não foi possível abrir o formulário de cadastro: " + e.getMessage());
        }
    }
    
    /**
     * Abre o formulário para editar um cliente existente
     * 
     * @param cliente O cliente a ser editado
     */
    private void editarCliente(Cliente cliente) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/NovoClienteView.fxml"));
            Parent root = loader.load();
            
            // Obter o controlador e passar o cliente para edição
            NovoClienteController controller = loader.getController();
            controller.setClienteParaEdicao(cliente);
            
            Stage stage = new Stage();
            stage.setTitle("Editar Cliente");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
            // Recarregar a lista de clientes após fechar o formulário
            carregarClientes();
            
        } catch (IOException e) {
            AlertUtil.showError("Erro ao abrir formulário", 
                "Não foi possível abrir o formulário de edição: " + e.getMessage());
        }
    }
    
    /**
     * Configura o Stage e a cena principal para navegação
     */
    public void setStage(Stage stage, Scene mainScene) {
        this.stage = stage;
        this.mainScene = mainScene;
    }
    
    /**
     * Retorna para a tela principal do sistema
     */
    @FXML
    private void voltarParaTelaPrincipal() {
        if (stage != null && mainScene != null) {
            // Guardar dimensões e estado atual
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean maximized = stage.isMaximized();
            
            // Voltar para a cena principal
            stage.setScene(mainScene);
            stage.setTitle("PDV - Sistema de Ponto de Venda");
            
            // Restaurar dimensões e estado
            stage.setWidth(width);
            stage.setHeight(height);
            stage.setMaximized(maximized);
        } else {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/br/com/pdv/gui/views/MainView.fxml"));
                Scene scene = new Scene(root);
                Stage stage = (Stage) backButton.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("PDV - Sistema de Ponto de Venda");
            } catch (IOException e) {
                AlertUtil.showError("Erro ao retornar", 
                    "Não foi possível retornar à tela principal: " + e.getMessage());
            }
        }
    }
}