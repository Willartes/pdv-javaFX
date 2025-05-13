package br.com.pdv.relatorio;

import br.com.pdv.dao.UsuarioDAO;
import br.com.pdv.dao.VendaDAO;
import br.com.pdv.util.LogUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Relatório de Vendas por Vendedor
 * Exibe os dados de vendas agrupados por vendedor, incluindo análise de desempenho
 */
public class RelatorioVendasPorVendedor extends RelatorioBase {
    private final VendaDAO vendaDAO;
    private final UsuarioDAO usuarioDAO;
    
    /**
     * Construtor do relatório de vendas por vendedor
     * 
     * @param dataInicio Data inicial do período
     * @param dataFim Data final do período
     */
    public RelatorioVendasPorVendedor(LocalDateTime dataInicio, LocalDateTime dataFim) {
        super("RELATÓRIO DE VENDAS POR VENDEDOR", dataInicio, dataFim);
        this.vendaDAO = RelatorioManager.getInstance().getVendaDAO();
        this.usuarioDAO = RelatorioManager.getInstance().getUsuarioDAO();
    }
    
    @Override
    public String gerarRelatorio() throws SQLException {
        StringBuilder relatorio = new StringBuilder(gerarCabecalho());
        relatorio.append("Implementação pendente para este relatório.\n");
        return relatorio.toString();
    }
}