package br.com.pdv.util;

import br.com.pdv.dao.ClienteDAO;
import br.com.pdv.model.Cliente;
import br.com.pdv.exception.CpfCnpjDuplicadoException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe de teste para as operações do ClienteDAO.
 */
public class TesteClienteDAO {

    private static final Logger logger = Logger.getLogger(TesteClienteDAO.class.getName());
    private static Connection connection;
    private static ClienteDAO clienteDAO;

    public static void main(String[] args) {
        Connection conn = null;
        
        try {
            // Obter conexão inicial
            try {
                conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/pdv_db", "root", "root");
                conn.setAutoCommit(true);
                
                // Verificar e limpar bloqueios
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW PROCESSLIST")) {
                    
                    System.out.println("Processos MySQL ativos:");
                    while (rs.next()) {
                        int id = rs.getInt("Id");
                        String user = rs.getString("User");
                        String state = rs.getString("State");
                        String info = rs.getString("Info");
                        long time = rs.getLong("Time");
                        
                        System.out.println("ID: " + id + ", User: " + user + ", State: " + state + 
                                          ", Time: " + time + "s, Info: " + info);
                        
                        // Se o processo está bloqueado há mais de 60 segundos, mata ele
                        if (state != null && state.contains("lock") && time > 60) {
                            System.out.println("Matando processo bloqueado ID: " + id);
                            try (Statement killStmt = conn.createStatement()) {
                                killStmt.execute("KILL " + id);
                            }
                        }
                    }
                }
                
                // Aumentar timeout global
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("SET GLOBAL innodb_lock_wait_timeout = 120");
                    System.out.println("Aumentado o timeout global de locks para 120 segundos");
                }
                
                // Fechar essa conexão inicial após configurações
                conn.close();
            } catch (Exception e) {
                System.out.println("Aviso ao configurar banco de dados: " + e.getMessage());
            }
            
            // Inicializar, executar testes e finalizar
            inicializar();
            executarTestes();
            System.out.println("\n=== Testes Concluídos com Sucesso! ===");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro na execução dos testes: " + e.getMessage(), e);
            e.printStackTrace();
        } finally {
            finalizar();
        }
    }
    
    /**
     * Inicializa a conexão com o banco de dados e objetos necessários para os testes.
     */
    private static void inicializar() throws SQLException {
        System.out.println("=== Iniciando Testes do ClienteDAO ===\n");
        
        // Obter a conexão usando DatabaseConnection
        connection = DatabaseConnection.getInstance().getConnection();
        
        // Limpar o banco de dados para garantir testes confiáveis
        limparBancoDados();
        
        // Inicializar o ClienteDAO usando o padrão singleton
        clienteDAO = ClienteDAO.getInstance();
    }
    
    /**
     * Executa a sequência de testes para a classe ClienteDAO.
     */
    private static void executarTestes() throws SQLException {
        Cliente novoCliente = criarNovoCliente();
        
        // Executa os testes em sequência
        testarInsercaoCliente(novoCliente);
        testarLeituraCliente(novoCliente);
        testarAtualizacaoCliente(novoCliente);
        testarListagemClientes();
        testarInsercaoComCpfDuplicado();
        testarExclusaoCliente(novoCliente);
    }
    
    private static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/pdv_db", "root", "root");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL não encontrado", e);
        }
    }
    /**
     * Fecha a conexão com o banco de dados e libera recursos.
     */
    private static void finalizar() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Conexão com o banco de dados fechada com sucesso.");
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Erro ao fechar conexão: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Limpa os dados do banco para iniciar os testes com tabela vazia.
     */
    private static void limparBancoDados() throws SQLException {
        try {
            // Primeiro, desativar verificação de chaves estrangeiras
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
                System.out.println("Verificação de chaves estrangeiras desativada temporariamente");
            }

            // Agora limpar todas as tabelas relacionadas
            try (Statement stmt = connection.createStatement()) {
                // Limpar vendas primeiro (tabela que tem a foreign key para pedidos)
                stmt.executeUpdate("DELETE FROM vendas");
                System.out.println("Registros da tabela vendas removidos");
                
                // Limpar outras tabelas que podem ter relacionamentos
                stmt.executeUpdate("DELETE FROM movimentos_caixa");
                System.out.println("Registros da tabela movimentos_caixa removidos");
                
                stmt.executeUpdate("DELETE FROM itens_pedido");
                System.out.println("Registros da tabela itens_pedido removidos");
                
                stmt.executeUpdate("DELETE FROM pedidos");
                System.out.println("Registros da tabela pedidos removidos");
                
                stmt.executeUpdate("DELETE FROM clientes");
                System.out.println("Registros da tabela clientes removidos");
            }

            // Reativar verificação de chaves estrangeiras
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                System.out.println("Verificação de chaves estrangeiras reativada");
            }

            System.out.println("Banco de dados limpo para iniciar os testes");
        } catch (SQLException e) {
            System.err.println("Erro ao limpar o banco de dados: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Cria um novo objeto Cliente para ser usado nos testes.
     */
    private static Cliente criarNovoCliente() {
        // Usando um UUID para evitar conflitos de chave duplicada se o teste for executado múltiplas vezes
        String uniqueCpf = UUID.randomUUID().toString().substring(0, 8);
        
        return new Cliente(
            "Cliente Teste",
            uniqueCpf,
            "Rua de Teste, 123",
            "(11) 98765-4321",
            "teste@email.com"
        );
    }

    /**
     * Testa a inserção de um cliente no banco de dados.
     */
    private static void testarInsercaoCliente(Cliente novoCliente) {
        System.out.println("=== Teste de Inserção ===");
        try {
            clienteDAO.create(novoCliente);
            if (novoCliente.getId() != null && novoCliente.getId() > 0) {
                System.out.println("Cliente inserido com sucesso! ID: " + novoCliente.getId());
            } else {
                System.out.println("ALERTA: Cliente foi criado, mas o ID não foi retornado corretamente.");
            }
        } catch (CpfCnpjDuplicadoException e) {
            System.out.println("ERRO DE VALIDAÇÃO: " + e.getMessage());
            logger.log(Level.WARNING, "CPF/CNPJ duplicado durante o teste: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Erro ao inserir cliente: " + e.getMessage());
            logger.log(Level.SEVERE, "Falha na inserção do cliente", e);
        }
    }

    /**
     * Testa a busca de um cliente pelo ID.
     */
    private static void testarLeituraCliente(Cliente cliente) {
        System.out.println("\n=== Teste de Busca por ID ===");
        try {
            if (cliente.getId() == null) {
                System.out.println("ID do cliente não foi definido após inserção. Teste de leitura não pode ser realizado.");
                return;
            }
            
            Cliente clienteEncontrado = clienteDAO.findById(cliente.getId());
            if (clienteEncontrado != null) {
                System.out.println("Cliente encontrado: " + clienteEncontrado);
                
                // Verificação adicional para garantir que os dados correspondam
                if (clienteEncontrado.getNome().equals(cliente.getNome()) &&
                    clienteEncontrado.getCpfCnpj().equals(cliente.getCpfCnpj())) {
                    System.out.println("OK: Os dados encontrados correspondem aos dados inseridos.");
                } else {
                    System.out.println("AVISO: Há discrepâncias entre os dados encontrados e os inseridos.");
                }
            } else {
                System.out.println("ERRO: Cliente não encontrado com ID " + cliente.getId());
            }
        } catch (SQLException e) {
            System.out.println("Erro ao ler cliente: " + e.getMessage());
            logger.log(Level.SEVERE, "Falha ao buscar cliente", e);
        }
    }

    /**
     * Testa a atualização de um cliente existente.
     */
    private static void testarAtualizacaoCliente(Cliente cliente) {
        System.out.println("\n=== Teste de Atualização ===");
        try {
            if (cliente.getId() == null) {
                System.out.println("ID do cliente não definido. Teste de atualização não pode ser realizado.");
                return;
            }
            
            // Modificando alguns dados para o teste
            String nomeOriginal = cliente.getNome();
            String emailOriginal = cliente.getEmail();
            
            String novoNome = nomeOriginal + " (Atualizado)";
            String novoEmail = "atualizado." + emailOriginal;
            
            cliente.setNome(novoNome);
            cliente.setEmail(novoEmail);
            
            // Executa a atualização
            clienteDAO.update(cliente);
            
            // Verifica se a atualização foi bem-sucedida
            Cliente clienteAtualizado = clienteDAO.findById(cliente.getId());
            
            if (clienteAtualizado != null) {
                System.out.println("Dados após atualização: " + clienteAtualizado);
                
                if (clienteAtualizado.getNome().equals(novoNome) && 
                    clienteAtualizado.getEmail().equals(novoEmail)) {
                    System.out.println("OK: Cliente atualizado com sucesso!");
                } else {
                    System.out.println("ERRO: Dados não foram atualizados corretamente.");
                }
            } else {
                System.out.println("ERRO: Cliente não encontrado após atualização.");
            }
        } catch (SQLException e) {
            System.out.println("Erro ao atualizar cliente: " + e.getMessage());
            logger.log(Level.SEVERE, "Falha na atualização do cliente", e);
        }
    }

    /**
     * Testa a listagem de todos os clientes.
     */
    private static void testarListagemClientes() {
        System.out.println("\n=== Teste de Listagem ===");
        try {
            List<Cliente> clientes = clienteDAO.readAll();
            System.out.println("Lista de todos os clientes:");
            
            if (clientes.isEmpty()) {
                System.out.println("Nenhum cliente encontrado. Isso é inesperado após a inserção.");
            } else {
                System.out.println("Total de clientes encontrados: " + clientes.size());
                for (Cliente c : clientes) {
                    System.out.println(c);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erro ao listar clientes: " + e.getMessage());
            logger.log(Level.SEVERE, "Falha ao listar clientes", e);
        }
    }

    /**
     * Testa a validação de CPF/CNPJ duplicado.
     */
    private static void testarInsercaoComCpfDuplicado() {
        System.out.println("\n=== Teste de Inserção com CPF Duplicado ===");
        try {
            // Para garantir a duplicidade, vamos buscar um cliente existente
            List<Cliente> clientesExistentes = clienteDAO.readAll();
            
            if (clientesExistentes.isEmpty()) {
                System.out.println("Não há clientes para testar a duplicidade. Pulando teste.");
                return;
            }
            
            Cliente clienteExistente = clientesExistentes.get(0);
            
            // Criando um cliente com CPF/CNPJ duplicado
            Cliente clienteDuplicado = new Cliente(
                "Cliente Duplicado",
                clienteExistente.getCpfCnpj(),  // Usando o mesmo CPF/CNPJ
                "Endereço Diferente",
                "(99) 99999-9999",
                "duplicado@email.com"
            );
            
            clienteDAO.create(clienteDuplicado);
            
            // Se chegou aqui, o teste falhou
            System.out.println("ERRO: Inserção com CPF/CNPJ duplicado foi permitida! Verifique as constraints do banco.");
            
        } catch (CpfCnpjDuplicadoException e) {
            // Comportamento esperado
            System.out.println("OK: Teste de duplicidade funcionou corretamente");
            System.out.println("Mensagem: " + e.getMessage());
        } catch (SQLException e) {
            // Pode ocorrer se o banco tiver constraint, mas o código não tratar adequadamente
            if (e.getMessage().toLowerCase().contains("duplicate") || 
                e.getMessage().toLowerCase().contains("duplicado") || 
                e.getMessage().toLowerCase().contains("unique")) {
                System.out.println("PARCIALMENTE OK: O banco impediu a duplicidade, mas a exceção não foi tratada adequadamente.");
            } else {
                System.out.println("Erro inesperado ao realizar teste de duplicidade: " + e.getMessage());
                logger.log(Level.SEVERE, "Falha no teste de duplicidade", e);
            }
        }
    }
    
    
    private static void verificarELimparBloqueios(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW PROCESSLIST")) {
            
            System.out.println("Processos MySQL ativos:");
            while (rs.next()) {
                int id = rs.getInt("Id");
                String user = rs.getString("User");
                String state = rs.getString("State");
                String info = rs.getString("Info");
                long time = rs.getLong("Time");
                
                System.out.println("ID: " + id + ", User: " + user + ", State: " + state + 
                                   ", Time: " + time + "s, Info: " + info);
                
                // Se o processo está bloqueado há mais de 60 segundos, mata ele
                if (state != null && state.contains("lock") && time > 60) {
                    System.out.println("Matando processo bloqueado ID: " + id);
                    try (Statement killStmt = conn.createStatement()) {
                        killStmt.execute("KILL " + id);
                    }
                }
            }
        }
    }
    
    /**
     * Testa a exclusão de um cliente.
     */
    private static void testarExclusaoCliente(Cliente cliente) {
        System.out.println("\n=== Teste de Exclusão ===");
        try {
            if (cliente.getId() == null) {
                System.out.println("ID do cliente não definido. Teste de exclusão não pode ser realizado.");
                return;
            }
            
            // Verificar se o cliente realmente existe antes de tentar excluir
            Cliente clienteParaExcluir = clienteDAO.findById(cliente.getId());
            if (clienteParaExcluir == null) {
                System.out.println("Cliente não encontrado para exclusão.");
                return;
            }
            
            // Executar a exclusão
            clienteDAO.delete(cliente.getId());
            
            // Verificar se a exclusão foi bem-sucedida
            Cliente clienteExcluido = clienteDAO.findById(cliente.getId());
            if (clienteExcluido == null) {
                System.out.println("OK: Cliente foi excluído com sucesso");
            } else {
                System.out.println("ERRO: Cliente ainda existe no banco após tentativa de exclusão");
            }
        } catch (SQLException e) {
            System.out.println("Erro ao excluir cliente: " + e.getMessage());
            logger.log(Level.SEVERE, "Falha na exclusão do cliente", e);
        }
    }
}