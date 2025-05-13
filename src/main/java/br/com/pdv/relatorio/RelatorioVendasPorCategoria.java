package br.com.pdv.relatorio;

import br.com.pdv.dao.ProdutoDAO;
import br.com.pdv.dao.VendaDAO;
import br.com.pdv.util.LogUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Relatório de Vendas por Categoria de Produto
 * Exibe os dados de vendas agrupados por categoria de produto dentro de um período
 */
public class RelatorioVendasPorCategoria extends RelatorioBase {
    private final VendaDAO vendaDAO;
    private final ProdutoDAO produtoDAO;
    
    /**
     * Construtor do relatório de vendas por categoria
     * 
     * @param dataInicio Data inicial do período
     * @param dataFim Data final do período
     */
    public RelatorioVendasPorCategoria(LocalDateTime dataInicio, LocalDateTime dataFim) {
        super("RELATÓRIO DE VENDAS POR CATEGORIA DE PRODUTO", dataInicio, dataFim);
        this.vendaDAO = RelatorioManager.getInstance().getVendaDAO();
        this.produtoDAO = RelatorioManager.getInstance().getProdutoDAO();
    }
    
    @Override
    public String gerarRelatorio() throws SQLException {
        StringBuilder relatorio = new StringBuilder(gerarCabecalho());
        relatorio.append("Implementação pendente para este relatório.\n");
        return relatorio.toString();
    }
}