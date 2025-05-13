package br.com.pdv.util;

import br.com.pdv.dao.*;
import br.com.pdv.model.*;
import br.com.pdv.exception.CpfCnpjDuplicadoException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Objects;

/**
 * Classe para testar as operações do PedidoDAO.
 */
public class TestePedidoDAO {

    private static final Logger logger = Logger.getLogger(TestePedidoDAO.class.getName());
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
            logger.log(Level.SEVERE, "\nErro ao executar testes: " + e.getMessage(), e);
            e.printStackTrace();
        } finally {
            fecharConexao();
        }
    }

    private static void inicializarConexao() throws SQLException {
        try {
            // Obtém uma conexão do singleton DatabaseConnection
            connection = DatabaseConnection.getInstance().getConnection();
            logger.info("Conexão estabelecida com sucesso.");
        } catch (SQLException e) {
            throw new SQLException("Erro ao estabelecer conexão: " + e.getMessage(), e);
        }
    }

    private static void inicializarDAOs() throws SQLException {
        try {
            // Inicializa todos os DAOs usando o padrão singleton
            clienteDAO = ClienteDAO.getInstance();
            usuarioDAO = UsuarioDAO.getInstance();
            produtoDAO = ProdutoDAO.getInstance();
            itemPedidoDAO = ItemPedidoDAO.getInstance();
            pedidoDAO = PedidoDAO.getInstance();
            
            logger.info("Todos os DAOs inicializados com sucesso.");
        } catch (Exception e) {
            throw new SQLException("Falha ao inicializar DAOs: " + e.getMessage(), e);
        }
    }

    private static void fecharConexao() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Conexão com o banco de dados fechada com sucesso.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao fechar conexão: " + e.getMessage(), e);
        }
    }

    private static void criarDadosTeste() throws SQLException, CpfCnpjDuplicadoException {
        logger.info("\n=== Criando dados para teste ===");

        // Criar um cliente para teste
        cliente = new Cliente(
            "Cliente Teste DAO",
            "111.222.333-99",
            "Rua de Teste, 123",
            "(11) 99999-8888",
            "teste@email.com"
        );
        
        // Definir a data de cadastro como a data atual
        cliente.setDataCadastro(new Date());
        
        try {
            cliente = clienteDAO.create(cliente);
            logger.info("Cliente criado com sucesso. ID: " + cliente.getId());
        } catch (CpfCnpjDuplicadoException e) {
            // Resto do código permanece igual
        }
        try {
            cliente = clienteDAO.create(cliente);
            logger.info("Cliente criado com sucesso. ID: " + cliente.getId());
        } catch (CpfCnpjDuplicadoException e) {
            logger.warning("Erro ao criar cliente devido a CPF/CNPJ duplicado. Buscando cliente existente...");
            cliente = buscarClienteExistente("111.222.333-99");
            if (cliente == null) {
                throw new SQLException("Não foi possível encontrar ou criar cliente para teste.");
            }
            logger.info("Cliente existente localizado. ID: " + cliente.getId());
        }

        // Criar um usuário para teste
        usuario = new Usuario("Usuario Teste", "usuarioteste", "senha123", "VENDEDOR");
        try {
            usuario = usuarioDAO.create(usuario);
            logger.info("Usuário criado com sucesso. ID: " + usuario.getId());
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warning("Erro ao criar usuário devido a login duplicado. Buscando usuário existente...");
            usuario = buscarUsuarioExistente("usuarioteste");
            if (usuario == null) {
                throw new SQLException("Não foi possível encontrar ou criar usuário para teste.");
            }
            logger.info("Usuário existente localizado. ID: " + usuario.getId());
        }

        // Criar produtos para teste
        // Criar produtos para teste com códigos únicos
        String codigoProduto1 = "PROD-TESTE-1-" + System.currentTimeMillis();
        String codigoProduto2 = "PROD-TESTE-2-" + System.currentTimeMillis();

        produto1 = new Produto(
            codigoProduto1,
            "Produto Teste 1",
            "Descrição do Produto Teste 1",
            new BigDecimal("50.00"),
            100,
            true
        );
        produto2 = new Produto(
            codigoProduto2,
            "Produto Teste 2",
            "Descrição do Produto Teste 2",
            new BigDecimal("75.50"),
            50,
            true
        );
        try {
            produto1 = produtoDAO.create(produto1);
            logger.info("Produto 1 criado com sucesso. ID: " + produto1.getId());
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warning("Erro ao criar produto 1 devido a código duplicado. Buscando produto existente...");
            produto1 = buscarProdutoExistente("PROD-TESTE-1");
            if (produto1 == null) {
                throw new SQLException("Não foi possível encontrar ou criar produto 1 para teste.");
            }
            logger.info("Produto 1 existente localizado. ID: " + produto1.getId());
        }
        try {
            produto2 = produtoDAO.create(produto2);
            logger.info("Produto 2 criado com sucesso. ID: " + produto2.getId());
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warning("Erro ao criar produto 2 devido a código duplicado. Buscando produto existente...");
            produto2 = buscarProdutoExistente("PROD-TESTE-2");
            if (produto2 == null) {
                throw new SQLException("Não foi possível encontrar ou criar produto 2 para teste.");
            }
            logger.info("Produto 2 existente localizado. ID: " + produto2.getId());
        }

        logger.info("Dados de teste criados com sucesso!");
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
        logger.info("\n=== Teste: Criação de Pedido ===");

        Pedido novoPedido = criarPedidoTeste();

        try {
            pedidoTeste = pedidoDAO.create(novoPedido);
            logger.info("Pedido criado com sucesso. ID: " + pedidoTeste.getId());
            adicionarItensPedido();

            logger.info("Teste de criação de pedido concluído com sucesso!");
        } catch (SQLException e) {
            throw new SQLException("Erro ao criar pedido", e);
        }
    }

    private static void testarBuscaPedidoPorId() throws SQLException {
        logger.info("\n=== Teste: Busca de Pedido por ID ===");

        try {
            Pedido pedidoEncontrado = pedidoDAO.findById(pedidoTeste.getId());

            if (pedidoEncontrado != null) {
                imprimirDetalhesPedido(pedidoEncontrado);
                logger.info("Teste de busca por ID concluído com sucesso!");
            } else {
                throw new SQLException("Pedido não encontrado");
            }
        } catch (SQLException e) {
            throw new SQLException("Erro ao buscar pedido por ID", e);
        }
    }

    private static void testarAtualizacaoPedido() throws SQLException {
        logger.info("\n=== Teste: Atualização de Pedido ===");
        atualizarValorPedido(new BigDecimal("10.00"));
        logger.info("Teste de atualização concluído com sucesso!");
    }

    private static void testarListagemPedidos() throws SQLException {
        logger.info("\n=== Teste: Listagem de Pedidos ===");

        try {
            List<Pedido> pedidos = pedidoDAO.findAll();
            imprimirListagemPedidos(pedidos);
        } catch (SQLException e) {
            throw new SQLException("Erro ao listar pedidos", e);
        }
    }

    private static void testarFinalizacaoPedido() throws SQLException {
        logger.info("\n=== Teste: Finalização de Pedido ===");
        atualizarEstoqueProdutos();
        logger.info("Teste de finalização de pedido concluído com sucesso!");
    }

    private static void testarExclusaoPedido() throws SQLException {
        logger.info("\n=== Teste: Exclusão de Pedido ===");
        excluirPedido();
    }

    private static String formatarData(LocalDateTime data) {
        return data != null ? data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "N/A";
    }

    private static Cliente buscarClienteExistente(String cpfCnpj) throws SQLException {
        try {
            return clienteDAO.findByCpfCnpj(cpfCnpj);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao buscar cliente existente por CPF/CNPJ: " + e.getMessage(), e);
            return null;
        }
    }

    private static Usuario buscarUsuarioExistente(String login) throws SQLException {
        try {
            return usuarioDAO.findByLogin(login);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao buscar usuário existente por login: " + e.getMessage(), e);
            return null;
        }
    }

    private static Produto buscarProdutoExistente(String codigo) throws SQLException {
        try {
            return produtoDAO.findByCodigo(codigo);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao buscar produto existente por código: " + e.getMessage(), e);
            return null;
        }
    }

    private static Pedido criarPedidoTeste() {
        Pedido novoPedido = new Pedido();
        novoPedido.setCliente(cliente);
        novoPedido.setUsuario(usuario);
        novoPedido.setVendedor(usuario);
        novoPedido.setDataPedido(LocalDateTime.now());
        novoPedido.setValorTotal(BigDecimal.ZERO);
        novoPedido.setStatus(PedidoDAO.STATUS_ABERTO); // Definir o status como ABERTO

        return novoPedido;
    }

    private static void adicionarItensPedido() throws SQLException {
        List<ItemPedido> itens = new ArrayList<>();
        ItemPedido item1 = new ItemPedido(produto1, 2);
        ItemPedido item2 = new ItemPedido(produto2, 1);

        item1.setPedido(pedidoTeste);
        item2.setPedido(pedidoTeste);

        item1 = itemPedidoDAO.create(item1);
        item2 = itemPedidoDAO.create(item2);

        itens.add(item1);
        itens.add(item2);
        pedidoTeste.setItens(itens);
    }

    private static void atualizarValorPedido(BigDecimal incremento) throws SQLException {
        BigDecimal novoValor = pedidoTeste.getValorTotal().add(incremento);
        pedidoTeste.setValorTotal(novoValor);

        if (pedidoDAO.update(pedidoTeste)) {
            logger.info("Pedido atualizado. Novo valor total: " + pedidoTeste.getValorTotal());
        } else {
            throw new SQLException("Falha ao atualizar pedido");
        }
    }

    private static void imprimirListagemPedidos(List<Pedido> pedidos) throws SQLException {
        logger.info("Total de pedidos encontrados: " + pedidos.size());

        for (Pedido pedido : pedidos) {
            imprimirDetalhesPedido(pedido);
        }
    }

    private static void imprimirDetalhesPedido(Pedido pedido) {
        logger.info("Pedido ID=" + pedido.getId()
                + ", Cliente=" + pedido.getCliente().getNome()
                + ", Data=" + formatarData(pedido.getDataPedido())
                + ", Valor=" + pedido.getValorTotal());

        List<ItemPedido> itens = pedido.getItens();
        if (itens != null && !itens.isEmpty()) {
            logger.info("Itens do pedido:");
            for (ItemPedido item : itens) {
                logger.info("  - Item ID=" + item.getId()
                        + ", Produto=" + item.getProduto().getNome()
                        + ", Quantidade=" + item.getQuantidade()
                        + ", Valor=" + item.getValorTotal());
            }
        }
    }

    private static void atualizarEstoqueProdutos() throws SQLException {
        List<ItemPedido> itens = pedidoTeste.getItens();
        if (itens != null && !itens.isEmpty()) {
            for (ItemPedido item : itens) {
                Produto produto = item.getProduto();
                int novoEstoque = produto.getEstoqueAtual() - item.getQuantidade();
                produto.setEstoqueAtual(novoEstoque);
                produtoDAO.update(produto);
            }
        } else {
            throw new SQLException("O pedido não possui itens para atualizar o estoque.");
        }
    }

    private static void excluirPedido() throws SQLException {
        List<ItemPedido> itens = pedidoTeste.getItens();

        if (itens != null) {
            for (ItemPedido item : itens) {
                itemPedidoDAO.delete(item.getId());
            }
        }

        if (pedidoDAO.delete(pedidoTeste.getId())) {
            logger.info("Pedido excluído com sucesso!");
        } else {
            throw new SQLException("Falha ao excluir pedido");
        }
    }
}