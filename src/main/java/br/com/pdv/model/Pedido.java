package br.com.pdv.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Classe que representa um Pedido no sistema.
 */
public class Pedido {

    private Integer id;
    private Cliente cliente;
    private Usuario usuario;
    private Usuario vendedor;
    private LocalDateTime dataPedido;
    private BigDecimal valorTotal;
    private String status; // Adicionado o campo status
    private List<ItemPedido> itens;
    private Long vendedorId;
    
    // Construtores
    public Pedido() {
    }

    public Pedido(Cliente cliente, Usuario usuario, LocalDateTime dataPedido, BigDecimal valorTotal, String status) {
        this.cliente = cliente;
        this.usuario = usuario;
        this.dataPedido = dataPedido;
        this.valorTotal = valorTotal;
        this.status = status;
    }

    // Getters e Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    
    public Long getVendedorId() {
        return vendedorId;
    }
    
    public void setVendedorId(Long vendedorId) {
        this.vendedorId = vendedorId;
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
        this.usuario = usuario;
    }

    public LocalDateTime getDataPedido() {
        return dataPedido;
    }

    public void setDataPedido(LocalDateTime dataPedido) {
        this.dataPedido = dataPedido;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public void setItens(List<ItemPedido> itens) {
        this.itens = itens;
    }

    // Equals e HashCode (opcional, mas recomendado)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pedido pedido = (Pedido) o;
        return Objects.equals(id, pedido.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // toString (opcional, mas útil para depuração)
    @Override
    public String toString() {
        return "Pedido{" +
                "id=" + id +
                ", cliente=" + cliente +
                ", usuario=" + usuario +
                ", dataPedido=" + dataPedido +
                ", valorTotal=" + valorTotal +
                ", status='" + status + '\'' +
                ", itens=" + itens +
                '}';
    }

	public Usuario getVendedor() {
		return vendedor;
	}

	public void setVendedor(Usuario vendedor) {
		this.vendedor = vendedor;
	}
}