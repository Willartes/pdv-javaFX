package br.com.pdv.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Compra {
    private Integer id;
    private LocalDateTime dataCompra;
    private Fornecedor fornecedor;
    private List<ItemCompra> itens;
    private BigDecimal valorTotal;
    private String status; // PENDENTE, FINALIZADA, CANCELADA
    private String numeroNF;
    private Usuario usuario;
    private String observacao;
    private LocalDateTime dataCancelamento;
    private String motivoCancelamento;
    
    // Construtores
    public Compra() {
        this.dataCompra = LocalDateTime.now();
        this.itens = new ArrayList<>();
        this.valorTotal = BigDecimal.ZERO;
        this.status = "PENDENTE";
    }
    
    public Compra(Fornecedor fornecedor, Usuario usuario) {
        this();
        this.fornecedor = fornecedor;
        this.usuario = usuario;
    }
    
    // Getters e Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getDataCompra() {
        return dataCompra;
    }

    public void setDataCompra(LocalDateTime dataCompra) {
        this.dataCompra = dataCompra;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(Fornecedor fornecedor) {
        if (fornecedor == null) {
            throw new IllegalArgumentException("Fornecedor não pode ser nulo");
        }
        this.fornecedor = fornecedor;
    }

    public List<ItemCompra> getItens() {
        return new ArrayList<>(itens); // Retorna uma cópia da lista
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNumeroNF() {
        return numeroNF;
    }

    public void setNumeroNF(String numeroNF) {
        this.numeroNF = numeroNF;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não pode ser nulo");
        }
        this.usuario = usuario;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDateTime getDataCancelamento() {
        return dataCancelamento;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }
    
    // Métodos de negócio
    public void adicionarItem(Produto produto, int quantidade, BigDecimal precoCusto) {
        if (!"PENDENTE".equals(status)) {
            throw new IllegalStateException("Não é possível adicionar itens a uma compra " + status);
        }
        
        // Verifica se o produto já existe na compra
        ItemCompra itemExistente = itens.stream()
                .filter(item -> item.getProduto().getId() == produto.getId())
                .findFirst()
                .orElse(null);
        
        if (itemExistente != null) {
            // Atualiza a quantidade e preço do item existente
            itemExistente.setQuantidade(itemExistente.getQuantidade() + quantidade);
            itemExistente.setPrecoUnitario(precoCusto);
        } else {
            // Cria um novo item
            ItemCompra novoItem = new ItemCompra(produto, quantidade, precoCusto);
            novoItem.setCompra(this);
            itens.add(novoItem);
        }
        
        calcularTotal();
    }
    
    public void removerItem(ItemCompra item) {
        if (!"PENDENTE".equals(status)) {
            throw new IllegalStateException("Não é possível remover itens de uma compra " + status);
        }
        
        if (itens.remove(item)) {
            calcularTotal();
        }
    }
    
    private void calcularTotal() {
        this.valorTotal = itens.stream()
                .map(ItemCompra::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public void finalizar() {
        if (!"PENDENTE".equals(status)) {
            throw new IllegalStateException("Compra não está pendente");
        }
        
        if (itens.isEmpty()) {
            throw new IllegalStateException("Não é possível finalizar uma compra sem itens");
        }
        
        if (numeroNF == null || numeroNF.trim().isEmpty()) {
            throw new IllegalStateException("Número da NF não informado");
        }
        
        this.status = "FINALIZADA";
        
        // Atualiza estoque dos produtos
        for (ItemCompra item : itens) {
            Produto produto = item.getProduto();
            produto.setEstoqueAtual(produto.getEstoqueAtual() + item.getQuantidade());
        }
    }
    
    public void cancelar(String motivo) {
        if ("FINALIZADA".equals(status)) {
            throw new IllegalStateException("Não é possível cancelar uma compra finalizada");
        }
        
        if ("CANCELADA".equals(status)) {
            throw new IllegalStateException("Compra já está cancelada");
        }
        
        this.status = "CANCELADA";
        this.dataCancelamento = LocalDateTime.now();
        this.motivoCancelamento = motivo;
    }
    
    @Override
    public String toString() {
        return "Compra{" +
                "id=" + id +
                ", dataCompra=" + dataCompra +
                ", fornecedor=" + (fornecedor != null ? fornecedor.getId() : null) +
                ", valorTotal=" + valorTotal +
                ", status='" + status + '\'' +
                ", numeroNF='" + numeroNF + '\'' +
                ", usuario=" + (usuario != null ? usuario.getId() : null) +
                ", itens=" + itens.size() +
                '}';
    }
}