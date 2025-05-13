package br.com.pdv.dao;

import br.com.pdv.model.Categoria;
import br.com.pdv.model.Marca;
import br.com.pdv.model.Produto;
import br.com.pdv.model.Subcategoria;
import br.com.pdv.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe DAO para operações de banco de dados relacionadas a Produtos
 */
public class ProdutoDAO  {
    private static ProdutoDAO instance;
    private Connection connection;
    
    // Construtor privado (padrão Singleton)
    private ProdutoDAO() {
        try {
            this.connection = DatabaseConnection.getInstance().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao conectar ao banco de dados", e);
        }
    }
    
    /**
     * Retorna a instância única do DAO (Padrão Singleton)
     * @return A instância do DAO
     */
    public static synchronized ProdutoDAO getInstance() {
        if (instance == null) {
            instance = new ProdutoDAO();
        }
        return instance;
    }
    
    /**
     * Cria um novo produto no banco de dados
     * @param produto O produto a ser criado
     * @return O produto criado com o ID gerado
     * @throws SQLException Em caso de erro no banco de dados
     */
    public Produto create(Produto produto) throws SQLException {
        String sql = "INSERT INTO produtos (nome, descricao, tipo, marca_id, subcategoria_id, unidade, " +
                "categoria_id, codigo_barra, codigo, cor, tamanho, custo, preco, cfop, icms, icms_sub, " +
                "estoque_atual, estoque_minimo, ativo, data_cadastro, data_atualizacao, data_vencimento) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Definir os parâmetros
            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setString(3, produto.getTipo());
            stmt.setInt(4, produto.getMarca() != null ? produto.getMarca().getId() : null);
            stmt.setInt(5, produto.getSubcategoria() != null ? produto.getSubcategoria().getId() : null);
            stmt.setString(6, produto.getUnidade());
            stmt.setInt(7, produto.getCategoria() != null ? produto.getCategoria().getId() : null);
            stmt.setString(8, produto.getCodigoBarra());
            stmt.setString(9, produto.getCodigo());
            stmt.setString(10, produto.getCor());
            stmt.setString(11, produto.getTamanho());
            stmt.setBigDecimal(12, produto.getCusto());
            stmt.setBigDecimal(13, produto.getPreco());
            stmt.setString(14, produto.getCfop());
            stmt.setBigDecimal(15, produto.getIcms());
            stmt.setBigDecimal(16, produto.getIcmsSub());
            stmt.setInt(17, produto.getEstoqueAtual());
            stmt.setInt(18, produto.getEstoqueMinimo());
            stmt.setBoolean(19, produto.isAtivo());
            
            // Converter LocalDateTime para Timestamp
            stmt.setTimestamp(20, Timestamp.valueOf(produto.getDataCadastro()));
            stmt.setTimestamp(21, Timestamp.valueOf(produto.getDataAtualizacao()));
            
            // Converter LocalDate para Date
            stmt.setDate(22, produto.getDataVencimento() != null ? 
                           java.sql.Date.valueOf(produto.getDataVencimento()) : null);
            
            // Executar a inserção
            stmt.executeUpdate();
            
            // Obter o ID gerado
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                produto.setId(rs.getInt(1));
            }
            
            return produto;
        }
    }
    
    /**
     * Busca um produto pelo código
     * @param codigo O código do produto
     * @return O produto encontrado ou null
     * @throws SQLException Em caso de erro no banco de dados
     */
    public Produto findByCodigo(String codigo) throws SQLException {
        String sql = "SELECT p.*, c.id as categoria_id, c.nome as categoria_nome, " +
                    "m.id as marca_id, m.nome as marca_nome, " +
                    "s.id as subcategoria_id, s.nome as subcategoria_nome " +
                    "FROM produtos p " +
                    "LEFT JOIN categorias c ON p.categoria_id = c.id " +
                    "LEFT JOIN marcas m ON p.marca_id = m.id " +
                    "LEFT JOIN subcategorias s ON p.subcategoria_id = s.id " +
                    "WHERE p.codigo = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, codigo);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToProduto(rs);
            }
            
            return null;
        }
    }
    
    /**
     * Atualiza um produto existente no banco de dados
     * @param produto O produto a ser atualizado
     * @return O produto atualizado
     * @throws SQLException Em caso de erro no banco de dados
     */
    
    public Produto update(Produto produto) throws SQLException {
        String sql = "UPDATE produtos SET nome = ?, descricao = ?, tipo = ?, marca_id = ?, " +
                "subcategoria_id = ?, unidade = ?, categoria_id = ?, codigo_barra = ?, codigo = ?, " +
                "cor = ?, tamanho = ?, custo = ?, preco = ?, cfop = ?, icms = ?, icms_sub = ?, " +
                "estoque_atual = ?, estoque_minimo = ?, ativo = ?, data_atualizacao = ?, data_vencimento = ? " +
                "WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            // Definir os parâmetros
            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setString(3, produto.getTipo());
            stmt.setInt(4, produto.getMarca() != null ? produto.getMarca().getId() : null);
            stmt.setInt(5, produto.getSubcategoria() != null ? produto.getSubcategoria().getId() : null);
            stmt.setString(6, produto.getUnidade());
            stmt.setInt(7, produto.getCategoria() != null ? produto.getCategoria().getId() : null);
            stmt.setString(8, produto.getCodigoBarra());
            stmt.setString(9, produto.getCodigo());
            stmt.setString(10, produto.getCor());
            stmt.setString(11, produto.getTamanho());
            stmt.setBigDecimal(12, produto.getCusto());
            stmt.setBigDecimal(13, produto.getPreco());
            stmt.setString(14, produto.getCfop());
            stmt.setBigDecimal(15, produto.getIcms());
            stmt.setBigDecimal(16, produto.getIcmsSub());
            stmt.setInt(17, produto.getEstoqueAtual());
            stmt.setInt(18, produto.getEstoqueMinimo());
            stmt.setBoolean(19, produto.isAtivo());
            
            // Converter LocalDateTime para Timestamp
            stmt.setTimestamp(20, Timestamp.valueOf(LocalDateTime.now()));
            
            // Converter LocalDate para Date
            stmt.setDate(21, produto.getDataVencimento() != null ? 
                           java.sql.Date.valueOf(produto.getDataVencimento()) : null);
            
            // ID do produto
            stmt.setInt(22, produto.getId());
            
            // Executar a atualização
            stmt.executeUpdate();
            
            return produto;
        }
    }
    
    /**
     * Busca um produto pelo ID usando uma conexão específica
     * @param id O ID do produto
     * @param connection A conexão a ser usada
     * @return O produto encontrado ou null
     * @throws SQLException Em caso de erro no banco de dados
     */
    public Produto findById(Integer id, Connection connection) throws SQLException {
        String sql = "SELECT p.*, c.id as categoria_id, c.nome as categoria_nome, " +
                    "m.id as marca_id, m.nome as marca_nome, " +
                    "s.id as subcategoria_id, s.nome as subcategoria_nome " +
                    "FROM produtos p " +
                    "LEFT JOIN categorias c ON p.categoria_id = c.id " +
                    "LEFT JOIN marcas m ON p.marca_id = m.id " +
                    "LEFT JOIN subcategorias s ON p.subcategoria_id = s.id " +
                    "WHERE p.id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToProduto(rs);
            }
            
            return null;
        }
    }
    
    /**
     * Busca um produto pelo ID usando a conexão padrão
     * @param id O ID do produto
     * @return O produto encontrado ou null
     * @throws SQLException Em caso de erro no banco de dados
     */
    public Produto findById(Integer id) throws SQLException {
        return findById(id, this.connection);
    }
    
    
    /**
     * Retorna a conexão utilizada por este DAO
     * @return A conexão com o banco de dados
     */
    public Connection getConnection() {
        return this.connection;
    }

    /**
     * Atualiza um produto existente no banco de dados usando uma conexão específica
     * @param produto O produto a ser atualizado
     * @param connection A conexão a ser usada
     * @return O produto atualizado
     * @throws SQLException Em caso de erro no banco de dados
     */
    public Produto update(Produto produto, Connection connection) throws SQLException {
        String sql = "UPDATE produtos SET nome = ?, descricao = ?, tipo = ?, marca_id = ?, " +
                "subcategoria_id = ?, unidade = ?, categoria_id = ?, codigo_barra = ?, codigo = ?, " +
                "cor = ?, tamanho = ?, custo = ?, preco = ?, cfop = ?, icms = ?, icms_sub = ?, " +
                "estoque_atual = ?, estoque_minimo = ?, ativo = ?, data_atualizacao = ?, data_vencimento = ? " +
                "WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            // Definir os parâmetros
            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setString(3, produto.getTipo());
            stmt.setObject(4, produto.getMarca() != null ? produto.getMarca().getId() : null);
            stmt.setObject(5, produto.getSubcategoria() != null ? produto.getSubcategoria().getId() : null);
            stmt.setString(6, produto.getUnidade());
            stmt.setObject(7, produto.getCategoria() != null ? produto.getCategoria().getId() : null);
            stmt.setString(8, produto.getCodigoBarra());
            stmt.setString(9, produto.getCodigo());
            stmt.setString(10, produto.getCor());
            stmt.setString(11, produto.getTamanho());
            stmt.setBigDecimal(12, produto.getCusto());
            stmt.setBigDecimal(13, produto.getPreco());
            stmt.setString(14, produto.getCfop());
            stmt.setBigDecimal(15, produto.getIcms());
            stmt.setBigDecimal(16, produto.getIcmsSub());
            stmt.setInt(17, produto.getEstoqueAtual());
            stmt.setInt(18, produto.getEstoqueMinimo());
            stmt.setBoolean(19, produto.isAtivo());
            
            // Converter LocalDateTime para Timestamp
            stmt.setTimestamp(20, Timestamp.valueOf(LocalDateTime.now()));
            
            // Converter LocalDate para Date
            if (produto.getDataVencimento() != null) {
                stmt.setDate(21, java.sql.Date.valueOf(produto.getDataVencimento()));
            } else {
                stmt.setNull(21, Types.DATE);
            }
            
            // ID do produto
            stmt.setInt(22, produto.getId());
            
            // Executar a atualização
            stmt.executeUpdate();
            
            return produto;
        }
    }
    
    /**
     * Busca todos os produtos
     * @return Lista de todos os produtos
     * @throws SQLException Em caso de erro no banco de dados
     */
    public List<Produto> findAll() throws SQLException {
        String sql = "SELECT p.*, c.id as categoria_id, c.nome as categoria_nome, " +
                    "m.id as marca_id, m.nome as marca_nome, " +
                    "s.id as subcategoria_id, s.nome as subcategoria_nome " +
                    "FROM produtos p " +
                    "LEFT JOIN categorias c ON p.categoria_id = c.id " +
                    "LEFT JOIN marcas m ON p.marca_id = m.id " +
                    "LEFT JOIN subcategorias s ON p.subcategoria_id = s.id " +
                    "ORDER BY p.nome";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            
            List<Produto> produtos = new ArrayList<>();
            
            while (rs.next()) {
                produtos.add(mapResultSetToProduto(rs));
            }
            
            return produtos;
        }
    }
    
    /**
     * Busca todos os produtos ativos
     * @return Lista de produtos ativos
     * @throws SQLException Em caso de erro no banco de dados
     */
    public List<Produto> findAllAtivos() throws SQLException {
        String sql = "SELECT p.*, c.id as categoria_id, c.nome as categoria_nome, " +
                    "m.id as marca_id, m.nome as marca_nome, " +
                    "s.id as subcategoria_id, s.nome as subcategoria_nome " +
                    "FROM produtos p " +
                    "LEFT JOIN categorias c ON p.categoria_id = c.id " +
                    "LEFT JOIN marcas m ON p.marca_id = m.id " +
                    "LEFT JOIN subcategorias s ON p.subcategoria_id = s.id " +
                    "WHERE p.ativo = true " +
                    "ORDER BY p.nome";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            
            List<Produto> produtos = new ArrayList<>();
            
            while (rs.next()) {
                produtos.add(mapResultSetToProduto(rs));
            }
            
            return produtos;
        }
    }
    
    /**
     * Busca produtos pelo nome ou parte do nome
     * @param nome O nome ou parte do nome a ser buscado
     * @return Lista de produtos que correspondem à busca
     * @throws SQLException Em caso de erro no banco de dados
     */
    public List<Produto> findByNome(String nome) throws SQLException {
        String sql = "SELECT p.*, c.id as categoria_id, c.nome as categoria_nome, " +
                    "m.id as marca_id, m.nome as marca_nome, " +
                    "s.id as subcategoria_id, s.nome as subcategoria_nome " +
                    "FROM produtos p " +
                    "LEFT JOIN categorias c ON p.categoria_id = c.id " +
                    "LEFT JOIN marcas m ON p.marca_id = m.id " +
                    "LEFT JOIN subcategorias s ON p.subcategoria_id = s.id " +
                    "WHERE p.ativo = true AND p.nome LIKE ? " +
                    "ORDER BY p.nome";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + nome + "%");
            
            ResultSet rs = stmt.executeQuery();
            
            List<Produto> produtos = new ArrayList<>();
            
            while (rs.next()) {
                produtos.add(mapResultSetToProduto(rs));
            }
            
            return produtos;
        }
    }
    
    /**
     * Busca todos os produtos com estoque abaixo do mínimo
     * 
     * @return Lista de produtos com estoque abaixo do mínimo
     * @throws SQLException Em caso de erro no banco de dados
     */
    public List<Produto> findWithLowStock() throws SQLException {
        String sql = "SELECT p.*, c.id as categoria_id, c.nome as categoria_nome, " +
                    "m.id as marca_id, m.nome as marca_nome, " +
                    "s.id as subcategoria_id, s.nome as subcategoria_nome " +
                    "FROM produtos p " +
                    "LEFT JOIN categorias c ON p.categoria_id = c.id " +
                    "LEFT JOIN marcas m ON p.marca_id = m.id " +
                    "LEFT JOIN subcategorias s ON p.subcategoria_id = s.id " +
                    "WHERE p.ativo = true AND p.estoque_atual < p.estoque_minimo " +
                    "ORDER BY p.nome";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            
            List<Produto> produtos = new ArrayList<>();
            
            while (rs.next()) {
                produtos.add(mapResultSetToProduto(rs));
            }
            
            return produtos;
        }
    }
    
    
    /**
     * Conta o número total de produtos ativos no sistema
     * 
     * @return O número de produtos ativos
     * @throws SQLException Em caso de erro no banco de dados
     */
    public int contarProdutosAtivos() throws SQLException {
        String sql = "SELECT COUNT(*) FROM produtos WHERE ativo = true";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
            return 0;
        }
    }

    /**
     * Calcula a quantidade total de itens em estoque (soma do estoque de todos os produtos)
     * 
     * @return O total de itens em estoque
     * @throws SQLException Em caso de erro no banco de dados
     */
    public int calcularTotalItensEstoque() throws SQLException {
        String sql = "SELECT SUM(estoque_atual) FROM produtos WHERE ativo = true";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
            return 0;
        }
    }

    /**
     * Conta a quantidade de produtos com estoque abaixo do mínimo
     * 
     * @return O número de produtos com estoque baixo
     * @throws SQLException Em caso de erro no banco de dados
     */
    public int contarProdutosComEstoqueBaixo() throws SQLException {
        String sql = "SELECT COUNT(*) FROM produtos WHERE ativo = true AND estoque_atual < estoque_minimo";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
            return 0;
        }
    }
    
    
    /**
     * Exclui um produto pelo ID
     * 
     * @param id O ID do produto a ser excluído
     * @throws SQLException Em caso de erro no banco de dados
     */
    
    public void  delete(Integer id) throws SQLException {
        // Na prática, muitas vezes não excluímos realmente os registros, apenas marcamos como inativos
        String sql = "UPDATE produtos SET ativo = false, data_atualizacao = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            // Definir a data de atualização como agora
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, id);
            
            // Executar a atualização
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected == 0) {
                throw new SQLException("Não foi possível excluir o produto com ID " + id + ". Produto não encontrado.");
            }
        }
    }
    
    /**
     * Mapeia um ResultSet para um objeto Produto
     * @param rs O ResultSet com os dados do produto
     * @return O objeto Produto mapeado
     * @throws SQLException Em caso de erro ao ler os dados
     */
    private Produto mapResultSetToProduto(ResultSet rs) throws SQLException {
        Produto produto = new Produto();
        
        produto.setId(rs.getInt("id"));
        produto.setNome(rs.getString("nome"));
        produto.setDescricao(rs.getString("descricao"));
        produto.setTipo(rs.getString("tipo"));
        produto.setUnidade(rs.getString("unidade"));
        produto.setCodigoBarra(rs.getString("codigo_barra"));
        produto.setCodigo(rs.getString("codigo"));
        produto.setCor(rs.getString("cor"));
        produto.setTamanho(rs.getString("tamanho"));
        produto.setCusto(rs.getBigDecimal("custo"));
        produto.setPreco(rs.getBigDecimal("preco"));
        produto.setCfop(rs.getString("cfop"));
        produto.setIcms(rs.getBigDecimal("icms"));
        produto.setIcmsSub(rs.getBigDecimal("icms_sub"));
        produto.setEstoqueAtual(rs.getInt("estoque_atual"));
        produto.setEstoqueMinimo(rs.getInt("estoque_minimo"));
        produto.setAtivo(rs.getBoolean("ativo"));
        
        // Converter Timestamp para LocalDateTime
        Timestamp dataCadastro = rs.getTimestamp("data_cadastro");
        if (dataCadastro != null) {
            produto.setDataCadastro(dataCadastro.toLocalDateTime());
        }
        
        Timestamp dataAtualizacao = rs.getTimestamp("data_atualizacao");
        if (dataAtualizacao != null) {
            produto.setDataAtualizacao(dataAtualizacao.toLocalDateTime());
        }
        
        // Converter Date para LocalDate
        Date dataVencimento = rs.getDate("data_vencimento");
        if (dataVencimento != null) {
            produto.setDataVencimento(dataVencimento.toLocalDate());
        }
        
        // Mapear relacionamentos
        try {
            // Categoria
            int categoriaId = rs.getInt("categoria_id");
            if (!rs.wasNull()) {
                Categoria categoria = new Categoria();
                categoria.setId(categoriaId);
                categoria.setNome(rs.getString("categoria_nome"));
                produto.setCategoria(categoria);
            }
            
            // Marca
            int marcaId = rs.getInt("marca_id");
            if (!rs.wasNull()) {
                Marca marca = new Marca();
                marca.setId(marcaId);
                marca.setNome(rs.getString("marca_nome"));
                produto.setMarca(marca);
            }
            
            // Subcategoria
            int subcategoriaId = rs.getInt("subcategoria_id");
            if (!rs.wasNull()) {
                Subcategoria subcategoria = new Subcategoria();
                subcategoria.setId(subcategoriaId);
                subcategoria.setNome(rs.getString("subcategoria_nome"));
                produto.setSubcategoria(subcategoria);
            }
        } catch (SQLException e) {
            // Pode ocorrer se alguma coluna de relacionamento não estiver no ResultSet
            // Ignoramos e retornamos o produto sem aquele relacionamento
        }
        
        
        
        return produto;
    }
}