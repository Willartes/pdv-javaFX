package br.com.pdv.util;

import br.com.pdv.dao.FornecedorDAO;
import br.com.pdv.model.Fornecedor;
import br.com.pdv.exception.CpfCnpjDuplicadoException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class TesteFornecedorDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/pdv_db";
    private static final String USER = "root";
    private static final String PASSWORD = "root";
    
    private static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL não encontrado", e);
        }
    }
    
    private static void limparBancoDados(Connection conn) throws SQLException {
        try {
            // Desabilitar verificação de chaves estrangeiras temporariamente
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            }
            
            // Limpar a tabela de compras primeiro (tabela filha)
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM compras WHERE fornecedor_id IN (SELECT id FROM fornecedores)");
                System.out.println("Registros relacionados na tabela compras removidos");
            }
            
            // Agora limpar a tabela de fornecedores
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM fornecedores");
                System.out.println("Banco de dados limpo para iniciar os testes");
            }
            
            // Reabilitar verificação de chaves estrangeiras
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao limpar o banco de dados: " + e.getMessage());
            throw e;
        }
    }
    
    public static void main(String[] args) {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Iniciar transação para garantir atomicidade dos testes
            
            System.out.println("=== Iniciando Testes do FornecedorDAO ===\n");
            
            // Limpa o banco antes de começar os testes
            limparBancoDados(conn);
            
            FornecedorDAO fornecedorDAO = new FornecedorDAO(conn);
            
            // Teste 1: Criar novo fornecedor
            System.out.println("=== Teste de Inserção ===");
            Fornecedor novoFornecedor = new Fornecedor(
                "Empresa ABC Ltda",     // nome
                "12.345.678/0001-90",   // cpfCnpj
                "Av. Comercial, 1000",  // endereco
                "(11) 3456-7890",       // telefone
                "contato@abc.com.br"    // email
            );
            
            try {
                fornecedorDAO.create(novoFornecedor);
                System.out.println("Fornecedor inserido com sucesso! ID: " + novoFornecedor.getId());
                System.out.println("Dados do fornecedor: " + novoFornecedor);
            } catch (CpfCnpjDuplicadoException e) {
                System.out.println("ERRO DE VALIDAÇÃO: " + e.getMessage());
                conn.rollback();
                return;
            }
            
            // Teste 2: Buscar fornecedor por ID
            System.out.println("\n=== Teste de Busca por ID ===");
            Fornecedor fornecedorEncontrado = fornecedorDAO.read(novoFornecedor.getId());
            if (fornecedorEncontrado != null) {
                System.out.println("Fornecedor encontrado: " + fornecedorEncontrado);
            } else {
                System.out.println("Fornecedor não encontrado");
            }
            
            // Teste 3: Atualizar fornecedor
            System.out.println("\n=== Teste de Atualização ===");
            fornecedorEncontrado.setNome("Empresa ABC e Cia Ltda");
            fornecedorEncontrado.setEmail("financeiro@abc.com.br");
            fornecedorDAO.update(fornecedorEncontrado);
            System.out.println("Fornecedor atualizado");
            
            // Verificar atualização
            Fornecedor fornecedorAtualizado = fornecedorDAO.read(fornecedorEncontrado.getId());
            System.out.println("Dados após atualização: " + fornecedorAtualizado);
            
            // Teste 4: Buscar por nome
            System.out.println("\n=== Teste de Busca por Nome ===");
            List<Fornecedor> fornecedoresPorNome = fornecedorDAO.buscarPorNome("ABC");
            System.out.println("Fornecedores encontrados com 'ABC' no nome:");
            for (Fornecedor f : fornecedoresPorNome) {
                System.out.println(f);
            }
            
            // Teste 5: Buscar por CPF/CNPJ
            System.out.println("\n=== Teste de Busca por CPF/CNPJ ===");
            Fornecedor fornecedorPorCpfCnpj = fornecedorDAO.buscarPorCpfCnpj("12.345.678/0001-90");
            if (fornecedorPorCpfCnpj != null) {
                System.out.println("Fornecedor encontrado: " + fornecedorPorCpfCnpj);
            } else {
                System.out.println("Fornecedor não encontrado pelo CPF/CNPJ");
            }
            
            // Teste 6: Listar todos os fornecedores
            System.out.println("\n=== Teste de Listagem ===");
            List<Fornecedor> fornecedores = fornecedorDAO.readAll();
            System.out.println("Lista de todos os fornecedores:");
            for (Fornecedor f : fornecedores) {
                System.out.println(f);
            }
            
            // Teste 7: Tentar inserir fornecedor com CNPJ duplicado
            System.out.println("\n=== Teste de Inserção com CNPJ Duplicado ===");
            try {
                Fornecedor fornecedorDuplicado = new Fornecedor(
                    "Empresa XYZ Ltda",      // nome
                    "12.345.678/0001-90",    // mesmo CNPJ do primeiro fornecedor
                    "Rua Comercial, 500",    // endereco
                    "(11) 2345-6789",        // telefone
                    "contato@xyz.com.br"     // email
                );
                fornecedorDAO.create(fornecedorDuplicado);
                System.out.println("ALERTA: Inserção com CNPJ duplicado foi permitida!");
            } catch (CpfCnpjDuplicadoException e) {
                System.out.println("OK: Teste de duplicidade funcionou");
                System.out.println("Mensagem: " + e.getMessage());
            }
            
            // Teste 8: Deletar fornecedor - Verificar se há registros relacionados primeiro
            System.out.println("\n=== Teste de Exclusão ===");
            
            try {
                // Verificar se há registros na tabela compras para este fornecedor
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM compras WHERE fornecedor_id = " + novoFornecedor.getId());
                }
                
                // Agora podemos excluir o fornecedor com segurança
                fornecedorDAO.delete(novoFornecedor.getId());
                System.out.println("Fornecedor deletado");
                
                // Verificar exclusão
                Fornecedor fornecedorDeletado = fornecedorDAO.read(novoFornecedor.getId());
                if (fornecedorDeletado == null) {
                    System.out.println("OK: Fornecedor foi realmente deletado");
                } else {
                    System.out.println("ERRO: Fornecedor ainda existe no banco");
                }
            } catch (SQLException e) {
                System.out.println("Não foi possível excluir o fornecedor devido a registros relacionados");
                System.out.println("Erro: " + e.getMessage());
                
                // Desativar o fornecedor em vez de excluí-lo
                System.out.println("\n=== Alternativa: Desativar Fornecedor ===");
                novoFornecedor.setAtivo(false);
                fornecedorDAO.update(novoFornecedor);
                
                Fornecedor fornecedorDesativado = fornecedorDAO.read(novoFornecedor.getId());
                if (fornecedorDesativado != null && !fornecedorDesativado.isAtivo()) {
                    System.out.println("OK: Fornecedor foi desativado com sucesso");
                } else {
                    System.out.println("ERRO: Fornecedor não foi desativado corretamente");
                }
            }
            
            // Confirmar transação se tudo correu bem
            conn.commit();
            System.out.println("\n=== Testes Concluídos com Sucesso! ===");
            
        } catch (SQLException e) {
            System.out.println("\nErro na execução dos testes: " + e.getMessage());
            e.printStackTrace();
            
            // Reverter transação em caso de erro
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("Transação revertida devido a erro");
                } catch (SQLException ex) {
                    System.out.println("Erro ao reverter transação: " + ex.getMessage());
                }
            }
        } finally {
            // Fechar conexão
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.out.println("Erro ao fechar conexão: " + e.getMessage());
                }
            }
        }
    }
}