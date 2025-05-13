package br.com.pdv.relatorio;

import br.com.pdv.dao.UsuarioDAO;
import br.com.pdv.dao.VendaDAO;
import br.com.pdv.model.Usuario;
import br.com.pdv.util.LogUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Relatório de Comissão para Vendedores
 * Calcula e exibe as comissões dos vendedores com base nas vendas realizadas
 */
public class RelatorioComissaoVendedores extends RelatorioBase {
    private final VendaDAO vendaDAO;
    private final UsuarioDAO usuarioDAO;
    
    // Taxa de comissão padrão (poderia vir de configuração do sistema)
    private static final BigDecimal TAXA_COMISSAO_PADRAO = new BigDecimal("0.03"); // 3%
    
    // Metas de vendas para bônus de comissão
    private static final BigDecimal META_BRONZE = new BigDecimal("5000.00");
    private static final BigDecimal META_PRATA = new BigDecimal("10000.00");
    private static final BigDecimal META_OURO = new BigDecimal("20000.00");
    
    // Bônus por atingir metas
    private static final BigDecimal BONUS_BRONZE = new BigDecimal("0.005"); // +0.5%
    private static final BigDecimal BONUS_PRATA = new BigDecimal("0.01");   // +1.0%
    private static final BigDecimal BONUS_OURO = new BigDecimal("0.02");    // +2.0%
    
    /**
     * Construtor do relatório de comissão de vendedores
     * 
     * @param dataInicio Data inicial do período
     * @param dataFim Data final do período
     */
    public RelatorioComissaoVendedores(LocalDateTime dataInicio, LocalDateTime dataFim) {
        super("RELATÓRIO DE COMISSÃO DE VENDEDORES", dataInicio, dataFim);
        this.vendaDAO = RelatorioManager.getInstance().getVendaDAO();
        this.usuarioDAO = RelatorioManager.getInstance().getUsuarioDAO();
    }
    
    @Override
    public String gerarRelatorio() throws SQLException {
        StringBuilder relatorio = new StringBuilder(gerarCabecalho());
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            
            // Parte 1: Resumo de vendas por vendedor
            relatorio.append("RESUMO DE VENDAS POR VENDEDOR\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            String sql = "SELECT u.id as vendedor_id, u.nome as vendedor_nome, " +
                       "COUNT(v.id) as total_vendas, " +
                       "SUM(v.valor_total) as valor_bruto, " +
                       "SUM(v.valor_desconto) as valor_desconto, " +
                       "SUM(v.valor_total - v.valor_desconto) as valor_liquido " +
                       "FROM vendas v " +
                       "JOIN usuarios u ON v.usuario_id = u.id " +
                       "WHERE v.data_venda BETWEEN ? AND ? " +
                       "AND v.status = 'FINALIZADA' " +
                       "GROUP BY u.id, u.nome " +
                       "ORDER BY valor_liquido DESC";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, dataInicio);
            stmt.setObject(2, dataFim);
            
            rs = stmt.executeQuery();
            
            relatorio.append(String.format("%-25s %-12s %-15s %-15s %-15s\n", 
                                         "VENDEDOR", "QTD VENDAS", "VALOR BRUTO", "VALOR LÍQUIDO", "COMISSÃO"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            BigDecimal totalVendas = BigDecimal.ZERO;
            BigDecimal totalComissoes = BigDecimal.ZERO;
            
            // Armazenar dados para cálculo de ranking e bônus
            Map<Integer, BigDecimal> vendasPorVendedor = new HashMap<>();
            Map<Integer, BigDecimal> comissoesPorVendedor = new HashMap<>();
            
            while (rs.next()) {
                int vendedorId = rs.getInt("vendedor_id");
                String vendedorNome = rs.getString("vendedor_nome");
                int qtdVendas = rs.getInt("total_vendas");
                BigDecimal valorBruto = rs.getBigDecimal("valor_bruto");
                BigDecimal valorLiquido = rs.getBigDecimal("valor_liquido");
                
                // Calcular comissão básica
                BigDecimal comissaoBasica = valorLiquido.multiply(TAXA_COMISSAO_PADRAO).setScale(2, RoundingMode.HALF_UP);
                
                // Armazenar para cálculos de bônus posteriormente
                vendasPorVendedor.put(vendedorId, valorLiquido);
                comissoesPorVendedor.put(vendedorId, comissaoBasica);
                
                relatorio.append(String.format("%-25s %-12d R$ %-12.2f R$ %-12.2f R$ %-12.2f\n", 
                                             vendedorNome,
                                             qtdVendas,
                                             valorBruto,
                                             valorLiquido,
                                             comissaoBasica));
                
                totalVendas = totalVendas.add(valorLiquido);
                totalComissoes = totalComissoes.add(comissaoBasica);
            }
            
            relatorio.append("--------------------------------------------------------------------------------\n");
            relatorio.append(String.format("%-25s %-12s R$ %-12.2f R$ %-12.2f R$ %-12.2f\n", 
                                         "TOTAL", "",
                                         totalVendas,
                                         totalVendas,
                                         totalComissoes));
            
            // Parte 2: Cálculo de bônus por metas
            relatorio.append("\nBÔNUS POR METAS ATINGIDAS\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            relatorio.append(String.format("Metas do período: Bronze R$ %.2f | Prata R$ %.2f | Ouro R$ %.2f\n", 
                                         META_BRONZE, META_PRATA, META_OURO));
            relatorio.append("--------------------------------------------------------------------------------\n");
            relatorio.append(String.format("%-25s %-15s %-15s %-15s %-15s\n", 
                                         "VENDEDOR", "VENDAS", "META ATINGIDA", "BÔNUS", "COMISSÃO TOTAL"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            BigDecimal totalComissoesComBonus = BigDecimal.ZERO;
            
            // Para cada vendedor, calcular bônus
            for (Map.Entry<Integer, BigDecimal> entry : vendasPorVendedor.entrySet()) {
                int vendedorId = entry.getKey();
                BigDecimal valorVendas = entry.getValue();
                BigDecimal comissaoBase = comissoesPorVendedor.get(vendedorId);
                
                // Buscar nome do vendedor
                Usuario vendedor = usuarioDAO.findById(vendedorId);
                String vendedorNome = vendedor != null ? vendedor.getNome() : "Vendedor #" + vendedorId;
                
                // Determinar meta atingida e bônus aplicável
                String metaAtingida = "Nenhuma";
                BigDecimal taxaBonus = BigDecimal.ZERO;
                
                if (valorVendas.compareTo(META_OURO) >= 0) {
                    metaAtingida = "OURO";
                    taxaBonus = BONUS_OURO;
                } else if (valorVendas.compareTo(META_PRATA) >= 0) {
                    metaAtingida = "PRATA";
                    taxaBonus = BONUS_PRATA;
                } else if (valorVendas.compareTo(META_BRONZE) >= 0) {
                    metaAtingida = "BRONZE";
                    taxaBonus = BONUS_BRONZE;
                }
                
                // Calcular bônus e comissão total
                BigDecimal valorBonus = valorVendas.multiply(taxaBonus).setScale(2, RoundingMode.HALF_UP);
                BigDecimal comissaoTotal = comissaoBase.add(valorBonus);
                
                relatorio.append(String.format("%-25s R$ %-12.2f %-15s R$ %-12.2f R$ %-12.2f\n", 
                                             vendedorNome,
                                             valorVendas,
                                             metaAtingida,
                                             valorBonus,
                                             comissaoTotal));
                
                totalComissoesComBonus = totalComissoesComBonus.add(comissaoTotal);
            }
            
            relatorio.append("--------------------------------------------------------------------------------\n");
            relatorio.append(String.format("%-25s %-15s %-15s %-15s R$ %-12.2f\n", 
                                         "TOTAL", "", "", "",
                                         totalComissoesComBonus));
            
            // Parte 3: Evolução mensal das comissões (se o período for maior que um mês)
            if (dataInicio.getMonth() != dataFim.getMonth() || dataInicio.getYear() != dataFim.getYear()) {
                relatorio.append("\nEVOLUÇÃO MENSAL DAS COMISSÕES\n");
                relatorio.append("--------------------------------------------------------------------------------\n");
                
                sql = "SELECT YEAR(v.data_venda) as ano, MONTH(v.data_venda) as mes, " +
                     "u.id as vendedor_id, u.nome as vendedor_nome, " +
                     "SUM(v.valor_total - v.valor_desconto) as valor_liquido " +
                     "FROM vendas v " +
                     "JOIN usuarios u ON v.usuario_id = u.id " +
                     "WHERE v.data_venda BETWEEN ? AND ? " +
                     "AND v.status = 'FINALIZADA' " +
                     "GROUP BY YEAR(v.data_venda), MONTH(v.data_venda), u.id, u.nome " +
                     "ORDER BY ano, mes, valor_liquido DESC";
                
                stmt = conn.prepareStatement(sql);
                stmt.setObject(1, dataInicio);
                stmt.setObject(2, dataFim);
                
                rs = stmt.executeQuery();
                
                relatorio.append(String.format("%-12s %-25s %-15s %-15s\n", 
                                             "MÊS/ANO", "VENDEDOR", "VALOR VENDAS", "COMISSÃO"));
                relatorio.append("--------------------------------------------------------------------------------\n");
                
                int anoAtual = -1;
                int mesAtual = -1;
                
                while (rs.next()) {
                    int ano = rs.getInt("ano");
                    int mes = rs.getInt("mes");
                    int vendedorId = rs.getInt("vendedor_id");
                    String vendedorNome = rs.getString("vendedor_nome");
                    BigDecimal valorLiquido = rs.getBigDecimal("valor_liquido");
                    
                    // Calcular comissão
                    BigDecimal comissao = valorLiquido.multiply(TAXA_COMISSAO_PADRAO).setScale(2, RoundingMode.HALF_UP);
                    
                    // Se mudou o mês, imprime uma linha separadora
                    if ((anoAtual != -1 && mesAtual != -1) && (anoAtual != ano || mesAtual != mes)) {
                        relatorio.append("--------------------------------------------------------------------------------\n");
                    }
                    
                    // Formatação do mês/ano
                    YearMonth ym = YearMonth.of(ano, mes);
                    String mesAnoFormatado = ym.format(DateTimeFormatter.ofPattern("MM/yyyy"));
                    
                    relatorio.append(String.format("%-12s %-25s R$ %-12.2f R$ %-12.2f\n", 
                                                 mesAnoFormatado,
                                                 vendedorNome,
                                                 valorLiquido,
                                                 comissao));
                    
                    anoAtual = ano;
                    mesAtual = mes;
                }
            }
            
        } catch (SQLException e) {
            LogUtil.error(this.getClass(), "Erro ao gerar relatório de comissão de vendedores", e);
            throw e;
        } finally {
            closeResources(conn, stmt, rs);
        }
        
        return relatorio.toString();
    }
}