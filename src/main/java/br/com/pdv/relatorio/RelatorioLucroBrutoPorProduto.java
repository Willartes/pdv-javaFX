package br.com.pdv.relatorio;

import br.com.pdv.dao.ProdutoDAO;
import br.com.pdv.dao.VendaDAO;
import br.com.pdv.util.LogUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Relatório de Lucro Bruto Por Produto
 * Calcula e exibe o lucro bruto obtido por produto no período, considerando
 * o preço de custo e preço de venda.
 */
public class RelatorioLucroBrutoPorProduto extends RelatorioBase {
    private final VendaDAO vendaDAO;
    private final ProdutoDAO produtoDAO;
    
    /**
     * Construtor do relatório de lucro bruto por produto
     * 
     * @param dataInicio Data inicial do período
     * @param dataFim Data final do período
     */
    public RelatorioLucroBrutoPorProduto(LocalDateTime dataInicio, LocalDateTime dataFim) {
        super("RELATÓRIO DE LUCRO BRUTO POR PRODUTO", dataInicio, dataFim);
        this.vendaDAO = RelatorioManager.getInstance().getVendaDAO();
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
            
            // Parte 1: Lucro bruto por produto
            relatorio.append("LUCRO BRUTO POR PRODUTO\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            // Assumindo que existe um campo preco_custo na tabela produtos
            String sql = "SELECT p.id, p.codigo, p.nome, " +
                       "p.preco as preco_venda, " +
                       "(SELECT COALESCE(AVG(preco_unitario), p.preco * 0.6) " +
                       " FROM itens_compra ic " +
                       " JOIN compras c ON ic.compra_id = c.id " +
                       " WHERE ic.produto_id = p.id " +
                       " AND c.data_compra <= v.data_venda) as preco_custo_estimado, " +
                       "SUM(ip.quantidade) as quantidade_total, " +
                       "SUM(ip.valor_total) as receita_total, " +
                       "SUM(ip.quantidade * " +
                       "   (SELECT COALESCE(AVG(ic.preco_unitario), p.preco * 0.6) " +
                       "    FROM itens_compra ic " +
                       "    JOIN compras c ON ic.compra_id = c.id " +
                       "    WHERE ic.produto_id = p.id " +
                       "    AND c.data_compra <= v.data_venda)) as custo_total " +
                       "FROM produtos p " +
                       "JOIN itens_pedido ip ON p.id = ip.produto_id " +
                       "JOIN pedidos pe ON ip.pedido_id = pe.id " +
                       "JOIN vendas v ON pe.id = v.pedido_id " +
                       "WHERE v.data_venda BETWEEN ? AND ? " +
                       "AND v.status = 'FINALIZADA' " +
                       "GROUP BY p.id, p.codigo, p.nome, p.preco " +
                       "ORDER BY (SUM(ip.valor_total) - SUM(ip.quantidade * " +
                       "   (SELECT COALESCE(AVG(ic.preco_unitario), p.preco * 0.6) " +
                       "    FROM itens_compra ic " +
                       "    JOIN compras c ON ic.compra_id = c.id " +
                       "    WHERE ic.produto_id = p.id " +
                       "    AND c.data_compra <= v.data_venda))) DESC";
            
            try {
                stmt = conn.prepareStatement(sql);
                stmt.setObject(1, dataInicio);
                stmt.setObject(2, dataFim);
                
                rs = stmt.executeQuery();
            } catch (SQLException e) {
                // Se a consulta falhar (talvez devido à subconsulta complexa ou tabelas não existentes)
                // Tentar uma consulta mais simples assumindo um custo estimado
                LogUtil.warn(this.getClass(), "Usando cálculo simplificado de lucro bruto: " + e.getMessage());
                
                sql = "SELECT p.id, p.codigo, p.nome, " +
                    "p.preco as preco_venda, " +
                    "(p.preco * 0.6) as preco_custo_estimado, " + // Estimativa simples: custo = 60% do preço
                    "SUM(ip.quantidade) as quantidade_total, " +
                    "SUM(ip.valor_total) as receita_total, " +
                    "SUM(ip.quantidade * (p.preco * 0.6)) as custo_total " +
                    "FROM produtos p " +
                    "JOIN itens_pedido ip ON p.id = ip.produto_id " +
                    "JOIN pedidos pe ON ip.pedido_id = pe.id " +
                    "JOIN vendas v ON pe.id = v.pedido_id " +
                    "WHERE v.data_venda BETWEEN ? AND ? " +
                    "AND v.status = 'FINALIZADA' " +
                    "GROUP BY p.id, p.codigo, p.nome, p.preco " +
                    "ORDER BY (SUM(ip.valor_total) - SUM(ip.quantidade * (p.preco * 0.6))) DESC";
                
                stmt = conn.prepareStatement(sql);
                stmt.setObject(1, dataInicio);
                stmt.setObject(2, dataFim);
                
                rs = stmt.executeQuery();
            }
            
            relatorio.append(String.format("%-8s %-25s %-8s %-8s %-10s %-12s %-12s %-12s %-8s\n", 
                                         "CÓDIGO", "PRODUTO", "P.VENDA", "P.CUSTO", "QUANT.", 
                                         "RECEITA", "CUSTO", "LUCRO", "MARGEM%"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            BigDecimal receitaTotal = BigDecimal.ZERO;
            BigDecimal custoTotal = BigDecimal.ZERO;
            BigDecimal lucroTotal = BigDecimal.ZERO;
            
            // Armazenar dados para análises adicionais
            List<ProdutoLucro> produtosLucro = new ArrayList<>();
            
            while (rs.next()) {
                String codigo = rs.getString("codigo");
                String nome = rs.getString("nome");
                BigDecimal precoVenda = rs.getBigDecimal("preco_venda");
                BigDecimal precoCusto = rs.getBigDecimal("preco_custo_estimado");
                int quantidade = rs.getInt("quantidade_total");
                BigDecimal receita = rs.getBigDecimal("receita_total");
                BigDecimal custo = rs.getBigDecimal("custo_total");
                BigDecimal lucro = receita.subtract(custo);
                
                // Calcular margem de lucro
                BigDecimal margem = BigDecimal.ZERO;
                if (receita.compareTo(BigDecimal.ZERO) > 0) {
                    margem = lucro.multiply(new BigDecimal(100))
                                .divide(receita, 2, RoundingMode.HALF_UP);
                }
                
                relatorio.append(String.format("%-8s %-25s R$%-6.2f R$%-6.2f %-10d R$%-10.2f R$%-10.2f R$%-10.2f %-7.2f%%\n", 
                                             codigo, 
                                             (nome.length() > 23 ? nome.substring(0, 20) + "..." : nome),
                                             precoVenda,
                                             precoCusto,
                                             quantidade,
                                             receita,
                                             custo,
                                             lucro,
                                             margem));
                
                receitaTotal = receitaTotal.add(receita);
                custoTotal = custoTotal.add(custo);
                lucroTotal = lucroTotal.add(lucro);
                
                // Armazenar para análises
                produtosLucro.add(new ProdutoLucro(codigo, nome, receita, custo, lucro, margem, quantidade));
            }
            
            // Calcular margem média
            BigDecimal margemMedia = BigDecimal.ZERO;
            if (receitaTotal.compareTo(BigDecimal.ZERO) > 0) {
                margemMedia = lucroTotal.multiply(new BigDecimal(100))
                                .divide(receitaTotal, 2, RoundingMode.HALF_UP);
            }
            
            relatorio.append("--------------------------------------------------------------------------------\n");
            relatorio.append(String.format("%-54s R$%-10.2f R$%-10.2f R$%-10.2f %-7.2f%%\n", 
                                         "TOTAL GERAL",
                                         receitaTotal,
                                         custoTotal,
                                         lucroTotal,
                                         margemMedia));
            
            // Parte 2: Top 5 produtos com maior margem de lucro
            relatorio.append("\nTOP 5 PRODUTOS COM MAIOR MARGEM DE LUCRO\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            relatorio.append(String.format("%-8s %-25s %-15s %-15s %-10s\n", 
                                         "CÓDIGO", "PRODUTO", "LUCRO", "RECEITA", "MARGEM%"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            // Ordenar produtos por margem
            produtosLucro.sort((p1, p2) -> p2.getMargem().compareTo(p1.getMargem()));
            
            // Exibir top 5
            int count = 0;
            for (ProdutoLucro produto : produtosLucro) {
                if (count < 5 && produto.getReceita().compareTo(BigDecimal.ZERO) > 0) {
                    relatorio.append(String.format("%-8s %-25s R$%-13.2f R$%-13.2f %-9.2f%%\n", 
                                                 produto.getCodigo(), 
                                                 (produto.getNome().length() > 23 ? produto.getNome().substring(0, 20) + "..." : produto.getNome()),
                                                 produto.getLucro(),
                                                 produto.getReceita(),
                                                 produto.getMargem()));
                    count++;
                }
            }
            
            // Parte 3: Top 5 produtos com maior lucro bruto
            relatorio.append("\nTOP 5 PRODUTOS COM MAIOR LUCRO BRUTO\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            relatorio.append(String.format("%-8s %-25s %-15s %-15s %-10s\n", 
                                         "CÓDIGO", "PRODUTO", "LUCRO", "QUANTIDADE", "MARGEM%"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            // Ordenar produtos por lucro
            produtosLucro.sort((p1, p2) -> p2.getLucro().compareTo(p1.getLucro()));
            
            // Exibir top 5
            count = 0;
            for (ProdutoLucro produto : produtosLucro) {
                if (count < 5) {
                    relatorio.append(String.format("%-8s %-25s R$%-13.2f %-15d %-9.2f%%\n", 
                                                 produto.getCodigo(), 
                                                 (produto.getNome().length() > 23 ? produto.getNome().substring(0, 20) + "..." : produto.getNome()),
                                                 produto.getLucro(),
                                                 produto.getQuantidade(),
                                                 produto.getMargem()));
                    count++;
                }
            }
            
            // Parte 4: Análise de contribuição no lucro total
            relatorio.append("\nCONTRIBUIÇÃO NO LUCRO TOTAL\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            // Ordenar produtos por contribuição no lucro
            produtosLucro.sort((p1, p2) -> p2.getLucro().compareTo(p1.getLucro()));
            
            // Para os top 10 produtos
            count = 0;
            for (ProdutoLucro produto : produtosLucro) {
                if (count < 10 && lucroTotal.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal contribuicao = produto.getLucro()
                                                .multiply(new BigDecimal(100))
                                                .divide(lucroTotal, 2, RoundingMode.HALF_UP);
                    
                    // Gráfico de barras
                    int barras = contribuicao.intValue() / 2; // Cada barra = 2%
                    StringBuilder grafico = new StringBuilder();
                    for (int i = 0; i < barras; i++) {
                        grafico.append("█");
                    }
                    
                    relatorio.append(String.format("%-30s [%-50s] %-5.2f%% (R$%.2f)\n", 
                                                 (produto.getNome().length() > 28 ? produto.getNome().substring(0, 25) + "..." : produto.getNome()),
                                                 grafico.toString(),
                                                 contribuicao,
                                                 produto.getLucro()));
                    count++;
                }
            }
            
        } catch (SQLException e) {
            LogUtil.error(this.getClass(), "Erro ao gerar relatório de lucro bruto por produto", e);
            throw e;
        } finally {
            closeResources(conn, stmt, rs);
        }
        
        return relatorio.toString();
    }
    
    /**
     * Classe interna auxiliar para armazenar informações de lucro por produto
     */
    private static class ProdutoLucro {
        private final String codigo;
        private final String nome;
        private final BigDecimal receita;
        private final BigDecimal custo;
        private final BigDecimal lucro;
        private final BigDecimal margem;
        private final int quantidade;
        
        public ProdutoLucro(String codigo, String nome, BigDecimal receita, BigDecimal custo, 
                           BigDecimal lucro, BigDecimal margem, int quantidade) {
            this.codigo = codigo;
            this.nome = nome;
            this.receita = receita;
            this.custo = custo;
            this.lucro = lucro;
            this.margem = margem;
            this.quantidade = quantidade;
        }
        
        public String getCodigo() {
            return codigo;
        }
        
        public String getNome() {
            return nome;
        }
        
        public BigDecimal getReceita() {
            return receita;
        }
        
        public BigDecimal getCusto() {
            return custo;
        }
        
        public BigDecimal getLucro() {
            return lucro;
        }
        
        public BigDecimal getMargem() {
            return margem;
        }
        
        public int getQuantidade() {
            return quantidade;
        }
    }
}