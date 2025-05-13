package br.com.pdv.relatorio;

import br.com.pdv.dao.*;
import br.com.pdv.model.*;
import br.com.pdv.util.DatabaseConnection;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe principal de gerenciamento de relatórios do sistema.
 * Responsável por criar e coordenar os diversos tipos de relatórios disponíveis.
 */
public class RelatorioManager {
    // Instância singleton
    private static RelatorioManager instance;
    
    // Mapa de tipos de relatórios disponíveis
    private final Map<TipoRelatorio, Class<?>> relatoriosDisponiveis;
    
    // DAOs necessários para os relatórios
    private final VendaDAO vendaDAO;
    private final ProdutoDAO produtoDAO;
    private final CaixaDAO caixaDAO;
    private final UsuarioDAO usuarioDAO;
    private final ClienteDAO clienteDAO;
    
    /**
     * Construtor privado (padrão Singleton)
     */
    private RelatorioManager() {
        this.vendaDAO = VendaDAO.getInstance();
        this.produtoDAO = ProdutoDAO.getInstance();
        this.caixaDAO = CaixaDAO.getInstance();
        this.usuarioDAO = UsuarioDAO.getInstance();
        this.clienteDAO = ClienteDAO.getInstance();
        
        // Inicializa o mapa de relatórios disponíveis
        this.relatoriosDisponiveis = new HashMap<>();
        registrarRelatorios();
    }
    
    /**
     * Obtém a instância única do RelatorioManager
     */
    public static synchronized RelatorioManager getInstance() {
        if (instance == null) {
            instance = new RelatorioManager();
        }
        return instance;
    }
    
    /**
     * Método auxiliar para registrar uma classe de relatório de forma segura
     */
    private <T extends RelatorioBase> void registrar(TipoRelatorio tipo, Class<T> classe) {
        relatoriosDisponiveis.put(tipo, classe);
    }
    
    /**
     * Registra todos os tipos de relatórios disponíveis no sistema
     */
    private void registrarRelatorios() {
        // Relatórios de Vendas
        registrar(TipoRelatorio.VENDAS_TOTAIS, RelatorioVendasTotais.class);
        
        // Verificação para classes que podem não estar implementadas completamente
        if (verificarClasse(RelatorioVendasPorProduto.class)) {
            registrar(TipoRelatorio.VENDAS_POR_PRODUTO, RelatorioVendasPorProduto.class);
        }
        
        if (verificarClasse(RelatorioVendasPorCategoria.class)) {
            registrar(TipoRelatorio.VENDAS_POR_CATEGORIA, RelatorioVendasPorCategoria.class);
        }
        
        registrar(TipoRelatorio.LUCRO_BRUTO_POR_PRODUTO, RelatorioLucroBrutoPorProduto.class);
        
        if (verificarClasse(RelatorioVendasPorVendedor.class)) {
            registrar(TipoRelatorio.VENDAS_POR_VENDEDOR, RelatorioVendasPorVendedor.class);
        }
        
        if (verificarClasse(RelatorioFrequenciaCompra.class)) {
            registrar(TipoRelatorio.FREQUENCIA_COMPRA, RelatorioFrequenciaCompra.class);
        }
        
        // Relatórios de Estoque
        registrar(TipoRelatorio.NIVEL_ESTOQUE, RelatorioNivelEstoque.class);
        
        if (verificarClasse(RelatorioValorEstoque.class)) {
            registrar(TipoRelatorio.VALOR_ESTOQUE, RelatorioValorEstoque.class);
        }
        
        // Relatórios Financeiros
        if (verificarClasse(RelatorioReceitas.class)) {
            registrar(TipoRelatorio.RECEITAS, RelatorioReceitas.class);
        }
        
        registrar(TipoRelatorio.COMISSAO_VENDEDORES, RelatorioComissaoVendedores.class);
        registrar(TipoRelatorio.FINANCEIRO_FORMAS_PAGAMENTO, RelatorioFinanceiroPorFormasPagamento.class);
        
        if (verificarClasse(RelatorioCreditoPorPeriodo.class)) {
            registrar(TipoRelatorio.CREDITO_POR_PERIODO, RelatorioCreditoPorPeriodo.class);
        }
        
        if (verificarClasse(RelatorioCreditoPorCliente.class)) {
            registrar(TipoRelatorio.CREDITO_POR_CLIENTE, RelatorioCreditoPorCliente.class);
        }
    }
    
    /**
     * Verifica se uma classe está implementada corretamente estendendo RelatorioBase
     */
    private boolean verificarClasse(Class<?> classe) {
        try {
            return RelatorioBase.class.isAssignableFrom(classe);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Cria um relatório do tipo especificado
     * 
     * @param tipo Tipo de relatório desejado
     * @param dataInicio Data de início do período do relatório (quando aplicável)
     * @param dataFim Data de fim do período do relatório (quando aplicável)
     * @return O relatório solicitado
     * @throws RelatorioPDVException Se houver erro na criação do relatório
     */
    @SuppressWarnings("unchecked")
    public RelatorioBase criarRelatorio(TipoRelatorio tipo, LocalDateTime dataInicio, LocalDateTime dataFim) 
            throws RelatorioPDVException {
        try {
            Class<?> relatorioClass = relatoriosDisponiveis.get(tipo);
            if (relatorioClass == null) {
                throw new RelatorioPDVException("Tipo de relatório não suportado: " + tipo);
            }
            
            // Verificar se a classe realmente estende RelatorioBase
            if (!RelatorioBase.class.isAssignableFrom(relatorioClass)) {
                throw new RelatorioPDVException("Classe de relatório não implementada corretamente: " + relatorioClass.getName());
            }
            
            Class<? extends RelatorioBase> classeRelatorio = (Class<? extends RelatorioBase>) relatorioClass;
            
            // Verificar se o tipo de relatório requer período
            if (requiresPeriodo(tipo)) {
                // Criar instância com período
                return classeRelatorio.getConstructor(LocalDateTime.class, LocalDateTime.class)
                                     .newInstance(dataInicio, dataFim);
            } else {
                // Criar instância sem período
                return classeRelatorio.getConstructor().newInstance();
            }
        } catch (Exception e) {
            throw new RelatorioPDVException("Erro ao criar relatório: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verifica se um tipo de relatório requer período (dataInicio e dataFim)
     */
    private boolean requiresPeriodo(TipoRelatorio tipo) {
        return tipo != TipoRelatorio.NIVEL_ESTOQUE && 
               tipo != TipoRelatorio.VALOR_ESTOQUE;
    }
    
    /**
     * Retorna o VendaDAO utilizado pelos relatórios
     */
    public VendaDAO getVendaDAO() {
        return vendaDAO;
    }
    
    /**
     * Retorna o ProdutoDAO utilizado pelos relatórios
     */
    public ProdutoDAO getProdutoDAO() {
        return produtoDAO;
    }
    
    /**
     * Retorna o CaixaDAO utilizado pelos relatórios
     */
    public CaixaDAO getCaixaDAO() {
        return caixaDAO;
    }
    
    /**
     * Retorna o UsuarioDAO utilizado pelos relatórios
     */
    public UsuarioDAO getUsuarioDAO() {
        return usuarioDAO;
    }
    
    /**
     * Retorna o ClienteDAO utilizado pelos relatórios
     */
    public ClienteDAO getClienteDAO() {
        return clienteDAO;
    }
    
    /**
     * Verifica se um relatório está disponível
     */
    public boolean isRelatorioDisponivel(TipoRelatorio tipo) {
        return relatoriosDisponiveis.containsKey(tipo);
    }
    
    /**
     * Enumera os tipos de relatórios disponíveis no sistema
     */
    public enum TipoRelatorio {
        // Relatórios de Vendas
        VENDAS_TOTAIS("Vendas Totais"), 
        VENDAS_POR_PRODUTO("Vendas por Produto"),
        VENDAS_POR_CATEGORIA("Vendas por Categoria de Produto"),
        LUCRO_BRUTO_POR_PRODUTO("Lucro Bruto Por Produto"),
        VENDAS_POR_VENDEDOR("Vendas por Vendedor"),
        FREQUENCIA_COMPRA("Frequência de Compra"),
        
        // Relatórios de Estoque
        NIVEL_ESTOQUE("Nível de Estoque"),
        VALOR_ESTOQUE("Valor de Estoque"),
        
        // Relatórios Financeiros
        RECEITAS("Receitas"),
        COMISSAO_VENDEDORES("Comissão Vendedores"),
        FINANCEIRO_FORMAS_PAGAMENTO("Financeiro por Formas de Pagamento"),
        CREDITO_POR_PERIODO("Crediário Por Período"),
        CREDITO_POR_CLIENTE("Crediário Por Cliente");
        
        private final String descricao;
        
        TipoRelatorio(String descricao) {
            this.descricao = descricao;
        }
        
        public String getDescricao() {
            return descricao;
        }
        
        @Override
        public String toString() {
            return descricao;
        }
    }
    
    /**
     * Exceção específica para erros nos relatórios
     */
    public static class RelatorioPDVException extends Exception {
        private static final long serialVersionUID = 1L;
        
        public RelatorioPDVException(String message) {
            super(message);
        }
        
        public RelatorioPDVException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}