package br.dev.marcus.praticagem.service;

import br.dev.marcus.praticagem.fetcher.HtmlFetcher;
import br.dev.marcus.praticagem.parser.HtmlParser;
import br.dev.marcus.praticagem.model.NavioMovimentacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jsoup.nodes.Document;
import java.util.List;

/**
 * Serviço de negócio responsável por coordenar a obtenção de dados de movimentação de navios.
 * 
 * <p>Esta classe atua como uma <b>camada de orquestração</b>, coordenando o trabalho
 * entre {@link HtmlFetcher} (responsável por buscar o HTML) e {@link HtmlParser}
 * (responsável por extrair os dados estruturados).</p>
 * 
 * <h2>Responsabilidades</h2>
 * <ul>
 *   <li>Coordenar o processo de web scraping (fetch → parse)</li>
 *   <li>Isolar a camada de apresentação (controllers) dos detalhes de implementação</li>
 *   <li>Centralizar a lógica de obtenção de dados</li>
 *   <li>Facilitar testes unitários através de injeção de dependências</li>
 * </ul>
 * 
 * <h2>Padrão de Design: Service Layer</h2>
 * <p>Implementa o padrão <i>Service Layer</i>, encapsulando a lógica de negócio
 * e mantendo os controllers enxutos (apenas delegação).</p>
 * 
 * <pre>
 * ┌─────────────────────────────────────────┐
 * │     Controller (Main.java)              │
 * │  app.get("/movimentacoes", ...)         │
 * └──────────────┬──────────────────────────┘
 *                │ service.buscarMovimentacoes()
 *                ▼
 * ┌─────────────────────────────────────────┐
 * │   MovimentacaoService (ESTA CLASSE)     │ ← Orquestra o processo
 * └──────────────┬──────────────────────────┘
 *                │
 *        ┌───────┴───────┐
 *        ▼               ▼
 * ┌──────────────┐ ┌───────────────┐
 * │ HtmlFetcher  │ │   HtmlParser  │
 * │ (busca HTML) │ │ (extrai dados)│
 * └──────────────┘ └───────────────┘
 * </pre>
 * 
 * <h2>Exemplo de Uso</h2>
 * <pre>{@code
 * // Inicialização (geralmente feita uma vez na aplicação)
 * HtmlFetcher fetcher = new HtmlFetcher("https://site.com", 10000);
 * HtmlParser parser = new HtmlParser();
 * MovimentacaoService service = new MovimentacaoService(fetcher, parser);
 * 
 * // Uso (pode ser chamado múltiplas vezes)
 * try {
 *     List<NavioMovimentacao> movimentacoes = service.buscarMovimentacoes();
 *     
 *     System.out.println("Encontradas " + movimentacoes.size() + " movimentações");
 *     
 *     for (NavioMovimentacao mov : movimentacoes) {
 *         System.out.println("Navio: " + mov.getNavio());
 *         System.out.println("Berço: " + mov.getBerco());
 *     }
 *     
 * } catch (IllegalStateException e) {
 *     System.err.println("Erro ao buscar dados: " + e.getMessage());
 * }
 * }</pre>
 * 
 * <h2>Injeção de Dependências</h2>
 * <p>A classe recebe suas dependências via construtor (<i>constructor injection</i>),
 * seguindo o princípio de <b>Inversão de Dependência</b> (Dependency Inversion Principle).</p>
 * 
 * <p><b>Vantagens desta abordagem:</b></p>
 * <ul>
 *   <li><b>Testabilidade:</b> Fácil criar mocks de fetcher e parser para testes</li>
 *   <li><b>Flexibilidade:</b> Pode trocar implementações sem mudar esta classe</li>
 *   <li><b>Imutabilidade:</b> Dependências são {@code final}, não podem ser alteradas</li>
 *   <li><b>Clareza:</b> Dependências explícitas no construtor</li>
 * </ul>
 * 
 * <h2>Tratamento de Erros</h2>
 * <p>Esta classe <b>não</b> captura exceções. Ela propaga {@link IllegalStateException}
 * lançadas por {@link HtmlFetcher} ou {@link HtmlParser}, permitindo que a camada
 * de apresentação (controllers) decida como tratá-las.</p>
 * 
 * <p><b>Possíveis exceções propagadas:</b></p>
 * <ul>
 *   <li><b>IllegalStateException (HtmlFetcher):</b> Falha de conexão após retries</li>
 *   <li><b>IllegalStateException (HtmlParser):</b> Estrutura da tabela mudou</li>
 * </ul>
 * 
 * <h2>Performance e Caching</h2>
 * <p><b>Nota:</b> Atualmente a classe <b>não implementa cache</b>. Cada chamada a
 * {@link #buscarMovimentacoes()} faz uma nova requisição HTTP ao site.</p>
 * 
 * <p>Para adicionar cache no futuro (se necessário):</p>
 * <pre>{@code
 * // Opção 1: Cache simples com timestamp
 * private List<NavioMovimentacao> cache;
 * private long ultimaAtualizacao;
 * private static final long CACHE_TTL = 5 * 60 * 1000; // 5 minutos
 * 
 * // Opção 2: Usar biblioteca de cache (Caffeine, Guava)
 * private LoadingCache<String, List<NavioMovimentacao>> cache;
 * }</pre>
 * 
 * <h2>Thread Safety</h2>
 * <p>Esta classe é <b>thread-safe</b> pois:</p>
 * <ul>
 *   <li>Não possui estado mutável (dependências são {@code final})</li>
 *   <li>Não compartilha dados entre chamadas</li>
 *   <li>Cada chamada a {@link #buscarMovimentacoes()} é independente</li>
 * </ul>
 * 
 * <p><b>Importante:</b> Se adicionar cache, será necessário sincronização!</p>
 * 
 * @author Marcus
 * @version 1.0
 * @since 2026-02-23
 * 
 * @see HtmlFetcher
 * @see HtmlParser
 * @see NavioMovimentacao
 */
public class MovimentacaoService {

    /**
     * Logger para registrar operações do serviço.
     */
    private static final Logger logger = LoggerFactory.getLogger(MovimentacaoService.class);
    
    /**
     * Componente responsável por buscar o HTML do site de praticagem.
     * 
     * <p>Implementa retry automático e timeout configurável.</p>
     * 
     * @see HtmlFetcher
     */
    private final HtmlFetcher fetcher;

    /**
     * Componente responsável por extrair dados estruturados do HTML.
     * 
     * <p>Implementa parsing resiliente que tolera mudanças menores na estrutura.</p>
     * 
     * @see HtmlParser
     */
    private final HtmlParser parser;

    /**
     * Constrói um novo serviço de movimentação com as dependências especificadas.
     * 
     * <p>Este construtor implementa <i>constructor injection</i>, uma forma de
     * injeção de dependências que torna a classe facilmente testável.</p>
     * 
     * <h3>Exemplo de Uso:</h3>
     * <pre>{@code
     * HtmlFetcher fetcher = new HtmlFetcher(
     *     "https://praticoszp21.com.br/movimentacao-de-navios/",
     *     10000
     * );
     * HtmlParser parser = new HtmlParser();
     * 
     * MovimentacaoService service = new MovimentacaoService(fetcher, parser);
     * }</pre>
     * 
     * <h3>Para Testes Unitários:</h3>
     * <pre>{@code
     * // Cria mocks das dependências
     * HtmlFetcher fetcherMock = mock(HtmlFetcher.class);
     * HtmlParser parserMock = mock(HtmlParser.class);
     * 
     * // Configura comportamento dos mocks
     * when(fetcherMock.fetch()).thenReturn(documentMock);
     * when(parserMock.parse(any())).thenReturn(listaMovimentacoes);
     * 
     * // Cria service com mocks
     * MovimentacaoService service = new MovimentacaoService(fetcherMock, parserMock);
     * 
     * // Testa sem fazer requisições HTTP reais!
     * List<NavioMovimentacao> resultado = service.buscarMovimentacoes();
     * }</pre>
     * 
     * @param fetcher Implementação de {@link HtmlFetcher} para buscar HTML.
     *                Não pode ser {@code null}.
     * @param parser Implementação de {@link HtmlParser} para extrair dados.
     *               Não pode ser {@code null}.
     * 
     * @throws NullPointerException se fetcher ou parser forem {@code null}
     *                              (validação implícita ao usar as dependências)
     */
    public MovimentacaoService(HtmlFetcher fetcher, HtmlParser parser) {
        this.fetcher = fetcher;
        this.parser = parser;

        logger.debug("MovimentacaoService inicializado");
    }

    /**
     * Busca e retorna a lista atualizada de movimentações de navios.
     * 
     * <p>Este método coordena o processo completo de web scraping:</p>
     * <ol>
     *   <li>Delega para {@link HtmlFetcher} buscar o HTML do site</li>
     *   <li>Delega para {@link HtmlParser} extrair os dados estruturados</li>
     *   <li>Retorna a lista de {@link NavioMovimentacao} encontradas</li>
     * </ol>
     * 
     * <h3>Fluxo de Execução Detalhado:</h3>
     * <pre>
     * buscarMovimentacoes()
     *     │
     *     ├─→ fetcher.fetch()
     *     │     │
     *     │     ├─→ Jsoup.connect(url).get()  [Tentativa 1]
     *     │     │   └─ Se falhar: aguarda 2s
     *     │     │
     *     │     ├─→ Jsoup.connect(url).get()  [Tentativa 2]
     *     │     │   └─ Se falhar: aguarda 2s
     *     │     │
     *     │     └─→ Jsoup.connect(url).get()  [Tentativa 3]
     *     │         └─ Se falhar: lança IllegalStateException
     *     │
     *     ├─→ parser.parse(document)
     *     │     │
     *     │     ├─→ Encontra tabela correta
     *     │     ├─→ Mapeia colunas dinamicamente
     *     │     ├─→ Extrai dados de cada linha
     *     │     └─→ Retorna List&lt;NavioMovimentacao&gt;
     *     │
     *     └─→ Retorna lista
     * </pre>
     * 
     * <h3>Dados Retornados:</h3>
     * <p>Cada {@link NavioMovimentacao} contém:</p>
     * <ul>
     *   <li><b>data:</b> Data da movimentação (ex: "23/02/2026")</li>
     *   <li><b>horario:</b> Horário previsto (ex: "08:00")</li>
     *   <li><b>manobra:</b> Tipo de operação (ex: "Atracação", "Desatracação")</li>
     *   <li><b>berco:</b> Identificação do berço (ex: "201", "TVIP")</li>
     *   <li><b>navio:</b> Nome do navio (ex: "MSC MARINA")</li>
     *   <li><b>situacao:</b> Status atual (ex: "Atracado", "Fundeado")</li>
     * </ul>
     * 
     * <h3>Exemplo de Resultado:</h3>
     * <pre>{@code
     * [
     *   NavioMovimentacao(
     *     data="23/02/2026",
     *     horario="08:00",
     *     manobra="Atracação",
     *     berco="201",
     *     navio="MSC MARINA",
     *     situacao="Atracado"
     *   ),
     *   NavioMovimentacao(
     *     data="23/02/2026",
     *     horario="14:30",
     *     manobra="Desatracação",
     *     berco="102",
     *     navio="EVER GIVEN",
     *     situacao="Fundeado"
     *   )
     * ]
     * }</pre>
     * 
     * <h3>Performance:</h3>
     * <ul>
     *   <li><b>Melhor caso:</b> ~2-3 segundos (conexão rápida, site responsivo)</li>
     *   <li><b>Caso médio:</b> ~5-10 segundos (com 1-2 retries)</li>
     *   <li><b>Pior caso:</b> ~34 segundos (3 tentativas × 10s timeout + 2 × 2s backoff)</li>
     * </ul>
     * 
     * <h3>Comportamento em Caso de Lista Vazia:</h3>
     * <p>Se a tabela existir mas estiver vazia, retorna lista vazia (não é erro).</p>
     * 
     * <h3>Thread Safety:</h3>
     * <p>Este método é thread-safe. Múltiplas threads podem chamá-lo simultaneamente
     * pois não compartilha estado mutável entre chamadas.</p>
     * 
     * @return Lista de {@link NavioMovimentacao} encontradas no site. Pode ser vazia
     *         se a tabela estiver vazia, mas nunca é {@code null}.
     * 
     * @throws IllegalStateException se houver falha de conexão após todas as tentativas
     *                               (lançada por {@link HtmlFetcher}) ou se a estrutura
     *                               da tabela HTML tiver mudado significativamente
     *                               (lançada por {@link HtmlParser})
     * 
     * @see HtmlFetcher#fetch()
     * @see HtmlParser#parse(Document)
     * @see NavioMovimentacao
     */
    public List<NavioMovimentacao> buscarMovimentacoes() {

        logger.info("Iniciando busca de movimentações");
        
        // ===== ETAPA 1: BUSCAR HTML =====
        // Delega para HtmlFetcher que implementa retry automático
        // Pode lançar IllegalStateException se todas as tentativas falharem
        logger.debug("Buscando HTML do site...");
        Document document = fetcher.fetch();
        logger.debug("HTML obtido com sucesso");

        // ===== ETAPA 2: PARSEAR HTML E EXTRAIR MOVIMENTAÇÕES =====
        // Delega para HtmlParser que extrai dados de forma resiliente
        // Pode lançar IllegalStateException se estrutura da tabela mudou
        logger.debug("Parseando HTML e extraindo movimentações...");
        List<NavioMovimentacao> movimentacoes = parser.parse(document);
        
        logger.info(
            "Busca concluída com sucesso. {} movimentação(ões) encontrada(s)",
            movimentacoes.size()
        );
        
        return movimentacoes;
    }

    // ===== MÉTODOS AUXILIARES (GETTERS) =====
    // Úteis para testes e debugging
    
    /**
     * Retorna a instância de {@link HtmlFetcher} usada por este serviço.
     * 
     * <p>Útil para testes e debugging.</p>
     * 
     * @return Fetcher configurado neste serviço
     */
    public HtmlFetcher getFetcher() {
        return fetcher;
    }

    /**
     * Retorna a instância de {@link HtmlParser} usada por este serviço.
     * 
     * <p>Útil para testes e debugging.</p>
     * 
     * @return Parser configurado neste serviço
     */
    public HtmlParser getParser() {
        return parser;
    }
}
