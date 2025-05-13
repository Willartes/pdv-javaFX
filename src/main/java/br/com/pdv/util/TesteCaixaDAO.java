package br.com.pdv.util;

import br.com.pdv.dao.CaixaDAO;
import br.com.pdv.dao.UsuarioDAO;
import br.com.pdv.model.Caixa;
import br.com.pdv.model.Usuario;
import br.com.pdv.dao.CaixaDAO.BalancoCaixa;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Classe para testar as operações do CaixaDAO.
 */
public class TesteCaixaDAO {
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
            
            // Limpar a tabela de movimentos_caixa primeiro (tabela filha)
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM movimentos_caixa");
                System.out.println("Registros da tabela movimentos_caixa removidos");
            }
            
            // Agora limpar a tabela de fluxo_caixa
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM fluxo_caixa");
                System.out.println("Registros da tabela fluxo_caixa removidos");
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
    
    
    /**
     * Teste simples para inserir diretamente na tabela fluxo_caixa sem usar o DAO.
     */
    private static void testeInserirCaixaDiretamente() {
        Connection conn = null;
        try {
            System.out.println("\n=== Teste de Inserção Direta no Banco ===");
            conn = getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO fluxo_caixa (data, data_abertura, saldo_inicial, saldo_final, status, usuario_abertura_id) " +
                    "VALUES (CURDATE(), NOW(), 1000.00, 1000.00, 'ABERTO', 1)")) {
                
                int rows = stmt.executeUpdate();
                conn.commit();
                System.out.println("Inserção direta bem-sucedida! Linhas afetadas: " + rows);
                
                // Agora limpar as tabelas para os próximos testes
                try (Statement cleanupStmt = conn.createStatement()) {
                    // Desativar verificação de chaves estrangeiras
                    cleanupStmt.execute("SET FOREIGN_KEY_CHECKS = 0");
                    
                    // Limpar movimentos_caixa primeiro (tabela filha)
                    cleanupStmt.executeUpdate("DELETE FROM movimentos_caixa");
                    System.out.println("Limpeza de movimentos_caixa realizada");
                    
                    // Limpar fluxo_caixa depois
                    cleanupStmt.executeUpdate("DELETE FROM fluxo_caixa");
                    System.out.println("Limpeza de fluxo_caixa realizada");
                    
                    // Reativar verificação
                    cleanupStmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                }
                
                conn.commit();
            }
        } catch (SQLException e) {
            System.out.println("ERRO NA INSERÇÃO DIRETA: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.out.println("Erro ao realizar rollback: " + ex.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.out.println("Erro ao fechar conexão: " + e.getMessage());
                }
            }
        }
    }
    
    public static void main(String[] args) {
        Connection conn = null;
        
        try {
            // Primeiro, executa o teste de inserção direta 
            // (que gerencia sua própria conexão independente)
            testeInserirCaixaDiretamente();
            
            // Criar e configurar a conexão apenas para verificação de estrutura
            try (Connection structConn = getConnection()) {
                System.out.println("Verificando estrutura da tabela fluxo_caixa...");
                try (Statement stmt = structConn.createStatement();
                     ResultSet rs = stmt.executeQuery("DESCRIBE fluxo_caixa")) {
                    
                    System.out.println("Colunas da tabela fluxo_caixa:");
                    while (rs.next()) {
                        System.out.println(rs.getString("Field") + " - " + rs.getString("Type"));
                    }
                }
            } // Conexão de verificação fecha automaticamente aqui
            
            // Agora obtenha uma nova conexão para os testes reais,
            // mantendo autoCommit=true até o momento dos testes
            conn = getConnection();
            
            System.out.println("=== Iniciando Testes do CaixaDAO ===\n");
            
            // Limpa o banco de dados - ainda com autoCommit=true
            limparBancoDados(conn);
            
            // Só AGORA ative o modo de transação
            conn.setAutoCommit(false);
            
            CaixaDAO caixaDAO = CaixaDAO.getInstance();
            UsuarioDAO usuarioDAO = UsuarioDAO.getInstance();
            
            // Buscar um usuário existente para usar como operador
            Usuario operador = null;
            try {
                operador = usuarioDAO.findById(1); // Assumindo que há um usuário com ID 1
                if (operador == null) {
                    throw new RuntimeException("Usuário com ID 1 não encontrado. Por favor, crie um usuário antes de executar este teste.");
                }
            } catch (SQLException e) {
                System.out.println("Erro ao buscar usuário: " + e.getMessage());
                return;
            }
            
            // Teste 1: Criar novo caixa
            System.out.println("=== Teste de Abertura de Caixa ===");
            Caixa novoCaixa = new Caixa(operador, new BigDecimal("1000.00"));
            
            try {
                // Antes de executar o método create, confirme a transação atual
                // para liberar quaisquer locks pendentes
                conn.commit();
                
                caixaDAO.create(novoCaixa);
                System.out.println("Caixa aberto com sucesso! ID: " + novoCaixa.getId());
                System.out.println("Dados do caixa: " + novoCaixa);
                System.out.println("Usuario abertura ID: " + novoCaixa.getOperador().getId());
            } catch (SQLException e) {
                System.out.println("ERRO AO ABRIR CAIXA: " + e.getMessage());
                conn.rollback();
                return;
            }
            
            // Commit a transação após cada teste bem-sucedido
            conn.commit();
            
            // Teste 2: Buscar caixa por ID
            System.out.println("\n=== Teste de Busca por ID ===");
            try {
                Caixa caixaEncontrado = caixaDAO.findById(novoCaixa.getId());
                if (caixaEncontrado != null) {
                    System.out.println("Caixa encontrado: " + caixaEncontrado);
                    System.out.println("Data: " + caixaEncontrado.getDataAbertura().toLocalDate());
                    System.out.println("Usuário abertura ID: " + caixaEncontrado.getOperador().getId());
                } else {
                    System.out.println("Caixa não encontrado");
                }
                conn.commit(); // Commit após o teste
            } catch (SQLException e) {
                System.out.println("ERRO AO BUSCAR CAIXA: " + e.getMessage());
                conn.rollback();
                return;
            }
            
            // Restante dos testes seguindo o mesmo padrão...
            // Cada teste deve ser seguido por um commit para liberar locks
            
            // Teste 3: Buscar caixa aberto
            System.out.println("\n=== Teste de Busca por Caixa Aberto ===");
            try {
                Caixa caixaAberto = caixaDAO.findCaixaAberto();
                if (caixaAberto != null) {
                    System.out.println("Caixa aberto encontrado: " + caixaAberto);
                    System.out.println("Status: " + caixaAberto.getStatus());
                } else {
                    System.out.println("Nenhum caixa aberto encontrado");
                }
                conn.commit();
            } catch (SQLException e) {
                System.out.println("ERRO AO BUSCAR CAIXA ABERTO: " + e.getMessage());
                conn.rollback();
                return;
            }
            
            // Continuar com os demais testes...
            
            // Confirmar transação final
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
                    conn.setAutoCommit(true); // Restaurar autoCommit antes de fechar
                    conn.close();
                    System.out.println("Conexão fechada com sucesso");
                } catch (SQLException e) {
                    System.out.println("Erro ao fechar conexão: " + e.getMessage());
                }
            }
        }
    }
    
    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
}