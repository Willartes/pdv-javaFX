package br.com.pdv.util;

import br.com.pdv.dao.ClienteDAO;
import br.com.pdv.model.Cliente;
import br.com.pdv.exception.CpfCnpjDuplicadoException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class TesteClienteDAO {

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
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM clientes");
            System.out.println("Banco de dados limpo para iniciar os testes");
        }
    }

    public static void main(String[] args) {
        Connection conn = null;
        try {
            conn = getConnection();
            System.out.println("=== Iniciando Testes do ClienteDAO ===\n");

            limparBancoDados(conn);

            // Instanciar ClienteDAO usando o construtor diretamente
            // Se a classe ClienteDAO não tiver construtor público, será necessário refatorar
            // para usar reflection ou modificar a classe ClienteDAO
            ClienteDAO clienteDAO = new ClienteDAO(conn);

            Cliente novoCliente = criarNovoCliente();
            testarInsercaoCliente(clienteDAO, novoCliente);
            testarLeituraCliente(clienteDAO, novoCliente);
            testarAtualizacaoCliente(clienteDAO, novoCliente);
            listarClientes(clienteDAO);
            testarInsercaoComCpfDuplicado(clienteDAO);
            testarExclusaoCliente(clienteDAO, novoCliente);

            System.out.println("\n=== Testes Concluídos com Sucesso! ===");

        } catch (SQLException e) {
            System.out.println("\nErro na execução dos testes: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Garantir que a conexão seja fechada ao final dos testes
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("Conexão com o banco de dados fechada com sucesso.");
                } catch (SQLException e) {
                    System.out.println("Erro ao fechar conexão: " + e.getMessage());
                }
            }
        }
    }

    private static Cliente criarNovoCliente() {
        return new Cliente(
            "João Silva",
            "987.654.321-00",
            "Rua das Flores, 123",
            "(11) 98765-4321",
            "joao@email.com"
        );
    }

    private static void testarInsercaoCliente(ClienteDAO clienteDAO, Cliente novoCliente) {
        System.out.println("=== Teste de Inserção ===");
        try {
            clienteDAO.create(novoCliente);
            System.out.println("Cliente inserido com sucesso! ID: " + novoCliente.getId());
        } catch (CpfCnpjDuplicadoException e) {
            System.out.println("ERRO DE VALIDAÇÃO: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Erro ao inserir cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testarLeituraCliente(ClienteDAO clienteDAO, Cliente novoCliente) {
        System.out.println("\n=== Teste de Busca por ID ===");
        try {
            if (novoCliente.getId() == null) {
                System.out.println("ID do cliente não foi definido após inserção.");
                return;
            }
            
            Cliente clienteEncontrado = clienteDAO.read(novoCliente.getId());
            if (clienteEncontrado != null) {
                System.out.println("Cliente encontrado: " + clienteEncontrado);
            } else {
                System.out.println("Cliente não encontrado");
            }
        } catch (SQLException e) {
            System.out.println("Erro ao ler cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testarAtualizacaoCliente(ClienteDAO clienteDAO, Cliente clienteEncontrado) {
        System.out.println("\n=== Teste de Atualização ===");
        try {
            if (clienteEncontrado.getId() == null) {
                System.out.println("ID do cliente não definido. Não é possível atualizar.");
                return;
            }
            
            clienteEncontrado.setNome("João Silva Santos");
            clienteEncontrado.setEmail("joao.silva@email.com");
            clienteDAO.update(clienteEncontrado);
            System.out.println("Cliente atualizado");

            Cliente clienteAtualizado = clienteDAO.read(clienteEncontrado.getId());
            System.out.println("Dados após atualização: " + clienteAtualizado);
        } catch (SQLException e) {
            System.out.println("Erro ao atualizar cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void listarClientes(ClienteDAO clienteDAO) {
        System.out.println("\n=== Teste de Listagem ===");
        try {
            List<Cliente> clientes = clienteDAO.readAll();
            System.out.println("Lista de todos os clientes:");
            if (clientes.isEmpty()) {
                System.out.println("Nenhum cliente encontrado.");
            } else {
                for (Cliente c : clientes) {
                    System.out.println(c);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erro ao listar clientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testarInsercaoComCpfDuplicado(ClienteDAO clienteDAO) {
        System.out.println("\n=== Teste de Inserção com CPF Duplicado ===");
        try {
            Cliente clienteDuplicado = new Cliente(
                "Maria Silva",
                "987.654.321-00", // Mesmo CPF usado anteriormente
                "Rua ABC, 123",
                "(11) 91234-5678",
                "maria@email.com"
            );
            clienteDAO.create(clienteDuplicado);
            System.out.println("ALERTA: Inserção com CPF duplicado foi permitida!");
        } catch (CpfCnpjDuplicadoException e) {
            System.out.println("OK: Teste de duplicidade funcionou");
            System.out.println("Mensagem: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Erro ao realizar inserção duplicada: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testarExclusaoCliente(ClienteDAO clienteDAO, Cliente novoCliente) {
        System.out.println("\n=== Teste de Exclusão ===");
        try {
            if (novoCliente.getId() == null) {
                System.out.println("ID do cliente não definido. Não é possível excluir.");
                return;
            }
            
            clienteDAO.delete(novoCliente.getId());
            System.out.println("Cliente deletado");

            Cliente clienteDeletado = clienteDAO.read(novoCliente.getId());
            if (clienteDeletado == null) {
                System.out.println("OK: Cliente foi realmente deletado");
            } else {
                System.out.println("ERRO: Cliente ainda existe no banco");
            }
        } catch (SQLException e) {
            System.out.println("Erro ao excluir cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
}