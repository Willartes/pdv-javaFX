package br.com.pdv.dao;

import br.com.pdv.model.Categoria;
import br.com.pdv.model.Subcategoria;
import br.com.pdv.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SubcategoriaDAO {

    private static final Logger logger = Logger.getLogger(SubcategoriaDAO.class.getName());
    private static SubcategoriaDAO instance;
    private final DatabaseConnection databaseConnection;
    private final CategoriaDAO categoriaDAO;
    
    private static final String SQL_INSERT = "INSERT INTO subcategorias (nome, descricao, categoria_id, ativo, data_cadastro, data_atualizacao) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM subcategorias WHERE id = ?";
    private static final String SQL_SELECT_ALL = "SELECT * FROM subcategorias";
    private static final String SQL_SELECT_BY_CATEGORIA = "SELECT * FROM subcategorias WHERE categoria_id = ?";
    private static final String SQL_UPDATE = "UPDATE subcategorias SET nome = ?, descricao = ?, categoria_id = ?, ativo = ?, data_atualizacao = ? WHERE id = ?";
    private static final String SQL_DELETE = "DELETE FROM subcategorias WHERE id = ?";
    
    private SubcategoriaDAO() {
        this.databaseConnection = DatabaseConnection.getInstance();
        this.categoriaDAO = CategoriaDAO.getInstance();
    }
    
    public static synchronized SubcategoriaDAO getInstance() {
        if (instance == null) {
            instance = new SubcategoriaDAO();
        }
        return instance;
    }
    
    public Subcategoria create(Subcategoria subcategoria) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet generatedKeys = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setString(1, subcategoria.getNome());
            stmt.setString(2, subcategoria.getDescricao());
            
            // Se a categoria for definida, use seu ID
            if (subcategoria.getCategoria() != null && subcategoria.getCategoria().getId() != null) {
                stmt.setInt(3, subcategoria.getCategoria().getId());
            } else {
                stmt.setNull(3, java.sql.Types.INTEGER);
            }
            
            stmt.setBoolean(4, subcategoria.isAtivo());
            stmt.setTimestamp(5, Timestamp.valueOf(subcategoria.getDataCadastro()));
            stmt.setTimestamp(6, Timestamp.valueOf(subcategoria.getDataAtualizacao()));
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Falha ao criar subcategoria, nenhuma linha afetada.");
            }
            
            generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                subcategoria.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("Falha ao criar subcategoria, nenhum ID obtido.");
            }
            
            return subcategoria;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao criar subcategoria: " + e.getMessage(), e);
            throw e;
        } finally {
            if (generatedKeys != null) try { generatedKeys.close(); } catch (SQLException e) { /* ignorado */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignorado */ }
            if (conn != null) try { conn.close(); } catch (SQLException e) { /* ignorado */ }
        }
    }
    
    public Subcategoria findById(Integer id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_BY_ID);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToSubcategoria(rs);
            } else {
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao buscar subcategoria por ID: " + e.getMessage(), e);
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignorado */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignorado */ }
            if (conn != null) try { conn.close(); } catch (SQLException e) { /* ignorado */ }
        }
    }
    
    public List<Subcategoria> findAll() throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_ALL);
            rs = stmt.executeQuery();
            
            List<Subcategoria> subcategorias = new ArrayList<>();
            while (rs.next()) {
                subcategorias.add(mapResultSetToSubcategoria(rs));
            }
            
            return subcategorias;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao listar subcategorias: " + e.getMessage(), e);
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignorado */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignorado */ }
            if (conn != null) try { conn.close(); } catch (SQLException e) { /* ignorado */ }
        }
    }
    
    public List<Subcategoria> findByCategoria(Categoria categoria) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_BY_CATEGORIA);
            stmt.setInt(1, categoria.getId());
            rs = stmt.executeQuery();
            
            List<Subcategoria> subcategorias = new ArrayList<>();
            while (rs.next()) {
                subcategorias.add(mapResultSetToSubcategoria(rs));
            }
            
            return subcategorias;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao listar subcategorias por categoria: " + e.getMessage(), e);
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignorado */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignorado */ }
            if (conn != null) try { conn.close(); } catch (SQLException e) { /* ignorado */ }
        }
    }
    
    public boolean update(Subcategoria subcategoria) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_UPDATE);
            
            subcategoria.setDataAtualizacao(LocalDateTime.now());
            
            stmt.setString(1, subcategoria.getNome());
            stmt.setString(2, subcategoria.getDescricao());
            
            // Se a categoria for definida, use seu ID
            if (subcategoria.getCategoria() != null && subcategoria.getCategoria().getId() != null) {
                stmt.setInt(3, subcategoria.getCategoria().getId());
            } else {
                stmt.setNull(3, java.sql.Types.INTEGER);
            }
            
            stmt.setBoolean(4, subcategoria.isAtivo());
            stmt.setTimestamp(5, Timestamp.valueOf(subcategoria.getDataAtualizacao()));
            stmt.setInt(6, subcategoria.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao atualizar subcategoria: " + e.getMessage(), e);
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignorado */ }
            if (conn != null) try { conn.close(); } catch (SQLException e) { /* ignorado */ }
        }
    }
    
    public boolean delete(Integer id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_DELETE);
            stmt.setInt(1, id);
            
            int affectedRows = stmt.executeUpdate();
            
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao excluir subcategoria: " + e.getMessage(), e);
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignorado */ }
            if (conn != null) try { conn.close(); } catch (SQLException e) { /* ignorado */ }
        }
    }
    
    private Subcategoria mapResultSetToSubcategoria(ResultSet rs) throws SQLException {
        Subcategoria subcategoria = new Subcategoria();
        
        subcategoria.setId(rs.getInt("id"));
        subcategoria.setNome(rs.getString("nome"));
        subcategoria.setDescricao(rs.getString("descricao"));
        subcategoria.setAtivo(rs.getBoolean("ativo"));
        
        // Buscar a categoria pelo ID
        Integer categoriaId = rs.getInt("categoria_id");
        if (!rs.wasNull()) {
            try {
                Categoria categoria = categoriaDAO.findById(categoriaId);
                subcategoria.setCategoria(categoria);
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Erro ao buscar categoria da subcategoria: " + e.getMessage(), e);
            }
        }
        
        Timestamp dataCadastro = rs.getTimestamp("data_cadastro");
        if (dataCadastro != null) {
            subcategoria.setDataCadastro(dataCadastro.toLocalDateTime());
        }
        
        Timestamp dataAtualizacao = rs.getTimestamp("data_atualizacao");
        if (dataAtualizacao != null) {
            subcategoria.setDataAtualizacao(dataAtualizacao.toLocalDateTime());
        }
        
        return subcategoria;
    }
}