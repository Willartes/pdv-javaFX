package br.com.pdv.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Classe que representa uma Venda finalizada no sistema.
 */
public class Venda {
    private int id;
    private Pedido pedido;
    private Cliente cliente;
    private Usuario usuario;
    private LocalDateTime dataVenda;
    private BigDecimal valorTotal;
    private BigDecimal valorDesconto;
    private BigDecimal valorPago;
    private BigDecimal troco;
    private String formaPagamento; // DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, PIX, BOLETO, etc.
    private String status; // FINALIZADA, CANCELADA
    private String numeroNF;
    private LocalDateTime dataCancelamento;
    private String motivoCancelamento;
    private List<Parcela> parcelas;
    private String observacao;
    private Usuario vendedor;
    private Usuario operador;
    
    
    private List<ItemPedido> itens = new ArrayList<>();

    // Construtor padrão
    public Venda() {
        this.dataVenda = LocalDateTime.now();
        this.valorTotal = BigDecimal.ZERO;
        this.valorDesconto = BigDecimal.ZERO;
        this.valorPago = BigDecimal.ZERO;
        this.troco = BigDecimal.ZERO;
        this.status = "FINALIZADA";
        this.parcelas = new ArrayList<>();
    }

    // Construtor com pedido
    public Venda(Pedido pedido) {
        this();
        this.pedido = pedido;
        this.cliente = pedido.getCliente();
        this.usuario = pedido.getUsuario();
        this.valorTotal = pedido.getValorTotal();
    }

    // Getters e Setters
    public Integer getId() {
        return id;
    }

    public void setId(long l) {
        this.id = (int) l;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        if (pedido == null) {
            throw new IllegalArgumentException("Pedido não pode ser nulo");
        }
        if (!"FINALIZADO".equals(pedido.getStatus())) {
            throw new IllegalArgumentException("Só é possível criar venda para pedidos finalizados");
        }
        this.pedido = pedido;
        this.cliente = pedido.getCliente();
        this.valorTotal = pedido.getValorTotal();
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
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

    public LocalDateTime getDataVenda() {
        return dataVenda;
    }

    public void setDataVenda(LocalDateTime dataVenda) {
        this.dataVenda = dataVenda;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor total não pode ser negativo");
        }
        this.valorTotal = valorTotal;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public void setValorDesconto(BigDecimal valorDesconto) {
        if (valorDesconto == null) {
            valorDesconto = BigDecimal.ZERO;
        }
        if (valorDesconto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor do desconto não pode ser negativo");
        }
        if (valorDesconto.compareTo(valorTotal) > 0) {
            throw new IllegalArgumentException("Valor do desconto não pode ser maior que o valor total");
        }
        this.valorDesconto = valorDesconto;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public void setValorPago(BigDecimal valorPago) {
        if (valorPago == null || valorPago.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor pago não pode ser negativo");
        }
        this.valorPago = valorPago;
        calcularTroco();
    }

    public BigDecimal getTroco() {
        return troco;
    }
    
    public void setTroco(BigDecimal troco) {
		this.troco = troco;
		
	}

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(String formaPagamento) {
        if (formaPagamento == null || formaPagamento.trim().isEmpty()) {
            throw new IllegalArgumentException("Forma de pagamento não pode ser vazia");
        }
        this.formaPagamento = formaPagamento;
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

    public LocalDateTime getDataCancelamento() {
        return dataCancelamento;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public List<Parcela> getParcelas() {
        return new ArrayList<>(parcelas); // Retorna uma cópia da lista
    }

    // Métodos de negócio
    public void adicionarParcela(Parcela parcela) {
        if (!"CARTAO_CREDITO".equals(formaPagamento) && !"BOLETO".equals(formaPagamento)) {
            throw new IllegalStateException("Parcelas só são permitidas para pagamentos com cartão de crédito ou boleto");
        }
        
        parcela.setVenda(this);
        parcelas.add(parcela);
    }
    
    public void removerParcela(Parcela parcela) {
        parcelas.remove(parcela);
    }
    
    public void setParcelas(List<Parcela> parcelas) {
		this.parcelas = parcelas;
		
	}
    
    public BigDecimal getValorComDesconto() {
        return valorTotal.subtract(valorDesconto);
    }
    
    private void calcularTroco() {
        BigDecimal valorAReceber = getValorComDesconto();
        
        if (valorPago.compareTo(valorAReceber) >= 0) {
            this.troco = valorPago.subtract(valorAReceber);
        } else {
            this.troco = BigDecimal.ZERO;
        }
    }
    
    public boolean isPagamentoSuficiente() {
        return valorPago.compareTo(getValorComDesconto()) >= 0;
    }
    
    public void finalizar() {
        if (!"FINALIZADA".equals(status)) {
            throw new IllegalStateException("Venda não está em estado válido para finalização");
        }
        
        if (formaPagamento == null || formaPagamento.trim().isEmpty()) {
            throw new IllegalStateException("Forma de pagamento não informada");
        }
        
        // Verifica se o pagamento é suficiente
        if (!isPagamentoSuficiente()) {
            throw new IllegalStateException("Valor pago é insuficiente");
        }
        
        // Verifica se tem parcelas quando necessário
        if (("CARTAO_CREDITO".equals(formaPagamento) || "BOLETO".equals(formaPagamento)) 
                && parcelas.isEmpty()) {
            throw new IllegalStateException("Pagamento parcelado requer parcelas");
        }
        
        // Atualiza o estoque dos produtos
        for (ItemPedido item : pedido.getItens()) {
            Produto produto = item.getProduto();
            produto.atualizarEstoque(item.getQuantidade(), "SAIDA");
        }
    }
    
    public void cancelar(String motivo) {
        if ("CANCELADA".equals(status)) {
            throw new IllegalStateException("Venda já está cancelada");
        }
        
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("Motivo do cancelamento não pode ser vazio");
        }
        
        this.status = "CANCELADA";
        this.dataCancelamento = LocalDateTime.now();
        this.motivoCancelamento = motivo;
        
        // Restaura o estoque dos produtos
        for (ItemPedido item : pedido.getItens()) {
            Produto produto = item.getProduto();
            produto.atualizarEstoque(item.getQuantidade(), "ENTRADA");
        }
    }
    
    
    public List<ItemPedido> getItens() {
        return itens;
    }
    
    public void setItens(List<ItemPedido> itens) {
        this.itens = itens;
    }
    
    
    public Usuario getVendedor() {
        return vendedor;
    }
    
    public void setVendedor(Usuario vendedor) {
        this.vendedor = vendedor;
    }
    
    public Usuario getOperador() {
        return operador;
    }
    
    public void setOperador(Usuario operador) {
        this.operador = operador;
    }
    
    
    public String getObservacao() {
        return observacao;
    }
    
    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
    
    // Método de conveniência para adicionar um item
    public void addItem(ItemPedido item) {
        if (this.itens == null) {
            this.itens = new ArrayList<>();
        }
        this.itens.add(item);
    }
    
    // Equals e HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Venda venda = (Venda) o;
        return Objects.equals(id, venda.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Venda{" +
                "id=" + id +
                ", pedido=" + (pedido != null ? pedido.getId() : null) +
                ", cliente=" + (cliente != null ? cliente.getNome() : null) +
                ", dataVenda=" + dataVenda +
                ", valorTotal=" + valorTotal +
                ", valorDesconto=" + valorDesconto +
                ", valorPago=" + valorPago +
                ", troco=" + troco +
                ", formaPagamento='" + formaPagamento + '\'' +
                ", status='" + status + '\'' +
                ", numeroNF='" + numeroNF + '\'' +
                '}';
    }

	

	
}