package br.com.pdv.relatorio;

import br.com.pdv.util.DatabaseConnection;
import br.com.pdv.util.LogUtil;
import br.com.pdv.util.PDFUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.itextpdf.text.DocumentException;

/**
 * Classe base abstrata para todos os relatórios do sistema.
 * Define a estrutura comum e métodos que todos os relatórios devem implementar.
 */
public abstract class RelatorioBase {
    protected final DatabaseConnection databaseConnection;
    protected LocalDateTime dataInicio;
    protected LocalDateTime dataFim;
    protected String titulo;
    
    // Formatadores de data comuns
    protected final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    protected final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Construtor para relatórios com período
     */
    public RelatorioBase(String titulo, LocalDateTime dataInicio, LocalDateTime dataFim) {
        this.databaseConnection = DatabaseConnection.getInstance();
        this.titulo = titulo;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
    }

    /**
     * Construtor para relatórios sem período (ex: estoque atual)
     */
    public RelatorioBase(String titulo) {
        this(titulo, LocalDateTime.now().minusDays(30), LocalDateTime.now());
    }

    /**
     * Gera o cabeçalho padrão do relatório em formato de texto
     */
    protected String gerarCabecalho() {
        StringBuilder sb = new StringBuilder();
        sb.append("============================================================\n");
        sb.append(titulo).append("\n");
        
        if (dataInicio != null && dataFim != null) {
            sb.append("Período: ").append(dataInicio.format(dateFormatter))
              .append(" a ").append(dataFim.format(dateFormatter)).append("\n");
        }
        
        sb.append("Data de geração: ").append(LocalDateTime.now().format(dateTimeFormatter)).append("\n");
        sb.append("============================================================\n\n");
        
        return sb.toString();
    }

    /**
     * Método que gera o relatório em formato de texto
     * Deve ser implementado por cada tipo de relatório
     */
    public abstract String gerarRelatorio() throws SQLException;

   

    /**
     * Método que gera o relatório em formato Excel (será implementado no futuro)
     */
    public byte[] gerarRelatorioExcel() throws Exception {
        // Implementação para gerar Excel seria feita aqui
        // Usaria uma biblioteca como Apache POI
        throw new UnsupportedOperationException("Geração de Excel não implementada");
    }
    
    
    
    /**
     * Método que gera o relatório em formato PDF
     * 
     * @return Array de bytes contendo o PDF gerado
     * @throws SQLException Se ocorrer erro ao gerar o relatório em texto
     * @throws DocumentException Se ocorrer erro na criação do PDF
     * @throws IOException Se ocorrer erro de I/O
     */
    public byte[] gerarRelatorioPDF() throws SQLException, DocumentException, IOException {
        try {
            // Obter o conteúdo do relatório em texto
            String conteudoTexto = gerarRelatorio();
            
            // Converter o conteúdo para PDF usando a classe PDFUtil
            return PDFUtil.gerarPDF(conteudoTexto, titulo);
        } catch (SQLException e) {
            LogUtil.error(this.getClass(), "Erro ao gerar relatório em texto: " + e.getMessage(), e);
            throw e;
        } catch (DocumentException e) {
            LogUtil.error(this.getClass(), "Erro ao criar documento PDF: " + e.getMessage(), e);
            throw e;
        } catch (IOException e) {
            LogUtil.error(this.getClass(), "Erro de I/O ao gerar PDF: " + e.getMessage(), e);
            throw e;
        }
    }
    /**
     * Método utilitário para fechar recursos do banco de dados de forma segura
     */
    protected void closeResources(Connection conn, PreparedStatement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LogUtil.warn(this.getClass(), "Erro ao fechar ResultSet: " + e.getMessage());
            }
        }

        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                LogUtil.warn(this.getClass(), "Erro ao fechar Statement: " + e.getMessage());
            }
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LogUtil.warn(this.getClass(), "Erro ao fechar Connection: " + e.getMessage());
            }
        }
    }
    
    // Getters e Setters
    
    public String getTitulo() {
        return titulo;
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public LocalDateTime getDataFim() {
        return dataFim;
    }
    
    public void setDataInicio(LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }
    
    public void setDataFim(LocalDateTime dataFim) {
        this.dataFim = dataFim;
    }
}