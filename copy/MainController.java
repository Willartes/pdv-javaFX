package br.com.pdv.controller;

import br.com.pdv.dao.*;
import br.com.pdv.exception.CpfCnpjDuplicadoException;
import br.com.pdv.model.Pedido;
import java.time.LocalDateTime;
import br.com.pdv.model.Produto;
import br.com.pdv.model.Usuario;
import br.com.pdv.util.AlertUtil;
import br.com.pdv.util.DatabaseConnection;
import br.com.pdv.util.FormatUtil;
import br.com.pdv.util.LogUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.SQLException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import br.com.pdv.model.Cliente;
import br.com.pdv.model.ItemPedido;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javafx.scene.control.ScrollPane;


import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;

import br.com.pdv.dao.ClienteDAO;
import br.com.pdv.dao.ItemPedidoDAO;
import br.com.pdv.dao.PedidoDAO;
import br.com.pdv.dao.ProdutoDAO;
import java.sql.SQLException;

public class MainController implements Initializable {

    @FXML
    private Button configButton;
    
    @FXML
    private TextField productField;
    
    @FXML
    private TextField clientField;
    
    @FXML
    private TextField quantityField;
    
    @FXML
    private TextField unitValueField;
    
    @FXML
    private TextField totalValueField;
    
    @FXML
    private Button addButton;
    
    @FXML
    private ComboBox<String> sellerComboBox;
    
    @FXML
    private Label subtotalLabel;
    
    @FXML
    private Label receivedLabel;
    
    @FXML
    private Label remainingLabel;
    
    @FXML
    private Label changeLabel;
    
    @FXML
    private Button salvarButton;
    
    
    @FXML
    private TableView<ItemPedido> pedidoTable;

    @FXML
    private TableColumn<ItemPedido, String> colProduto;

    @FXML
    private TableColumn<ItemPedido, String> colCodigo;

    @FXML
    private TableColumn<ItemPedido, Integer> colQtd;

    @FXML
    private TableColumn<ItemPedido, String> colPreco;

    @FXML
    private TableColumn<ItemPedido, String> colTotal;

    @FXML
    private Button incluirButton;
    
    @FXML
    private Button cancelButton;
    
    private Scene mainScene;
    private ProdutosController produtosController;
    
    
 // Lista observável para armazenar os itens do pedido atual
    private ObservableList<ItemPedido> itensPedido = FXCollections.observableArrayList();

    // Pedido atual
    private Pedido pedidoAtual;

    // Método modificado initialize para configurar a tabela e o botão incluir
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Código atual do initialize...
        
        // Adicionar o evento para o botão incluir
        incluirButton.setOnAction(event -> incluirItemPedido());
        
        
        // Verificar se o botão existe antes de configurar
        if (cancelButton != null) {
            cancelButton.setOnAction(event -> cancelarPedido());
        } else {
            System.err.println("AVISO: O botão cancelarButton não foi encontrado no FXML!");
        }
        // Configurar colunas da tabela
        configurarTabela();
        
        // Inicializar o pedido atual
        inicializarPedido();
        
        // Configurar atalhos de teclado
        configurarAtalhosTeclado();
        
        // Configurar eventos de cálculo para quantidade e valor unitário
        quantityField.textProperty().addListener((obs, oldVal, newVal) -> calcularValorTotal());
        unitValueField.textProperty().addListener((obs, oldVal, newVal) -> calcularValorTotal());
        
        // Configurar a barra superior com busca e menu
        Platform.runLater(() -> {
            Node root = salvarButton.getScene().getRoot();
            if (root instanceof BorderPane) {
                configurarBarraSuperior((BorderPane)root);
            }
        });
        
        // Se a ComboBox de vendedores estiver vazia, preenchê-la com vendedores do banco de dados
     // No método initialize() do MainController
        if (sellerComboBox.getItems().isEmpty()) {
            try {
                UsuarioDAO usuarioDAO = UsuarioDAO.getInstance();
                List<Usuario> vendedores = usuarioDAO.findByPerfil("VENDEDOR");
                
                if (vendedores.isEmpty()) {
                    // Não há vendedores cadastrados, criar um padrão para testes
                    Usuario vendedorPadrao = new Usuario();
                    vendedorPadrao.setNome("Vendedor Teste");
                    vendedorPadrao.setLogin("vendedor2");
                    vendedorPadrao.setSenha("123456");
                    vendedorPadrao.setPerfil("VENDEDOR");
                    vendedorPadrao.setAtivo(true);
                    vendedorPadrao.setDataCadastro(LocalDateTime.now());
                    
                    try {
                        usuarioDAO.create(vendedorPadrao);
                        vendedores.add(vendedorPadrao);
                        LogUtil.info(getClass(), "Vendedor padrão criado para testes");
                    } catch (Exception e) {
                        LogUtil.error(getClass(), "Erro ao criar vendedor padrão", e);
                    }
                }
                
                ObservableList<String> vendedoresNomes = FXCollections.observableArrayList();
                for (Usuario vendedor : vendedores) {
                    vendedoresNomes.add(vendedor.getNome());
                }
                
                sellerComboBox.setItems(vendedoresNomes);
                
                // Selecionar o primeiro vendedor por padrão
                if (!vendedoresNomes.isEmpty()) {
                    sellerComboBox.getSelectionModel().selectFirst();
                }
            } catch (SQLException e) {
                LogUtil.error(getClass(), "Erro ao carregar vendedores", e);
            }
        }
	     // Opcional: Adicione atalho de teclado (tecla Escape) para cancelar o pedido
	     Platform.runLater(() -> {
	         Scene scene = salvarButton.getScene();
	         if (scene != null) {
	             scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
	                 if (event.getCode() == KeyCode.ESCAPE) {
	                     cancelarPedido();
	                     event.consume();
	                 }
	             });
	         }
	     });
    }

    /**
     * Configura as colunas da tabela de pedidos
     */
    private void configurarTabela() {
        // Configurar as colunas da tabela
        colProduto.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduto().getNome()));
        
        colCodigo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduto().getCodigo()));
        
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        
        colPreco.setCellValueFactory(data -> {
            DecimalFormat df = new DecimalFormat("0.00");
            return new SimpleStringProperty("R$ " + df.format(data.getValue().getValorUnitario()));
        });
        
        colTotal.setCellValueFactory(data -> {
            DecimalFormat df = new DecimalFormat("0.00");
            return new SimpleStringProperty("R$ " + df.format(data.getValue().getValorTotal()));
        });
        
        // Associar a lista de itens à tabela
        pedidoTable.setItems(itensPedido);
    }

    
    /**
     * Inicializa um novo pedido
     */
    private void inicializarPedido() {
        // Criar um novo pedido
        pedidoAtual = new Pedido();
        pedidoAtual.setDataPedido(LocalDateTime.now());
        pedidoAtual.setStatus("ABERTO");
        pedidoAtual.setValorTotal(BigDecimal.ZERO);
        pedidoAtual.setItens(new ArrayList<>());
        
        // Limpar a lista de itens observável
        itensPedido.clear();
        
        // Limpar o campo de cliente
        clientField.clear();
        
        // Limpar a seleção de vendedor (opcional)
        sellerComboBox.getSelectionModel().clearSelection();
        
        // Se a ComboBox de vendedores estiver vazia, preencher com alguns valores (apenas para exemplo)
        if (sellerComboBox.getItems().isEmpty()) {
            sellerComboBox.setItems(FXCollections.observableArrayList("Vendedor 1", "Gislaine H Rodrigues", "Vendedor 3"));
        }
        
        // Atualizar os labels de totais
        atualizarTotais();
    }
    
    /**
     * Inclui um item ao pedido atual e atualiza a tabela
     */
    @FXML
    private void incluirItemPedido() {
        try {
            // Validar campos obrigatórios
            if (productField.getText().isEmpty() || unitValueField.getText().isEmpty() || quantityField.getText().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Campos Obrigatórios");
                alert.setHeaderText("Preencha todos os campos obrigatórios");
                alert.setContentText("Os campos Produto, Valor Unitário e Quantidade são obrigatórios.");
                alert.showAndWait();
                return;
            }

            // Obter os valores dos campos
            String nomeProduto = productField.getText();
            BigDecimal valorUnitario = new BigDecimal(unitValueField.getText().replace(",", "."));
            int quantidade = Integer.parseInt(quantityField.getText());
            
            // Verificar quantidade válida
            if (quantidade <= 0) {
                AlertUtil.showWarning("Quantidade Inválida", "A quantidade deve ser maior que zero.");
                quantityField.requestFocus();
                return;
            }
            
            // Buscar o produto no banco de dados (em vez de criar um temporário)
            Produto produto = null;
            try {
                ProdutoDAO produtoDAO = ProdutoDAO.getInstance();
                
                // Tentar buscar primeiro por nome exato
                List<Produto> produtos = produtoDAO.findByNome(nomeProduto);
                if (!produtos.isEmpty()) {
                    produto = produtos.get(0);
                } else {
                    // Tentar buscar por código
                    produto = produtoDAO.findByCodigo(nomeProduto);
                }
                
                // Se não encontrou o produto
                if (produto == null) {
                    AlertUtil.showWarning("Produto não encontrado", 
                        "O produto '" + nomeProduto + "' não foi encontrado no banco de dados.");
                    return;
                }
            } catch (SQLException e) {
                AlertUtil.showError("Erro ao buscar produto", 
                    "Ocorreu um erro ao buscar o produto no banco de dados: " + e.getMessage());
                return;
            }
            
            // Verificar se há estoque disponível
            if (!verificarEstoqueDisponivel(produto, quantidade)) {
                return; // Método vai mostrar mensagem adequada
            }
            
            // Criar o item de pedido
            ItemPedido item = new ItemPedido();
            item.setProduto(produto);
            item.setQuantidade(quantidade);
            item.setValorUnitario(valorUnitario);
            item.setValorTotal(valorUnitario.multiply(new BigDecimal(quantidade)));
            
            // Adicionar o item à lista de itens do pedido
            itensPedido.add(item);
            
            // Adicionar à lista de itens do objeto pedido
            if (pedidoAtual.getItens() == null) {
                pedidoAtual.setItens(new ArrayList<>());
            }
            pedidoAtual.getItens().add(item);
            
            // Recalcular o valor total do pedido
            recalcularValorTotalPedido();
            
            // Atualizar os totais exibidos
            atualizarTotais();
            
            // Limpar os campos para o próximo item
            productField.clear();
            unitValueField.setText("0,00");
            quantityField.setText("1");
            totalValueField.setText("0,00");
            
            // Focar no campo de produto
            productField.requestFocus();
            
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro de Formato");
            alert.setHeaderText("Valor inválido");
            alert.setContentText("Certifique-se de que os valores numéricos estão corretos.");
            alert.showAndWait();
        }
    }
    /**
     * Recalcula o valor total do pedido
     */
    private void recalcularValorTotalPedido() {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itensPedido) {
            total = total.add(item.getValorTotal());
        }
        pedidoAtual.setValorTotal(total);
    }

    /**
     * Atualiza os labels de totais na tela
     */
    private void atualizarTotais() {
        DecimalFormat df = new DecimalFormat("0.00");
        
        // Atualizar o valor do subtotal
        subtotalLabel.setText(df.format(pedidoAtual.getValorTotal()));
        
        // Inicialmente, o valor recebido é zero
        receivedLabel.setText("0,00");
        
        // O valor restante é igual ao subtotal
        remainingLabel.setText(df.format(pedidoAtual.getValorTotal()));
        
        // O troco inicialmente é zero
        changeLabel.setText("0,00");
    }

    

    /**
     * Salva o pedido atual no banco de dados e dá baixa no estoque
     * Implementa um mecanismo de retry para lidar com timeout de bloqueio
     */
    @FXML
    private void salvarPedido() {
    	final int MAX_RETRIES = 3;
        int tentativas = 0;
        boolean sucesso = false;
        
        while (tentativas < MAX_RETRIES && !sucesso) {
            Connection conn = null;
            try {
                tentativas++;
                
                // Validar o pedido (itens, cliente, vendedor)
                if (!validarPedido()) {
                    return; // Se não for válido, o método de validação já mostra a mensagem apropriada
                }
                
                // Definir o vendedor selecionado
                if (sellerComboBox.getSelectionModel().getSelectedItem() != null) {
                    String nomeVendedor = sellerComboBox.getSelectionModel().getSelectedItem();
                    
                    // Buscar o usuário no banco de dados pelo nome
                    try {
                        UsuarioDAO usuarioDAO = UsuarioDAO.getInstance();
                        Usuario vendedor = usuarioDAO.findByLogin(nomeVendedor);
                        
                        // Se não encontrou pelo login, tentar pelo nome
                        if (vendedor == null) {
                            vendedor = usuarioDAO.findByNome(nomeVendedor);
                        }
                        
                        if (vendedor != null) {
                            pedidoAtual.setUsuario(vendedor);
                            // Adicionar o vendedor_id explicitamente - esta é a correção principal
                            pedidoAtual.setVendedorId(vendedor.getId().longValue());
                            pedidoAtual.setVendedor(vendedor); // Se o método setVendedor existe
                            LogUtil.info(getClass(), "Vendedor associado ao pedido: " + vendedor.getNome() + ", ID: " + vendedor.getId());
                        } else {
                            // Não encontrou o vendedor - mostrar um erro
                            AlertUtil.showError("Erro", "Vendedor '" + nomeVendedor + "' não encontrado no banco de dados");
                            return; // Adicionado return para não continuar
                        }
                    } catch (SQLException e) {
                        AlertUtil.showError("Erro ao buscar vendedor", 
                            "Ocorreu um erro ao buscar o vendedor no banco de dados: " + e.getMessage());
                        LogUtil.error(getClass(), "Erro ao buscar vendedor", e);
                        return; // Adicionado return para não continuar
                    }
                } else {
                    // Nenhum vendedor selecionado - mostrar um erro
                    AlertUtil.showWarning("Vendedor Obrigatório", 
                        "É necessário selecionar um vendedor para o pedido.");
                    return; // Adicionado return para não continuar
                }
                
                // Se não for a primeira tentativa, perguntar se o usuário quer tentar novamente
                if (tentativas > 1) {
                    boolean continuar = AlertUtil.showConfirmation(
                        "Tentar novamente?", 
                        "Ocorreu um erro ao salvar o pedido anteriormente. Deseja tentar novamente? (Tentativa " 
                            + tentativas + " de " + MAX_RETRIES + ")");
                    
                    if (!continuar) {
                        return;
                    }
                } else {
                    // Primeira tentativa - Perguntar se o usuário confirma a venda
                    boolean confirmado = AlertUtil.showConfirmation(
                        "Confirmar Venda", 
                        "Deseja finalizar a venda e dar baixa no estoque dos produtos?");
                    
                    if (!confirmado) {
                        return;
                    }
                }
                
                // Iniciar transação
                conn = DatabaseConnection.getInstance().getConnection();
                conn.setAutoCommit(false);
                
                // Dar baixa no estoque dos produtos
                boolean baixaRealizada = darBaixaEstoque(conn);
                
                if (!baixaRealizada) {
                    // Reverter transação se a baixa de estoque falhar
                    conn.rollback();
                    return; // A mensagem de erro já foi exibida pelo método darBaixaEstoque
                }
                
                // Atualizar data da venda para o momento atual
                pedidoAtual.setDataPedido(LocalDateTime.now());
                
                // Configurar status como finalizado
                pedidoAtual.setStatus("FINALIZADO");
                
                // Salvar o pedido e seus itens em uma única transação
                PedidoDAO pedidoDAO = PedidoDAO.getInstance();
                ItemPedidoDAO itemPedidoDAO = ItemPedidoDAO.getInstance();
                
                // Verificar se o vendedor_id está definido antes de salvar
                if (pedidoAtual.getVendedorId() == null) {
                    // Se o vendedor_id ainda não estiver definido, usar o ID do usuário como fallback
                    if (pedidoAtual.getUsuario() != null && pedidoAtual.getUsuario().getId() != null) {
                        pedidoAtual.setVendedorId(pedidoAtual.getUsuario().getId().longValue());
                        LogUtil.info(getClass(), "Usando ID do usuário como vendedor_id: " + pedidoAtual.getVendedorId());
                    } else {
                        throw new SQLException("O ID do vendedor é obrigatório para criar um pedido");
                    }
                }
                
                // Salvar o pedido
                Pedido pedidoSalvo = pedidoDAO.create(pedidoAtual, conn);
                
                // Salvar os itens do pedido
                for (ItemPedido item : pedidoAtual.getItens()) {
                    item.setPedido(pedidoSalvo);
                    itemPedidoDAO.create(item, conn);
                }
                
                // Commit da transação
                conn.commit();
                
                // Operação concluída com sucesso
                sucesso = true;
                
                // Mostrar mensagem de sucesso
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Sucesso");
                alert.setHeaderText("Pedido Salvo");
                alert.setContentText("O pedido foi salvo com sucesso e o estoque foi atualizado.");
                alert.showAndWait();
                
                // Limpar o pedido atual e iniciar um novo
                inicializarPedido();
                
            } catch (SQLException e) {
                // Tentar reverter a transação
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        LogUtil.error(MainController.class, "Erro ao reverter transação", ex);
                    }
                }
                
                // Verificar se é o erro de timeout de bloqueio
                if (e.getMessage().contains("Lock wait timeout exceeded") && tentativas < MAX_RETRIES) {
                    // Erro de timeout - será tratado na próxima iteração
                    LogUtil.info(MainController.class, "Erro de timeout de bloqueio - tentativa " + tentativas + " de " + MAX_RETRIES);
                    
                    // Aguardar um pouco antes da próxima tentativa (tempo crescente entre tentativas)
                    try {
                        Thread.sleep(1000 * tentativas);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // Continuar para a próxima tentativa
                    continue;
                } else {
                    // Outro tipo de erro SQL - mostrar mensagem
                    AlertUtil.showError("Erro ao Salvar Pedido", 
                        "Ocorreu um erro ao salvar o pedido no banco de dados: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            } catch (Exception e) {
                // Erro inesperado
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erro ao Salvar");
                alert.setHeaderText("Erro ao salvar o pedido");
                alert.setContentText("Ocorreu um erro ao tentar salvar o pedido: " + e.getMessage());
                alert.showAndWait();
                e.printStackTrace();
                return;
            } finally {
                // Garantir que a conexão seja sempre devolvida ao pool
                if (conn != null) {
                    try {
                        // Restaurar autocommit para seu estado padrão
                        conn.setAutoCommit(true);
                        // Devolver a conexão ao pool
                        DatabaseConnection.getInstance().releaseConnection(conn);
                    } catch (SQLException ex) {
                        LogUtil.error(MainController.class, "Erro ao liberar conexão", ex);
                    }
                }
            }
        }
        
        // Se chegou aqui sem sucesso após todas as tentativas
        if (!sucesso && tentativas >= MAX_RETRIES) {
            AlertUtil.showError("Erro ao Salvar Pedido", 
                "Não foi possível salvar o pedido após " + MAX_RETRIES + " tentativas. " +
                "O sistema está ocupado ou há um problema de conexão com o banco de dados.");
        }
    }
   
    
    // Método para configurar atalhos de teclado - deve ser chamado no initialize()
    private void configurarAtalhosTeclado() {
        Platform.runLater(() -> {
            Scene scene = salvarButton.getScene();
            if (scene != null) {
                // Configurar o atalho F2 para salvar pedido
                scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.F2) {
                        salvarPedido();
                        event.consume(); // Impede que o evento seja processado por outros handlers
                    }
                });
            } else {
                System.err.println("Erro: Cena não inicializada. Atalhos de teclado não configurados.");
            }
        });
    }
    
    
    
    
    
    
    
   
    /**
     * Método para salvar o pedido
     *//*
    @FXML
    private void salvarPedido() {
        try {
            // Validar campos obrigatórios
            if (productField.getText().isEmpty() || unitValueField.getText().isEmpty() || quantityField.getText().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Campos Obrigatórios");
                alert.setHeaderText("Preencha todos os campos obrigatórios");
                alert.setContentText("Os campos Produto, Valor Unitário e Quantidade são obrigatórios.");
                alert.showAndWait();
                return;
            }

            // Obter os valores dos campos
            String produto = productField.getText();
            BigDecimal valorUnitario = new BigDecimal(unitValueField.getText().replace(",", "."));
            int quantidade = Integer.parseInt(quantityField.getText());
            BigDecimal valorTotal = valorUnitario.multiply(new BigDecimal(quantidade));

            // Exibir os valores no console (substitua por lógica de salvamento no banco de dados)
            System.out.println("Produto: " + produto);
            System.out.println("Valor Unitário: " + valorUnitario);
            System.out.println("Quantidade: " + quantidade);
            System.out.println("Valor Total: " + valorTotal);

            // Limpar os campos após salvar
            productField.clear();
            unitValueField.clear();
            quantityField.clear();
            totalValueField.clear();

            // Exibir mensagem de sucesso
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Sucesso");
            alert.setHeaderText("Pedido Salvo");
            alert.setContentText("O pedido foi salvo com sucesso.");
            alert.showAndWait();

        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro de Formato");
            alert.setHeaderText("Valor inválido");
            alert.setContentText("Certifique-se de que os valores numéricos estão corretos.");
            alert.showAndWait();
        }
    }
    
    */
    
    
    
    /**
     * Armazena a referência à cena principal para poder voltar a ela após abrir a tela de produtos
     * 
     * @param scene A cena principal
     */
    public void setMainScene(Scene scene) {
    	this.mainScene = scene;
        // Armazenar a referência do controlador na raiz da cena para acesso posterior
        scene.getRoot().setUserData(this);
    }
    
    /**
     * Abre a tela de produtos em uma nova janela
     */
    @FXML
    public void abrirTelaProdutos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/ProdutosView.fxml"));
            Parent root = loader.load();
            
            produtosController = loader.getController();
            
            // Se estamos na mesma janela principal
            Stage stage = (Stage) configButton.getScene().getWindow();
            Scene produtosScene = new Scene(root);
            
            // Configurar o controlador com referências para voltar
            produtosController.setStage(stage, mainScene);
            
            // Mudar para a cena de produtos
            stage.setScene(produtosScene);
            stage.setTitle("PDV - Cadastro de Produtos");
            
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro ao Carregar Interface");
            alert.setHeaderText("Não foi possível carregar a tela de produtos");
            alert.setContentText("Ocorreu um erro ao tentar carregar a interface: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    
    
 // Modifique o método abrirTelaRelatorios() na classe MainController

    @FXML
    public void abrirTelaRelatorios() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/ReportsView.fxml"));
            Parent root = loader.load();
            
            ReportsController controller = loader.getController();
            
            Stage stage = (Stage) salvarButton.getScene().getWindow();
            
            // Guardar dimensões e estado atual
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean maximized = stage.isMaximized();
            
            // Armazenar a cena atual para voltar
            if (mainScene == null) {
                mainScene = stage.getScene();
            }
            
            // Criar nova cena com as mesmas dimensões
            Scene relatoriosScene = new Scene(root, width, height);
            
            // Configurar a nova cena
            stage.setScene(relatoriosScene);
            stage.setTitle("PDV - Relatórios");
            stage.setMaximized(maximized);
            
        } catch (IOException e) {
            // Tratamento de erro existente
        }
    }
    
    @FXML
    public void abrirTelaClientes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/ClientesView.fxml"));
            Parent root = loader.load();
            
            ClienteController controller = loader.getController();
            
            // Obter o stage atual
            Stage stage = (Stage) salvarButton.getScene().getWindow();
            
            // Guardar dimensões e estado atual
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean maximized = stage.isMaximized();
            
            // Armazenar a cena atual como referência para retornar
            if (mainScene == null) {
                mainScene = stage.getScene();
            }
            
            // Configurar o controlador com referências para voltar
            if (controller != null) {
                controller.setStage(stage, mainScene);
            }
            
            // Criar nova cena com as mesmas dimensões
            Scene clienteScene = new Scene(root, width, height);
            
            // Mudar para a cena de clientes
            stage.setScene(clienteScene);
            stage.setTitle("PDV - Clientes");
            
            // Restaurar estado maximizado se necessário
            stage.setMaximized(maximized);
            
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro ao Carregar Interface");
            alert.setHeaderText("Não foi possível carregar a tela de Clientes");
            alert.setContentText("Ocorreu um erro ao tentar carregar a interface: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    public void abrirTelaDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/DashboardView.fxml"));
            Parent root = loader.load();
            
            DashboardController controller = loader.getController();
            if (controller != null) {
                controller.setMainScene(mainScene != null ? mainScene : salvarButton.getScene());
            }
            
            Stage stage = (Stage) salvarButton.getScene().getWindow();
            Scene dashboardScene = new Scene(root);
            
            // Armazenar a cena atual como referência para retornar
            if (mainScene == null) {
                mainScene = stage.getScene();
            }
            
            // Mudar para a cena do dashboard
            stage.setScene(dashboardScene);
            stage.setTitle("PDV - Dashboard");
            
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro ao Carregar Interface");
            alert.setHeaderText("Não foi possível carregar o Dashboard");
            alert.setContentText("Ocorreu um erro ao tentar carregar a interface: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    /**
     * Preenche os campos de produto na tela de PDV
     * 
     * @param produto O produto selecionado
     */
    public void selecionarProduto(Produto produto) {
        if (produto != null) {
            productField.setText(produto.getNome());
            
            // Usar o DecimalFormat para formatar o valor como moeda
            DecimalFormat df = new DecimalFormat("0.00");
            String valorFormatado = df.format(produto.getPreco());
            
            unitValueField.setText(valorFormatado);
            
            // Definir a quantidade como 1 por padrão
            quantityField.setText("1");
            
            // Calcular o valor total (preço x quantidade)
            calcularValorTotal();
        }
    }
    
    
    @FXML
    public void selecionarAbaProdutos() {
        // Abrir a tela de produtos aqui
        abrirTelaProdutos();
    }
    
    @FXML
    public void selecionarAbaCliente() {
    	abrirTelaClientes();
    }

    
    
    /**
     * Calcula o valor total baseado na quantidade e valor unitário
     */
    private void calcularValorTotal() {
        try {
            // Obter a quantidade, usar 1 como padrão se vazio
            int quantidade = 1;
            if (!quantityField.getText().isEmpty()) {
                quantidade = Integer.parseInt(quantityField.getText());
            }
            
            // Obter o valor unitário, formatado corretamente
            String valorUnitarioStr = unitValueField.getText().replace(",", ".");
            BigDecimal valorUnitario = BigDecimal.ZERO;
            if (!valorUnitarioStr.isEmpty()) {
                valorUnitario = new BigDecimal(valorUnitarioStr);
            }
            
            // Calcular o total
            BigDecimal total = valorUnitario.multiply(new BigDecimal(quantidade));
            
            // Formatar e exibir o total
            DecimalFormat df = new DecimalFormat("0.00");
            totalValueField.setText(df.format(total));
            
        } catch (NumberFormatException e) {
            totalValueField.setText("0,00");
        }
    }
    
    /**
     * Adiciona o item atual ao pedido
     */
    private void adicionarItemPedido() {
        // Implementar a adição de itens ao pedido aqui
        System.out.println("Adicionando item ao pedido: " + productField.getText());
        
        // Por enquanto, apenas limpa os campos
        productField.clear();
        unitValueField.setText("0,00");
        quantityField.setText("1");
        totalValueField.setText("0,00");
        
        // Focar no campo de produto
        productField.requestFocus();
    }
    
    /**
     * Abre a tela de configurações (não implementado)
     */
    private void abrirConfiguracoes() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Configurações");
        alert.setHeaderText("Tela de Configurações");
        alert.setContentText("Esta funcionalidade será implementada em breve.");
        alert.showAndWait();
    }
    
 // Código para substituir o botão verde por uma barra de busca e botão de menu
    private void configurarBarraSuperior(BorderPane root) {
        // Criar uma HBox para conter os elementos da barra superior
        HBox barraSuperior = new HBox();
        barraSuperior.setAlignment(Pos.CENTER_LEFT);
        barraSuperior.setPadding(new Insets(10));
        barraSuperior.setSpacing(10);
        barraSuperior.setStyle("-fx-background-color: #1b5583;"); 
        
        // Criar campo de busca
        TextField campoBusca = new TextField();
        campoBusca.setPromptText("Nome ou ID do Produto...");
        campoBusca.setPrefWidth(300);
        
        // Criar botão de busca com texto
        Button botaoBusca = new Button("🔍");
        botaoBusca.setStyle("-fx-background-color: #f0f0f0;");
        
        // Adicionar evento de busca ao botão
        botaoBusca.setOnAction(event -> realizarBusca(campoBusca.getText()));
        
        // Adicionar evento de busca ao pressionar Enter no campo
        campoBusca.setOnAction(event -> realizarBusca(campoBusca.getText()));
        
        // Criar botão de menu
        Button botaoMenu = new Button("☰");
        botaoMenu.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px;");
        
        // Adicionar um evento para o botão de menu
        botaoMenu.setOnAction(event -> abrirMenu());
        
        // Adicionar componentes à barra superior
        barraSuperior.getChildren().addAll(botaoMenu, campoBusca, botaoBusca);
        
        // Configurar para que o campo de busca se expanda
        HBox.setHgrow(campoBusca, Priority.ALWAYS);
        
        // Adicionar a barra superior ao topo do layout
        root.setTop(barraSuperior);
    }

    /**
     * Realiza a busca de produtos no banco de dados
     * @param termo Termo de busca inserido pelo usuário
     */
    private void realizarBusca(String termo) {
        try {
            // Obter o DAO
            ProdutoDAO produtoDAO = ProdutoDAO.getInstance();
            
            // Realizar a busca
            List<Produto> produtos;
            termo = termo.trim();
            
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
            
            // Exibir os resultados em uma nova janela
            mostrarResultadosBusca(produtos, termo);
            
        } catch (SQLException e) {
            AlertUtil.showError("Erro ao buscar produtos", e.getMessage());
        }
    }

    /**
     * Exibe os resultados da busca em uma nova janela
     * @param produtos Lista de produtos encontrados
     * @param termo Termo de busca usado
     */
    private void mostrarResultadosBusca(List<Produto> produtos, String termo) {
        // Criar uma tabela para exibir os resultados
        TableView<Produto> tabelaResultados = new TableView<>();
        
        // Configurar colunas
        TableColumn<Produto, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        TableColumn<Produto, String> nomeColumn = new TableColumn<>("Nome");
        nomeColumn.setCellValueFactory(new PropertyValueFactory<>("nome"));
        nomeColumn.setPrefWidth(200);
        
        TableColumn<Produto, String> codigoColumn = new TableColumn<>("Código");
        codigoColumn.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        
        TableColumn<Produto, String> precoColumn = new TableColumn<>("Preço");
        precoColumn.setCellValueFactory(cellData -> {
            BigDecimal preco = cellData.getValue().getPreco();
            return new SimpleStringProperty("R$ " + FormatUtil.formatarValor(preco));
        });
        
        TableColumn<Produto, String> estoqueColumn = new TableColumn<>("Estoque");
        estoqueColumn.setCellValueFactory(cellData -> {
            Integer estoque = cellData.getValue().getEstoqueAtual();
            return new SimpleStringProperty(estoque.toString());
        });
        
        // Adicionar colunas à tabela
        tabelaResultados.getColumns().addAll(idColumn, nomeColumn, codigoColumn, precoColumn, estoqueColumn);
        
        // Adicionar os produtos à tabela
        tabelaResultados.setItems(FXCollections.observableArrayList(produtos));
        
        // Configurar evento de duplo clique para selecionar um produto
        tabelaResultados.setRowFactory(tv -> {
            TableRow<Produto> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Produto selectedProduto = row.getItem();
                    Stage stage = (Stage) row.getScene().getWindow();
                    stage.close();
                    
                    // Agora preenchemos os campos da tela principal com o produto selecionado
                    selecionarProduto(selectedProduto);
                }
            });
            return row;
        });
        
        // Criar um label para mostrar o termo de busca e quantidade de resultados
        Label infoLabel = new Label("Resultados para \"" + termo + "\": " + produtos.size() + " produto(s) encontrado(s)");
        infoLabel.setPadding(new Insets(10));
        
        // Criar um layout para a janela
        VBox layout = new VBox(10);
        layout.getChildren().addAll(infoLabel, tabelaResultados);
        VBox.setVgrow(tabelaResultados, Priority.ALWAYS);
        
        // Criar e configurar a janela de resultados
        Stage resultStage = new Stage();
        resultStage.setTitle("Resultados da Busca");
        resultStage.setScene(new Scene(layout, 600, 400));
        resultStage.initOwner(salvarButton.getScene().getWindow());
        resultStage.initModality(Modality.APPLICATION_MODAL);
        resultStage.show();
    }
    
    
    /**
     * Método para abrir o menu quando o botão for clicado
     */
    
    private void abrirMenu() {
    	// Obter referência à janela principal
        Stage primaryStage = (Stage) salvarButton.getScene().getWindow();
        
        // Criar um menu lateral
        VBox menuLateral = new VBox();
        menuLateral.setPrefWidth(200);
        menuLateral.setStyle("-fx-background-color: white; -fx-border-color: #ccc;");

        // Adicionar os itens de menu
        Button dashboardItem = criarItemMenu("Dashboard", "");
        Button caixaItem = criarItemMenu("Caixa", "");
        Button pedidosItem = criarItemMenu("Pedidos", "");
        Button clientesItem = criarItemMenu("Clientes", "");
        Button produtosItem = criarItemMenu("Produtos", "");
        Button comprasItem = criarItemMenu("Compras (Entrada)", "");
        Button fluxoDeCaixaItem = criarItemMenu("Fluxo de Caixa", "");
        Button relatoriosItem = criarItemMenu("Relatórios", "");
        
        // Configurar ações para os botões
        produtosItem.setOnAction(event -> {
            // Fechar o menu antes de abrir a tela de produtos
            ((Stage) produtosItem.getScene().getWindow()).close();
            
            // Chamar diretamente o método para abrir a tela de produtos
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/ProdutosView.fxml"));
                Parent root = loader.load();
                
                ProdutosController controller = loader.getController();
                
                // Configurar o controlador com referências para voltar
                controller.setStage(primaryStage, primaryStage.getScene());
                
                // Mudar para a cena de produtos
                primaryStage.setScene(new Scene(root));
                primaryStage.setTitle("PDV - Cadastro de Produtos");
                
            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erro ao Carregar Interface");
                alert.setHeaderText("Não foi possível carregar a tela de produtos");
                alert.setContentText("Ocorreu um erro ao tentar carregar a interface: " + e.getMessage());
                alert.showAndWait();
            }
        });
        
        dashboardItem.setOnAction(event -> {
            // Fechar o menu antes de abrir a tela de relatórios
            ((Stage) dashboardItem.getScene().getWindow()).close();
            
            
            
         // Chamar diretamente o método para abrir a tela de produtos
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/DashboardView.fxml"));
                Parent root = loader.load();
                
                DashboardController controller = loader.getController();
                
                // Obter dimensões atuais e estado maximizado
                double width = primaryStage.getWidth();
                double height = primaryStage.getHeight();
                boolean maximized = primaryStage.isMaximized();
                
                if (controller != null) {
                    // Verificar se o método existe antes de chamar
                    try {
                        controller.setMainScene(primaryStage.getScene());
                    } catch (Exception e) {
                        System.err.println("Método setMainScene não disponível: " + e.getMessage());
                    }
                }
                // Mudar para a cena de produtos
                Scene novaScene = new Scene(root, width, height);
                primaryStage.setScene(novaScene);
                primaryStage.setTitle("PDV - Dashboard");
                primaryStage.setMaximized(maximized);
                
            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erro ao Carregar Interface");
                alert.setHeaderText("Não foi possível carregar a tela de produtos");
                alert.setContentText("Ocorreu um erro ao tentar carregar a interface: " + e.getMessage());
                alert.showAndWait();
            }
        });
        
        // Ao clicar nos itens do menu e abrir novas telas, use padrão similar:
        relatoriosItem.setOnAction(event -> {
            // Fechar o menu
            ((Stage) relatoriosItem.getScene().getWindow()).close();
            
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/ReportsView.fxml"));
                Parent root = loader.load();
                
                // Obter dimensões atuais e estado maximizado
                double width = primaryStage.getWidth();
                double height = primaryStage.getHeight();
                boolean maximized = primaryStage.isMaximized();
                
                Scene novaScene = new Scene(root, width, height);
                primaryStage.setScene(novaScene);
                primaryStage.setTitle("PDV - Relatórios");
                primaryStage.setMaximized(maximized);
                
            } catch (IOException e) {
                // Tratamento de erro existente
            }
        });
        
        clientesItem.setOnAction(event -> {
            // Fechar o menu
            ((Stage) clientesItem.getScene().getWindow()).close();
            
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/ClientesView.fxml"));
                Parent root = loader.load();
                
                // Obter dimensões atuais e estado maximizado
                double width = primaryStage.getWidth();
                double height = primaryStage.getHeight();
                boolean maximized = primaryStage.isMaximized();
                
                Scene novaScene = new Scene(root, width, height);
                primaryStage.setScene(novaScene);
                primaryStage.setTitle("PDV - Clientes");
                primaryStage.setMaximized(maximized);
                
            } catch (IOException e) {
                // Tratamento de erro existente
            }
        });

        menuLateral.getChildren().addAll(dashboardItem, caixaItem, pedidosItem, clientesItem, produtosItem, 
                                       comprasItem, fluxoDeCaixaItem, relatoriosItem);

        // Criar um ScrollPane e adicionar o VBox nele
        ScrollPane scrollPane = new ScrollPane(menuLateral);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent;");

        // Mostrar o menu como um popup
        Stage menuStage = new Stage();
        menuStage.setTitle("Menu");
        menuStage.setScene(new Scene(scrollPane));
        menuStage.initOwner(primaryStage);
        menuStage.show();
    }

    /**
     * Método para abrir a tela de produtos utilizando uma referência a Stage
     */
    private void abrirTelaProdutosComStage(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/ProdutosView.fxml"));
            Parent root = loader.load();
            
            produtosController = loader.getController();
            
            Scene produtosScene = new Scene(root);
            
            // Configurar o controlador com referências para voltar
            produtosController.setStage(stage, mainScene);
            
            // Mudar para a cena de produtos
            stage.setScene(produtosScene);
            stage.setTitle("PDV - Cadastro de Produtos");
            
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro ao Carregar Interface");
            alert.setHeaderText("Não foi possível carregar a tela de produtos");
            alert.setContentText("Ocorreu um erro ao tentar carregar a interface: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    /*
    private boolean salvarPedidoComRetry(int maxRetries) {
        int tentativas = 0;
        while (tentativas < maxRetries) {
            try {
                // Lógica para salvar o pedido
                return true; // Sucesso
            } catch (SQLException e) {
                if (e.getMessage().contains("Lock wait timeout exceeded")) {
                    tentativas++;
                    // Aguardar um pouco antes de tentar novamente
                    try {
                        Thread.sleep(1000 * tentativas); // Espera progressiva
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    // Outro tipo de erro SQL
                    throw e;
                }
            }
        }
        return false; // Falha após várias tentativas
    }
    */
    private void trocarTela(Parent root, String titulo) {
        Stage stage = (Stage) salvarButton.getScene().getWindow();
        
        // Obter tamanho e estado da janela atual antes de mudar
        double width = stage.getWidth();
        double height = stage.getHeight();
        boolean maximized = stage.isMaximized();
        
        // Criar nova cena com mesmo tamanho da anterior
        Scene novaCena = new Scene(root, width, height);
        
        // Definir a nova cena
        stage.setScene(novaCena);
        stage.setTitle(titulo);
        
        // Restaurar estado maximizado se necessário
        stage.setMaximized(maximized);
    }
    
    /**
     * Método auxiliar para criar um item de menu com ícone
     */
    private Button criarItemMenu(String texto, String caminhoIcone) {
        Button item = new Button(texto);
        item.setPrefWidth(200);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle("-fx-background-color: transparent; -fx-padding: 10 15;");
        
        // Adicionar efeito hover
        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 10 15;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-background-color: transparent; -fx-padding: 10 15;"));
        
        return item;
    }
    
    /**
     * Verifica se um produto tem estoque suficiente para ser incluído no pedido
     * 
     * @param produto O produto a ser verificado
     * @param quantidade A quantidade desejada
     * @return true se há estoque suficiente, false caso contrário
     */
    private boolean verificarEstoqueDisponivel(Produto produto, int quantidade) {
        if (produto == null) {
            return false;
        }
        
        // Verificar se o produto está ativo
        if (!produto.isAtivo()) {
            AlertUtil.showWarning("Produto Inativo", 
                "O produto '" + produto.getNome() + "' está inativo e não pode ser vendido.");
            return false;
        }
        
        // Verificar se há estoque suficiente
        if (produto.getEstoqueAtual() < quantidade) {
            AlertUtil.showWarning("Estoque Insuficiente", 
                "Estoque insuficiente para o produto '" + produto.getNome() + "'.\n" +
                "Estoque atual: " + produto.getEstoqueAtual() + "\n" +
                "Quantidade solicitada: " + quantidade);
            return false;
        }
        
        return true;
    }

    
    /**
     * Dá baixa no estoque dos produtos incluídos no pedido atual
     * 
     * @param conn Conexão com o banco de dados a ser usada
     * @return true se a baixa foi realizada com sucesso, false caso contrário
     */
    private boolean darBaixaEstoque(Connection conn) {
        try {
            ProdutoDAO produtoDAO = ProdutoDAO.getInstance();
            
            // Verificar novamente o estoque antes de confirmar a baixa
            for (ItemPedido item : itensPedido) {
                Produto produtoAtual = produtoDAO.findById(item.getProduto().getId(), conn);
                
                if (produtoAtual == null) {
                    AlertUtil.showError("Erro ao Dar Baixa", 
                        "Produto ID " + item.getProduto().getId() + " não encontrado no banco de dados.");
                    return false;
                }
                
                if (produtoAtual.getEstoqueAtual() < item.getQuantidade()) {
                    AlertUtil.showWarning("Estoque Insuficiente", 
                        "Estoque insuficiente para o produto '" + produtoAtual.getNome() + "'.\n" +
                        "Estoque atual: " + produtoAtual.getEstoqueAtual() + "\n" +
                        "Quantidade no pedido: " + item.getQuantidade() + "\n\n" +
                        "O estoque pode ter sido alterado por outro usuário.");
                    return false;
                }
                
                // Atualizar o estoque do produto
                produtoAtual.setEstoqueAtual(produtoAtual.getEstoqueAtual() - item.getQuantidade());
                produtoDAO.update(produtoAtual, conn);
                
                LogUtil.info(MainController.class, "Baixa de estoque realizada: Produto = " + produtoAtual.getNome() + 
                              ", Quantidade = " + item.getQuantidade() + 
                              ", Novo estoque = " + produtoAtual.getEstoqueAtual());
            }
            
            return true;
        } catch (SQLException e) {
            AlertUtil.showError("Erro ao Dar Baixa no Estoque", 
                "Ocorreu um erro ao atualizar o estoque: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            AlertUtil.showError("Erro Inesperado", 
                "Ocorreu um erro inesperado ao dar baixa no estoque: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    
    
    /**
     * Inclui um cliente ao pedido atual com base no nome
     * 
     * @param nomeCliente Nome do cliente a ser associado ao pedido
     * @return true se o cliente foi associado com sucesso, false caso contrário
     */
    private boolean associarClienteAoPedido(String nomeCliente) {
        if (nomeCliente == null || nomeCliente.trim().isEmpty()) {
            AlertUtil.showWarning("Cliente Obrigatório", 
                "É necessário informar um cliente para o pedido.");
            return false;
        }
        
        try {
            ClienteDAO clienteDAO = ClienteDAO.getInstance();
            
            // Buscar o cliente pelo nome
            Cliente cliente = clienteDAO.findByNome(nomeCliente);
            
            // Se encontrou o cliente
            if (cliente != null) {
                pedidoAtual.setCliente(cliente);
                return true;
            }
            
            // Se não encontrou, perguntar se deseja criar um novo cliente
            boolean criarCliente = AlertUtil.showConfirmation(
                "Cliente não encontrado", 
                "O cliente '" + nomeCliente + "' não foi encontrado. Deseja cadastrar um novo cliente?");
            
            if (criarCliente) {
                // Criar um novo cliente
                Cliente novoCliente = new Cliente();
                novoCliente.setNome(nomeCliente);
                novoCliente.setAtivo(true);
                novoCliente.setData(LocalDateTime.now());
                
                try {
                    // Salvar o novo cliente no banco de dados
                    Cliente clienteCriado = clienteDAO.create(novoCliente);
                    
                    // Associar ao pedido
                    pedidoAtual.setCliente(clienteCriado);
                    return true;
                } catch (CpfCnpjDuplicadoException e) {
                    // Tratamento específico para CPF/CNPJ duplicado
                    AlertUtil.showWarning("CPF/CNPJ Duplicado", 
                        "Não foi possível criar o cliente pois já existe um cliente com este CPF/CNPJ.");
                    return false;
                }
            } else {
                return false;
            }
            
        } catch (SQLException e) {
            AlertUtil.showError("Erro ao Buscar Cliente", 
                "Ocorreu um erro ao buscar o cliente no banco de dados: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica se o pedido está pronto para ser salvo
     * 
     * @return true se o pedido está válido para salvamento, false caso contrário
     */
    private boolean validarPedido() {
        // Verificar se há itens no pedido
        if (itensPedido.isEmpty()) {
            AlertUtil.showWarning("Pedido Vazio", 
                "Não é possível salvar um pedido vazio. Adicione pelo menos um item ao pedido.");
            return false;
        }
        
        // Verificar se tem cliente associado
        if (pedidoAtual.getCliente() == null) {
            String nomeCliente = clientField.getText().trim();
            if (!associarClienteAoPedido(nomeCliente)) {
                return false;
            }
        }
        
        // Verificar se tem vendedor selecionado
        if (sellerComboBox.getSelectionModel().isEmpty()) {
            AlertUtil.showWarning("Vendedor Obrigatório", 
                "É necessário selecionar um vendedor para o pedido.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Cancela o pedido atual e reinicia um novo
     */
    @FXML
    private void cancelarPedido() {
        // Verificar se há itens no pedido
        if (itensPedido.isEmpty()) {
            // Se não há itens, apenas reinicializar sem perguntar
            inicializarPedido();
            return;
        }
        
        // Perguntar ao usuário se realmente deseja cancelar
        boolean confirmado = AlertUtil.showConfirmation(
            "Cancelar Pedido", 
            "Tem certeza que deseja cancelar este pedido? Todos os itens serão removidos.");
        
        if (confirmado) {
            // Se o usuário confirmou, reinicializar o pedido
            inicializarPedido();
            
            // Limpar os campos para garantir
            productField.clear();
            unitValueField.setText("0,00");
            quantityField.setText("1");
            totalValueField.setText("0,00");
            clientField.clear();
            
            // Mostrar mensagem de sucesso
            AlertUtil.showInfo("Pedido Cancelado", 
                "O pedido foi cancelado com sucesso. Um novo pedido foi iniciado.");
            
            // Focar no campo de cliente para iniciar um novo pedido
            Platform.runLater(() -> clientField.requestFocus());
        }
    }
}