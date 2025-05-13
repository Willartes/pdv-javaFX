package br.com.pdv.relatorio;

import br.com.pdv.dao.ProdutoDAO;
import br.com.pdv.util.LogUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Relatório de Nível de Estoque
 * Exibe os níveis de estoque atuais dos produtos, identificando situações críticas
 */
public class RelatorioNivelEstoque extends RelatorioBase {
    private final ProdutoDAO produtoDAO;
    
    /**
     * Construtor do relatório de nível de estoque
     */
    public RelatorioNivelEstoque() {
        super("RELATÓRIO DE NÍVEL DE ESTOQUE");
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
            
            // Consulta para obter níveis de estoque
            String sql = "SELECT p.id, p.codigo, p.nome, p.estoque_atual, p.estoque_minimo, " +
                       "CASE " +
                       "  WHEN p.estoque_atual <= 0 THEN 'Sem Estoque' " +
                       "  WHEN p.estoque_atual < p.estoque_minimo THEN 'Crítico' " +
                       "  WHEN p.estoque_atual < (p.estoque_minimo * 2) THEN 'Baixo' " +
                       "  WHEN p.estoque_atual < (p.estoque_minimo * 4) THEN 'Regular' " +
                       "  ELSE 'Adequado' " +
                       "END as situacao_estoque " +
                       "FROM produtos p " +
                       "WHERE p.ativo = 1 " +
                       "ORDER BY " +
                       "CASE situacao_estoque " +
                       "  WHEN 'Sem Estoque' THEN 1 " +
                       "  WHEN 'Crítico' THEN 2 " +
                       "  WHEN 'Baixo' THEN 3 " +
                       "  WHEN 'Regular' THEN 4 " +
                       "  ELSE 5 " +
                       "END, " +
                       "p.nome";
            
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            relatorio.append(String.format("%-8s %-30s %-15s %-15s %-15s\n", 
                                         "CÓDIGO", "PRODUTO", "ATUAL", "MÍNIMO", "SITUAÇÃO"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            Map<String, Integer> contadorSituacoes = new HashMap<>();
            
            while (rs.next()) {
                String codigo = rs.getString("codigo");
                String nome = rs.getString("nome");
                int estoqueAtual = rs.getInt("estoque_atual");
                int estoqueMinimo = rs.getInt("estoque_minimo");
                String situacaoEstoque = rs.getString("situacao_estoque");
                
                // Definir a cor para a situação (representação textual)
                String indicadorSituacao;
                switch (situacaoEstoque) {
                    case "Sem Estoque":
                        indicadorSituacao = "[URGENTE]";
                        break;
                    case "Crítico":
                        indicadorSituacao = "[CRÍTICO]";
                        break;
                    case "Baixo":
                        indicadorSituacao = "[BAIXO]";
                        break;
                    case "Regular":
                        indicadorSituacao = "[REGULAR]";
                        break;
                    default:
                        indicadorSituacao = "[OK]";
                        break;
                }
                
                relatorio.append(String.format("%-8s %-30s %-15d %-15d %s\n", 
                                             codigo, 
                                             (nome.length() > 28 ? nome.substring(0, 25) + "..." : nome),
                                             estoqueAtual,
                                             estoqueMinimo,
                                             indicadorSituacao));
                
                // Contabilizar para resumo
                contadorSituacoes.put(situacaoEstoque, contadorSituacoes.getOrDefault(situacaoEstoque, 0) + 1);
            }
            
            // Adicionar resumo de situação de estoque
            relatorio.append("\nRESUMO DA SITUAÇÃO DE ESTOQUE\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            List<String> situacoes = Arrays.asList("Sem Estoque", "Crítico", "Baixo", "Regular", "Adequado");
            
            for (String situacao : situacoes) {
                int count = contadorSituacoes.getOrDefault(situacao, 0);
                
                // Representação gráfica simples
                int barras = Math.min(count, 50); // Limitar a 50 barras
                StringBuilder grafico = new StringBuilder();
                for (int i = 0; i < barras; i++) {
                    grafico.append("█");
                }
                
                relatorio.append(String.format("%-15s %3d produtos %s\n", 
                                             situacao,
                                             count,
                                             grafico.toString()));
            }
            
            // Adicionar lista de produtos para reposição imediata
            relatorio.append("\nPRODUTOS PARA REPOSIÇÃO IMEDIATA\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            sql = "SELECT p.id, p.codigo, p.nome, p.estoque_atual, p.estoque_minimo, " +
                 "(p.estoque_minimo - p.estoque_atual) as qtd_reposicao " +
                 "FROM produtos p " +
                 "WHERE p.ativo = 1 AND p.estoque_atual < p.estoque_minimo " +
                 "ORDER BY qtd_reposicao DESC";
            
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            relatorio.append(String.format("%-8s %-30s %-15s %-15s %-15s\n", 
                                         "CÓDIGO", "PRODUTO", "ATUAL", "MÍNIMO", "REPOR"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            while (rs.next()) {
                String codigo = rs.getString("codigo");
                String nome = rs.getString("nome");
                int estoqueAtual = rs.getInt("estoque_atual");
                int estoqueMinimo = rs.getInt("estoque_minimo");
                int qtdReposicao = rs.getInt("qtd_reposicao");
                
                relatorio.append(String.format("%-8s %-30s %-15d %-15d %-15d\n", 
                                             codigo, 
                                             (nome.length() > 28 ? nome.substring(0, 25) + "..." : nome),
                                             estoqueAtual,
                                             estoqueMinimo,
                                             qtdReposicao));
            }
            
        } catch (SQLException e) {
            LogUtil.error(this.getClass(), "Erro ao gerar relatório de nível de estoque", e);
            throw e;
        } finally {
            closeResources(conn, stmt, rs);
        }
        
        return relatorio.toString();
    }
}