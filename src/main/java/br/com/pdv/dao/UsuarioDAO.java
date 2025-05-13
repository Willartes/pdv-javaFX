package br.com.pdv.dao;

import br.com.pdv.model.Usuario;
import br.com.pdv.util.DatabaseConnection;
import br.com.pdv.util.LogUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {
    private static UsuarioDAO instance;
    private final DatabaseConnection databaseConnection;
    
    private static final String SQL_INSERT = 
        "INSERT INTO usuarios (nome, login, senha, perfil, ativo, data_cadastro, data_atualizacao) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
    private static final String SQL_UPDATE = 
        "UPDATE usuarios SET nome = ?, login = ?, senha = ?, " +
        "perfil = ?, ativo = ?, data_atualizacao = ? WHERE id = ?";
        
    private static final String SQL_DELETE = "DELETE FROM usuarios WHERE id = ?";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM usuarios WHERE id = ?";
    private static final String SQL_SELECT_BY_LOGIN = "SELECT * FROM usuarios WHERE login = ?";
    private static final String SQL_SELECT_ALL = "SELECT * FROM usuarios ORDER BY nome";
    private static final String SQL_EXISTS = "SELECT 1 FROM usuarios WHERE id = ?";
    private static final String SQL_COUNT = "SELECT COUNT(*) FROM usuarios";
    private static final String SQL_AUTENTICAR = 
        "SELECT 1 FROM usuarios WHERE login = ? AND senha = ? AND ativo = true";
    
    private UsuarioDAO() {
        this.databaseConnection = DatabaseConnection.getInstance();
    }
    
    public static synchronized UsuarioDAO getInstance() {
        if (instance == null) {
            instance = new UsuarioDAO();
        }
        return instance;
    }

    public Usuario create(Usuario usuario) throws SQLException {
        LogUtil.info(UsuarioDAO.class, "Iniciando criação de usuário: " + usuario.getLogin());
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getLogin());
            stmt.setString(3, usuario.getSenha());
            stmt.setString(4, usuario.getPerfil());
            stmt.setBoolean(5, usuario.isAtivo());
            stmt.setObject(6, usuario.getDataCadastro());
            stmt.setObject(7, LocalDateTime.now());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                SQLException e = new SQLException("Falha ao criar usuário, nenhuma linha afetada.");
                LogUtil.error(UsuarioDAO.class, "Falha ao criar usuário: nenhuma linha afetada", e);
                throw e;
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    usuario.setId(generatedKeys.getInt(1));
                    LogUtil.info(UsuarioDAO.class, "Usuário criado com sucesso. ID: " + usuario.getId());
                    return usuario;
                } else {
                    SQLException e = new SQLException("Falha ao criar usuário, nenhum ID obtido.");
                    LogUtil.error(UsuarioDAO.class, "Falha ao criar usuário: ID não gerado", e);
                    throw e;
                }
            }
        } catch (SQLException e) {
            LogUtil.error(UsuarioDAO.class, "Erro ao criar usuário: " + e.getMessage(), e);
            throw e;
        }
    }

    public Usuario findById(Integer id) throws SQLException {
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BY_ID)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Usuario usuario = createUsuarioFromResultSet(rs);
                    LogUtil.info(UsuarioDAO.class, "Usuário encontrado - ID: " + id);
                    return usuario;
                }
                LogUtil.info(UsuarioDAO.class, "Nenhum usuário encontrado com ID: " + id);
                return null;
            }
        } catch (SQLException e) {
            LogUtil.error(UsuarioDAO.class, "Erro ao buscar usuário por ID: " + e.getMessage(), e);
            throw e;
        }
    }
    
    
    /**
     * Busca usuários por perfil
     * 
     * @param perfil O perfil a ser buscado (ex: "VENDEDOR", "ADMIN")
     * @return Lista de usuários com o perfil especificado
     * @throws SQLException Se ocorrer um erro de SQL
     */
    public List<Usuario> findByPerfil(String perfil) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Usuario> usuarios = new ArrayList<>();
        
        try {
            conn = databaseConnection.getConnection();
            
            String sql = "SELECT * FROM usuarios WHERE perfil = ? AND ativo = true ORDER BY nome";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, perfil);
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setId(rs.getInt("id"));
                usuario.setNome(rs.getString("nome"));
                usuario.setLogin(rs.getString("login"));
                usuario.setSenha(rs.getString("senha"));
                usuario.setPerfil(rs.getString("perfil"));
                usuario.setAtivo(rs.getBoolean("ativo"));
                
                // Verificar se existem as colunas de data
                try {
                    if (rs.getTimestamp("data_cadastro") != null) {
                        usuario.setDataCadastro(rs.getTimestamp("data_cadastro").toLocalDateTime());
                    }
                    
                    if (rs.getTimestamp("data_atualizacao") != null) {
                        usuario.setDataAtualizacao(rs.getTimestamp("data_atualizacao").toLocalDateTime());
                    }
                } catch (SQLException e) {
                    // Ignora se as colunas não existirem
                }
                
                usuarios.add(usuario);
            }
            
            return usuarios;
        } finally {
            // Garantir que todos os recursos sejam fechados
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { /* Ignorar */ }
            }
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) { /* Ignorar */ }
            }
            if (conn != null) {
                databaseConnection.releaseConnection(conn);
            }
        }
    }
    
    /**
     * Busca um usuário pelo nome
     * 
     * @param nome Nome do usuário a ser buscado
     * @return Usuário encontrado ou null se não existir
     * @throws SQLException Se ocorrer um erro de SQL
     */
    public Usuario findByNome(String nome) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            
            String sql = "SELECT * FROM usuarios WHERE nome = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nome);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setId(rs.getInt("id"));
                usuario.setNome(rs.getString("nome"));
                usuario.setLogin(rs.getString("login"));
                usuario.setSenha(rs.getString("senha"));
                usuario.setPerfil(rs.getString("perfil"));
                usuario.setAtivo(rs.getBoolean("ativo"));
                
                // Verificar se existem as colunas de data
                try {
                    if (rs.getTimestamp("data_cadastro") != null) {
                        usuario.setDataCadastro(rs.getTimestamp("data_cadastro").toLocalDateTime());
                    }
                    
                    if (rs.getTimestamp("data_atualizacao") != null) {
                        usuario.setDataAtualizacao(rs.getTimestamp("data_atualizacao").toLocalDateTime());
                    }
                } catch (SQLException e) {
                    // Ignora se as colunas não existirem
                }
                
                return usuario;
            }
            
            return null;
        } finally {
            // Garantir que todos os recursos sejam fechados
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { /* Ignorar */ }
            }
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) { /* Ignorar */ }
            }
            if (conn != null) {
                databaseConnection.releaseConnection(conn);
            }
        }
    }

    /**
     * Busca um usuário pelo nome usando padrão LIKE para busca parcial
     * 
     * @param nomeParcial Parte do nome do usuário a ser buscado
     * @return Lista de usuários encontrados
     * @throws SQLException Se ocorrer um erro de SQL
     */
    public List<Usuario> findByNomeLike(String nomeParcial) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Usuario> usuarios = new ArrayList<>();
        
        try {
            conn = databaseConnection.getConnection();
            
            String sql = "SELECT * FROM usuarios WHERE nome LIKE ? ORDER BY nome";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + nomeParcial + "%");
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setId(rs.getInt("id"));
                usuario.setNome(rs.getString("nome"));
                usuario.setLogin(rs.getString("login"));
                usuario.setSenha(rs.getString("senha"));
                usuario.setPerfil(rs.getString("perfil"));
                usuario.setAtivo(rs.getBoolean("ativo"));
                
                // Verificar se existem as colunas de data
                try {
                    if (rs.getTimestamp("data_cadastro") != null) {
                        usuario.setDataCadastro(rs.getTimestamp("data_cadastro").toLocalDateTime());
                    }
                    
                    if (rs.getTimestamp("data_atualizacao") != null) {
                        usuario.setDataAtualizacao(rs.getTimestamp("data_atualizacao").toLocalDateTime());
                    }
                } catch (SQLException e) {
                    // Ignora se as colunas não existirem
                }
                
                usuarios.add(usuario);
            }
            
            return usuarios;
        } finally {
            // Garantir que todos os recursos sejam fechados
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { /* Ignorar */ }
            }
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) { /* Ignorar */ }
            }
            if (conn != null) {
                databaseConnection.releaseConnection(conn);
            }
        }
    }

    public Usuario findByLogin(String login) throws SQLException {
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BY_LOGIN)) {
            
            stmt.setString(1, login);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Usuario usuario = createUsuarioFromResultSet(rs);
                    LogUtil.info(UsuarioDAO.class, "Usuário encontrado - Login: " + login);
                    return usuario;
                }
                LogUtil.info(UsuarioDAO.class, "Nenhum usuário encontrado com login: " + login);
                return null;
            }
        } catch (SQLException e) {
            LogUtil.error(UsuarioDAO.class, "Erro ao buscar usuário por login: " + e.getMessage(), e);
            throw e;
        }
    }

    public List<Usuario> findAll() throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                usuarios.add(createUsuarioFromResultSet(rs));
            }
            LogUtil.info(UsuarioDAO.class, "Total de usuários encontrados: " + usuarios.size());
        } catch (SQLException e) {
            LogUtil.error(UsuarioDAO.class, "Erro ao listar todos os usuários: " + e.getMessage(), e);
            throw e;
        }
        return usuarios;
    }

    public boolean update(Usuario usuario) throws SQLException {
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            
            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getLogin());
            stmt.setString(3, usuario.getSenha());
            stmt.setString(4, usuario.getPerfil());
            stmt.setBoolean(5, usuario.isAtivo());
            stmt.setObject(6, LocalDateTime.now());
            stmt.setInt(7, usuario.getId());
            
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) {
                LogUtil.info(UsuarioDAO.class, "Usuário atualizado com sucesso. ID: " + usuario.getId());
            } else {
                LogUtil.warn(UsuarioDAO.class, "Nenhum usuário atualizado para o ID: " + usuario.getId());
            }
            return updated;
        } catch (SQLException e) {
            LogUtil.error(UsuarioDAO.class, "Erro ao atualizar usuário: " + e.getMessage(), e);
            throw e;
        }
    }

    public boolean delete(Integer id) throws SQLException {
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            
            stmt.setInt(1, id);
            
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) {
                LogUtil.info(UsuarioDAO.class, "Usuário excluído com sucesso. ID: " + id);
            } else {
                LogUtil.warn(UsuarioDAO.class, "Nenhum usuário excluído para o ID: " + id);
            }
            return deleted;
        } catch (SQLException e) {
            LogUtil.error(UsuarioDAO.class, "Erro ao excluir usuário: " + e.getMessage(), e);
            throw e;
        }
    }

    public boolean exists(Integer id) throws SQLException {
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_EXISTS)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                boolean exists = rs.next();
                LogUtil.info(UsuarioDAO.class, 
                    String.format("Verificação de existência do usuário ID %d: %s", id, exists));
                return exists;
            }
        } catch (SQLException e) {
            LogUtil.error(UsuarioDAO.class, "Erro ao verificar existência do usuário: " + e.getMessage(), e);
            throw e;
        }
    }

    public long count() throws SQLException {
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_COUNT);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                long count = rs.getLong(1);
                LogUtil.info(UsuarioDAO.class, "Total de usuários cadastrados: " + count);
                return count;
            }
            return 0;
        } catch (SQLException e) {
            LogUtil.error(UsuarioDAO.class, "Erro ao contar usuários: " + e.getMessage(), e);
            throw e;
        }
    }

    public boolean autenticar(String login, String senha) throws SQLException {
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_AUTENTICAR)) {
            
            stmt.setString(1, login);
            stmt.setString(2, senha);
            
            try (ResultSet rs = stmt.executeQuery()) {
                boolean autenticado = rs.next();
                if (autenticado) {
                    LogUtil.info(UsuarioDAO.class, "Autenticação bem-sucedida para o login: " + login);
                } else {
                    LogUtil.warn(UsuarioDAO.class, "Falha na autenticação para o login: " + login);
                }
                return autenticado;
            }
        } catch (SQLException e) {
            LogUtil.error(UsuarioDAO.class, "Erro ao autenticar usuário: " + e.getMessage(), e);
            throw e;
        }
    }

    private Usuario createUsuarioFromResultSet(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("id"));
        usuario.setNome(rs.getString("nome"));
        usuario.setLogin(rs.getString("login"));
        usuario.setSenha(rs.getString("senha"));
        usuario.setPerfil(rs.getString("perfil"));
        usuario.setAtivo(rs.getBoolean("ativo"));
        usuario.setDataCadastro(rs.getObject("data_cadastro", LocalDateTime.class));
        usuario.setDataAtualizacao(rs.getObject("data_atualizacao", LocalDateTime.class));
        return usuario;
    }
}