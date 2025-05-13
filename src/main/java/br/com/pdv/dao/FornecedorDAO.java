package br.com.pdv.dao;

import br.com.pdv.model.Fornecedor;
import br.com.pdv.exception.CpfCnpjDuplicadoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

public class FornecedorDAO {
    private Connection connection;
    
    public FornecedorDAO(Connection connection) {
        this.connection = connection;
    }
    
    public void create(Fornecedor fornecedor) throws SQLException, CpfCnpjDuplicadoException {
        String sql = "INSERT INTO fornecedores (nome, cpf_cnpj, endereco, telefone, email, ativo, data_cadastro) " +
                    "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, fornecedor.getNome());
            pstmt.setString(2, fornecedor.getCpfCnpj());
            pstmt.setString(3, fornecedor.getEndereco());
            pstmt.setString(4, fornecedor.getTelefone());
            pstmt.setString(5, fornecedor.getEmail());
            pstmt.setBoolean(6, true);
            
            try {
                pstmt.executeUpdate();
                
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        fornecedor.setId(rs.getInt(1));
                    }
                }
            } catch (SQLIntegrityConstraintViolationException e) {
                throw new CpfCnpjDuplicadoException(fornecedor.getCpfCnpj());
            }
        }
    }
    
    public Fornecedor read(int id) throws SQLException {
        String sql = "SELECT * FROM fornecedores WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Fornecedor fornecedor = new Fornecedor();
                    fornecedor.setId(rs.getInt("id"));
                    fornecedor.setNome(rs.getString("nome"));
                    fornecedor.setCpfCnpj(rs.getString("cpf_cnpj"));
                    fornecedor.setEndereco(rs.getString("endereco"));
                    fornecedor.setTelefone(rs.getString("telefone"));
                    fornecedor.setEmail(rs.getString("email"));
                    fornecedor.setAtivo(rs.getBoolean("ativo"));
                    fornecedor.setDataCadastro(rs.getTimestamp("data_cadastro"));
                    return fornecedor;
                }
            }
        }
        return null;
    }
    
    public List<Fornecedor> readAll() throws SQLException {
        List<Fornecedor> fornecedores = new ArrayList<>();
        String sql = "SELECT * FROM fornecedores";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                Fornecedor fornecedor = new Fornecedor();
                fornecedor.setId(rs.getInt("id"));
                fornecedor.setNome(rs.getString("nome"));
                fornecedor.setCpfCnpj(rs.getString("cpf_cnpj"));
                fornecedor.setEndereco(rs.getString("endereco"));
                fornecedor.setTelefone(rs.getString("telefone"));
                fornecedor.setEmail(rs.getString("email"));
                fornecedor.setAtivo(rs.getBoolean("ativo"));
                fornecedor.setDataCadastro(rs.getTimestamp("data_cadastro"));
                fornecedores.add(fornecedor);
            }
        }
        return fornecedores;
    }
    
    public void update(Fornecedor fornecedor) throws SQLException {
        String sql = "UPDATE fornecedores SET nome = ?, cpf_cnpj = ?, endereco = ?, " +
                    "telefone = ?, email = ?, ativo = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, fornecedor.getNome());
            pstmt.setString(2, fornecedor.getCpfCnpj());
            pstmt.setString(3, fornecedor.getEndereco());
            pstmt.setString(4, fornecedor.getTelefone());
            pstmt.setString(5, fornecedor.getEmail());
            pstmt.setBoolean(6, fornecedor.isAtivo());
            pstmt.setInt(7, fornecedor.getId());
            
            pstmt.executeUpdate();
        }
    }
    
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM fornecedores WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
    
    public List<Fornecedor> buscarPorNome(String nome) throws SQLException {
        List<Fornecedor> fornecedores = new ArrayList<>();
        String sql = "SELECT * FROM fornecedores WHERE nome LIKE ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + nome + "%");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Fornecedor fornecedor = new Fornecedor();
                    fornecedor.setId(rs.getInt("id"));
                    fornecedor.setNome(rs.getString("nome"));
                    fornecedor.setCpfCnpj(rs.getString("cpf_cnpj"));
                    fornecedor.setEndereco(rs.getString("endereco"));
                    fornecedor.setTelefone(rs.getString("telefone"));
                    fornecedor.setEmail(rs.getString("email"));
                    fornecedor.setAtivo(rs.getBoolean("ativo"));
                    fornecedor.setDataCadastro(rs.getTimestamp("data_cadastro"));
                    fornecedores.add(fornecedor);
                }
            }
        }
        return fornecedores;
    }
    
    public Fornecedor buscarPorCpfCnpj(String cpfCnpj) throws SQLException {
        String sql = "SELECT * FROM fornecedores WHERE cpf_cnpj = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, cpfCnpj);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Fornecedor fornecedor = new Fornecedor();
                    fornecedor.setId(rs.getInt("id"));
                    fornecedor.setNome(rs.getString("nome"));
                    fornecedor.setCpfCnpj(rs.getString("cpf_cnpj"));
                    fornecedor.setEndereco(rs.getString("endereco"));
                    fornecedor.setTelefone(rs.getString("telefone"));
                    fornecedor.setEmail(rs.getString("email"));
                    fornecedor.setAtivo(rs.getBoolean("ativo"));
                    fornecedor.setDataCadastro(rs.getTimestamp("data_cadastro"));
                    return fornecedor;
                }
            }
        }
        return null;
    }
}