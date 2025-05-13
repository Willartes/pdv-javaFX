package br.com.pdv.controller;

import br.com.pdv.dao.*;
import br.com.pdv.model.*;
import br.com.pdv.util.AlertUtil;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador para o Dashboard principal do PDV
 * Exibe métricas importantes e gráficos para análise de vendas e estoque
 */
public class DashboardController implements Initializable {

    // Componentes para os cards de informações
    @FXML
    private Label totalVendasLabel;
    
    @FXML
    private Label totalProdutosLabel;
    
    @FXML
    private Label totalClientesLabel;
    
    @FXML
    private Label valorTotalLabel;
    
    @FXML
    private Label totalEstoqueLabel;
    
    @FXML
    private Label estoqueEmAlertaLabel;
    
    // Gráficos
    @FXML
    private BarChart<String, Number> vendasMensaisChart;
    
    @FXML
    private LineChart<String, Number> valorVendasMensaisChart;
    
    // Tabela de produtos mais vendidos
    @FXML
    private TableView<ProdutoVendas> topProdutosTable;
    
    @FXML
    private TableColumn<ProdutoVendas, String> produtoNomeColumn;
    
    @FXML
    private TableColumn<ProdutoVendas, Integer> quantidadeVendidaColumn;
    
    @FXML
    private TableColumn<ProdutoVendas, String> valorTotalColumn;
    
    // Tabela de produtos com estoque baixo
    @FXML
    private TableView<Produto> estoqueBaixoTable;
    
    @FXML
    private TableColumn<Produto, String> produtoEstoqueColumn;
    
    @FXML
    private TableColumn<Produto, Integer> estoqueAtualColumn;
    
    @FXML
    private TableColumn<Produto, Integer> estoqueMinColumn;
    
    @FXML
    private Button menuButton;
    
    @FXML
    private Button refreshButton;
    
    // DAOs para acesso a dados
    private ProdutoDAO produtoDAO;
    private VendaDAO vendaDAO;
    private PedidoDAO pedidoDAO;
    private ClienteDAO clienteDAO;
    private ItemPedidoDAO itemPedidoDAO;
    
    // Lista para armazenar os produtos mais vendidos
    private ObservableList<ProdutoVendas> topProdutosList = FXCollections.observableArrayList();
    
    // Lista para armazenar os produtos com estoque baixo
    private ObservableList<Produto> estoqueBaixoList = FXCollections.observableArrayList();
    
    // Referência à cena principal
    private Scene mainScene;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Inicializar DAOs
        inicializarDAOs();
        
        // Configurar colunas da tabela de produtos mais vendidos
        configurarTabelaTopProdutos();
        
        // Configurar colunas da tabela de produtos com estoque baixo
        configurarTabelaEstoqueBaixo();
        
        // Configurar eventos dos botões
        configurarEventosBotoes();
        
        // Carregar dados para exibição
        carregarDados();
    }

    /**
     * Inicializa os DAOs necessários para o dashboard
     */
    private void inicializarDAOs() {
        produtoDAO = ProdutoDAO.getInstance();
        vendaDAO = VendaDAO.getInstance();
        pedidoDAO = PedidoDAO.getInstance();
        clienteDAO = ClienteDAO.getInstance();
        itemPedidoDAO = ItemPedidoDAO.getInstance();
    }

    /**
     * Configura as colunas da tabela de produtos mais vendidos
     */
    private void configurarTabelaTopProdutos() {
        produtoNomeColumn.setCellValueFactory(new PropertyValueFactory<>("nomeProduto"));
        quantidadeVendidaColumn.setCellValueFactory(new PropertyValueFactory<>("quantidadeVendida"));
        valorTotalColumn.setCellValueFactory(cellData -> 
            cellData.getValue().valorTotalFormatadoProperty());
        
        topProdutosTable.setItems(topProdutosList);
    }

    /**
     * Configura as colunas da tabela de produtos com estoque baixo
     */
    private void configurarTabelaEstoqueBaixo() {
        produtoEstoqueColumn.setCellValueFactory(new PropertyValueFactory<>("nome"));
        estoqueAtualColumn.setCellValueFactory(new PropertyValueFactory<>("estoqueAtual"));
        estoqueMinColumn.setCellValueFactory(new PropertyValueFactory<>("estoqueMinimo"));
        
        estoqueBaixoTable.setItems(estoqueBaixoList);
        
        // Colorir as linhas baseado na criticidade do estoque
        estoqueBaixoTable.setRowFactory(tv -> new TableRow<Produto>() {
            @Override
            protected void updateItem(Produto produto, boolean empty) {
                super.updateItem(produto, empty);
                
                if (produto == null || empty) {
                    setStyle("");
                } else {
                    // Estoque zerado - vermelho
                    if (produto.getEstoqueAtual() <= 0) {
                        setStyle("-fx-background-color: #ffcccc;");
                    }
                    // Estoque crítico - laranja
                    else if (produto.getEstoqueAtual() <= produto.getEstoqueMinimo() * 0.5) {
                        setStyle("-fx-background-color: #ffddcc;");
                    }
                    // Estoque baixo - amarelo
                    else if (produto.getEstoqueAtual() <= produto.getEstoqueMinimo()) {
                        setStyle("-fx-background-color: #ffffcc;");
                    }
                }
            }
        });
    }

    /**
     * Configura os eventos dos botões
     */
    private void configurarEventosBotoes() {
        refreshButton.setOnAction(event -> carregarDados());
        menuButton.setOnAction(event -> abrirMenu());
    }

    /**
     * Abre o menu principal
     */
    private void abrirMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/MainView.fxml"));
            Parent root = loader.load();
            
            MainController controller = loader.getController();
            
            Stage stage = (Stage) menuButton.getScene().getWindow();
            Scene scene = new Scene(root);
            
            if (controller != null) {
                controller.setMainScene(scene);
            }
            
            stage.setScene(scene);
            stage.setTitle("PDV - Sistema de Ponto de Venda");
            
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Erro ao carregar menu", 
                "Ocorreu um erro ao tentar carregar o menu principal: " + e.getMessage());
        }
    }

    /**
     * Carrega todos os dados para exibição no dashboard
     */
    private void carregarDados() {
        try {
            // Atualizar os cards de informações
            atualizarCards();
            
            // Carregar dados dos gráficos
            carregarGraficos();
            
            // Carregar produtos mais vendidos
            carregarTopProdutos();
            
            // Carregar produtos com estoque baixo
            carregarProdutosEstoqueBaixo();
            
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Erro ao carregar dados", 
                "Ocorreu um erro ao carregar os dados do dashboard: " + e.getMessage());
        }
    }

    /**
     * Atualiza os cards com informações de totais
     */
    private void atualizarCards() throws SQLException {
        // Total de vendas
        int totalVendas = vendaDAO.contarVendasFinalizadas();
        totalVendasLabel.setText(String.valueOf(totalVendas));
        
        // Total de produtos
        int totalProdutos = produtoDAO.contarProdutosAtivos();
        totalProdutosLabel.setText(String.valueOf(totalProdutos));
        
        // Total de clientes
        int totalClientes = clienteDAO.contarClientesAtivos();
        totalClientesLabel.setText(String.valueOf(totalClientes));
        
        // Valor total de vendas
        BigDecimal valorTotal = vendaDAO.calcularValorTotalVendas();
        valorTotalLabel.setText("R$ " + valorTotal.toString().replace(".", ","));
        
        // Total de itens em estoque
        int totalEstoque = produtoDAO.calcularTotalItensEstoque();
        totalEstoqueLabel.setText(String.valueOf(totalEstoque));
        
        // Produtos em alerta de estoque
        int produtosAlerta = produtoDAO.contarProdutosComEstoqueBaixo();
        estoqueEmAlertaLabel.setText(String.valueOf(produtosAlerta));
    }

    /**
     * Carrega os dados para os gráficos de vendas
     */
    private void carregarGraficos() throws SQLException {
        // Dados de vendas por mês
        Map<Month, Integer> vendasPorMes = obterVendasPorMes();
        Map<Month, BigDecimal> valorPorMes = obterValorVendasPorMes();
        
        // Configurar gráfico de vendas mensais
        XYChart.Series<String, Number> seriesVendas = new XYChart.Series<>();
        seriesVendas.setName("Vendas");
        
        // Configurar gráfico de valor mensal
        XYChart.Series<String, Number> seriesValor = new XYChart.Series<>();
        seriesValor.setName("Valor");
        
        // Preencher dados dos gráficos
        for (Month mes : Month.values()) {
            String nomeMes = mes.getDisplayName(TextStyle.SHORT, new Locale("pt", "BR"));
            
            // Adicionar dados de quantidade de vendas
            int qtdVendas = vendasPorMes.getOrDefault(mes, 0);
            seriesVendas.getData().add(new XYChart.Data<>(nomeMes, qtdVendas));
            
            // Adicionar dados de valor de vendas
            BigDecimal valor = valorPorMes.getOrDefault(mes, BigDecimal.ZERO);
            seriesValor.getData().add(new XYChart.Data<>(nomeMes, valor.doubleValue()));
        }
        
        // Limpar dados antigos
        vendasMensaisChart.getData().clear();
        valorVendasMensaisChart.getData().clear();
        
        // Adicionar novas séries
        vendasMensaisChart.getData().add(seriesVendas);
        valorVendasMensaisChart.getData().add(seriesValor);
        
        // Configurar cores
        setBarColors(vendasMensaisChart, Color.LIGHTSEAGREEN);
        setLineColors(valorVendasMensaisChart, Color.DODGERBLUE);
    }
    
    /**
     * Configura as cores das barras do gráfico
     */
    private void setBarColors(BarChart<String, Number> chart, Color color) {
        for (XYChart.Series<String, Number> series : chart.getData()) {
            for (XYChart.Data<String, Number> data : series.getData()) {
                data.getNode().setStyle("-fx-bar-fill: " + toRGBCode(color) + ";");
            }
        }
    }
    
    /**
     * Configura as cores das linhas do gráfico
     */
    private void setLineColors(LineChart<String, Number> chart, Color color) {
        String rgb = toRGBCode(color);
        for (XYChart.Series<String, Number> series : chart.getData()) {
            series.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: " + rgb + ";");
            
            for (XYChart.Data<String, Number> data : series.getData()) {
                data.getNode().lookup(".chart-line-symbol").setStyle(
                        "-fx-background-color: " + rgb + ", white;"
                );
            }
        }
    }
    
    /**
     * Converte uma cor para código RGB em hexadecimal
     */
    private String toRGBCode(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    /**
     * Obtém o número de vendas agrupadas por mês
     */
    private Map<Month, Integer> obterVendasPorMes() throws SQLException {
        Map<Month, Integer> vendasPorMes = new HashMap<>();
        
        // Obter data atual e calcular o início do ano
        LocalDate hoje = LocalDate.now();
        LocalDate inicioAno = LocalDate.of(hoje.getYear(), 1, 1);
        
        // Buscar vendas do ano atual
        List<Venda> vendas = vendaDAO.findByPeriod(inicioAno.atStartOfDay(), hoje.atTime(23, 59, 59));
        
        // Agrupar por mês
        for (Venda venda : vendas) {
            Month mes = venda.getDataVenda().getMonth();
            vendasPorMes.put(mes, vendasPorMes.getOrDefault(mes, 0) + 1);
        }
        
        return vendasPorMes;
    }

    /**
     * Obtém o valor total de vendas agrupadas por mês
     */
    private Map<Month, BigDecimal> obterValorVendasPorMes() throws SQLException {
        Map<Month, BigDecimal> valorPorMes = new HashMap<>();
        
        // Obter data atual e calcular o início do ano
        LocalDate hoje = LocalDate.now();
        LocalDate inicioAno = LocalDate.of(hoje.getYear(), 1, 1);
        
        // Buscar vendas do ano atual
        List<Venda> vendas = vendaDAO.findByPeriod(inicioAno.atStartOfDay(), hoje.atTime(23, 59, 59));
        
        // Agrupar por mês
        for (Venda venda : vendas) {
            Month mes = venda.getDataVenda().getMonth();
            BigDecimal valorAtual = valorPorMes.getOrDefault(mes, BigDecimal.ZERO);
            valorPorMes.put(mes, valorAtual.add(venda.getValorTotal()));
        }
        
        return valorPorMes;
    }

    /**
     * Carrega os dados dos top 5 produtos mais vendidos
     */
    private void carregarTopProdutos() throws SQLException {
        // Obter os produtos mais vendidos (top 5)
        List<ProdutoVendas> topProdutos = itemPedidoDAO.obterProdutosMaisVendidos(10);
        
        // Adicionar log para ver o tamanho da lista retornada
        System.out.println("Produtos mais vendidos encontrados: " + topProdutos.size());
        
        // Se a lista estiver vazia, registrar isso também
        if (topProdutos.isEmpty()) {
            System.out.println("Nenhum produto vendido encontrado no banco de dados");
        }
        
        // Atualizar lista
        topProdutosList.clear();
        topProdutosList.addAll(topProdutos);
    }

    /**
     * Carrega os produtos com estoque baixo
     */
    private void carregarProdutosEstoqueBaixo() throws SQLException {
        // Obter produtos com estoque abaixo do mínimo
        List<Produto> produtosEstoqueBaixo = produtoDAO.findWithLowStock();
        
        // Atualizar lista
        estoqueBaixoList.clear();
        estoqueBaixoList.addAll(produtosEstoqueBaixo);
    }

    /**
     * Armazena a referência à cena principal
     */
    public void setMainScene(Scene scene) {
        this.mainScene = scene;
    }
    
    /**
     * Classe auxiliar para exibir os produtos mais vendidos
     */
    public static class ProdutoVendas {
    	private final Integer idProduto;
        private final String nomeProduto;
        private final Integer quantidadeVendida;
        private final BigDecimal valorTotal;
        
        public ProdutoVendas(Integer idProduto, String nomeProduto, Integer quantidadeVendida, BigDecimal valorTotal) {
            this.idProduto = idProduto;
            this.nomeProduto = nomeProduto != null ? nomeProduto : "";
            this.quantidadeVendida = quantidadeVendida != null ? quantidadeVendida : 0;
            this.valorTotal = valorTotal != null ? valorTotal : BigDecimal.ZERO;
        }
        
        public Integer getIdProduto() {
            return idProduto;
        }
        
        public String getNomeProduto() {
            return nomeProduto;
        }
        
        public Integer getQuantidadeVendida() {
            return quantidadeVendida;
        }
        
        public BigDecimal getValorTotal() {
            return valorTotal;
        }
        
        
        public javafx.beans.property.StringProperty valorTotalFormatadoProperty() {
            if (this.valorTotal == null) {
                return new javafx.beans.property.SimpleStringProperty("R$ 0,00");
            }
            return new javafx.beans.property.SimpleStringProperty("R$ " + valorTotal.toString().replace(".", ","));
        }
    }
}