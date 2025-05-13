package br.com.pdv.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Classe que representa um movimento de caixa.
 */
public class MovimentoCaixa {
    private Integer id;
    private Caixa caixa;
    private Usuario usuario; // Novo atributo para armazenar o usuário
    private String tipo; // ENTRADA, SAIDA
    private BigDecimal valor;
    private String descricao;
    private LocalDateTime dataHora;
    private String formaPagamento; // Adicionado conforme estrutura da tabela
    
    // Construtor padrão
    public MovimentoCaixa() {
        this.dataHora = LocalDateTime.now();
    }
    
    // Construtor com parâmetros básicos
    public MovimentoCaixa(Caixa caixa, String tipo, BigDecimal valor, String descricao) {
        this();
        this.caixa = caixa;
        setTipo(tipo);
        setValor(valor);
        this.descricao = descricao;
        // Capturar automaticamente o usuário do caixa para facilitar
        if (caixa != null && caixa.getOperador() != null) {
            this.usuario = caixa.getOperador();
        }
    }
    
    // Construtor completo
    public MovimentoCaixa(Caixa caixa, Usuario usuario, String tipo, BigDecimal valor, String descricao, String formaPagamento) {
        this();
        this.caixa = caixa;
        this.usuario = usuario;
        setTipo(tipo);
        setValor(valor);
        this.descricao = descricao;
        this.formaPagamento = formaPagamento;
    }
    
    // Getters e Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Caixa getCaixa() {
        return caixa;
    }
    
    public void setCaixa(Caixa caixa) {
        this.caixa = caixa;
    }
    
    public Usuario getUsuario() {
        return usuario;
    }
    
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
    
    public String getTipo() {
        return tipo;
    }
    
    public void setTipo(String tipo) {
        if (!"ENTRADA".equals(tipo) && !"SAIDA".equals(tipo)) {
            throw new IllegalArgumentException("Tipo deve ser ENTRADA ou SAIDA");
        }
        this.tipo = tipo;
    }
    
    public BigDecimal getValor() {
        return valor;
    }
    
    public void setValor(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor deve ser maior que zero");
        }
        this.valor = valor;
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
    
    public LocalDateTime getDataHora() {
        return dataHora;
    }
    
    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }
    
    public String getFormaPagamento() {
        return formaPagamento;
    }
    
    public void setFormaPagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
    }
    
    @Override
    public String toString() {
        return "MovimentoCaixa{" +
                "id=" + id +
                ", caixa=" + (caixa != null ? caixa.getId() : null) +
                ", usuario=" + (usuario != null ? usuario.getId() : null) +
                ", tipo='" + tipo + '\'' +
                ", valor=" + valor +
                ", descricao='" + descricao + '\'' +
                ", dataHora=" + dataHora +
                ", formaPagamento='" + formaPagamento + '\'' +
                '}';
    }
}