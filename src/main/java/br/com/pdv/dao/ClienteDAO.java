package br.com.pdv.dao;

import br.com.pdv.model.Cliente;
import br.com.pdv.exception.CpfCnpjDuplicadoException;
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
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class ClienteDAO {
    private static ClienteDAO instance;
    private final DatabaseConnection databaseConnection;
    
    // SQL statements
    private static final String SQL_INSERT = 
    	    "INSERT INTO clientes (nome, cpf_cnpj, endereco, telefone, email, data_nascimento, observacao, data_cadastro) " +
    	    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = 
    	    "UPDATE clientes SET nome = ?, cpf_cnpj = ?, endereco = ?, telefone = ?, email = ?, data_cadastro = ?" +
    	    "data_nascimento = ?, observacao = ? WHERE id = ?";
    private static final String SQL_DELETE = "DELETE FROM clientes WHERE id = ?";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM clientes WHERE id = ?";
    private static final String SQL_SELECT_ALL = "SELECT * FROM clientes ORDER BY nome";
    private static final String SQL_SELECT_BY_CPF_CNPJ = "SELECT * FROM clientes WHERE cpf_cnpj = ?";
    private static final String SQL_SELECT_BY_NOME = "SELECT * FROM clientes WHERE nome = ?";
    
    private ClienteDAO() {
        this.databaseConnection = DatabaseConnection.getInstance();
    }
    
    public static synchronized ClienteDAO getInstance() {
        if (instance == null) {
            instance = new ClienteDAO();
        }
        return instance;
    }
    
    public Cliente create(Cliente cliente) throws SQLException, CpfCnpjDuplicadoException {
        // Verifica se já existe cliente com mesmo CPF/CNPJ
        if (cliente.getCpfCnpj() != null && !cliente.getCpfCnpj().isEmpty()) {
            if (findByCpfCnpj(cliente.getCpfCnpj()) != null) {
                throw new CpfCnpjDuplicadoException("Já existe um cliente cadastrado com o CPF/CNPJ: " + cliente.getCpfCnpj());
            }
        }
        
        // Certifica-se de que a data de cadastro está definida
        if (cliente.getDataCadastro() == null) {
            cliente.setDataCadastro(new Date());
        }
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, cliente.getNome());
            stmt.setString(2, cliente.getCpfCnpj());
            stmt.setString(3, cliente.getEndereco());
            stmt.setString(4, cliente.getTelefone());
            stmt.setString(5, cliente.getEmail());
            
            // Definir data de nascimento
            if (cliente.getDataNascimento() != null) {
                stmt.setDate(6, new java.sql.Date(cliente.getDataNascimento().getTime()));
            } else {
                stmt.setNull(6, java.sql.Types.DATE);
            }
            
            // Definir observação
            stmt.setString(7, cliente.getObservacao());
            
            // Definir data de cadastro - PARÂMETRO 8 QUE ESTAVA FALTANDO
            stmt.setDate(8, new java.sql.Date(cliente.getDataCadastro().getTime()));
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Falha ao criar cliente, nenhuma linha afetada.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    cliente.setId(generatedKeys.getInt(1));
                    return cliente;
                } else {
                    throw new SQLException("Falha ao criar cliente, nenhum ID obtido.");
                }
            }
        }
    }
    
    public Cliente findByNome(String nome) throws SQLException {
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BY_NOME)) {
            stmt.setString(1, nome);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCliente(rs);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Salva um cliente no banco de dados.
     * Se o cliente já tiver ID, atualiza; caso contrário, insere um novo.
     * 
     * @param cliente O cliente a ser salvo
     * @return O ID do cliente salvo
     * @throws SQLException Se ocorrer um erro no banco de dados
     */
    public Long save(Cliente cliente) throws SQLException {
        if (cliente.getId() != null && cliente.getId() > 0) {
            return update(cliente);
        } else {
            return insert(cliente);
        }
    }
    
    /**
     * Insere um novo cliente no banco de dados
     * 
     * @param cliente O cliente a ser inserido
     * @return O ID do cliente inserido
     * @throws SQLException Se ocorrer um erro no banco de dados
     */
    private Long insert(Cliente cliente) throws SQLException {
        String sql = "INSERT INTO clientes (nome, cpf_cnpj, data_cadastro, ativo, data_nascimento, observacao) VALUES (?, ?, ?, ?, ?, ?)";
        
        PreparedStatement stmt = null;
        ResultSet generatedKeys = null;
        Connection conn = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, cliente.getNome());
            stmt.setString(2, cliente.getCpfCnpj());
            
            // Converter Date para Timestamp se necessário
            if (cliente.getDataCadastro() != null) {
                stmt.setTimestamp(3, new java.sql.Timestamp(cliente.getDataCadastro().getTime()));
            } else {
                stmt.setTimestamp(3, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            }
            
            stmt.setBoolean(4, cliente.isAtivo());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Falha ao inserir cliente, nenhuma linha afetada.");
            }
            
            generatedKeys = stmt.getGeneratedKeys();
            
            if (generatedKeys.next()) {
                Long id = generatedKeys.getLong(1);
                cliente.setId(id.intValue());
                return id;
            } else {
                throw new SQLException("Falha ao inserir cliente, nenhum ID obtido.");
            }
        } finally {
            if (generatedKeys != null) generatedKeys.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    public Cliente findById(Long id) throws SQLException {
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BY_ID)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCliente(rs);
                }
                return null;
            }
        }
    }
    
    public List<Cliente> readAll() throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                clientes.add(mapResultSetToCliente(rs));
            }
        }
        
        return clientes;
    }
    
    public Long update(Cliente cliente) throws SQLException {
    	
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setString(1, cliente.getNome());
            stmt.setString(2, cliente.getCpfCnpj());
            stmt.setString(3, cliente.getEndereco());
            stmt.setString(4, cliente.getTelefone());
            stmt.setString(5, cliente.getEmail());
            
            // Adicionando data de nascimento
            if (cliente.getDataNascimento() != null) {
                stmt.setDate(6, new java.sql.Date(cliente.getDataNascimento().getTime()));
            } else {
                stmt.setNull(6, java.sql.Types.DATE);
            }
            
            // Adicionando observação
            stmt.setString(7, cliente.getObservacao());
            
            // O ID agora é o parâmetro 8
            stmt.setLong(8, cliente.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                return cliente.getId();
            } else {
                throw new SQLException("Falha ao atualizar cliente, nenhuma linha afetada.");
            }
        }
    }
    
    public boolean delete(Long id) throws SQLException {
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            
            stmt.setLong(1, id);
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    public Cliente findByCpfCnpj(String cpfCnpj) throws SQLException {
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BY_CPF_CNPJ)) {
            
            stmt.setString(1, cpfCnpj);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCliente(rs);
                }
                return null;
            }
        }
    }
    
    /**
     * Conta o número de clientes ativos no sistema
     * 
     * @return Número de clientes ativos
     * @throws SQLException Em caso de erro no banco de dados
     */
    public int contarClientesAtivos() throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            
            String sql = "SELECT COUNT(*) FROM clientes WHERE ativo = true";
            
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
    
    public List<Cliente> findByNomeLike(String nomeParcial) throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT * FROM clientes WHERE nome LIKE ? ORDER BY nome";
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + nomeParcial + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    clientes.add(mapResultSetToCliente(rs));
                }
            }
        }
        
        return clientes;
    }
    
    private Cliente mapResultSetToCliente(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente(
            rs.getString("nome"),
            rs.getString("cpf_cnpj"),
            rs.getString("endereco"),
            rs.getString("telefone"),
            rs.getString("email")
        );
        cliente.setId(rs.getInt("id"));
        
        // Verificar se a coluna 'ativo' existe
        try {
            cliente.setAtivo(rs.getBoolean("ativo"));
        } catch (SQLException e) {
            cliente.setAtivo(true);
        }
        
        // Definir a data de cadastro
        java.sql.Date sqlDateCadastro = rs.getDate("data_cadastro");
        if (sqlDateCadastro != null) {
            cliente.setDataCadastro(new Date(sqlDateCadastro.getTime()));
        }
        
        // Adicionar leitura da data de nascimento
        java.sql.Date sqlDateNascimento = rs.getDate("data_nascimento");
        if (sqlDateNascimento != null) {
            cliente.setDataNascimento(new Date(sqlDateNascimento.getTime()));
        }
        
        // Adicionar leitura da observação
        cliente.setObservacao(rs.getString("observacao"));
        
        return cliente;
    }
}