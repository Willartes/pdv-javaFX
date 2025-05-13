package br.com.pdv.dao;

import br.com.pdv.model.Categoria;
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

public class CategoriaDAO {

    private static final Logger logger = Logger.getLogger(CategoriaDAO.class.getName());
    private static CategoriaDAO instance;
    private final DatabaseConnection databaseConnection;
    
    private static final String SQL_INSERT = "INSERT INTO categorias (nome, descricao, ativo, data_cadastro, data_atualizacao) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM categorias WHERE id = ?";
    private static final String SQL_SELECT_ALL = "SELECT * FROM categorias";
    private static final String SQL_UPDATE = "UPDATE categorias SET nome = ?, descricao = ?, ativo = ?, data_atualizacao = ? WHERE id = ?";
    private static final String SQL_DELETE = "DELETE FROM categorias WHERE id = ?";
    
    private CategoriaDAO() {
        this.databaseConnection = DatabaseConnection.getInstance();
    }
    
    public static synchronized CategoriaDAO getInstance() {
        if (instance == null) {
            instance = new CategoriaDAO();
        }
        return instance;
    }
    
    public Categoria create(Categoria categoria) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet generatedKeys = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setString(1, categoria.getNome());
            stmt.setString(2, categoria.getDescricao());
            stmt.setBoolean(3, categoria.isAtivo());
            stmt.setTimestamp(4, Timestamp.valueOf(categoria.getDataCadastro()));
            stmt.setTimestamp(5, Timestamp.valueOf(categoria.getDataAtualizacao()));
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Falha ao criar categoria, nenhuma linha afetada.");
            }
            
            generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                categoria.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("Falha ao criar categoria, nenhum ID obtido.");
            }
            
            return categoria;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao criar categoria: " + e.getMessage(), e);
            throw e;
        } finally {
            if (generatedKeys != null) try { generatedKeys.close(); } catch (SQLException e) { /* ignorado */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignorado */ }
            if (conn != null) try { conn.close(); } catch (SQLException e) { /* ignorado */ }
        }
    }
    
    public Categoria findById(Integer id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_BY_ID);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToCategoria(rs);
            } else {
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao buscar categoria por ID: " + e.getMessage(), e);
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignorado */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignorado */ }
            if (conn != null) try { conn.close(); } catch (SQLException e) { /* ignorado */ }
        }
    }
    
    public List<Categoria> findAll() throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_ALL);
            rs = stmt.executeQuery();
            
            List<Categoria> categorias = new ArrayList<>();
            while (rs.next()) {
                categorias.add(mapResultSetToCategoria(rs));
            }
            
            return categorias;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao listar categorias: " + e.getMessage(), e);
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignorado */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignorado */ }
            if (conn != null) try { conn.close(); } catch (SQLException e) { /* ignorado */ }
        }
    }
    
    public boolean update(Categoria categoria) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_UPDATE);
            
            categoria.setDataAtualizacao(LocalDateTime.now());
            
            stmt.setString(1, categoria.getNome());
            stmt.setString(2, categoria.getDescricao());
            stmt.setBoolean(3, categoria.isAtivo());
            stmt.setTimestamp(4, Timestamp.valueOf(categoria.getDataAtualizacao()));
            stmt.setInt(5, categoria.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao atualizar categoria: " + e.getMessage(), e);
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
            logger.log(Level.SEVERE, "Erro ao excluir categoria: " + e.getMessage(), e);
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignorado */ }
            if (conn != null) try { conn.close(); } catch (SQLException e) { /* ignorado */ }
        }
    }
    
    private Categoria mapResultSetToCategoria(ResultSet rs) throws SQLException {
        Categoria categoria = new Categoria();
        
        categoria.setId(rs.getInt("id"));
        categoria.setNome(rs.getString("nome"));
        categoria.setDescricao(rs.getString("descricao"));
        categoria.setAtivo(rs.getBoolean("ativo"));
        
        Timestamp dataCadastro = rs.getTimestamp("data_cadastro");
        if (dataCadastro != null) {
            categoria.setDataCadastro(dataCadastro.toLocalDateTime());
        }
        
        Timestamp dataAtualizacao = rs.getTimestamp("data_atualizacao");
        if (dataAtualizacao != null) {
            categoria.setDataAtualizacao(dataAtualizacao.toLocalDateTime());
        }
        
        return categoria;
    }
}