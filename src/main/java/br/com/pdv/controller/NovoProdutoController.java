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
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class NovoProdutoController implements Initializable {

	// Campos do formulário
	@FXML private TextField nomeField;
	@FXML private TextArea descricaoArea;
	@FXML private ComboBox<String> tipoComboBox;
	@FXML private ComboBox<Marca> marcaComboBox;
	@FXML private ComboBox<Subcategoria> subcategoriaComboBox;
	@FXML private TextField unidadeField;
	@FXML private ComboBox<Categoria> categoriaComboBox;
	@FXML private TextField codigoBarrasField;
	@FXML private TextField codigoProdutoField;
	@FXML private ComboBox<String> corComboBox;
	@FXML private ComboBox<String> tamanhoComboBox;
	@FXML private TextField custoField;
	@FXML private TextField markupField;
	@FXML private TextField valorField;
	@FXML private TextField cfopField;
	@FXML private TextField icmsField;
	@FXML private TextField icmsSubField;
	@FXML private TextField estoqueField;
	@FXML private TextField estoqueMinField;
	@FXML private DatePicker vencimentoPicker;

	// Botões
	@FXML private Button novaMarcaButton;
	@FXML private Button novaSubcategoriaButton;
	@FXML private Button novaCategoriaButton;
	@FXML private Button novaCorButton;
	@FXML private Button novoTamanhoButton;
	@FXML private Button cancelarButton;
	@FXML private Button salvarButton;
	@FXML private Button novoMarkupButton;
	
	// DAOs
	private ProdutoDAO produtoDAO;
	private CategoriaDAO categoriaDAO;
	private MarcaDAO marcaDAO;
	private SubcategoriaDAO subcategoriaDAO;

	// Produto sendo editado
	private Produto produtoAtual;
	private boolean modoEdicao = false;

	// Flags para controle de edição e cálculos
	private boolean isEditingProduct = false;
	private boolean blockCalculation = false;

	// Listas observáveis para os comboboxes
	private ObservableList<Categoria> categorias;
	private ObservableList<Marca> marcas;
	private ObservableList<Subcategoria> subcategorias;
	private ObservableList<String> cores;
	private ObservableList<String> tamanhos;
	private ObservableList<String> tipos;

	// Ouvintes para cálculo de valores (não utilizados individualmente, pois os listeners são definidos inline)
	private ChangeListener<String> custoChangeListener;
	private ChangeListener<String> markupChangeListener;
	private ChangeListener<String> valorChangeListener;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
	    // Inicializar DAOs
	    produtoDAO = ProdutoDAO.getInstance();
	    categoriaDAO = CategoriaDAO.getInstance();
	    marcaDAO = MarcaDAO.getInstance();
	    subcategoriaDAO = SubcategoriaDAO.getInstance();

	    // Inicializar listas
	    categorias = FXCollections.observableArrayList();
	    marcas = FXCollections.observableArrayList();
	    subcategorias = FXCollections.observableArrayList();
	    cores = FXCollections.observableArrayList();
	    tamanhos = FXCollections.observableArrayList();
	    tipos = FXCollections.observableArrayList();

	    // Carregar dados para os comboboxes
	    carregarCategorias();
	    carregarMarcas();
	    carregarSubcategorias();
	    carregarCores();
	    carregarTamanhos();
	    carregarTipos();
	    
	    markupField.textProperty().addListener((observable, oldValue, newValue) -> {
	        if (!blockCalculation && !newValue.isEmpty()) {
	            try {
	                BigDecimal markup = new BigDecimal(newValue.replace(",", "."));
	                validarValorNaoNegativo(markup, "O markup não pode ser negativo.");
	                // Atualizar o objeto produtoAtual
	                produtoAtual.setMarkup(markup);
	                // Recalcular o valor final se necessário
	                calcularValorFinal();
	            } catch (IllegalArgumentException e) {
	                // Mostrar alerta e reverter o valor
	                AlertUtil.showWarning("Valor Inválido", e.getMessage());
	                markupField.setText(oldValue);
	            }
	        }
	    });
	    /**
	    markupField.textProperty().addListener((obs, oldVal, newVal) -> {
	        System.out.println("Markup alterado: " + oldVal + " -> " + newVal);
	    });
		*/
	    
	    custoField.textProperty().addListener((observable, oldValue, newValue) -> {
	        if (!blockCalculation && !newValue.isEmpty() && !valorField.getText().isEmpty()) {
	            calcularMarkup();
	        }
	    });
	    
	    valorField.textProperty().addListener((observable, oldValue, newValue) -> {
	        if (!blockCalculation && !newValue.isEmpty() && !custoField.getText().isEmpty()) {
	            calcularMarkup();
	        }
	    });

	    // Configurar formatação de campos numéricos
	    configureDecimalFields();

	    // Configurar eventos para cálculo de valores
	    setupCalculoPreco();

	    // Configurar eventos dos botões
	    configurarBotoes();

	    // Configurar o DatePicker de vencimento
	    configureVencimentoPicker();

	    // Inicializar a tela para novo cadastro
	    novoProdutoView();
	}

	/**
	 * Carrega as categorias do banco de dados e as popula no ComboBox.
	 */
	public void carregarCategorias() {
	    try {
	        List<Categoria> lista = categoriaDAO.findAll();
	        categorias.clear();
	        categorias.addAll(lista);
	        categoriaComboBox.setItems(categorias);
	        categoriaComboBox.setConverter(new StringConverter<Categoria>() {
	            @Override
	            public String toString(Categoria categoria) {
	                return (categoria != null) ? categoria.getNome() : null;
	            }
	            @Override
	            public Categoria fromString(String string) {
	                return null;
	            }
	        });
	    } catch (SQLException e) {
	        AlertUtil.showError("Erro ao carregar categorias", e.getMessage());
	    }
	}

	/**
	 * Carrega as marcas do banco de dados e as popula no ComboBox.
	 */
	public void carregarMarcas() {
	    try {
	        List<Marca> lista = marcaDAO.findAll();
	        marcas.clear();
	        marcas.addAll(lista);
	        marcaComboBox.setItems(marcas);
	        marcaComboBox.setConverter(new StringConverter<Marca>() {
	            @Override
	            public String toString(Marca marca) {
	                return (marca != null) ? marca.getNome() : null;
	            }
	            @Override
	            public Marca fromString(String string) {
	                return null;
	            }
	        });
	    } catch (SQLException e) {
	        AlertUtil.showError("Erro ao carregar marcas", e.getMessage());
	    }
	}

	/**
	 * Carrega as subcategorias do banco de dados e as popula no ComboBox.
	 */
	public void carregarSubcategorias() {
	    try {
	        List<Subcategoria> lista = subcategoriaDAO.findAll();
	        subcategorias.clear();
	        subcategorias.addAll(lista);
	        subcategoriaComboBox.setItems(subcategorias);
	        subcategoriaComboBox.setConverter(new StringConverter<Subcategoria>() {
	            @Override
	            public String toString(Subcategoria subcategoria) {
	                return (subcategoria != null) ? subcategoria.getNome() : null;
	            }
	            @Override
	            public Subcategoria fromString(String string) {
	                return null;
	            }
	        });
	    } catch (SQLException e) {
	        AlertUtil.showError("Erro ao carregar subcategorias", e.getMessage());
	    }
	}

	/**
	 * Adiciona uma lista fixa de cores ao ComboBox.
	 */
	public void carregarCores() {
	    cores.clear();
	    cores.addAll("Vermelho", "Azul", "Verde", "Amarelo", "Preto", "Branco", "Rosa", "Laranja", "Roxo", "Marrom");
	    corComboBox.setItems(cores);
	}

	/**
	 * Adiciona uma lista fixa de tamanhos ao ComboBox.
	 */
	public void carregarTamanhos() {
	    tamanhos.clear();
	    tamanhos.addAll("P", "M", "G", "GG", "XG", "Único");
	    tamanhoComboBox.setItems(tamanhos);
	}

	/**
	 * Adiciona os tipos de produtos ao ComboBox.
	 */
	public void carregarTipos() {
	    tipos.clear();
	    tipos.addAll("Produto", "Serviço", "Kit");
	    tipoComboBox.setItems(tipos);
	}

	/**
	 * Configura o cálculo automático de preços com base no custo, markup e valor.
	 * O listener do campo markup não dispara enquanto um produto está sendo carregado para edição.
	 */
	public void setupCalculoPreco() {
	    custoField.textProperty().addListener((observable, oldValue, newValue) -> {
	        if (!blockCalculation && !newValue.isEmpty() && !markupField.getText().isEmpty()) {
	            calcularValorFinal();
	        }
	    });

	    markupField.textProperty().addListener((observable, oldValue, newValue) -> {
	        if (!blockCalculation && !isEditingProduct && !newValue.isEmpty() && !custoField.getText().isEmpty()) {
	            calcularValorFinal();
	        }
	    });

	    valorField.textProperty().addListener((observable, oldValue, newValue) -> {
	        if (!blockCalculation && !newValue.isEmpty() && !custoField.getText().isEmpty()) {
	            calcularMarkup();
	        }
	    });
	}

	/**
	 * Configura os eventos dos botões presentes na interface.
	 */
	private void configurarBotoes() {
	    cancelarButton.setOnAction(event -> cancelarCadastro());
	    salvarButton.setOnAction(event -> salvarProduto());
	    novaCategoriaButton.setOnAction(event -> abrirCadastroCategoria());
	    novaMarcaButton.setOnAction(event -> abrirCadastroMarca());
	    novaSubcategoriaButton.setOnAction(event -> abrirCadastroSubcategoria());
	    novaCorButton.setOnAction(event -> abrirCadastroCor());
	    novoTamanhoButton.setOnAction(event -> abrirCadastroTamanho());
	    
	    // Verificar se o botão de markup está definido no FXML
	    try {
	        if (novoMarkupButton != null) {
	            novoMarkupButton.setOnAction(event -> abrirCadastroMarkup());
	            System.out.println("Botão de novo markup configurado com sucesso");
	        } else {
	            System.out.println("AVISO: Botão de novo markup não está definido no FXML");
	        }
	    } catch (Exception e) {
	        System.out.println("Erro ao configurar botão de markup: " + e.getMessage());
	    }
	}

	/**
	 * Configura o DatePicker de vencimento com formatação dd/MM/yyyy.
	 */
	private void configureVencimentoPicker() {
	    vencimentoPicker.setConverter(new StringConverter<LocalDate>() {
	        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	        @Override
	        public String toString(LocalDate date) {
	            return (date != null) ? dateFormatter.format(date) : "";
	        }
	        @Override
	        public LocalDate fromString(String string) {
	            return (string != null && !string.isEmpty()) ? LocalDate.parse(string, dateFormatter) : null;
	        }
	    });
	    vencimentoPicker.setValue(LocalDate.now().plusYears(1));
	}

	/**
	 * Configura a formatação dos campos numéricos, utilizando utilitários.
	 */
	private void configureDecimalFields() {
	    FormatUtil.formatTextFieldAsCurrency(custoField);
	    FormatUtil.formatTextFieldAsCurrency(valorField);
	    // Validação simples para o campo de markup
	    markupField.textProperty().addListener((observable, oldValue, newValue) -> {
	        if (!newValue.matches("\\d*(,\\d{0,2})?")) {
	            markupField.setText(oldValue);
	        }
	    });	
	}

	/**
	 * Valida os campos obrigatórios do formulário.
	 * @return true se os campos estiverem válidos; false caso contrário.
	 */
	private boolean validaCampos() {
	    if (nomeField.getText() == null || nomeField.getText().trim().isEmpty()) {
	        AlertUtil.showWarning("Campo obrigatório", "O campo Nome é obrigatório.");
	        nomeField.requestFocus();
	        return false;
	    }
	    if (categoriaComboBox.getValue() == null) {
	        AlertUtil.showWarning("Campo obrigatório", "Selecione uma Categoria.");
	        categoriaComboBox.requestFocus();
	        return false;
	    }
	    if (marcaComboBox.getValue() == null) {
	        AlertUtil.showWarning("Campo obrigatório", "Selecione uma Marca.");
	        marcaComboBox.requestFocus();
	        return false;
	    }
	    return true;
	}

	/**
	 * Prepara a interface para cadastro de um novo produto.
	 */
	public void novoProdutoView() {
	    produtoAtual = new Produto();
	    nomeField.clear();
	    descricaoArea.clear();
	    tipoComboBox.getSelectionModel().clearSelection();
	    marcaComboBox.getSelectionModel().clearSelection();
	    subcategoriaComboBox.getSelectionModel().clearSelection();
	    categoriaComboBox.getSelectionModel().clearSelection();
	    codigoBarrasField.clear();
	    codigoProdutoField.clear();
	    corComboBox.getSelectionModel().clearSelection();
	    tamanhoComboBox.getSelectionModel().clearSelection();
	    custoField.setText("0");
	    markupField.setText("0");
	    valorField.setText("0");
	    cfopField.clear();
	    icmsField.setText("0");
	    icmsSubField.setText("0");
	    estoqueField.setText("0");
	    estoqueMinField.setText("0");
	    vencimentoPicker.setValue(LocalDate.now().plusYears(1));
	}

	/**
	 * Cancela o processo de cadastro e fecha a janela atual.
	 */
	private void cancelarCadastro() {
	    Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
	    confirmDialog.setTitle("Cancelar Cadastro");
	    confirmDialog.setHeaderText("Deseja realmente cancelar?");
	    confirmDialog.setContentText("Todos os dados inseridos serão descartados.");
	    confirmDialog.showAndWait().ifPresent(response -> {
	        if (response == ButtonType.OK) {
	            Stage stage = (Stage) cancelarButton.getScene().getWindow();
	            stage.close();
	        }
	    });
	}

	/**
	 * Salva o produto atual no banco de dados.
	 * Garante que todos os valores da interface, incluindo o markup,
	 * sejam corretamente transferidos para o objeto produto antes de salvar.
	 */
	private void salvarProduto() {
	    if (!validaCampos()) return;
	    try {
	        // Log inicial para depuração
	        System.out.println("Iniciando salvamento do produto...");
	        if (produtoAtual != null && produtoAtual.getId() != null) {
	            System.out.println("Produto existente com ID: " + produtoAtual.getId());
	        } else {
	            System.out.println("Novo produto sendo criado");
	        }
	        
	        // Preservar o ID se estiver em modo de edição
	        Integer produtoId = null;
	        if (modoEdicao && produtoAtual != null && produtoAtual.getId() != null) {
	            produtoId = produtoAtual.getId();
	            System.out.println("Modo de edição ativo. ID preservado: " + produtoId);
	        }
	        
	        // Garantir que temos um objeto produto válido
	        if (produtoAtual == null) {
	            produtoAtual = new Produto();
	            modoEdicao = false;
	        }
	        
	        // Restaurar o ID se necessário
	        if (produtoId != null) {
	            produtoAtual.setId(produtoId);
	        }
	        
	        // Transferir TODOS os valores da interface para o objeto
	        produtoAtual.setNome(nomeField.getText());
	        produtoAtual.setDescricao(descricaoArea.getText());
	        produtoAtual.setCategoria(categoriaComboBox.getValue());
	        produtoAtual.setMarca(marcaComboBox.getValue());
	        produtoAtual.setSubcategoria(subcategoriaComboBox.getValue());
	        produtoAtual.setUnidade(unidadeField.getText());
	        produtoAtual.setCodigoBarra(codigoBarrasField.getText());
	        produtoAtual.setCodigo(codigoProdutoField.getText());
	        produtoAtual.setCor(corComboBox.getValue());
	        produtoAtual.setTamanho(tamanhoComboBox.getValue());
	        
	        // Valores de preço e CORREÇÃO CRUCIAL para o markup
	        BigDecimal custo = parseBigDecimal(custoField.getText(), "0.00");
	        BigDecimal preco = parseBigDecimal(valorField.getText(), "0.00");
	        
	        // CORREÇÃO CRUCIAL: Obter o markup diretamente da interface
	        BigDecimal markup = parseBigDecimal(markupField.getText(), "0.00");
	        System.out.println("Markup obtido da interface: " + markup);
	        
	        produtoAtual.setCusto(custo);
	        produtoAtual.setPreco(preco);
	        produtoAtual.setMarkup(markup); // Definir explicitamente o markup
	        
	        // Outros valores
	        produtoAtual.setCfop(cfopField.getText() != null ? cfopField.getText().trim() : "");
	        produtoAtual.setIcms(parseBigDecimal(icmsField.getText(), "0.00"));
	        produtoAtual.setIcmsSub(parseBigDecimal(icmsSubField.getText(), "0.00"));
	        produtoAtual.setEstoqueAtual(parseInteger(estoqueField.getText(), 0));
	        produtoAtual.setEstoqueMinimo(parseInteger(estoqueMinField.getText(), 0));
	        produtoAtual.setDataVencimento(vencimentoPicker.getValue());
	        produtoAtual.setDataAtualizacao(LocalDateTime.now());
	        
	        // Se não tiver tipo definido, usar um valor padrão
	        if (produtoAtual.getTipo() == null || produtoAtual.getTipo().trim().isEmpty()) {
	            produtoAtual.setTipo("Produto");
	        }
	        
	        // Garantir que o produto está marcado como ativo
	        produtoAtual.setAtivo(true);
	        
	        // Log dos valores antes de salvar
	        System.out.println("Valores a serem salvos:");
	        System.out.println("Nome: " + produtoAtual.getNome());
	        System.out.println("Custo: " + produtoAtual.getCusto());
	        System.out.println("Preço: " + produtoAtual.getPreco());
	        System.out.println("Markup: " + produtoAtual.getMarkup());
	        
	        // Verificar modo de operação e salvar
	        if (modoEdicao && produtoAtual.getId() != null) {
	            System.out.println("Atualizando produto com ID: " + produtoAtual.getId());
	            
	            // Usar o método update com Connection que aceita valores nulos corretamente
	            produtoDAO.update(produtoAtual, produtoDAO.getConnection());
	            
	            AlertUtil.showInfo("Sucesso", "Produto atualizado com sucesso!");
	        } else {
	            // Definir a data de cadastro para novos produtos
	            produtoAtual.setDataCadastro(LocalDateTime.now());
	            
	            System.out.println("Criando novo produto");
	            produtoDAO.create(produtoAtual);
	            
	            AlertUtil.showInfo("Sucesso", "Produto cadastrado com sucesso!");
	        }
	        
	        System.out.println("Produto salvo com sucesso!");
	        
	        // Limpar a tela após salvar
	        novoProdutoView();
	    } catch (SQLException e) {
	        AlertUtil.showError("Erro ao salvar", "Falha ao salvar no banco de dados: " + e.getMessage());
	        e.printStackTrace();
	    } catch (NumberFormatException e) {
	        AlertUtil.showWarning("Erro de Formatação", "Um valor numérico foi digitado incorretamente. Por favor, revise os campos.");
	        e.printStackTrace();
	    } catch (Exception e) {
	        AlertUtil.showError("Erro inesperado", "Ocorreu um erro inesperado: " + e.getMessage());
	        e.printStackTrace();
	    }
	}
	
	
	/**
	 * Carrega os dados de um produto para edição.
	 * Garante que o markup seja corretamente calculado e exibido na interface.
	 * @param produtoParam 0 produto a ser editado
	 */
	public void editarProduto(Produto produtoParam) {
	    try {
	        blockCalculation = true;
	        isEditingProduct = true;
	        this.produtoAtual = produtoParam;
	        modoEdicao = true;

	        // Preencher campos básicos
	        nomeField.setText(produtoAtual.getNome());
	        descricaoArea.setText(produtoAtual.getDescricao());
	        unidadeField.setText(produtoAtual.getUnidade());
	        codigoBarrasField.setText(produtoAtual.getCodigoBarra());
	        codigoProdutoField.setText(produtoAtual.getCodigo());
	        cfopField.setText(produtoAtual.getCfop());

	        // IMPORTANTE: Primeiro definir o custo e preço
	        custoField.setText(formatarMoeda(produtoAtual.getCusto()));
	        valorField.setText(formatarMoeda(produtoAtual.getPreco()));

	        // CRUCIAL: Definir o markup
	        BigDecimal markup;
	        if (produtoAtual.getMarkup() != null && produtoAtual.getMarkup().compareTo(BigDecimal.ZERO) > 0) {
	            markup = produtoAtual.getMarkup();
	        } else {
	            BigDecimal custo = produtoAtual.getCusto();
	            BigDecimal preco = produtoAtual.getPreco();
	            
	            if (custo != null && custo.compareTo(BigDecimal.ZERO) > 0 && preco != null) {
	                markup = preco.subtract(custo)
	                        .divide(custo, 4, RoundingMode.HALF_UP)
	                        .multiply(new BigDecimal("100"))
	                        .setScale(2, RoundingMode.HALF_UP);
	            } else {
	                markup = BigDecimal.ZERO;
	            }
	        }

	        // Atualizar o campo de markup e o objeto
	        markupField.setText(markup.toString().replace(".", ","));
	        produtoAtual.setMarkup(markup);

	        // Preencher outros campos
	        icmsField.setText(formatarMoeda(produtoAtual.getIcms()));
	        icmsSubField.setText(formatarMoeda(produtoAtual.getIcmsSub()));
	        estoqueField.setText(String.valueOf(produtoAtual.getEstoqueAtual()));
	        estoqueMinField.setText(String.valueOf(produtoAtual.getEstoqueMinimo()));

	        // Preencher ComboBoxes
	        if (produtoAtual.getCategoria() != null) categoriaComboBox.setValue(produtoAtual.getCategoria());
	        if (produtoAtual.getMarca() != null) marcaComboBox.setValue(produtoAtual.getMarca());
	        if (produtoAtual.getSubcategoria() != null) subcategoriaComboBox.setValue(produtoAtual.getSubcategoria());
	        if (produtoAtual.getCor() != null) corComboBox.setValue(produtoAtual.getCor());
	        if (produtoAtual.getTamanho() != null) tamanhoComboBox.setValue(produtoAtual.getTamanho());
	        if (produtoAtual.getTipo() != null) tipoComboBox.setValue(produtoAtual.getTipo());

	        // Configurar data de vencimento
	        if (produtoAtual.getDataVencimento() != null) {
	            vencimentoPicker.setValue(produtoAtual.getDataVencimento());
	        }

	        System.out.println("Produto carregado - Custo: " + produtoAtual.getCusto() + 
	                         ", Preço: " + produtoAtual.getPreco() + 
	                         ", Markup: " + markup);

	        blockCalculation = false;
	        isEditingProduct = false;

	    } catch (Exception e) {
	        System.err.println("Erro ao editar produto: " + e.getMessage());
	        e.printStackTrace();
	        blockCalculation = false;
	        isEditingProduct = false;
	    }
	}

	
	
	/**
	 * Método auxiliar para calcular o markup a partir dos dados do produto.
	 * Se o produto já possui markup cadastrado, retorna-o; caso contrário, calcula-o.
	 * @param produtoParam O produto para o qual calcular o markup
	 * @return BigDecimal contendo o valor do markup calculado
	 */
	private BigDecimal calcularMarkupDosProdutos(Produto produtoParam) {
	    // Verificar se o markup já está definido no produto
	    if (produtoParam.getMarkup() != null && produtoParam.getMarkup().compareTo(BigDecimal.ZERO) > 0) {
	        System.out.println("Usando markup já cadastrado: " + produtoParam.getMarkup());
	        return produtoParam.getMarkup();
	    } 
	    
	    // Calcular o markup com base no custo e preço
	    if (produtoParam.getCusto() != null && produtoParam.getPreco() != null) {
	        BigDecimal custo = produtoParam.getCusto();
	        BigDecimal preco = produtoParam.getPreco();
	        
	        // Verificar se o custo é maior que zero para evitar divisão por zero
	        if (custo.compareTo(BigDecimal.ZERO) > 0) {
	            BigDecimal calculatedMarkup = preco.subtract(custo)
	                    .divide(custo, 4, RoundingMode.HALF_UP)
	                    .multiply(new BigDecimal("100"))
	                    .setScale(2, RoundingMode.HALF_UP);
	            
	            System.out.println("Markup calculado: " + calculatedMarkup + 
	                             " (preco: " + preco + ", custo: " + custo + ")");
	            return calculatedMarkup;
	        }
	    }
	    
	    // Se não for possível calcular um markup, retornar um valor padrão
	    System.out.println("Usando markup padrão: 0");
	    return BigDecimal.ZERO;
	}

	/**
	 * Abre o formulário para cadastro de uma nova categoria.
	 */
	private void abrirCadastroCategoria() {
	    TextInputDialog dialog = new TextInputDialog();
	    dialog.setTitle("Nova Categoria");
	    dialog.setHeaderText("Cadastrar nova categoria");
	    dialog.setContentText("Insira o nome da categoria:");
	    dialog.showAndWait().ifPresent(nome -> {
	        String nomeTrim = nome != null ? nome.trim() : "";
	        if (!nomeTrim.isEmpty()) {
	            try {
	                Categoria novaCategoria = new Categoria();
	                novaCategoria.setNome(nomeTrim);
	                categoriaDAO.create(novaCategoria);
	                carregarCategorias();
	                categoriaComboBox.getItems().stream()
	                    .filter(c -> c.getNome().equals(nomeTrim))
	                    .findFirst()
	                    .ifPresent(categoriaComboBox::setValue);
	                AlertUtil.showInfo("Sucesso", "Nova categoria cadastrada com sucesso!");
	            } catch (SQLException e) {
	                AlertUtil.showError("Erro ao Cadastrar", "Não foi possível salvar a categoria: " + e.getMessage());
	            }
	        } else {
	            AlertUtil.showWarning("Entrada Inválida", "O nome da categoria não pode estar vazio.");
	        }
	    });
	}

	/**
	 * Abre o formulário para cadastro de uma nova marca.
	 */
	private void abrirCadastroMarca() {
	    TextInputDialog dialog = new TextInputDialog();
	    dialog.setTitle("Nova Marca");
	    dialog.setHeaderText("Cadastrar nova marca");
	    dialog.setContentText("Insira o nome da marca:");
	    dialog.showAndWait().ifPresent(nome -> {
	        String nomeTrim = nome != null ? nome.trim() : "";
	        if (!nomeTrim.isEmpty()) {
	            try {
	                Marca novaMarca = new Marca();
	                novaMarca.setNome(nomeTrim);
	                marcaDAO.create(novaMarca);
	                carregarMarcas();
	                marcaComboBox.getItems().stream()
	                    .filter(m -> m.getNome().equals(nomeTrim))
	                    .findFirst()
	                    .ifPresent(marcaComboBox::setValue);
	                AlertUtil.showInfo("Sucesso", "Nova marca cadastrada com sucesso!");
	            } catch (SQLException e) {
	                AlertUtil.showError("Erro ao cadastrar marca", "Não foi possível salvar a marca: " + e.getMessage());
	            }
	        } else {
	            AlertUtil.showWarning("Entrada Inválida", "O nome da marca não pode estar vazio.");
	        }
	    });
	}

	private void atualizarMarkupField(BigDecimal markup) {
	    if (markup != null) {
	        Platform.runLater(() -> {
	            blockCalculation = true;
	            markupField.setText(markup.setScale(2, RoundingMode.HALF_UP).toString().replace(".", ","));
	            blockCalculation = false;
	        });
	    }
	}

	
	/**
	 * Abre o formulário para cadastro de um novo valor de markup.
	 * Este método permite ao usuário inserir um novo valor de markup padrão
	 * que será aplicado ao produto atual.
	 */
	private void abrirCadastroMarkup() {
	    // Garantir que temos um produto para trabalhar
	    if (produtoAtual == null) {
	        produtoAtual = new Produto();
	    }
	    
	    // Criar e configurar a caixa de diálogo
	    TextInputDialog dialog = new TextInputDialog();
	    dialog.setTitle("Novo Markup");
	    dialog.setHeaderText("Cadastrar novo valor de markup");
	    dialog.setContentText("Insira o valor do markup (%):");
	    
	    // Configurar o campo para aceitar apenas números e vírgula/ponto
	    dialog.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
	        if (!newValue.matches("\\d*([,.]\\d*)?")) {
	            dialog.getEditor().setText(oldValue);
	        }
	    });
	    
	    // Se já existe um markup, pré-preencher o campo
	    if (markupField.getText() != null && !markupField.getText().isEmpty() && 
	        !markupField.getText().equals("0") && !markupField.getText().equals("0,00")) {
	        dialog.getEditor().setText(markupField.getText());
	    }
	    
	    // Mostrar diálogo e processar resultado
	    dialog.showAndWait().ifPresent(valor -> {
	        String valorTrim = valor != null ? valor.trim() : "";
	        if (!valorTrim.isEmpty()) {
	            try {
	                // Validar o valor informado (substituir vírgula por ponto para conversão)
	                BigDecimal markup = new BigDecimal(valorTrim.replace(",", "."));
	                
	                if (markup.compareTo(BigDecimal.ZERO) < 0) {
	                    AlertUtil.showWarning("Valor Inválido", "O markup não pode ser negativo.");
	                    return;
	                }
	                
	                // Obter o custo atual
	                BigDecimal custo = parseBigDecimal(custoField.getText(), "0.00");
	                
	                // Temporariamente bloquear cálculos automáticos
	                blockCalculation = true;
	                
	                // CORREÇÃO CRUCIAL: Formatar o markup adequadamente
	                String formattedMarkup = markup.toPlainString().replace(".", ",");
	                markupField.setText(formattedMarkup);
	                
	                // CORREÇÃO CRUCIAL: Atualizar o markup no objeto produto
	                produtoAtual.setMarkup(markup);
	                
	                // Desbloquear cálculos automáticos
	                blockCalculation = false;
	                
	                // Calcular o novo preço: custo + (custo * markup / 100)
	                BigDecimal novoPreco = custo.add(custo.multiply(markup).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
	                
	                // Atualizar o campo de valor final
	                valorField.setText(formatarMoeda(novoPreco));
	                
	                // Atualizar o preço no objeto produto
	                produtoAtual.setPreco(novoPreco);
	                
	                System.out.println("Novo markup definido manualmente: " + markup);
	                System.out.println("Novo preço calculado: " + novoPreco);
	                System.out.println("Valores atualizados: Custo=" + produtoAtual.getCusto() + 
	                                  ", Preço=" + produtoAtual.getPreco() + 
	                                  ", Markup=" + produtoAtual.getMarkup());
	                
	                AlertUtil.showInfo("Sucesso", "Novo markup definido com sucesso!");
	            } catch (NumberFormatException e) {
	                AlertUtil.showWarning("Entrada Inválida", "O valor informado não é um número válido.");
	                e.printStackTrace();
	            } catch (Exception e) {
	                AlertUtil.showError("Erro inesperado", "Ocorreu um erro ao definir o markup: " + e.getMessage());
	                e.printStackTrace();
	            }
	        } else {
	            AlertUtil.showWarning("Entrada Inválida", "O valor do markup não pode estar vazio.");
	        }
	    });
	}
	
	/**
	 * Abre o formulário para cadastro de uma nova subcategoria associada à categoria selecionada.
	 */
	private void abrirCadastroSubcategoria() {
	    if (categoriaComboBox.getValue() == null) {
	        AlertUtil.showWarning("Categoria Obrigatória", "Selecione uma categoria antes de cadastrar uma subcategoria.");
	        return;
	    }
	    TextInputDialog dialog = new TextInputDialog();
	    dialog.setTitle("Nova Subcategoria");
	    dialog.setHeaderText("Cadastrar nova subcategoria associada à categoria: " + categoriaComboBox.getValue().getNome());
	    dialog.setContentText("Insira o nome da subcategoria:");
	    dialog.showAndWait().ifPresent(nome -> {
	        String nomeTrim = nome != null ? nome.trim() : "";
	        if (!nomeTrim.isEmpty()) {
	            try {
	                Subcategoria novaSubcategoria = new Subcategoria();
	                novaSubcategoria.setNome(nomeTrim);
	                novaSubcategoria.setCategoria(categoriaComboBox.getValue());
	                subcategoriaDAO.create(novaSubcategoria);
	                carregarSubcategorias();
	                subcategoriaComboBox.getItems().stream()
	                    .filter(s -> s.getNome().equals(nomeTrim))
	                    .findFirst()
	                    .ifPresent(subcategoriaComboBox::setValue);
	                AlertUtil.showInfo("Sucesso", "Nova subcategoria cadastrada com sucesso!");
	            } catch (SQLException e) {
	                AlertUtil.showError("Erro ao Cadastrar", "Não foi possível salvar a subcategoria: " + e.getMessage());
	            }
	        } else {
	            AlertUtil.showWarning("Entrada Inválida", "O nome da subcategoria não pode estar vazio.");
	        }
	    });
	}

	/**
	 * Abre o formulário para cadastro de uma nova cor.
	 */
	private void abrirCadastroCor() {
	    TextInputDialog dialog = new TextInputDialog();
	    dialog.setTitle("Nova Cor");
	    dialog.setHeaderText("Cadastrar nova cor");
	    dialog.setContentText("Insira o nome da cor:");
	    dialog.showAndWait().ifPresent(nome -> {
	        String nomeTrim = nome != null ? nome.trim() : "";
	        if (!nomeTrim.isEmpty()) {
	            if (!cores.contains(nomeTrim)) {
	                cores.add(nomeTrim);
	                corComboBox.setItems(cores);
	                corComboBox.setValue(nomeTrim);
	                AlertUtil.showInfo("Sucesso", "Nova cor cadastrada com sucesso!");
	            } else {
	                AlertUtil.showWarning("Cor Existe", "Esta cor já está cadastrada.");
	            }
	        } else {
	            AlertUtil.showWarning("Entrada Inválida", "O nome da cor não pode estar vazio.");
	        }
	    });
	}
	

	/**
	 * Abre a interface para cadastro de um novo tamanho.
	 */
	private void abrirCadastroTamanho() {
	    TextInputDialog dialog = new TextInputDialog();
	    dialog.setTitle("Novo Tamanho");
	    dialog.setHeaderText("Cadastrar novo tamanho");
	    dialog.setContentText("Insira o tamanho:");
	    dialog.showAndWait().ifPresent(nome -> {
	        String nomeTrim = nome != null ? nome.trim() : "";
	        if (!nomeTrim.isEmpty()) {
	            if (!tamanhos.contains(nomeTrim)) {
	                tamanhos.add(nomeTrim);
	                tamanhoComboBox.setItems(tamanhos);
	                tamanhoComboBox.setValue(nomeTrim);
	                AlertUtil.showInfo("Sucesso", "Novo tamanho cadastrado com sucesso!");
	            } else {
	                AlertUtil.showWarning("Tamanho Existe", "Este tamanho já está cadastrado.");
	            }
	        } else {
	            AlertUtil.showWarning("Entrada Inválida", "O tamanho não pode estar vazio.");
	        }
	    });
	}

	/**
	 * Converte uma string em BigDecimal, utilizando um valor padrão se necessário.
	 */
	private BigDecimal parseBigDecimal(String value, String defaultValue) {
	    try {
	        return new BigDecimal(value != null && !value.trim().isEmpty() ? value.replace(",", ".").trim() : defaultValue);
	    } catch (NumberFormatException e) {
	        return new BigDecimal(defaultValue);
	    }
	}
	
	/**
	 * Método para calcular o valor final do produto com base no custo e no markup.
	 * Fórmula aplicada: valorFinal = custo + (custo * markup / 100).
	 */
	private void calcularValorFinal() {
	    try {
	        BigDecimal custo = parseBigDecimal(custoField.getText(), "0.00");
	        BigDecimal markup = parseBigDecimal(markupField.getText(), "0.00");
	        
	        validarValorNaoNegativo(custo, "O custo não pode ser negativo.");
	        validarValorNaoNegativo(markup, "O markup não pode ser negativo.");
	        
	        BigDecimal acrescimo = custo.multiply(markup).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
	        BigDecimal valorFinal = custo.add(acrescimo);
	        
	        // Atualizar o campo de valor
	        blockCalculation = true;
	        valorField.setText(formatarMoeda(valorFinal));
	        blockCalculation = false;
	        
	        // Atualizar o objeto produtoAtual
	        if (produtoAtual != null) {
	            produtoAtual.setCusto(custo);
	            produtoAtual.setPreco(valorFinal);
	            produtoAtual.setMarkup(markup);
	        }
	    } catch (Exception e) {
	        System.err.println("Erro ao calcular valor final: " + e.getMessage());
	        e.printStackTrace();
	    }
	}

	
	 /**
     * Valida que o valor fornecido não seja negativo.
     * @param valor O valor a ser validado
     * @param mensagem Mensagem de erro caso o valor seja inválido
     */
    private void validarValorNaoNegativo(BigDecimal valor, String mensagem) {
        if (valor.signum() < 0) {
            throw new IllegalArgumentException(mensagem);
        }
    }
    
	/**
	 * Método para calcular o markup a partir dos valores de custo e valor final.
	 * Fórmula aplicada: markup = ((valorFinal - custo) / custo) * 100.
	 
	private void calcularMarkup() {
	    try {
	        // Recupera e valida o custo
	        BigDecimal custo = parseBigDecimal(custoField.getText(), "0.00");
	        validarValorNaoNegativo(custo, "O custo deve ser maior que zero para o cálculo de markup.");
	        if (custo.compareTo(BigDecimal.ZERO) == 0) {
	            throw new IllegalArgumentException("O custo deve ser maior que zero para o cálculo de markup.");
	        }
	        // Recupera e valida o valor final
	        BigDecimal valorFinal = parseBigDecimal(valorField.getText(), "0.00");
	        validarValorNaoNegativo(valorFinal, "O preço final não pode ser negativo.");
	        // Calcula o markup
	        BigDecimal markup = valorFinal.subtract(custo)
	                .divide(custo, 4, RoundingMode.HALF_UP)
	                .multiply(new BigDecimal("100"));
	        // Atualiza o campo de markup
	        blockCalculation = true;
	        markupField.setText(markup.setScale(2, RoundingMode.HALF_UP).toString().replace(".", ","));
	        blockCalculation = false;
	    } catch (NumberFormatException e) {
	        AlertUtil.showWarning("Erro de Formatação", "Os campos de custo ou valor final contêm valores inválidos.");
	    } catch (IllegalArgumentException e) {
	        AlertUtil.showWarning("Valor Inválido", e.getMessage());
	    }
	}
	**/
    
    /**
     * Método para calcular o markup a partir dos valores de custo e valor final.
     * Fórmula aplicada: markup = ((valorFinal - custo) / custo) * 100.
     */
    private void calcularMarkup() {
        try {
            if (blockCalculation) return;
            
            // Recupera e valida o custo
            BigDecimal custo = parseBigDecimal(custoField.getText(), "0.00");
            if (custo.compareTo(BigDecimal.ZERO) <= 0) {
                markupField.setText("0,00");
                return;
            }

            // Recupera e valida o valor final
            BigDecimal valorFinal = parseBigDecimal(valorField.getText(), "0.00");
            if (valorFinal.compareTo(BigDecimal.ZERO) < 0) {
                markupField.setText("0,00");
                return;
            }

            // Calcula o markup
            BigDecimal markup = valorFinal.subtract(custo)
                    .divide(custo, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);

            // Atualiza o campo de markup e o objeto produto
            blockCalculation = true;
            String markupFormatado = markup.toString().replace(".", ",");
            markupField.setText(markupFormatado);
            
            if (produtoAtual != null) {
                produtoAtual.setMarkup(markup);
            }
            
            System.out.println("Markup calculado: " + markupFormatado);
            blockCalculation = false;
        } catch (Exception e) {
            System.err.println("Erro ao calcular markup: " + e.getMessage());
            markupField.setText("0,00");
        }
    }


	/**
	 * Converte uma string em Integer, utilizando um valor padrão se necessário.
	 */
	private Integer parseInteger(String value, int defaultValue) {
	    try {
	        return value != null && !value.trim().isEmpty() ? Integer.parseInt(value.trim()) : defaultValue;
	    } catch (NumberFormatException e) {
	        return defaultValue;
	    }
	}

	/**
	 * Formata um BigDecimal para o padrão de duas casas decimais, substituindo ponto por vírgula.
	 */
	private String formatarMoeda(BigDecimal valor) {
	    return valor.setScale(2, RoundingMode.HALF_UP).toString().replace(".", ",");
	}
    
}