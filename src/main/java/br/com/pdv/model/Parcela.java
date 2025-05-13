package br.com.pdv.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Classe que representa uma parcela de pagamento.
 */
public class Parcela {
    private Integer id;
    private Venda venda;
    private Integer numeroParcela;
    private Integer totalParcelas;
    private BigDecimal valor;
    private LocalDate dataVencimento;
    private LocalDate dataPagamento;
    private String status; // PENDENTE, PAGA, ATRASADA, CANCELADA
    
    // Construtor padrão
    public Parcela() {
        this.status = "PENDENTE";
    }
    
    // Construtor com parâmetros
    public Parcela(Integer numeroParcela, Integer totalParcelas, BigDecimal valor, LocalDate dataVencimento) {
        this();
        this.numeroParcela = numeroParcela;
        this.totalParcelas = totalParcelas;
        this.valor = valor;
        this.dataVencimento = dataVencimento;
    }
    
    // Getters e Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Venda getVenda() {
        return venda;
    }
    
    public void setVenda(Venda venda) {
        this.venda = venda;
    }
    
    public Integer getNumeroParcela() {
        return numeroParcela;
    }
    
    public void setNumeroParcela(Integer numeroParcela) {
        if (numeroParcela == null || numeroParcela <= 0) {
            throw new IllegalArgumentException("Número da parcela deve ser maior que zero");
        }
        this.numeroParcela = numeroParcela;
    }
    
    public Integer getTotalParcelas() {
        return totalParcelas;
    }
    
    public void setTotalParcelas(Integer totalParcelas) {
        if (totalParcelas == null || totalParcelas <= 0) {
            throw new IllegalArgumentException("Total de parcelas deve ser maior que zero");
        }
        this.totalParcelas = totalParcelas;
    }
    
    public BigDecimal getValor() {
        return valor;
    }
    
    public void setValor(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da parcela deve ser maior que zero");
        }
        this.valor = valor;
    }
    
    public LocalDate getDataVencimento() {
        return dataVencimento;
    }
    
    public void setDataVencimento(LocalDate dataVencimento) {
        if (dataVencimento == null) {
            throw new IllegalArgumentException("Data de vencimento não pode ser nula");
        }
        this.dataVencimento = dataVencimento;
    }
    
    public LocalDate getDataPagamento() {
        return dataPagamento;
    }
    
    public void setDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
        
        if (dataPagamento != null) {
            this.status = "PAGA";
        }
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    // Métodos de negócio
    public void pagar() {
        if ("PAGA".equals(status)) {
            throw new IllegalStateException("Parcela já está paga");
        }
        
        if ("CANCELADA".equals(status)) {
            throw new IllegalStateException("Não é possível pagar uma parcela cancelada");
        }
        
        this.status = "PAGA";
        this.dataPagamento = LocalDate.now();
    }
    
    public void cancelar() {
        if ("CANCELADA".equals(status)) {
            throw new IllegalStateException("Parcela já está cancelada");
        }
        
        this.status = "CANCELADA";
    }
    
    public boolean isAtrasada() {
        if (dataPagamento != null) {
            return false; // Parcela já foi paga
        }
        
        return LocalDate.now().isAfter(dataVencimento);
    }
    
    public void verificarStatus() {
        if ("PENDENTE".equals(status) && isAtrasada()) {
            this.status = "ATRASADA";
        }
    }
    
    @Override
    public String toString() {
        return "Parcela{" +
                "id=" + id +
                ", venda=" + (venda != null ? venda.getId() : null) +
                ", numeroParcela=" + numeroParcela +
                ", totalParcelas=" + totalParcelas +
                ", valor=" + valor +
                ", dataVencimento=" + dataVencimento +
                ", dataPagamento=" + dataPagamento +
                ", status='" + status + '\'' +
                '}';
    }
}