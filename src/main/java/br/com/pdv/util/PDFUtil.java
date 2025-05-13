package br.com.pdv.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Classe utilitária para geração de arquivos PDF
 */
public class PDFUtil {

    /**
     * Converte texto para PDF real usando iText
     * 
     * @param texto O texto a ser convertido para PDF
     * @param titulo O título do documento
     * @return Array de bytes contendo o PDF gerado
     * @throws DocumentException Se ocorrer erro na criação do PDF
     * @throws IOException Se ocorrer erro de I/O
     */
    public static byte[] gerarPDF(String texto, String titulo) throws DocumentException, IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Configurar documento
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            
            // Adicionar eventos para cabeçalho e rodapé
            writer.setPageEvent(new PdfHeaderFooter(titulo));
            
            document.open();
            document.addTitle(titulo);
            document.addCreator("Sistema PDV");
            document.addCreationDate();
            
            // Adicionar título
            Font fonteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.BLACK);
            Paragraph paragraphTitulo = new Paragraph(titulo, fonteTitulo);
            paragraphTitulo.setAlignment(Element.ALIGN_CENTER);
            document.add(paragraphTitulo);
            document.add(Chunk.NEWLINE);
            
            // Adicionar data/hora atual
            Font fonteData = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.DARK_GRAY);
            Paragraph paragraphData = new Paragraph("Gerado em: " + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), fonteData);
            paragraphData.setAlignment(Element.ALIGN_RIGHT);
            document.add(paragraphData);
            document.add(Chunk.NEWLINE);
            
            // Adicionar linha separadora
            LineSeparator linha = new LineSeparator();
            linha.setOffset(-2);
            document.add(linha);
            document.add(Chunk.NEWLINE);
            
            // Adicionar texto com fonte monoespaçada para preservar a formatação
            Font fonteCourier = FontFactory.getFont(FontFactory.COURIER, 9, BaseColor.BLACK);
            
            // Dividir o texto em linhas para preservar a formatação
            String[] linhas = texto.split("\\n");
            for (String linha_texto : linhas) {
                Paragraph p = new Paragraph(linha_texto, fonteCourier);
                document.add(p);
            }
            
            document.close();
            
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentException("Erro ao gerar PDF: " + e.getMessage(), e);
        }
    }
    
    /**
     * Salva um array de bytes em um arquivo PDF
     * 
     * @param pdfBytes O conteúdo do PDF em array de bytes
     * @param caminhoArquivo O caminho do arquivo onde o PDF será salvo
     * @throws IOException Se ocorrer erro ao salvar o arquivo
     */
    public static void salvarPDF(byte[] pdfBytes, String caminhoArquivo) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(caminhoArquivo)) {
            fos.write(pdfBytes);
            fos.flush();
        }
    }
    
    /**
     * Classe interna para gerenciar cabeçalho e rodapé do PDF
     */
    static class PdfHeaderFooter extends PdfPageEventHelper {
        private final String titulo;
        private PdfTemplate template;
        private BaseFont baseFont;
        
        public PdfHeaderFooter(String titulo) {
            this.titulo = titulo;
        }
        
        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            try {
                template = writer.getDirectContent().createTemplate(30, 16);
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                // Ignorar erro
            }
        }
        
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            
            // Rodapé
            String text = "Página " + writer.getPageNumber();
            float textSize = baseFont.getWidthPoint(text, 8);
            float textBase = document.bottom() - 20;
            
            cb.beginText();
            cb.setFontAndSize(baseFont, 8);
            cb.setTextMatrix(document.right() - textSize - 20, textBase);
            cb.showText(text);
            cb.endText();
            
            // Data no rodapé
            String dataAtual = "Gerado em: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            cb.beginText();
            cb.setFontAndSize(baseFont, 8);
            cb.setTextMatrix(document.left() + 20, textBase);
            cb.showText(dataAtual);
            cb.endText();
            
            // Linha no rodapé
            cb.moveTo(document.left(), textBase + 10);
            cb.lineTo(document.right(), textBase + 10);
            cb.stroke();
            
            // Cabeçalho (linha)
            cb.moveTo(document.left(), document.top() + 10);
            cb.lineTo(document.right(), document.top() + 10);
            cb.stroke();
            
            // Nome do sistema no cabeçalho
            cb.beginText();
            cb.setFontAndSize(baseFont, 8);
            cb.setTextMatrix(document.left() + 20, document.top() + 20);
            cb.showText("Sistema PDV");
            cb.endText();
        }
    }
}