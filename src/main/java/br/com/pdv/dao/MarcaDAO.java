package br.com.pdv.dao;

import br.com.pdv.model.Marca;
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

public class MarcaDAO {

    private static final Logger logger = Logger.getLogger(MarcaDAO.class.getName());
    private static MarcaDAO instance;
    private final DatabaseConnection databaseConnection;
    
    private static final String SQL_INSERT = "INSERT INTO marcas (nome, descricao, ativo, data_cadastro, data_atualizacao) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM marcas WHERE id = ?";
    private static final String SQL_SELECT_ALL = "SELECT * FROM marcas";
    private static final String SQL_UPDATE = "UPDATE marcas SET nome = ?, descricao = ?, ativo = ?, data_atualizacao = ? WHERE id = ?";
    private static final String SQL_DELETE = "DELETE FROM marcas WHERE id = ?";
    
    private MarcaDAO() {
        this.databaseConnection = DatabaseConnection.getInstance();
    }
    
    public static synchronized MarcaDAO getInstance() {
        if (instance == null) {
            instance = new MarcaDAO();
        }
        return instance;
    }
    
    public Marca create(Marca marca) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet generatedKeys = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setString(1, marca.getNome());
            stmt.setString(2, marca.getDescricao());
            stmt.setBoolean(3, marca.isAtivo());
            stmt.setTimestamp(4, Timestamp.valueOf(marca.getDataCadastro()));
            stmt.setTimestamp(5, Timestamp.valueOf(marca.getDataAtualizacao()));
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Falha ao criar marca, nenhuma linha afetada.");
            }
            
            generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                marca.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("Falha ao criar marca, nenhum ID obtido.");
            }
            
            return marca;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao criar marca: " + e.getMessage(), e);
            throw e;
        } finally {
            if (generatedKeys != null) try { generatedKeys.close(); } catch (SQLException e) { /* ignorado */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignorado */ }
            if (conn != null) try { conn.close(); } catch (SQLException e) { /* ignorado */ }
        }
    }
    
    public Marca findById(Integer id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_BY_ID);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToMarca(rs);
            } else {
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao buscar marca por ID: " + e.getMessage(), e);
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignorado */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignorado */ }
            if (conn != null) try { conn.close(); } catch (SQLException e) { /* ignorado */ }
        }
    }
    
    public List<Marca> findAll() throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_ALL);
            rs = stmt.executeQuery();
            
            List<Marca> marcas = new ArrayList<>();
            while (rs.next()) {
                marcas.add(mapResultSetToMarca(rs));
            }
            
            return marcas;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao listar marcas: " + e.getMessage(), e);
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignorado */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignorado */ }
            if (conn != null) try { conn.close(); } catch (SQLException e) { /* ignorado */ }
        }
    }
    
    public boolean update(Marca marca) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_UPDATE);
            
            marca.setDataAtualizacao(LocalDateTime.now());
            
            stmt.setString(1, marca.getNome());
            stmt.setString(2, marca.getDescricao());
            stmt.setBoolean(3, marca.isAtivo());
            stmt.setTimestamp(4, Timestamp.valueOf(marca.getDataAtualizacao()));
            stmt.setInt(5, marca.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao atualizar marca: " + e.getMessage(), e);
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
            logger.log(Level.SEVERE, "Erro ao excluir marca: " + e.getMessage(), e);
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignorado */ }
            if (conn != null) try { conn.close(); } catch (SQLException e) { /* ignorado */ }
        }
    }
    
    private Marca mapResultSetToMarca(ResultSet rs) throws SQLException {
        Marca marca = new Marca();
        
        marca.setId(rs.getInt("id"));
        marca.setNome(rs.getString("nome"));
        marca.setDescricao(rs.getString("descricao"));
        marca.setAtivo(rs.getBoolean("ativo"));
        
        Timestamp dataCadastro = rs.getTimestamp("data_cadastro");
        if (dataCadastro != null) {
            marca.setDataCadastro(dataCadastro.toLocalDateTime());
        }
        
        Timestamp dataAtualizacao = rs.getTimestamp("data_atualizacao");
        if (dataAtualizacao != null) {
            marca.setDataAtualizacao(dataAtualizacao.toLocalDateTime());
        }
        
        return marca;
    }
}