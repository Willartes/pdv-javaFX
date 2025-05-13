package br.com.pdv.relatorio;

import br.com.pdv.dao.ProdutoDAO;
import br.com.pdv.util.LogUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Relatório de Valor de Estoque
 * Exibe o valor monetário do estoque atual, calculando o custo e o valor
 * potencial de venda dos produtos em estoque.
 */
public class RelatorioValorEstoque extends RelatorioBase {
    private final ProdutoDAO produtoDAO;
    
    /**
     * Construtor do relatório de valor de estoque
     */
    public RelatorioValorEstoque() {
        super("RELATÓRIO DE VALOR DE ESTOQUE");
        this.produtoDAO = RelatorioManager.getInstance().getProdutoDAO();
    }
    
    @Override
    public String gerarRelatorio() throws SQLException {
        StringBuilder relatorio = new StringBuilder(gerarCabecalho());
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            
            // Parte 1: Valor do estoque por produto
            relatorio.append("VALOR DO ESTOQUE POR PRODUTO\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            // Consulta para obter valor do estoque
            String sql = "SELECT p.id, p.codigo, p.nome, p.estoque_atual, " +
                       "p.preco as preco_venda, " +
                       // Tentativa de obter preço de custo, poderia vir de uma tabela de compras ou ser um campo na tabela produtos
                       "COALESCE(p.preco_custo, p.preco * 0.6) as preco_custo, " + 
                       "(p.estoque_atual * COALESCE(p.preco_custo, p.preco * 0.6)) as valor_custo, " +
                       "(p.estoque_atual * p.preco) as valor_venda " +
                       "FROM produtos p " +
                       "WHERE p.ativo = 1 " +
                       "ORDER BY valor_custo DESC";
            
            try {
                stmt = conn.prepareStatement(sql);
                rs = stmt.executeQuery();
            } catch (SQLException e) {
                // Se falhar (talvez o campo preco_custo não exista), tenta uma consulta alternativa
                LogUtil.warn(this.getClass(), "Usando cálculo simplificado de valor de estoque: " + e.getMessage());
                
                sql = "SELECT p.id, p.codigo, p.nome, p.estoque_atual, " +
                    "p.preco as preco_venda, " +
                    "(p.preco * 0.6) as preco_custo, " + // Estimativa simples: custo = 60% do preço
                    "(p.estoque_atual * (p.preco * 0.6)) as valor_custo, " +
                    "(p.estoque_atual * p.preco) as valor_venda " +
                    "FROM produtos p " +
                    "WHERE p.ativo = 1 " +
                    "ORDER BY valor_custo DESC";
                
                stmt = conn.prepareStatement(sql);
                rs = stmt.executeQuery();
            }
            
            relatorio.append(String.format("%-8s %-30s %-10s %-15s %-15s %-15s %-15s\n", 
                                         "CÓDIGO", "PRODUTO", "ESTOQUE", "CUSTO UNIT.", "PREÇO VENDA", "VALOR CUSTO", "VALOR VENDA"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            BigDecimal valorTotalCusto = BigDecimal.ZERO;
            BigDecimal valorTotalVenda = BigDecimal.ZERO;
            int quantidadeTotal = 0;
            
            // Armazenar dados para análises adicionais
            Map<String, BigDecimal> valorCustoPorCategoria = new HashMap<>();
            Map<String, BigDecimal> valorVendaPorCategoria = new HashMap<>();
            
            while (rs.next()) {
                String codigo = rs.getString("codigo");
                String nome = rs.getString("nome");
                int estoqueAtual = rs.getInt("estoque_atual");
                BigDecimal precoCusto = rs.getBigDecimal("preco_custo");
                BigDecimal precoVenda = rs.getBigDecimal("preco_venda");
                BigDecimal valorCusto = rs.getBigDecimal("valor_custo");
                BigDecimal valorVenda = rs.getBigDecimal("valor_venda");
                
                relatorio.append(String.format("%-8s %-30s %-10d R$ %-12.2f R$ %-12.2f R$ %-12.2f R$ %-12.2f\n", 
                                             codigo, 
                                             (nome.length() > 28 ? nome.substring(0, 25) + "..." : nome),
                                             estoqueAtual,
                                             precoCusto,
                                             precoVenda,
                                             valorCusto,
                                             valorVenda));
                
                valorTotalCusto = valorTotalCusto.add(valorCusto);
                valorTotalVenda = valorTotalVenda.add(valorVenda);
                quantidadeTotal += estoqueAtual;
                
                // Tentar obter categoria do produto (se existir na consulta)
                try {
                    String categoria = rs.getString("categoria");
                    if (categoria != null) {
                        // Acumular por categoria
                        valorCustoPorCategoria.put(categoria, 
                                valorCustoPorCategoria.getOrDefault(categoria, BigDecimal.ZERO).add(valorCusto));
                        valorVendaPorCategoria.put(categoria, 
                                valorVendaPorCategoria.getOrDefault(categoria, BigDecimal.ZERO).add(valorVenda));
                    }
                } catch (SQLException e) {
                    // Categoria não disponível na consulta, ignorar
                }
            }
            
            relatorio.append("--------------------------------------------------------------------------------\n");
            relatorio.append(String.format("%-49s %-10d R$ %-12.2f R$ %-12.2f\n", 
                                         "TOTAL GERAL",
                                         quantidadeTotal,
                                         valorTotalCusto,
                                         valorTotalVenda));
            
            // Parte 2: Análise de valor do estoque por categoria
            try {
                // Tentar obter valor por categoria se não foi possível obter da consulta anterior
                if (valorCustoPorCategoria.isEmpty()) {
                    sql = "SELECT c.nome as categoria, " +
                         "SUM(p.estoque_atual) as quantidade_total, " +
                         "SUM(p.estoque_atual * COALESCE(p.preco_custo, p.preco * 0.6)) as valor_custo, " +
                         "SUM(p.estoque_atual * p.preco) as valor_venda " +
                         "FROM categorias c " +
                         "JOIN produtos p ON c.id = p.categoria_id " +
                         "WHERE p.ativo = 1 " +
                         "GROUP BY c.nome " +
                         "ORDER BY valor_custo DESC";
                    
                    try {
                        stmt = conn.prepareStatement(sql);
                        rs = stmt.executeQuery();
                        
                        // Limpar mapas anteriores
                        valorCustoPorCategoria.clear();
                        valorVendaPorCategoria.clear();
                        
                        while (rs.next()) {
                            String categoria = rs.getString("categoria");
                            BigDecimal valorCusto = rs.getBigDecimal("valor_custo");
                            BigDecimal valorVenda = rs.getBigDecimal("valor_venda");
                            
                            valorCustoPorCategoria.put(categoria, valorCusto);
                            valorVendaPorCategoria.put(categoria, valorVenda);
                        }
                    } catch (SQLException e) {
                        // Se falhar, continuar sem análise por categoria
                        LogUtil.warn(this.getClass(), "Não foi possível obter valor de estoque por categoria: " + e.getMessage());
                    }
                }
                
                // Se há dados por categoria, exibi-los
                if (!valorCustoPorCategoria.isEmpty()) {
                    relatorio.append("\nVALOR DE ESTOQUE POR CATEGORIA\n");
                    relatorio.append("--------------------------------------------------------------------------------\n");
                    
                    relatorio.append(String.format("%-30s %-15s %-15s %-15s\n", 
                                                 "CATEGORIA", "VALOR CUSTO", "VALOR VENDA", "% DO TOTAL"));
                    relatorio.append("--------------------------------------------------------------------------------\n");
                    
                    for (Map.Entry<String, BigDecimal> entry : valorCustoPorCategoria.entrySet()) {
                        String categoria = entry.getKey();
                        BigDecimal valorCusto = entry.getValue();
                        BigDecimal valorVenda = valorVendaPorCategoria.get(categoria);
                        
                        // Calcular percentual do valor total
                        double percentual = valorTotalCusto.doubleValue() > 0 ? 
                                valorCusto.doubleValue() * 100 / valorTotalCusto.doubleValue() : 0;
                        
                        relatorio.append(String.format("%-30s R$ %-12.2f R$ %-12.2f %.2f%%\n", 
                                                     (categoria.length() > 28 ? categoria.substring(0, 25) + "..." : categoria),
                                                     valorCusto,
                                                     valorVenda,
                                                     percentual));
                    }
                }
            } catch (Exception e) {
                // Se ocorrer erro na análise por categoria, apenas registrar e continuar
                LogUtil.warn(this.getClass(), "Erro ao gerar análise por categoria: " + e.getMessage());
            }
            
            // Parte 3: Análise de giro de estoque
            relatorio.append("\nANÁLISE DE GIRO DE ESTOQUE\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            // Período de 30 dias para análise de giro
            LocalDateTime dataInicio30Dias = LocalDateTime.now().minusDays(30);
            
            sql = "SELECT p.id, p.codigo, p.nome, p.estoque_atual, " +
                 "COALESCE(SUM(ip.quantidade), 0) as vendas_periodo, " +
                 "p.estoque_atual * COALESCE(p.preco_custo, p.preco * 0.6) as valor_estoque, " +
                 "CASE WHEN p.estoque_atual > 0 " +
                 "  THEN COALESCE(SUM(ip.quantidade), 0) / p.estoque_atual " +
                 "  ELSE 0 END as indice_giro " +
                 "FROM produtos p " +
                 "LEFT JOIN itens_pedido ip ON p.id = ip.produto_id " +
                 "LEFT JOIN pedidos pe ON ip.pedido_id = pe.id " +
                 "LEFT JOIN vendas v ON pe.id = v.pedido_id " +
                 "AND v.data_venda BETWEEN ? AND NOW() " +
                 "AND v.status = 'FINALIZADA' " +
                 "WHERE p.ativo = 1 AND p.estoque_atual > 0 " +
                 "GROUP BY p.id, p.codigo, p.nome, p.estoque_atual, p.preco, p.preco_custo " +
                 "ORDER BY indice_giro DESC " +
                 "LIMIT 20";
            
            try {
                stmt = conn.prepareStatement(sql);
                stmt.setObject(1, dataInicio30Dias);
                
                rs = stmt.executeQuery();
                
                relatorio.append(String.format("%-8s %-30s %-15s %-15s %-15s %-15s\n", 
                                             "CÓDIGO", "PRODUTO", "ESTOQUE", "VENDAS 30d", "VALOR ESTOQUE", "ÍNDICE GIRO"));
                relatorio.append("--------------------------------------------------------------------------------\n");
                
                while (rs.next()) {
                    String codigo = rs.getString("codigo");
                    String nome = rs.getString("nome");
                    int estoqueAtual = rs.getInt("estoque_atual");
                    int vendasPeriodo = rs.getInt("vendas_periodo");
                    BigDecimal valorEstoque = rs.getBigDecimal("valor_estoque");
                    double indiceGiro = rs.getDouble("indice_giro");
                    
                    relatorio.append(String.format("%-8s %-30s %-15d %-15d R$ %-12.2f %.2f\n", 
                                                 codigo, 
                                                 (nome.length() > 28 ? nome.substring(0, 25) + "..." : nome),
                                                 estoqueAtual,
                                                 vendasPeriodo,
                                                 valorEstoque,
                                                 indiceGiro));
                }
            } catch (SQLException e) {
                // Se falhar, continuar sem análise de giro
                LogUtil.warn(this.getClass(), "Não foi possível obter análise de giro de estoque: " + e.getMessage());
                relatorio.append("Análise de giro de estoque não disponível.\n");
            }
            
            // Parte 4: Resumo e métricas de estoque
            relatorio.append("\nRESUMO E MÉTRICAS DE ESTOQUE\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            // Calcular valor médio por item e outras métricas
            int produtosComEstoque = 0;
            int produtosSemEstoque = 0;
            
            sql = "SELECT COUNT(*) as total_produtos, " +
                 "SUM(CASE WHEN estoque_atual > 0 THEN 1 ELSE 0 END) as produtos_com_estoque, " +
                 "SUM(CASE WHEN estoque_atual = 0 THEN 1 ELSE 0 END) as produtos_sem_estoque " +
                 "FROM produtos " +
                 "WHERE ativo = 1";
            
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                int totalProdutos = rs.getInt("total_produtos");
                produtosComEstoque = rs.getInt("produtos_com_estoque");
                produtosSemEstoque = rs.getInt("produtos_sem_estoque");
                
                relatorio.append(String.format("Total de produtos ativos: %d\n", totalProdutos));
                relatorio.append(String.format("Produtos com estoque: %d (%.2f%%)\n", 
                                             produtosComEstoque,
                                             (double) produtosComEstoque / totalProdutos * 100));
                relatorio.append(String.format("Produtos sem estoque: %d (%.2f%%)\n", 
                                             produtosSemEstoque,
                                             (double) produtosSemEstoque / totalProdutos * 100));
            }
            
            // Custo médio por item em estoque
            if (quantidadeTotal > 0) {
                BigDecimal custoMedioPorItem = valorTotalCusto.divide(new BigDecimal(quantidadeTotal), 2, BigDecimal.ROUND_HALF_UP);
                relatorio.append(String.format("Custo médio por item em estoque: R$ %.2f\n", custoMedioPorItem));
            }
            
            // Margem potencial total
            if (valorTotalCusto.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal margemPotencial = valorTotalVenda.subtract(valorTotalCusto);
                double percentualMargem = margemPotencial.doubleValue() * 100 / valorTotalCusto.doubleValue();
                relatorio.append(String.format("Margem potencial do estoque: R$ %.2f (%.2f%%)\n", 
                                             margemPotencial, percentualMargem));
            }
            
        } catch (SQLException e) {
            LogUtil.error(this.getClass(), "Erro ao gerar relatório de valor de estoque", e);
            throw e;
        } finally {
            closeResources(conn, stmt, rs);
        }
        
        return relatorio.toString();
    }
}