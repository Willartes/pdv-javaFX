package br.com.pdv.model;

public enum StatusPedido {
    ABERTO {
        @Override
        public boolean podeAlterarPara(StatusPedido novoStatus) {
            return novoStatus == FINALIZADO || novoStatus == CANCELADO;
        }
    },
    FINALIZADO {
        @Override
        public boolean podeAlterarPara(StatusPedido novoStatus) {
            return false;
        }
    },
    CANCELADO {
        @Override
        public boolean podeAlterarPara(StatusPedido novoStatus) {
            return false;
        }
    };
    
    public abstract boolean podeAlterarPara(StatusPedido novoStatus);
}