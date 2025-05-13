package br.com.pdv.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe que representa um Caixa no sistema PDV.
 */
public class Caixa {
    private Integer id;
    private Usuario operador;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataFechamento;
    private BigDecimal saldoInicial;
    private BigDecimal saldoFinal;
    private String status; // ABERTO, FECHADO
    private String observacao;
    private List<MovimentoCaixa> movimentos;
    
    // Construtor padrão
    public Caixa() {
        this.dataAbertura = LocalDateTime.now();
        this.saldoInicial = BigDecimal.ZERO;
        this.saldoFinal = BigDecimal.ZERO;
        this.status = "ABERTO";
        this.movimentos = new ArrayList<>();
    }
    
    // Construtor com operador e saldo inicial
    public Caixa(Usuario operador, BigDecimal saldoInicial) {
        this();
        this.operador = operador;
        this.saldoInicial = saldoInicial;
        this.saldoFinal = saldoInicial; // Inicialmente, o saldo final é igual ao inicial
    }
    
    // Getters e Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Usuario getOperador() {
        return operador;
    }
    
    public void setOperador(Usuario operador) {
        if (operador == null) {
            throw new IllegalArgumentException("Operador não pode ser nulo");
        }
        this.operador = operador;
    }
    
    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }
    
    public void setDataAbertura(LocalDateTime dataAbertura) {
        this.dataAbertura = dataAbertura;
    }
    
    public LocalDateTime getDataFechamento() {
        return dataFechamento;
    }
    
    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }
    
    public void setSaldoInicial(BigDecimal saldoInicial) {
        if (saldoInicial == null) {
            throw new IllegalArgumentException("Saldo inicial não pode ser nulo");
        }
        this.saldoInicial = saldoInicial;
        
        // Se não houver movimentos, o saldo final também é atualizado
        if (movimentos.isEmpty()) {
            this.saldoFinal = saldoInicial;
        }
    }
    
    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getObservacao() {
        return observacao;
    }
    
    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
    
    public List<MovimentoCaixa> getMovimentos() {
        return new ArrayList<>(movimentos);
    }
    
    // Métodos de negócio
    public void adicionarMovimento(String tipo, BigDecimal valor, String descricao) {
        if (!"ABERTO".equals(status)) {
            throw new IllegalStateException("Não é possível adicionar movimentos a um caixa fechado");
        }
        
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do movimento deve ser maior que zero");
        }
        
        MovimentoCaixa movimento = new MovimentoCaixa(this, tipo, valor, descricao);
        movimentos.add(movimento);
        
        // Atualiza o saldo final
        if ("ENTRADA".equals(tipo)) {
            saldoFinal = saldoFinal.add(valor);
        } else if ("SAIDA".equals(tipo)) {
            if (saldoFinal.compareTo(valor) < 0) {
                throw new IllegalStateException("Saldo insuficiente para realizar a saída");
            }
            saldoFinal = saldoFinal.subtract(valor);
        }
    }
    
    public void registrarVenda(Venda venda) {
        if (!"ABERTO".equals(status)) {
            throw new IllegalStateException("Não é possível registrar vendas em um caixa fechado");
        }
        
        if (venda == null) {
            throw new IllegalArgumentException("Venda não pode ser nula");
        }
        
        // Só adiciona ao caixa se for pagamento em dinheiro
        if ("DINHEIRO".equals(venda.getFormaPagamento())) {
            adicionarMovimento("ENTRADA", venda.getValorPago(), 
                    "Venda #" + venda.getId() + " - Cliente: " + venda.getCliente().getNome());
            
            // Se tiver troco, registra a saída
            if (venda.getTroco().compareTo(BigDecimal.ZERO) > 0) {
                adicionarMovimento("SAIDA", venda.getTroco(), 
                        "Troco da venda #" + venda.getId());
            }
        }
    }
    
    public void fechar(String observacao) {
        if (!"ABERTO".equals(status)) {
            throw new IllegalStateException("Caixa já está fechado");
        }
        
        this.status = "FECHADO";
        this.dataFechamento = LocalDateTime.now();
        this.observacao = observacao;
    }
    
    public BigDecimal getTotalEntradas() {
        return movimentos.stream()
                .filter(m -> "ENTRADA".equals(m.getTipo()))
                .map(MovimentoCaixa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public BigDecimal getTotalSaidas() {
        return movimentos.stream()
                .filter(m -> "SAIDA".equals(m.getTipo()))
                .map(MovimentoCaixa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public boolean isAberto() {
        return "ABERTO".equals(status);
    }
    
    @Override
    public String toString() {
        return "Caixa{" +
                "id=" + id +
                ", operador=" + (operador != null ? operador.getNome() : null) +
                ", dataAbertura=" + dataAbertura +
                ", dataFechamento=" + dataFechamento +
                ", saldoInicial=" + saldoInicial +
                ", saldoFinal=" + saldoFinal +
                ", status='" + status + '\'' +
                ", movimentos=" + movimentos.size() +
                '}';
    }
}