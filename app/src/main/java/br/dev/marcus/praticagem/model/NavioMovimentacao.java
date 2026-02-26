package br.dev.marcus.praticagem.model;

 /**
 * Representa uma movimentação de navio no Porto de Itajaí-SC (DTO imutável).
 * 
 * <p>Esta classe encapsula as informações de uma linha da tabela de movimentações
 * obtida do site de praticagem. Implementada como Java Record para garantir
 * imutabilidade, simplicidade e segurança entre threads.</p>
 * 
 * <h2>Por que usar Record?</h2>
 * <p>A partir do Java 16, os <i>records</i> são a forma recomendada para criar
 * classes de dados imutáveis. Esta implementação substitui o padrão antigo de
 * criar classes com muitos getters, equals, hashCode e toString manualmente.</p>
 * 
 * <h3>Vantagens do Record:</h3>
 * <ul>
 *   <li><b>Imutabilidade automática:</b> Todos os campos são {@code final}</li>
 *   <li><b>Getters automáticos:</b> {@code navio()} ao invés de {@code getNavio()}</li>
 *   <li><b>equals() e hashCode():</b> Gerados automaticamente baseados nos campos</li>
 *   <li><b>toString():</b> Representação legível gerada automaticamente</li>
 *   <li><b>Menos código:</b> ~80% menos código que classe tradicional</li>
 *   <li><b>Thread-safe:</b> Impossível modificar após criação</li>
 * </ul>
 * 
 * <h4>Comparação: Record vs Classe Tradicional</h4>
 * <pre>{@code
 * // COM RECORD (6 linhas) ✅
 * public record NavioMovimentacao(String data, String navio) {}
 * 
 * // SEM RECORD (classe tradicional - ~40 linhas) ❌
 * public class NavioMovimentacao {
 *     private final String data;
 *     private final String navio;
 *     
 *     public NavioMovimentacao(String data, String navio) {
 *         this.data = data;
 *         this.navio = navio;
 *     }
 *     
 *     public String getData() { return data; }
 *     public String getNavio() { return navio; }
 *     
 *     // + equals, hashCode, toString...
 * }
 * }</pre>
 * 
 * <h4>Estrutura dos Dados</h4>
 * <p>Cada instância representa uma linha da tabela HTML de movimentações:</p>
 * <table border="1">
 *   <caption>Campos da Movimentação</caption>
 *   <tr>
 *     <th>Campo</th>
 *     <th>Tipo</th>
 *     <th>Exemplo</th>
 *     <th>Descrição</th>
 *   </tr>
 *   <tr>
 *     <td>data</td>
 *     <td>String</td>
 *     <td>"23/02/2026"</td>
 *     <td>Data da movimentação</td>
 *   </tr>
 *   <tr>
 *     <td>horario</td>
 *     <td>String</td>
 *     <td>"08:00"</td>
 *     <td>Horário previsto</td>
 *   </tr>
 *   <tr>
 *     <td>manobra</td>
 *     <td>String</td>
 *     <td>"Atracação"</td>
 *     <td>Tipo de operação</td>
 *   </tr>
 *   <tr>
 *     <td>berco</td>
 *     <td>String</td>
 *     <td>"201"</td>
 *     <td>Berço de atracação</td>
 *   </tr>
 *   <tr>
 *     <td>navio</td>
 *     <td>String</td>
 *     <td>"MSC MARINA"</td>
 *     <td>Nome do navio</td>
 *   </tr>
 *   <tr>
 *     <td>situacao</td>
 *     <td>String</td>
 *     <td>"Confirmado"</td>
 *     <td>Status atual</td>
 *   </tr>
 * </table>
 * 
 * <h4>Exemplo de Uso</h4>
 * <pre>{@code
 * // Criar uma nova movimentação
 * NavioMovimentacao mov = new NavioMovimentacao(
 *     "23/02/2026", "08:00", "Atracação",
 *     "201", "MSC MARINA", "Confirmado"
 * );
 * 
 * // Acessar dados (getters automáticos - sem "get")
 * String nomeNavio = mov.navio();     // "MSC MARINA"
 * String berco = mov.berco();         // "201"
 * 
 * // toString() automático
 * System.out.println(mov);
 * // NavioMovimentacao[data=23/02/2026, horario=08:00, ...]
 * 
 * // equals() automático (comparação por valor)
 * NavioMovimentacao outra = new NavioMovimentacao(...);
 * boolean iguais = mov.equals(outra); // true se valores iguais
 * }</pre>
 * 
 * <h4>Serialização JSON</h4>
 * <p>Automaticamente serializável para JSON pelo Jackson (usado pelo Javalin):</p>
 * <pre>{@code
 * {
 *   "data": "23/02/2026",
 *   "horario": "08:00",
 *   "manobra": "Atracação",
 *   "berco": "201",
 *   "navio": "MSC MARINA",
 *   "situacao": "Confirmado"
 * }
 * }</pre>
 * 
 * <h4>Imutabilidade e Thread Safety</h4>
 * <p>Records são imutáveis por design. Uma vez criado, o objeto não pode ser
 * modificado, tornando-o naturalmente thread-safe.</p>
 * <pre>{@code
 * // Para "modificar", precisa criar NOVO objeto
 * NavioMovimentacao atualizada = new NavioMovimentacao(
 *     original.data(),
 *     original.horario(),
 *     "Desatracação",     // mudança
 *     original.berco(),
 *     original.navio(),
 *     "Em andamento"      // mudança
 * );
 * }</pre>
 * 
 * <h4>Uso em Coleções</h4>
 * <pre>{@code
 * // List (permite duplicatas)
 * List<NavioMovimentacao> lista = new ArrayList<>();
 * lista.add(mov);
 * 
 * // Set (remove duplicatas via equals/hashCode)
 * Set<NavioMovimentacao> unicas = new HashSet<>(lista);
 * 
 * // Map (usar como chave)
 * Map<NavioMovimentacao, String> notas = new HashMap<>();
 * }</pre>
 * 
 * <h4>Compatibilidade</h4>
 * <table border="1">
 *   <caption>Compatibilidade com Frameworks</caption>
 *   <tr>
 *     <th>Framework</th>
 *     <th>Status</th>
 *   </tr>
 *   <tr><td>Jackson (JSON)</td><td>✅ Suportado</td></tr>
 *   <tr><td>Gson (JSON)</td><td>✅ Suportado</td></tr>
 *   <tr><td>Spring Framework</td><td>✅ Desde 5.3</td></tr>
 *   <tr><td>JPA/Hibernate</td><td>⚠️ Limitado</td></tr>
 * </table>
 * 
 * @param data Data da movimentação no formato dd/MM/yyyy (ex: "23/02/2026")
 * @param horario Horário previsto no formato HH:mm (ex: "08:00")
 * @param manobra Tipo de operação (ex: "Atracação", "Desatracação")
 * @param berco Identificação do berço (ex: "201", "TVIP")
 * @param navio Nome completo do navio (ex: "MSC MARINA")
 * @param situacao Status atual (ex: "Confirmado", "Em andamento")
 * 
 * @author Marcus
 * @version 1.0
 * @since 2026-02-23
 * 
 * @see br.dev.marcus.praticagem.parser.HtmlParser
 * @see br.dev.marcus.praticagem.service.MovimentacaoService
 */
public record NavioMovimentacao(
    String data,
    String horario,
    String manobra,
    String berco,
    String navio,
    String situacao
) {
    // Record compacto - não precisa de corpo
    // Todos os métodos (getters, equals, hashCode, toString) são gerados automaticamente
    
    // Se precisar de validação ou métodos auxiliares no futuro, adicione aqui
    // Exemplo:
    // public NavioMovimentacao {
    //     Objects.requireNonNull(navio, "Nome do navio não pode ser null");
    //     Objects.requireNonNull(berco, "Berço não pode ser null");
    // 

}
