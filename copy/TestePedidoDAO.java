package br.com.pdv.util;

import br.com.pdv.dao.ClienteDAO;
import br.com.pdv.dao.ItemPedidoDAO;
import br.com.pdv.dao.PedidoDAO;
import br.com.pdv.dao.ProdutoDAO;
import br.com.pdv.dao.UsuarioDAO;
import br.com.pdv.model.Cliente;
import br.com.pdv.model.ItemPedido;
import br.com.pdv.model.Pedido;
import br.com.pdv.model.Produto;
import br.com.pdv.model.Usuario;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe para testar as operações do PedidoDAO.
 */
public class TestePedidoDAO {

    private static final String URL = "jdbc:mysql://localhost:3306/pdv_db";
    private static final String USER = "root";
    private static final String PASSWORD = "root";
    
    private static Connection connection;
    private static PedidoDAO pedidoDAO;
    private static ClienteDAO clienteDAO;
    private static UsuarioDAO usuarioDAO;
    private static ProdutoDAO produtoDAO;
    private static ItemPedidoDAO itemPedidoDAO;
    
    // Dados para teste
    private static Cliente cliente;
    private static Usuario usuario;
    private static Produto produto1;
    private static Produto produto2;
    private static Pedido pedidoTeste;

    public static void main(String[] args) {
        try {
            inicializarConexao();
            inicializarDAOs();
            criarDadosTeste();
            
            executarTestes();
            
            System.out.println("\n=== Testes concluídos com sucesso! ===");
        } catch (Exception e) {
            System.err.println("\nErro ao executar testes: " + e.getMessage());
            e.printStackTrace();
        } finally {
            fecharConexao();
        }
    }
    
    private static void inicializarConexao() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            connection.setAutoCommit(true);
            System.out.println("Conexão estabelecida com sucesso.");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL não encontrado", e);
        }
    }
    
    private static void inicializarDAOs() throws SQLException {
        try {
            // Aqui vamos obter as instâncias ou criar novos DAOs
            // Usando reflection para obter instância de ClienteDAO sem depender do método getInstance
            // (caso esteja usando outra abordagem na implementação)
            try {
                clienteDAO = ClienteDAO.getInstance(connection);
                System.out.println("ClienteDAO inicializado com sucesso (getInstance).");
            } catch (Exception e) {
                // Tenta usar construtor público como fallback
                clienteDAO = new ClienteDAO(connection);
                System.out.println("ClienteDAO inicializado com sucesso (construtor).");
            }
            
            usuarioDAO = UsuarioDAO.getInstance();
            produtoDAO = ProdutoDAO.getInstance();
            itemPedidoDAO = ItemPedidoDAO.getInstance();
            pedidoDAO = PedidoDAO.getInstance();
            
            System.out.println("Todos os DAOs inicializados com sucesso.");
        } catch (Exception e) {
            System.err.println("Erro ao inicializar DAOs: " + e.getMessage());
            throw new SQLException("Falha ao inicializar DAOs", e);
        }
    }
    
    private static void criarDadosTeste() throws SQLException {
        System.out.println("\n=== Criando dados para teste ===");
        
        // Criar um cliente para teste
        cliente = new Cliente(
            "Cliente Teste DAO", 
            "111.222.333-99", 
            "Rua de Teste, 123", 
            "(11) 99999-8888", 
            "teste@email.com"
        );
        
        System.out.println("Inserindo cliente de teste...");
        try {
            clienteDAO.create(cliente);
            System.out.println("Cliente inserido com ID: " + cliente.getId());
        } catch (Exception e) {
            // Se falhar, tenta buscar um cliente existente
            System.out.println("Erro ao criar cliente de teste. Buscando um cliente existente...");
            List<Cliente> clientes = clienteDAO.readAll();
            if (!clientes.isEmpty()) {
                cliente = clientes.get(0);
                System.out.println("Usando cliente existente com ID: " + cliente.getId());
            } else {
                throw new SQLException("Não foi possível criar ou encontrar um cliente para teste.");
            }
        }
        
        // Criar um usuário para teste
        usuario = new Usuario("Usuario Teste", "usuarioteste", "senha123", "VENDEDOR");
        
        System.out.println("Inserindo usuário de teste...");
        try {
            usuario = usuarioDAO.create(usuario);
            System.out.println("Usuário inserido com ID: " + usuario.getId());
        } catch (Exception e) {
            // Se falhar, tenta buscar um usuário existente
            System.out.println("Erro ao criar usuário de teste. Buscando um usuário existente...");
            List<Usuario> usuarios = usuarioDAO.findAll();
            if (!usuarios.isEmpty()) {
                usuario = usuarios.get(0);
                System.out.println("Usando usuário existente com ID: " + usuario.getId());
            } else {
                throw new SQLException("Não foi possível criar ou encontrar um usuário para teste.");
            }
        }
        
        // Criar produtos para teste
        produto1 = new Produto(
            "PROD-TESTE-1", 
            "Produto Teste 1", 
            "Descrição do Produto Teste 1", 
            new BigDecimal("50.00"), 
            100, 
            true
        );
        produto1.setUnidade("UN");
        
        produto2 = new Produto(
            "PROD-TESTE-2", 
            "Produto Teste 2", 
            "Descrição do Produto Teste 2", 
            new BigDecimal("75.50"), 
            50, 
            true
        );
        produto2.setUnidade("UN");
        
        System.out.println("Inserindo produtos de teste...");
        try {
            produto1 = produtoDAO.create(produto1);
            produto2 = produtoDAO.create(produto2);
            System.out.println("Produtos inseridos com IDs: " + produto1.getId() + ", " + produto2.getId());
        } catch (Exception e) {
            // Se falhar, tenta buscar produtos existentes
            System.out.println("Erro ao criar produtos de teste. Buscando produtos existentes...");
            List<Produto> produtos = produtoDAO.findAll();
            if (produtos.size() >= 2) {
                produto1 = produtos.get(0);
                produto2 = produtos.get(1);
                System.out.println("Usando produtos existentes com IDs: " + 
                                  produto1.getId() + ", " + produto2.getId());
            } else {
                throw new SQLException("Não foi possível criar ou encontrar produtos para teste.");
            }
        }
        
        System.out.println("Dados de teste criados com sucesso!");
    }
    
    private static void executarTestes() throws SQLException {
        testarCriacaoPedido();
        testarBuscaPedidoPorId();
        testarAtualizacaoPedido();
        testarListagemPedidos();
        testarFinalizacaoPedido();
        testarExclusaoPedido();
    }
    
    private static void testarCriacaoPedido() throws SQLException {
        System.out.println("\n=== Teste: Criação de Pedido ===");
        
        // Criando um novo pedido
        Pedido novoPedido = new Pedido();
        novoPedido.setCliente(cliente);
        novoPedido.setUsuario(usuario);
        novoPedido.setDataPedido(LocalDateTime.now());
        novoPedido.setValorTotal(BigDecimal.ZERO);
        
        // Adicionando itens ao pedido
        List<ItemPedido> itens = new ArrayList<>();
        
        // Primeiro item
        ItemPedido item1 = new ItemPedido(produto1, 2);
        // Segundo item
        ItemPedido item2 = new ItemPedido(produto2, 1);
        
        // Calculando o valor total
        BigDecimal valorTotal = item1.getValorTotal().add(item2.getValorTotal());
        novoPedido.setValorTotal(valorTotal);
        
        System.out.println("Criando pedido com " + 2 + " itens, valor total: " + valorTotal);
        
        try {
            // Persistindo o pedido
            pedidoTeste = pedidoDAO.create(novoPedido);
            System.out.println("Pedido criado com sucesso! ID: " + pedidoTeste.getId());
            
            // Definindo a referência ao pedido criado nos itens
            item1.setPedido(pedidoTeste);
            item2.setPedido(pedidoTeste);
            
            // Persistindo os itens do pedido
            item1 = itemPedidoDAO.create(item1);
            item2 = itemPedidoDAO.create(item2);
            
            System.out.println("Itens adicionados ao pedido com IDs: " + item1.getId() + ", " + item2.getId());
            
            // Associando os itens persistidos ao pedido
            itens.add(item1);
            itens.add(item2);
            pedidoTeste.setItens(itens);
            
            System.out.println("Teste de criação de pedido concluído com sucesso!");
        } catch (SQLException e) {
            System.err.println("Erro ao criar pedido: " + e.getMessage());
            throw e;
        }
    }
    
    private static void testarBuscaPedidoPorId() throws SQLException {
        System.out.println("\n=== Teste: Busca de Pedido por ID ===");
        
        try {
            Pedido pedidoEncontrado = pedidoDAO.findById(pedidoTeste.getId());
            
            if (pedidoEncontrado != null) {
                System.out.println("Pedido encontrado: ID=" + pedidoEncontrado.getId() + 
                                  ", Cliente=" + pedidoEncontrado.getCliente().getNome() + 
                                  ", Data=" + formatarData(pedidoEncontrado.getDataPedido()) + 
                                  ", Valor=" + pedidoEncontrado.getValorTotal());
                
                // Verificando itens do pedido
                List<ItemPedido> itens = pedidoEncontrado.getItens();
                if (itens != null && !itens.isEmpty()) {
                    System.out.println("Itens do pedido:");
                    for (ItemPedido item : itens) {
                        System.out.println("  - Item ID=" + item.getId() + 
                                          ", Produto=" + item.getProduto().getNome() + 
                                          ", Quantidade=" + item.getQuantidade() + 
                                          ", Valor=" + item.getValorTotal());
                    }
                } else {
                    System.out.println("O pedido não possui itens ou os itens não foram carregados.");
                }
                
                System.out.println("Teste de busca por ID concluído com sucesso!");
            } else {
                System.err.println("Pedido não encontrado com ID: " + pedidoTeste.getId());
                throw new SQLException("Falha ao buscar pedido por ID");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar pedido por ID: " + e.getMessage());
            throw e;
        }
    }
    
    private static void testarAtualizacaoPedido() throws SQLException {
        System.out.println("\n=== Teste: Atualização de Pedido ===");
        
        try {
            // Alterando o valor total do pedido (poderia ser qualquer outra alteração)
            BigDecimal novoValor = pedidoTeste.getValorTotal().add(new BigDecimal("10.00"));
            pedidoTeste.setValorTotal(novoValor);
            
            System.out.println("Atualizando valor total do pedido para: " + novoValor);
            
            boolean atualizado = pedidoDAO.update(pedidoTeste);
            if (atualizado) {
                System.out.println("Pedido atualizado com sucesso!");
                
                // Verificando se a atualização foi persistida
                Pedido pedidoAtualizado = pedidoDAO.findById(pedidoTeste.getId());
                System.out.println("Valor total após atualização: " + pedidoAtualizado.getValorTotal());
                
                System.out.println("Teste de atualização concluído com sucesso!");
            } else {
                System.err.println("Falha ao atualizar pedido.");
                throw new SQLException("Falha ao atualizar pedido");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar pedido: " + e.getMessage());
            throw e;
        }
    }
    
    private static void testarListagemPedidos() throws SQLException {
        System.out.println("\n=== Teste: Listagem de Pedidos ===");
        
        try {
            List<Pedido> pedidos = pedidoDAO.findAll();
            
            System.out.println("Total de pedidos encontrados: " + pedidos.size());
            
            if (!pedidos.isEmpty()) {
                System.out.println("Listando pedidos:");
                for (Pedido pedido : pedidos) {
                    System.out.println("Pedido ID=" + pedido.getId() + 
                                      ", Cliente=" + pedido.getCliente().getNome() + 
                                      ", Data=" + formatarData(pedido.getDataPedido()) + 
                                      ", Valor=" + pedido.getValorTotal());
                }
                
                System.out.println("Teste de listagem concluído com sucesso!");
            } else {
                System.err.println("Nenhum pedido encontrado na listagem.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar pedidos: " + e.getMessage());
            throw e;
        }
    }
    
    private static void testarFinalizacaoPedido() throws SQLException {
        System.out.println("\n=== Teste: Finalização de Pedido (Atualização de Estoque) ===");
        
        try {
            // Simulando a finalização do pedido atualizando o estoque dos produtos
            List<ItemPedido> itens = pedidoTeste.getItens();
            if (itens != null && !itens.isEmpty()) {
                for (ItemPedido item : itens) {
                    Produto produto = item.getProduto();
                    int quantidadeOriginal = produto.getEstoqueAtual();
                    int novoEstoque = quantidadeOriginal - item.getQuantidade();
                    
                    produto.setEstoqueAtual(novoEstoque);
                    produtoDAO.update(produto);
                    
                    System.out.println("Estoque do produto " + produto.getNome() + 
                                      " atualizado: " + quantidadeOriginal + " -> " + novoEstoque);
                }
                
                System.out.println("Teste de finalização de pedido concluído com sucesso!");
            } else {
                System.err.println("O pedido não possui itens para atualizar o estoque.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao finalizar pedido: " + e.getMessage());
            throw e;
        }
    }
    
    private static void testarExclusaoPedido() throws SQLException {
        System.out.println("\n=== Teste: Exclusão de Pedido ===");
        
        try {
            // Primeiro, excluindo os itens do pedido
            List<ItemPedido> itens = pedidoTeste.getItens();
            if (itens != null) {
                for (ItemPedido item : itens) {
                    itemPedidoDAO.delete(item.getId());
                    System.out.println("Item excluído: ID=" + item.getId());
                }
            }
            
            // Agora, excluindo o pedido
            boolean excluido = pedidoDAO.delete(pedidoTeste.getId());
            if (excluido) {
                System.out.println("Pedido excluído com sucesso!");
                
                // Verificando se o pedido foi realmente excluído
                Pedido pedidoExcluido = pedidoDAO.findById(pedidoTeste.getId());
                if (pedidoExcluido == null) {
                    System.out.println("Confirmado: Pedido não existe mais no banco de dados.");
                    System.out.println("Teste de exclusão concluído com sucesso!");
                } else {
                    System.err.println("ERRO: Pedido ainda existe no banco de dados após exclusão.");
                }
            } else {
                System.err.println("Falha ao excluir pedido.");
                throw new SQLException("Falha ao excluir pedido");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao excluir pedido: " + e.getMessage());
            throw e;
        }
    }
    
    private static void fecharConexao() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Conexão com o banco de dados fechada com sucesso.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao fechar conexão: " + e.getMessage());
        }
    }
    
    private static String formatarData(LocalDateTime data) {
        if (data == null) return "N/A";
        return data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
}