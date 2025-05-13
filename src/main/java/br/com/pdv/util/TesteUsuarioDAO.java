package br.com.pdv.util;

import br.com.pdv.dao.UsuarioDAO;
import br.com.pdv.model.Usuario;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class TesteUsuarioDAO {
    
    private static UsuarioDAO usuarioDAO;
    private static Usuario usuarioTeste;
    
    public static void main(String[] args) {
        try {
            System.out.println("Iniciando testes do UsuarioDAO...\n");
            
            // Inicializa o DAO
            usuarioDAO = UsuarioDAO.getInstance();
            
            // Executa os testes em sequência
            executarTestes();
            
            System.out.println("\nTodos os testes concluídos com sucesso!");
            
        } catch (Exception e) {
            System.err.println("Erro durante os testes: " + e.getMessage());
            e.printStackTrace();
        } finally {
      
                DatabaseConnection.getInstance().closeAllConnections();
        }
    }
    
    private static void executarTestes() throws SQLException {
        testarCreate();
        testarFindById();
        testarFindByLogin();
        testarUpdate();
        testarAutenticacao();
        testarFindAll();
        testarExists();
        testarCount();
        testarDelete();
    }
    
    private static void testarCreate() throws SQLException {
        System.out.println("Teste de criação de usuário:");
        
        usuarioTeste = new Usuario();
        usuarioTeste.setNome("Usuário Teste");
        usuarioTeste.setLogin("teste" + LocalDateTime.now().getNano());
        usuarioTeste.setSenha("123456");
        usuarioTeste.setPerfil("VENDEDOR");
        usuarioTeste.setAtivo(true);
        usuarioTeste.setDataCadastro(LocalDateTime.now());
        usuarioTeste.setDataAtualizacao(LocalDateTime.now());
        
        usuarioTeste = usuarioDAO.create(usuarioTeste);
        
        if (usuarioTeste != null && usuarioTeste.getId() != null && usuarioTeste.getId() > 0) {
            System.out.println("✅ Usuário criado com sucesso! ID: " + usuarioTeste.getId());
        } else {
            throw new RuntimeException("❌ Falha ao criar usuário");
        }
    }
    
    private static void testarFindById() throws SQLException {
        System.out.println("\nTeste de busca por ID:");
        
        Usuario usuarioEncontrado = usuarioDAO.findById(usuarioTeste.getId());
        
        if (usuarioEncontrado != null && usuarioEncontrado.getId().equals(usuarioTeste.getId())) {
            System.out.println("✅ Usuário encontrado por ID com sucesso!");
        } else {
            throw new RuntimeException("❌ Falha ao buscar usuário por ID");
        }
    }
    
    private static void testarFindByLogin() throws SQLException {
        System.out.println("\nTeste de busca por Login:");
        
        Usuario usuarioEncontrado = usuarioDAO.findByLogin(usuarioTeste.getLogin());
        
        if (usuarioEncontrado != null && usuarioEncontrado.getLogin().equals(usuarioTeste.getLogin())) {
            System.out.println("✅ Usuário encontrado por login com sucesso!");
        } else {
            throw new RuntimeException("❌ Falha ao buscar usuário por login");
        }
    }
    
    private static void testarUpdate() throws SQLException {
        System.out.println("\nTeste de atualização de usuário:");
        
        String novoNome = "Nome Atualizado " + LocalDateTime.now().getNano();
        usuarioTeste.setNome(novoNome);
        
        if (usuarioDAO.update(usuarioTeste)) {
            Usuario usuarioAtualizado = usuarioDAO.findById(usuarioTeste.getId());
            if (usuarioAtualizado.getNome().equals(novoNome)) {
                System.out.println("✅ Usuário atualizado com sucesso!");
            } else {
                throw new RuntimeException("❌ Dados do usuário não foram atualizados corretamente");
            }
        } else {
            throw new RuntimeException("❌ Falha ao atualizar usuário");
        }
    }
    
    private static void testarAutenticacao() throws SQLException {
        System.out.println("\nTeste de autenticação:");
        
        if (usuarioDAO.autenticar(usuarioTeste.getLogin(), usuarioTeste.getSenha())) {
            System.out.println("✅ Autenticação realizada com sucesso!");
        } else {
            throw new RuntimeException("❌ Falha na autenticação");
        }
    }
    
    private static void testarFindAll() throws SQLException {
        System.out.println("\nTeste de listagem de usuários:");
        
        List<Usuario> usuarios = usuarioDAO.findAll();
        
        if (!usuarios.isEmpty() && 
            usuarios.stream().anyMatch(u -> u.getId().equals(usuarioTeste.getId()))) {
            System.out.println("✅ Lista de usuários retornada com sucesso!");
            System.out.println("   Total de usuários: " + usuarios.size());
        } else {
            System.out.println("⚠️ Lista de usuários vazia ou usuário teste não encontrado!");
        }
    }
    
    private static void testarExists() throws SQLException {
        System.out.println("\nTeste de verificação de existência:");
        
        if (usuarioDAO.exists(usuarioTeste.getId())) {
            System.out.println("✅ Verificação de existência funcionando!");
        } else {
            throw new RuntimeException("❌ Falha na verificação de existência");
        }
    }
    
    private static void testarCount() throws SQLException {
        System.out.println("\nTeste de contagem de registros:");
        
        long count = usuarioDAO.count();
        if (count > 0) {
            System.out.println("✅ Total de usuários cadastrados: " + count);
        } else {
            throw new RuntimeException("❌ Contagem de usuários retornou zero");
        }
    }
    
    private static void testarDelete() throws SQLException {
        System.out.println("\nTeste de exclusão de usuário:");
        
        if (usuarioDAO.delete(usuarioTeste.getId())) {
            System.out.println("✅ Usuário excluído com sucesso!");
            
            if (!usuarioDAO.exists(usuarioTeste.getId())) {
                System.out.println("✅ Confirmado: usuário não existe mais no banco!");
            } else {
                throw new RuntimeException("❌ Usuário ainda existe após exclusão");
            }
        } else {
            throw new RuntimeException("❌ Falha ao excluir usuário");
        }
    }
}