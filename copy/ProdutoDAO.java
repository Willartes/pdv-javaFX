package br.com.pdv.dao;

import br.com.pdv.model.Produto;
import br.com.pdv.util.DatabaseConnection;
import br.com.pdv.util.DatabaseUtil;
import br.com.pdv.util.LogUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Objects;

/**
 * Classe DAO para manipulação de dados de produtos no banco de dados.
 */
public class ProdutoDAO implements IProdutoDAO {

    private static ProdutoDAO instance;
    private final DatabaseConnection databaseConnection;

    // Constantes para comandos SQL
    private static final String SQL_INSERT = "INSERT INTO produtos (codigo, nome, descricao, preco, custo, estoque_minimo, estoque_atual, unidade, ativo, data_vencimento) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";					
    private static final String SQL_UPDATE = "UPDATE produtos SET codigo = ?, nome = ?, descricao = ?, preco = ?, custo = ?, estoque_minimo = ?, estoque_atual = ?, unidade = ?, ativo = ?, data_vencimento = ? WHERE id = ?";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM produtos WHERE id = ?";
    private static final String SQL_SELECT_BY_CODIGO = "SELECT * FROM produtos WHERE codigo = ?";
    private static final String SQL_SELECT_BY_NOME = "SELECT * FROM produtos WHERE nome LIKE ?";   
    private static final String SQL_DELETE = "DELETE FROM produtos WHERE id = ?";
    private static final String SQL_SELECT_ALL = "SELECT * FROM produtos";
    private static final String SQL_SELECT_ESTOQUE_BAIXO = "SELECT * FROM produtos WHERE estoque_atual < estoque_minimo";
    private static final String SQL_COUNT = "SELECT COUNT(*) FROM produtos";
    
    private ProdutoDAO() {
        this.databaseConnection = DatabaseConnection.getInstance();
    }

    public static synchronized ProdutoDAO getInstance() {
        if (instance == null) {
            instance = new ProdutoDAO();
        }
        return instance;
    }

    /**
     * Cria um novo produto no banco de dados.
     *
     * @param produto O produto a ser criado.
     * @return O produto criado com o ID gerado.
     * @throws SQLException Se ocorrer um erro ao acessar o banco de dados.
     */
    @Override
    public Produto create(Produto produto) throws SQLException {
    	Objects.requireNonNull(produto, "Produto não pode ser nulo");
        UUID transactionId = UUID.randomUUID();

        LogUtil.info(ProdutoDAO.class, String.format("[%s] Iniciando criação de produto: %s", transactionId, produto.getNome()));

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, produto.getCodigo());
            stmt.setString(2, produto.getNome());
            stmt.setString(3, produto.getDescricao());
            stmt.setBigDecimal(4, produto.getPreco());
            stmt.setBigDecimal(5, produto.getCusto());
            stmt.setInt(6, produto.getEstoqueMinimo());
            stmt.setInt(7, produto.getEstoqueAtual());
            stmt.setString(8, produto.getUnidade());
            stmt.setBoolean(9, produto.isAtivo());
            
            // Adicionando data de vencimento
            if (produto.getDataVencimento() != null) {
                stmt.setDate(10, java.sql.Date.valueOf(produto.getDataVencimento()));
            } else {
                stmt.setNull(10, java.sql.Types.DATE);
            }

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                String errorMessage = String.format("[%s] Falha ao criar produto, nenhuma linha afetada.", transactionId);
                LogUtil.error(ProdutoDAO.class, errorMessage, null);
                throw new SQLException(errorMessage);
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    produto.setId(generatedKeys.getInt(1));
                    LogUtil.info(ProdutoDAO.class, String.format("[%s] Produto criado com sucesso. ID: %d", transactionId, produto.getId()));
                    return produto;
                } else {
                    String errorMessage = String.format("[%s] Falha ao criar produto, nenhum ID obtido.", transactionId);
                    LogUtil.error(ProdutoDAO.class, errorMessage, null);
                    throw new SQLException(errorMessage);
                }
            }
        } catch (SQLException e) {
            String errorMessage = String.format("[%s] Erro ao criar produto: %s", transactionId, e.getMessage());
            LogUtil.error(ProdutoDAO.class, errorMessage, e);
            throw new SQLException(errorMessage, e);
        }
    }
    
    
    /**
     * Conta o total de produtos ativos no sistema
     * 
     * @return Número de produtos ativos
     * @throws SQLException Em caso de erro no banco de dados
     */
    public int contarProdutosAtivos() throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            
            String sql = "SELECT COUNT(*) FROM produtos WHERE ativo = true";
            
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
            return 0;
            
        } finally {
        	DatabaseUtil.closeResources(conn, stmt, rs);
        }
    }

    /**
     * Calcula o total de itens em estoque
     * 
     * @return Número total de itens em estoque
     * @throws SQLException Em caso de erro no banco de dados
     */
    public int calcularTotalItensEstoque() throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            
            String sql = "SELECT SUM(estoque_atual) FROM produtos WHERE ativo = true";
            
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
            return 0;
            
        } finally {
        	DatabaseUtil.closeResources(conn, stmt, rs);
        }
    }

    /**
     * Conta o número de produtos com estoque abaixo do mínimo
     * 
     * @return Número de produtos com estoque baixo
     * @throws SQLException Em caso de erro no banco de dados
     */
    public int contarProdutosComEstoqueBaixo() throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            
            String sql = "SELECT COUNT(*) FROM produtos " +
                         "WHERE ativo = true AND estoque_atual <= estoque_minimo";
            
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
            return 0;
            
        } finally {
        	DatabaseUtil.closeResources(conn, stmt, rs);
        }
    }

    /**
     * Busca produtos com estoque abaixo do mínimo
     * 
     * @return Lista de produtos com estoque baixo
     * @throws SQLException Em caso de erro no banco de dados
     */
    public List<Produto> findWithLowStock() throws SQLException {
        List<Produto> produtosEstoqueBaixo = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            
            String sql = "SELECT * FROM produtos " +
                         "WHERE ativo = true AND estoque_atual <= estoque_minimo " +
                         "ORDER BY estoque_atual ASC";
            
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                Produto produto = mapResultSetToProduto(rs);
                produtosEstoqueBaixo.add(produto);
            }
            
            return produtosEstoqueBaixo;
            
        } finally {
            DatabaseUtil.closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * Busca um produto pelo ID usando uma conexão fornecida
     * 
     * @param id ID do produto
     * @param conn Conexão com o banco de dados
     * @return Produto encontrado ou null se não existir
     * @throws SQLException Se ocorrer um erro de SQL
     */
    @Override
    public Produto findById(Integer id) throws SQLException {
        Connection conn = null;
        try {
            conn = databaseConnection.getConnection();
            return findById(id, conn);
        } finally {
            if (conn != null) {
                databaseConnection.releaseConnection(conn);
            }
        }
    }
    
    
    /**
     * Busca um produto pelo ID usando uma conexão fornecida
     * 
     * @param id ID do produto
     * @param conn Conexão com o banco de dados
     * @return Produto encontrado ou null se não existir
     * @throws SQLException Se ocorrer um erro de SQL
     */
    public Produto findById(Integer id, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            stmt = conn.prepareStatement("SELECT * FROM produtos WHERE id = ?");
            stmt.setInt(1, id);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToProduto(rs);
            } else {
                return null;
            }
        } finally {
            // Fechar apenas statement e resultset, não a conexão
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    /**
     * Atualiza um produto usando uma conexão fornecida
     * 
     * @param produto Produto a ser atualizado
     * @param conn Conexão com o banco de dados
     * @return true se a atualização foi bem-sucedida
     * @throws SQLException Se ocorrer um erro de SQL
     */
    public boolean update(Produto produto, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        
        try {
        	String sql = "UPDATE produtos SET nome = ?, codigo = ?, preco = ?, custo = ?, estoque_atual = ?, "
                    + "estoque_minimo = ?, ativo = ?, data_atualizacao = ? WHERE id = ?";
                    
	         stmt = conn.prepareStatement(sql);
	         
	         stmt.setString(1, produto.getNome());
	         stmt.setString(2, produto.getCodigo());
	         stmt.setBigDecimal(3, produto.getPreco());
	         stmt.setBigDecimal(4, produto.getCusto()); // Adicionado o custo
	         stmt.setInt(5, produto.getEstoqueAtual());
	         stmt.setInt(6, produto.getEstoqueMinimo());
	         stmt.setBoolean(7, produto.isAtivo());
	         stmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
	         stmt.setInt(9, produto.getId());
	         
	         
            
            return stmt.executeUpdate() > 0;
        } finally {
            // Fechar apenas statement, não a conexão
            if (stmt != null) stmt.close();
        }
    }
    
    

    /**
     * Busca um produto pelo código.
     *
     * @param codigo O código do produto a ser buscado.
     * @return O produto encontrado ou null se não encontrado.
     * @throws SQLException Se ocorrer um erro ao acessar o banco de dados.
     */
    @Override
    public Produto findByCodigo(String codigo) throws SQLException {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("Código inválido para busca: " + codigo);
        }
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(ProdutoDAO.class, String.format("[%s] Buscando produto por código: %s", transactionId, codigo));

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BY_CODIGO)) {

            stmt.setString(1, codigo);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Produto produto = mapResultSetToProduto(rs);
                    LogUtil.info(ProdutoDAO.class, String.format("[%s] Produto encontrado - Código: %s", transactionId, codigo));
                    return produto;
                } else {
                    LogUtil.warn(ProdutoDAO.class, String.format("[%s] Produto não encontrado - Código: %s", transactionId, codigo));
                    return null;
                }
            }
        } catch (SQLException e) {
            String errorMessage = String.format("[%s] Erro ao buscar produto por código: %s", transactionId, e.getMessage());
            LogUtil.error(ProdutoDAO.class, errorMessage, e);
            throw new SQLException(errorMessage, e);
        }
    }

    /**
     * Busca produtos pelo nome (LIKE %nome%).
     *
     * @param nome O nome ou parte do nome dos produtos a serem buscados.
     * @return Uma lista de produtos encontrados.
     * @throws SQLException Se ocorrer um erro ao acessar o banco de dados.
     */
    @Override
    public List<Produto> findByNome(String nome) throws SQLException {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome inválido para busca: " + nome);
        }
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(ProdutoDAO.class, String.format("[%s] Buscando produtos por nome: %s", transactionId, nome));

        List<Produto> produtos = new ArrayList<>();
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BY_NOME)) {

            stmt.setString(1, "%" + nome + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Produto produto = mapResultSetToProduto(rs);
                    produtos.add(produto);
                }
            }
            LogUtil.info(ProdutoDAO.class, String.format("[%s] Encontrados %d produtos com nome contendo: %s", transactionId, produtos.size(), nome));
        } catch (SQLException e) {
            String errorMessage = String.format("[%s] Erro ao buscar produtos por nome: %s", transactionId, e.getMessage());
            LogUtil.error(ProdutoDAO.class, errorMessage, e);
            throw new SQLException(errorMessage, e);
        }
        return produtos;
    }

    /**
     * Atualiza um produto no banco de dados.
     *
     * @param produto O produto a ser atualizado.
     * @return true se o produto foi atualizado com sucesso, false caso contrário.
     * @throws SQLException Se ocorrer um erro ao acessar o banco de dados.
     */
    @Override
    public boolean update(Produto produto) throws SQLException {
        Objects.requireNonNull(produto, "Produto não pode ser nulo");
        if (produto.getId() == null || produto.getId() <= 0) {
            throw new IllegalArgumentException("ID do produto inválido para atualização: " + produto.getId());
        }
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(ProdutoDAO.class, String.format("[%s] Iniciando atualização de produto: %s", transactionId, produto.getNome()));

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setString(1, produto.getCodigo());
            stmt.setString(2, produto.getNome());
            stmt.setString(3, produto.getDescricao());
            stmt.setBigDecimal(4, produto.getPreco());
            
            // Certifique-se de que o custo está sendo salvo
            stmt.setBigDecimal(5, produto.getCusto() != null ? produto.getCusto() : BigDecimal.ZERO);
            
            stmt.setInt(6, produto.getEstoqueMinimo());
            stmt.setInt(7, produto.getEstoqueAtual());
            stmt.setString(8, produto.getUnidade());
            stmt.setBoolean(9, produto.isAtivo());
            
            // Adicionando data de vencimento
            if (produto.getDataVencimento() != null) {
                stmt.setDate(10, java.sql.Date.valueOf(produto.getDataVencimento()));
            } else {
                stmt.setNull(10, java.sql.Types.DATE);
            }
            
            stmt.setInt(11, produto.getId());
            
            // Adicionar log para depuração
            System.out.println("SQL Update sendo executado para produto ID: " + produto.getId());
            System.out.println("Custo sendo salvo: " + produto.getCusto());
            
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                LogUtil.info(ProdutoDAO.class, String.format("[%s] Produto atualizado com sucesso. ID: %d", transactionId, produto.getId()));
                return true;
            } else {
                LogUtil.warn(ProdutoDAO.class, String.format("[%s] Nenhum produto atualizado - ID: %d", transactionId, produto.getId()));
                return false;
            }
        } catch (SQLException e) {
            String errorMessage = String.format("[%s] Erro ao atualizar produto: %s", transactionId, e.getMessage());
            LogUtil.error(ProdutoDAO.class, errorMessage, e);
            throw new SQLException(errorMessage, e);
        }
    }
    /**
     * Exclui um produto do banco de dados.
     *
     * @param id O ID do produto a ser excluído.
     * @return true se o produto foi excluído com sucesso, false caso contrário.
     * @throws SQLException Se ocorrer um erro ao acessar o banco de dados.
     */
    @Override
    public boolean delete(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido para exclusão: " + id);
        }
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(ProdutoDAO.class, String.format("[%s] Iniciando exclusão de produto. ID: %d", transactionId, id));

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {

            stmt.setInt(1, id);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                LogUtil.info(ProdutoDAO.class, String.format("[%s] Produto excluído com sucesso - ID: %d", transactionId, id));
                return true;
            } else {
                LogUtil.warn(ProdutoDAO.class, String.format("[%s] Nenhum produto excluído - ID: %d", transactionId, id));
                return false;
            }
        } catch (SQLException e) {
            String errorMessage = String.format("[%s] Erro ao excluir produto: %s", transactionId, e.getMessage());
            LogUtil.error(ProdutoDAO.class, errorMessage, e);
            throw new SQLException(errorMessage, e);
        }
    }

    /**
     * Lista todos os produtos do banco de dados.
     *
     * @return Uma lista de todos os produtos.
     * @throws SQLException Se ocorrer um erro ao acessar o banco de dados.
     */
    @Override
    public List<Produto> findAll() throws SQLException {
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(ProdutoDAO.class, String.format("[%s] Listando todos os produtos.", transactionId));

        List<Produto> produtos = new ArrayList<>();
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Produto produto = mapResultSetToProduto(rs);
                produtos.add(produto);
            }
            LogUtil.info(ProdutoDAO.class, String.format("[%s] Total de produtos encontrados: %d", transactionId, produtos.size()));
        } catch (SQLException e) {
            String errorMessage = String.format("[%s] Erro ao listar todos os produtos: %s", transactionId, e.getMessage());
            LogUtil.error(ProdutoDAO.class, errorMessage, e);
            throw new SQLException(errorMessage, e);
        }
        return produtos;
    }

    /**
     * Lista todos os produtos com estoque abaixo do estoque mínimo.
     *
     * @return Uma lista de todos os produtos com estoque baixo.
     * @throws SQLException Se ocorrer um erro ao acessar o banco de dados.
     */
    @Override
    public List<Produto> findEstoqueBaixo() throws SQLException {
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(ProdutoDAO.class, String.format("[%s] Listando todos os produtos com estoque baixo.", transactionId));

        List<Produto> produtos = new ArrayList<>();
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ESTOQUE_BAIXO);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Produto produto = mapResultSetToProduto(rs);
                produtos.add(produto);
            }
            LogUtil.info(ProdutoDAO.class, String.format("[%s] Total de produtos com estoque baixo encontrados: %d", transactionId, produtos.size()));
        } catch (SQLException e) {
            String errorMessage = String.format("[%s] Erro ao listar todos os produtos com estoque baixo: %s", transactionId, e.getMessage());
            LogUtil.error(ProdutoDAO.class, errorMessage, e);
            throw new SQLException(errorMessage, e);
        }
        return produtos;
    }

    /**
     * Conta o número total de produtos no banco de dados.
     *
     * @return O número total de produtos.
     * @throws SQLException Se ocorrer um erro ao acessar o banco de dados.
     */
    @Override
    public long count() throws SQLException {
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(ProdutoDAO.class, String.format("[%s] Contando o número total de produtos.", transactionId));

        try (Connection conn = databaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_COUNT)) {

            if (rs.next()) {
                long count = rs.getLong(1);
                LogUtil.info(ProdutoDAO.class, String.format("[%s] Total de produtos encontrados: %d", transactionId, count));
                return count;
            } else {
                LogUtil.warn(ProdutoDAO.class, String.format("[%s] Nenhuma linha retornada na contagem de produtos.", transactionId));
                return 0;
            }
        } catch (SQLException e) {
            String errorMessage = String.format("[%s] Erro ao contar o número de produtos: %s", transactionId, e.getMessage());
            LogUtil.error(ProdutoDAO.class, errorMessage, e);
            throw new SQLException(errorMessage, e);
        }
    }

    /**
     * Método auxiliar para mapear um ResultSet para um objeto Produto.
     *
     * @param rs O ResultSet a ser mapeado.
     * @return Um objeto Produto com os dados do ResultSet.
     * @throws SQLException Se ocorrer um erro ao acessar os dados do ResultSet.
     */
    private Produto mapResultSetToProduto(ResultSet rs) throws SQLException {
        Produto produto = new Produto();
        produto.setId(rs.getInt("id"));
        produto.setCodigo(rs.getString("codigo"));
        produto.setCusto(rs.getBigDecimal("custo"));
        
        // Obter o nome do produto e verificar se é nulo ou vazio
        String nome = rs.getString("nome");
        if (nome != null && !nome.trim().isEmpty()) {
            produto.setNome(nome);
        } else {
            // Definir um nome padrão para evitar exceção
            produto.setNome("Produto #" + produto.getId());
        }
        
        // Obter a descrição (pode ser nula)
        String descricao = rs.getString("descricao");
        produto.setDescricao(descricao);
        
        produto.setPreco(rs.getBigDecimal("preco"));
        produto.setEstoqueMinimo(rs.getInt("estoque_minimo"));
        produto.setEstoqueAtual(rs.getInt("estoque_atual"));
        produto.setUnidade(rs.getString("unidade"));
        produto.setAtivo(rs.getBoolean("ativo"));
        
        // Carregar a data de vencimento
        java.sql.Date dataVencimento = rs.getDate("data_vencimento");
        if (dataVencimento != null) {
            produto.setDataVencimento(dataVencimento.toLocalDate());
        }
        
        return produto;
    }
}