package br.com.pdv.model;

import br.com.pdv.model.Compra;
import br.com.pdv.model.Produto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ItemCompra {
    private Integer id;
    private Compra compra;
    private Produto produto;
    private int quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal desconto;
    private BigDecimal valorTotal;
    private LocalDateTime dataCadastro;
    
    // Construtores
    public ItemCompra() {
        this.desconto = BigDecimal.ZERO;
        this.dataCadastro = LocalDateTime.now();
        this.valorTotal = BigDecimal.ZERO;
    }
    
    public ItemCompra(Produto produto, int quantidade, BigDecimal precoUnitario) {
        this();
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        calcularTotal();
    }
    
    // Getters e Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        if (produto == null) {
            throw new IllegalArgumentException("Produto não pode ser nulo");
        }
        this.produto = produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        this.quantidade = quantidade;
        calcularTotal();
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        if (precoUnitario == null || precoUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço unitário deve ser maior que zero");
        }
        this.precoUnitario = precoUnitario;
        calcularTotal();
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public void setDesconto(BigDecimal desconto) {
        if (desconto == null) {
            desconto = BigDecimal.ZERO;
        }
        if (desconto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Desconto não pode ser negativo");
        }
        this.desconto = desconto;
        calcularTotal();
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }
    
    // Métodos de negócio
    public void calcularTotal() {
        if (quantidade > 0 && precoUnitario != null) {
            BigDecimal subtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
            if (desconto != null && desconto.compareTo(subtotal) <= 0) {
                valorTotal = subtotal.subtract(desconto);
            } else {
                valorTotal = subtotal;
            }
        } else {
            valorTotal = BigDecimal.ZERO;
        }
    }
    
    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
    
    public boolean verificarDisponibilidadeEstoque() {
        return produto != null && produto.getEstoqueAtual() >= quantidade;
    }
    
    @Override
    public String toString() {
        return "ItemCompra{" +
                "id=" + id +
                ", produto=" + (produto != null ? produto.getId() : null) +
                ", quantidade=" + quantidade +
                ", precoUnitario=" + precoUnitario +
                ", desconto=" + desconto +
                ", valorTotal=" + valorTotal +
                ", dataCadastro=" + dataCadastro +
                '}';
    }
}