package br.dev.marcus.praticagem.parser;

import br.dev.marcus.praticagem.model.NavioMovimentacao;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parser HTML resiliente para extração de dados de movimentação de navios.
 * 
 * <p>Esta classe é responsável por processar o HTML do site de praticagem
 * (https://praticoszp21.com.br/movimentacao-de-navios) e extrair informações
 * estruturadas sobre a movimentação de navios no Porto de Itajaí-SC.</p>
 * 
 * <h2>Estratégia de Parsing Resiliente</h2>
 * <p>O parser foi desenvolvido para ser resistente a mudanças menores na
 * estrutura do HTML. A estratégia implementada segue estes passos:</p>
 * <ol>
 *   <li>Localiza a tabela correta verificando a presença de colunas essenciais</li>
 *   <li>Lê o cabeçalho (elementos &lt;th&gt;) da tabela</li>
 *   <li>Cria um mapa dinâmico: nome-da-coluna → índice-da-coluna</li>
 *   <li>Extrai os dados das linhas usando o mapeamento dinâmico</li>
 * </ol>
 * 
 * <h2>Resiliência a Mudanças</h2>
 * <p>Características que tornam este parser robusto:</p>
 * <ul>
 *   <li><b>Seleção dinâmica de colunas:</b> Não assume posição fixa das colunas</li>
 *   <li><b>Normalização de texto:</b> Remove acentos, espaços extras e unifica case</li>
 *   <li><b>Busca por palavra-chave:</b> Encontra colunas mesmo com nomes ligeiramente diferentes</li>
 *   <li><b>Validação de estrutura:</b> Falha explicitamente se colunas essenciais não existem</li>
 *   <li><b>Tratamento de erros:</b> Retorna lista vazia em vez de quebrar a aplicação</li>
 * </ul>
 * 
 * <h2>Exemplo de Uso</h2>
 * <pre>{@code
 * Document doc = Jsoup.connect("https://praticoszp21.com.br/movimentacao-de-navios").get();
 * HtmlParser parser = new HtmlParser();
 * List<NavioMovimentacao> movimentacoes = parser.parse(doc);
 * 
 * for (NavioMovimentacao mov : movimentacoes) {
 *     System.out.println("Navio: " + mov.getNavio());
 *     System.out.println("Berço: " + mov.getBerco());
 * }
 * }</pre>
 * 
 * <h2>Colunas Essenciais Esperadas</h2>
 * <p>O parser procura por estas colunas (busca case-insensitive e sem acentos):</p>
 * <ul>
 *   <li><b>data:</b> Data da movimentação</li>
 *   <li><b>horario:</b> Horário previsto</li>
 *   <li><b>manobra:</b> Tipo de manobra (atracação, desatracação, etc.)</li>
 *   <li><b>berco:</b> Berço de atracação</li>
 *   <li><b>navio:</b> Nome do navio</li>
 *   <li><b>situacao:</b> Status da movimentação</li>
 * </ul>
 * 
 * @author Marcus
 * @version 1.0
 * @since 2026-02-23
 * 
 * @see NavioMovimentacao
 * @see org.jsoup.nodes.Document
 */
public class HtmlParser {

    /**
     * Logger para registrar eventos e erros durante o parsing.
     * Útil para diagnóstico quando o site muda sua estrutura.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(HtmlParser.class);

    /**
     * Processa o documento HTML e extrai lista de movimentações de navios.
     * 
     * <p>Este é o método principal da classe. Ele coordena todo o processo de
     * parsing e garante que erros não quebrem a aplicação consumidora.</p>
     * 
     * <h4>Comportamento em Caso de Erro</h4>
     * <ul>
     *   <li><b>Tabela não encontrada:</b> Lança {@link IllegalStateException}</li>
     *   <li><b>Estrutura mudou:</b> Lança {@link IllegalStateException}</li>
     *   <li><b>Erro inesperado:</b> Retorna lista vazia e loga o erro</li>
     * </ul>
     * 
     * @param document Documento HTML já parseado pelo Jsoup. Obtido através de
     *                 {@code Jsoup.connect(url).get()} ou {@code Jsoup.parse(html)}
     * @return Lista de objetos {@link NavioMovimentacao} encontrados na tabela.
     *         Retorna lista vazia se ocorrer erro não crítico.
     * @throws IllegalStateException se a tabela não for encontrada ou se a
     *                               estrutura da tabela mudou significativamente
     * 
     * @see #parseInterno(Document)
     */
    public List<NavioMovimentacao> parse(Document document) {

        // Envolvemos tudo em try/catch para garantir que falhas no parser
        // não quebrem a aplicação consumidora (princípio: fail gracefully)
        try {
            return parseInterno(document);

        } catch (IllegalStateException e) {
            // Erros estruturais (tabela não encontrada, estrutura mudou)
            // devem continuar propagando para que a aplicação saiba que
            // há um problema sério que precisa ser corrigido
            throw e;

        } catch (Exception e) {
            // Para erros inesperados (NullPointerException, etc), logamos
            // o problema mas retornamos lista vazia para não quebrar o serviço
            logger.error("Erro ao processar HTML da praticagem", e);

            // Fallback seguro: retorna lista vazia em vez de quebrar
            return Collections.emptyList();
        }
    }

    /**
     * Implementação interna do parsing.
     * 
     * <p>Método separado do {@link #parse(Document)} para facilitar o
     * tratamento diferenciado de erros estruturais vs. erros inesperados.</p>
     * 
     * <h3>Algoritmo de Parsing</h3>
     * <ol>
     *   <li>Localiza a tabela correta usando {@link #encontrarTabelaMovimentacao}</li>
     *   <li>Extrai cabeçalhos e cria mapa nome→índice</li>
     *   <li>Valida presença de todas as colunas essenciais</li>
     *   <li>Itera sobre as linhas de dados e extrai informações</li>
     *   <li>Cria objetos {@link NavioMovimentacao} para cada linha</li>
     * </ol>
     * 
     * @param document Documento HTML a ser processado
     * @return Lista de movimentações encontradas
     * @throws IllegalStateException se estrutura esperada não for encontrada
     */
    private List<NavioMovimentacao> parseInterno(Document document) {

        // Lista que acumula todas as movimentações encontradas
        List<NavioMovimentacao> movimentacoes = new ArrayList<>();

        // ===== PASSO 1: Encontrar a tabela correta =====
        Element tabela = encontrarTabelaMovimentacao(document);

        // Se não encontramos a tabela de movimentação, retornamos lista vazia
        if (tabela == null) {
            logger.error("Tabela de movimentação não encontrada no HTML recebido");
            throw new IllegalStateException("Tabela de movimentação não encontrada");
        }

        // ===== PASSO 2: Extrair todas as linhas (tr) da tabela =====
        Elements linhas = tabela.select("tr");

        // Validação básica: precisa ter pelo menos 2 linhas (cabeçalho + 1 dado)
        if (linhas.size() < 2) {
            logger.warn("Tabela encontrada, mas está vazia (menos de 2 linhas)");
            return movimentacoes; // Retorna lista vazia
        }

        // ===== PASSO 3: Mapear cabeçalho da tabela =====
        // Primeira linha sempre contém os headers (th)
        Element headerRow = linhas.get(0);
        Elements headers = headerRow.select("th");

        // Mapa que relaciona: nome-normalizado-da-coluna → índice-da-coluna
        // Exemplo: "data" → 0, "horario" → 1, "manobra" → 2, etc.
        Map<String, Integer> indiceColunas = new HashMap<>();

        for (int i = 0; i < headers.size(); i++) {
            String nomeColuna = normalizar(headers.get(i).text());
            indiceColunas.put(nomeColuna, i);
            logger.debug("Coluna mapeada: '{}' → índice {}", nomeColuna, i);
        }

        // ===== PASSO 4: Validar presença de colunas essenciais =====
        // Usamos busca por palavra-chave para ser mais tolerante a variações
        // Ex: "horário previsto" ainda vai casar com "horario"
        Integer idxData = encontrarIndicePorPalavra(indiceColunas, "data");
        Integer idxHorario = encontrarIndicePorPalavra(indiceColunas, "horario");
        Integer idxManobra = encontrarIndicePorPalavra(indiceColunas, "manobra");
        Integer idxBerco = encontrarIndicePorPalavra(indiceColunas, "berco");
        Integer idxNavio = encontrarIndicePorPalavra(indiceColunas, "navio");
        Integer idxSituacao = encontrarIndicePorPalavra(indiceColunas, "situacao");

        // Se qualquer coluna essencial não foi encontrada, a estrutura mudou
        if (idxData == null || idxHorario == null || idxManobra == null ||
            idxBerco == null || idxNavio == null || idxSituacao == null) {

            logger.error(
                "Mudança crítica detectada na estrutura da tabela. " +
                "Headers encontrados: {}. " +
                "Índices: data={}, horario={}, manobra={}, berco={}, navio={}, situacao={}",
                indiceColunas.keySet(),
                idxData, idxHorario, idxManobra, idxBerco, idxNavio, idxSituacao
            );

            throw new IllegalStateException(
                "Estrutura da tabela mudou. Colunas esperadas não foram encontradas."
            );
        }
            
        // ===== PASSO 5: Ler linhas de dados =====
        // Começamos em i=1 porque i=0 é o cabeçalho
        for (int i = 1; i < linhas.size(); i++) {

            // Extrai todas as células (td) da linha atual
            Elements colunas = linhas.get(i).select("td");

            // Pula linhas vazias (podem existir no HTML)
            if (colunas.isEmpty()) {
                logger.debug("Linha {} está vazia, pulando...", i);
                continue;
            }

            // Extrai dados de cada coluna usando os índices mapeados
            // Se índice for inválido, pegarPorIndice retorna string vazia
            String data = pegarPorIndice(colunas, idxData);
            String horario = pegarPorIndice(colunas, idxHorario);
            String manobra = pegarPorIndice(colunas, idxManobra);
            String berco = pegarPorIndice(colunas, idxBerco);
            String navio = pegarPorIndice(colunas, idxNavio);
            String situacao = pegarPorIndice(colunas, idxSituacao);

            // Cria objeto de movimentação e adiciona à lista
            NavioMovimentacao movimentacao = new NavioMovimentacao(
                data,
                horario,
                manobra,
                berco,
                navio,
                situacao
            );

            movimentacoes.add(movimentacao);

            logger.debug(
                    "Linha {} processada: Navio={}, Berço={}, Situação={}",
                    i, navio, berco, situacao
            );
        }

        logger.info("Parsing concluído. {} movimentações encontradas", movimentacoes.size());
        return movimentacoes;

    }

    /**
     * Busca a tabela de movimentação dentro do documento HTML.
     * 
     * <p>Como a página pode conter múltiplas tabelas (menu, rodapé, etc),
     * precisamos identificar qual é a tabela correta. A estratégia é procurar
     * por uma tabela que contenha TODAS as colunas essenciais no cabeçalho.</p>
     * 
     * <h3>Algoritmo de Identificação</h3>
     * <ol>
     *   <li>Seleciona todas as tags &lt;table&gt; do documento</li>
     *   <li>Para cada tabela, extrai os headers (&lt;th&gt;)</li>
     *   <li>Verifica se contém todas as 6 colunas essenciais</li>
     *   <li>Retorna a primeira tabela que atende aos critérios</li>
     * </ol>
     * 
     * <p><b>Vantagem desta abordagem:</b> Mesmo se adicionarem outras tabelas
     * na página, continuaremos encontrando a tabela correta.</p>
     * 
     * @param document Documento HTML a ser pesquisado
     * @return Elemento &lt;table&gt; da movimentação, ou {@code null} se não encontrada
     * 
     * @see #contemColuna(Set, String)
     */
    private Element encontrarTabelaMovimentacao(Document document) {

        // Seleciona todas as tabelas da página (pode haver várias)
        Elements tabelas = document.select("table");

        logger.debug("Encontradas {} tabelas no documento", tabelas.size());

        // Testa cada tabela para ver se é a que queremos
        for (Element tabela : tabelas) {

            // Extrai todos os headers desta tabela
            Elements headers = tabela.select("th");
            
            // Cria conjunto com nomes normalizados dos headers
            Set<String> nomes = new HashSet<>();
            for (Element th : headers) {
                nomes.add(normalizar(th.text()));
            }

            logger.debug("Testando tabela com headers: {}", nomes);

            // Verificamos se contém TODAS as colunas essenciais
            // Se sim, esta é nossa tabela!
            if (contemColuna(nomes, "data") &&
                contemColuna(nomes, "horario") &&
                contemColuna(nomes, "manobra") &&
                contemColuna(nomes, "berco") &&
                contemColuna(nomes, "navio") &&
                contemColuna(nomes, "situacao")) {

                logger.info("Tabela de movimentação identificada com sucesso");
                return tabela;

            }
        }

        // Nenhuma tabela atendeu aos critérios
        logger.warn("Nenhuma tabela com as colunas esperadas foi encontrada");

        return null;

    }

    /**
     * Normaliza texto para comparação consistente.
     * 
     * <p>Processo de normalização aplicado:</p>
     * <ol>
     *   <li>Remove acentuação (á→a, ç→c, ô→o, etc.)</li>
     *   <li>Converte para minúsculas (A→a, B→b, etc.)</li>
     *   <li>Remove espaços em branco extras no início e fim</li>
     * </ol>
     * 
     * <h3>Por que normalizar?</h3>
     * <p>Exemplos de variações que a normalização resolve:</p>
     * <ul>
     *   <li>"Horário" e "horario" → ambos viram "horario"</li>
     *   <li>"BERÇO" e "berço" → ambos viram "berco"</li>
     *   <li>" Situação " e "situacao" → ambos viram "situacao"</li>
     * </ul>
     * 
     * <p><b>Importante:</b> Esta normalização é aplicada tanto nos headers da
     * tabela quanto nas palavras-chave de busca, garantindo matching consistente.</p>
     * 
     * @param texto String a ser normalizada (pode ser {@code null})
     * @return String normalizada (sem acentos, minúsculas, sem espaços extras).
     *         Retorna string vazia se entrada for {@code null}
     * 
     * @see java.text.Normalizer
     */
    private String normalizar(String texto) {
        
        // Proteção contra null
        if (texto == null) {
            return "";
        }

        // Decomposição Unicode seguida de remoção de marcas diacríticas
        // NFD = Canonical Decomposition (á vira a + ́ )
        // Regex remove os combining marks (os acentos separados)
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Remove espaços extras e converte para minúsculas
        return semAcento.trim().toLowerCase();
    }

    /**
     * Procura o índice de uma coluna que contenha a palavra-chave especificada.
     * 
     * <p>Esta busca é <b>case-insensitive</b> e <b>sem acentos</b>, usando o
     * método {@link #normalizar(String)}. Além disso, usa busca por substring
     * (contains) ao invés de match exato, tornando o parser mais tolerante.</p>
     * 
     * <h3>Exemplos de Matching</h3>
     * <table border="1">
     *   <tr>
     *     <th>Palavra-chave</th>
     *     <th>Nome da Coluna</th>
     *     <th>Match?</th>
     *   </tr>
     *   <tr><td>"horario"</td><td>"Horário Previsto"</td><td>✅ Sim</td></tr>
     *   <tr><td>"horario"</td><td>"horario"</td><td>✅ Sim</td></tr>
     *   <tr><td>"situacao"</td><td>"Situação da Manobra"</td><td>✅ Sim</td></tr>
     *   <tr><td>"berco"</td><td>"Porto"</td><td>❌ Não</td></tr>
     * </table>
     * 
     * <p><b>Vantagem:</b> Se o site mudar "Horário" para "Horário Previsto",
     * o parser continua funcionando sem modificações.</p>
     * 
     * @param mapa Mapeamento nome-da-coluna → índice (já normalizado)
     * @param palavraChave Palavra a buscar dentro dos nomes das colunas
     * @return Índice da primeira coluna que contém a palavra-chave,
     *         ou {@code null} se não encontrada
     * 
     * @see #normalizar(String)
     */
    private Integer encontrarIndicePorPalavra(
        Map<String, Integer> mapa,
        String palavraChave
    ) {
        for (Map.Entry<String, Integer> entry : mapa.entrySet()) {

            String nomeColuna = entry.getKey();

            // Usa 'contains' ao invés de 'equals' para ser mais tolerante
            // Ex: "horario" casa com "horario previsto"
            if (nomeColuna.contains(palavraChave)) {
                logger.debug(
                    "Palavra-chave '{}' encontrada na coluna '{}' (índice {})",
                    palavraChave, nomeColuna, entry.getValue()
                );
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Extrai valor de uma célula da tabela de forma segura.
     * 
     * <p>Método utilitário que acessa uma coluna específica pelo índice,
     * com proteção contra índices inválidos.</p>
     * 
     * <h3>Proteções Implementadas</h3>
     * <ul>
     *   <li>Verifica se índice não é {@code null}</li>
     *   <li>Verifica se índice está dentro dos limites da lista</li>
     *   <li>Remove espaços em branco extras do texto</li>
     *   <li>Retorna string vazia em caso de problema (fail-safe)</li>
     * </ul>
     * 
     * @param colunas Lista de elementos &lt;td&gt; da linha atual
     * @param indice Índice da coluna desejada (pode ser {@code null})
     * @return Texto da célula (trimmed), ou string vazia se índice inválido
     */
    private String pegarPorIndice(Elements colunas, Integer indice) {

        // Proteção: índice null ou fora dos limites da lista
        if (indice == null || indice >= colunas.size()) {
            logger.debug(
                "Índice inválido {} para lista de {} colunas",
                indice, colunas.size()
            );
            return ""; // Retorna vazio em vez de quebrar
        }
        
        // Extrai texto da célula e remove espaços extras
        String valor = colunas.get(indice).text().trim();
        return valor;
    }

    /**
     * Verifica se um conjunto de nomes contém determinada palavra-chave.
     * 
     * <p>Similar ao {@link #encontrarIndicePorPalavra}, mas trabalha com
     * {@link Set} ao invés de {@link Map}, e retorna boolean ao invés de índice.</p>
     * 
     * <p>Usada durante a identificação da tabela correta em
     * {@link #encontrarTabelaMovimentacao(Document)}.</p>
     * 
     * @param nomes Conjunto de nomes de colunas (já normalizados)
     * @param palavraChave Palavra a procurar (será usada com 'contains')
     * @return {@code true} se algum nome contém a palavra-chave,
     *         {@code false} caso contrário
     * 
     * @see #encontrarTabelaMovimentacao(Document)
     */
    private boolean contemColuna(Set<String> nomes, String palavraChave) {
        for (String nome : nomes) {
            if (nome.contains(palavraChave)) {
                return true;
            }
        }

        return false;
    }

}
