package br.dev.marcus.praticagem.fetcher;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Cliente HTTP resiliente para busca de páginas HTML usando Jsoup.
 * 
 * <p>Esta classe é responsável por fazer requisições HTTP ao site de praticagem
 * e retornar o documento HTML parseado. Implementa estratégias de resiliência
 * para lidar com falhas temporárias de rede.</p>
 * 
 * <h2>Características de Resiliência</h2>
 * <ul>
 *   <li><b>Retry automático:</b> Até 3 tentativas em caso de falha de rede</li>
 *   <li><b>Backoff entre tentativas:</b> Aguarda 2 segundos antes de retentar</li>
 *   <li><b>Timeout configurável:</b> Evita travamento em conexões lentas</li>
 *   <li><b>User-Agent customizado:</b> Identifica o bot para o servidor</li>
 *   <li><b>Tratamento de erros diferenciado:</b> URL inválida falha imediatamente</li>
 * </ul>
 * 
 * <h2>Cenários de Falha</h2>
 * <p>A classe trata diferentes tipos de falha de forma adequada:</p>
 * <table border="1">
 * <caption>Cenários de Falha</caption>
 *   <tr>
 *     <th>Tipo de Erro</th>
 *     <th>Comportamento</th>
 *     <th>Retry?</th>
 *   </tr>
 *   <tr>
 *     <td>URL malformada</td>
 *     <td>Falha imediatamente</td>
 *     <td>❌ Não (erro de config)</td>
 *   </tr>
 *   <tr>
 *     <td>Timeout</td>
 *     <td>Aguarda 2s e retenta</td>
 *     <td>✅ Sim (até 3x)</td>
 *   </tr>
 *   <tr>
 *     <td>Erro de rede</td>
 *     <td>Aguarda 2s e retenta</td>
 *     <td>✅ Sim (até 3x)</td>
 *   </tr>
 *   <tr>
 *     <td>Site fora do ar</td>
 *     <td>Aguarda 2s e retenta</td>
 *     <td>✅ Sim (até 3x)</td>
 *   </tr>
 * </table>
 * 
 * <h2>Exemplo de Uso</h2>
 * <pre>{@code
 * // Criar fetcher com timeout de 10 segundos
 * HtmlFetcher fetcher = new HtmlFetcher(
 *     "https://praticoszp21.com.br/movimentacao-de-navios",
 *     10000
 * );
 * 
 * try {
 *     // Busca o HTML (com retry automático)
 *     Document doc = fetcher.fetch();
 *     
 *     // Processa o documento...
 *     String titulo = doc.title();
 *     System.out.println("Página: " + titulo);
 *     
 * } catch (IllegalStateException e) {
 *     // Falhou após todas as tentativas
 *     System.err.println("Não foi possível acessar o site: " + e.getMessage());
 * }
 * }</pre>
 * 
 * <h2>Configuração Recomendada</h2>
 * <ul>
 *   <li><b>Timeout:</b> 10000ms (10 segundos) para conexões normais</li>
 *   <li><b>Timeout:</b> 30000ms (30 segundos) para sites lentos</li>
 *   <li><b>User-Agent:</b> Identifique seu bot adequadamente (exemplo: "MeuBot/1.0")</li>
 * </ul>
 * 
 * <h2>Considerações de Performance</h2>
 * <p>Com 3 tentativas e backoff de 2s, o tempo máximo de execução é:</p>
 * <ul>
 *   <li>Melhor caso: ~tempo de conexão (poucos segundos)</li>
 *   <li>Pior caso: (timeout × 3) + (2s × 2) = ~34 segundos com timeout de 10s</li>
 * </ul>
 * 
 * @author Marcus
 * @version 1.0
 * @since 2026-02-23
 * 
 * @see org.jsoup.Jsoup
 * @see org.jsoup.nodes.Document
 */
public class HtmlFetcher {

    /**
     * Logger para registrar tentativas de conexão e falhas.
     * Essencial para diagnosticar problemas de conectividade em produção.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(HtmlFetcher.class);

    /**
     * URL do site a ser acessado.
     * Deve ser uma URL válida e completa (incluindo protocolo http/https).
     */
    private final String url;

    /**
     * Tempo máximo de espera por uma resposta, em milissegundos.
     * 
     * <p>Valores típicos:</p>
     * <ul>
     *   <li>5000ms (5s): Sites rápidos, boa conexão</li>
     *   <li>10000ms (10s): Padrão recomendado</li>
     *   <li>30000ms (30s): Sites lentos ou conexão instável</li>
     * </ul>
     */
    private final int timeout;

    /**
     * Número máximo de tentativas antes de desistir.
     * Atualmente fixo em 3, mas poderia ser configurável no futuro.
     */
    private final int maxRetries;

    /**
     * Tempo de espera entre tentativas, em milissegundos.
     * Backoff fixo de 2 segundos para dar tempo do servidor se recuperar.
     */
    private final int retryBackoff;

/**
 * Identificação do bot nos headers HTTP (User-Agent).
 * 
 * <p>Seguindo as boas práticas de web scraping, identificamos claramente
 * nosso bot com nome, versão e contato para que administradores do site
 * possam nos contatar em caso de problemas ou bloqueios.</p>
 * 
 * <p>Formato: NomeBot/Versão (Contact: email-de-contato)</p>
 * 
 * @see <a href="https://developers.google.com/search/docs/crawling-indexing/overview-google-crawlers">Google Bot Guidelines</a>
 */
    private static final String USER_AGENT = "GVI-Itajaí-Bot/1.0 (Contact: marcus-silva.ms@marinha.mil.br)";

    /**
     * Constrói um novo fetcher configurado para uma URL específica.
     * 
     * <p>O construtor não valida a URL - a validação ocorre na primeira
     * chamada ao método {@link #fetch()}.</p>
     * 
     * @param url URL completa do site a ser acessado (ex: "https://example.com")
     * @param timeout Timeout em milissegundos para a requisição HTTP.
     *                      Valores típicos: 5000 (5s) a 30000 (30s)
     * @param maxRetries Número máximo de tentativas antes de desistir (atualmente 3)
     * @param retryBackoff Tempo de espera entre tentativas, em milissegundos
     * 
     * @see #fetch()
     */
    public HtmlFetcher(String url, int timeout, int maxRetries, int retryBackoff) {
        this.url = url;
        this.timeout = timeout;
        this.maxRetries = maxRetries;
        this.retryBackoff = retryBackoff;
    }

    /**
     * Busca e parseia o HTML da URL configurada, com retry automático.
     * 
     * <p>Este método implementa a lógica de retry com backoff exponencial.
     * Em caso de falhas temporárias (rede, timeout), faz até 3 tentativas
     * com intervalo de 2 segundos entre elas.</p>
     * 
     * <h4>Algoritmo de Retry</h4>
     * <pre>
     * Para cada tentativa (1 a 3):
     *   1. Tenta conectar via Jsoup
     *   2. Se sucesso → retorna Document
     *   3. Se falha de rede/timeout:
     *      a. Loga o erro
     *      b. Se não é última tentativa → aguarda 2s e retenta
     *      c. Se é última tentativa → lança exceção
     *   4. Se URL inválida → falha imediatamente (erro de config)
     * </pre>
     * 
     * <h4>Configuração da Requisição</h4>
     * <ul>
     *   <li><b>Timeout:</b> Valor configurado no construtor</li>
     *   <li><b>User-Agent:</b> "Mozilla/5.0" (identifica o cliente)</li>
     *   <li><b>Método HTTP:</b> GET</li>
     *   <li><b>Follow redirects:</b> Sim (comportamento padrão do Jsoup)</li>
     * </ul>
     * 
     * <h4>Tratamento de Erros</h4>
     * <table border="1">
     * <caption>Tratamento de Erros</caption>
     *   <tr>
     *     <th>Exceção Capturada</th>
     *     <th>Causa Comum</th>
     *     <th>Ação</th>
     *   </tr>
     *   <tr>
     *     <td>IllegalArgumentException</td>
     *     <td>URL malformada</td>
     *     <td>Falha imediata (não retenta)</td>
     *   </tr>
     *   <tr>
     *     <td>IOException (timeout)</td>
     *     <td>Servidor demorou demais</td>
     *     <td>Aguarda 2s e retenta</td>
     *   </tr>
     *   <tr>
     *     <td>IOException (conexão recusada)</td>
     *     <td>Servidor fora do ar</td>
     *     <td>Aguarda 2s e retenta</td>
     *   </tr>
     *   <tr>
     *     <td>IOException (host não encontrado)</td>
     *     <td>DNS falhou / site não existe</td>
     *     <td>Aguarda 2s e retenta</td>
     *   </tr>
     * </table>
     * 
     * <h4>Logs Gerados</h4>
     * <ul>
     *   <li><b>INFO:</b> Cada tentativa de conexão</li>
     *   <li><b>WARN:</b> Falha em tentativa individual</li>
     *   <li><b>ERROR:</b> URL inválida ou falha após todas as tentativas</li>
     * </ul>
     * 
     * @return Documento HTML parseado pelo Jsoup, pronto para ser processado
     *         pelo {@link br.dev.marcus.praticagem.parser.HtmlParser}
     * 
     * @throws IllegalStateException se a URL for inválida (erro de configuração)
     *         ou se todas as tentativas de conexão falharem (erro de rede/disponibilidade)
     * 
     * @see org.jsoup.Jsoup#connect(String)
     * @see br.dev.marcus.praticagem.parser.HtmlParser#parse(Document)
     */
    public Document fetch() {

        int tentativaAtual = 0;

        // Loop de retry: tenta até maxRetries vezes
        while (tentativaAtual < maxRetries) {

            try {
                tentativaAtual++;

                logger.info(
                    "Tentativa {}/{} de buscar HTML da URL: {}",
                    tentativaAtual, maxRetries, url
                );

                // ===== EXECUÇÃO DA REQUISIÇÃO HTTP =====
                // Jsoup.connect() cria a conexão e .get() executa a requisição
                Document document = Jsoup.connect(url)
                    .timeout(timeout)          // Timeout configurável
                    .userAgent(USER_AGENT)           // Identifica o cliente
                    .get();                          // Executa GET e parseia HTML
                
                // Se chegamos aqui, a requisição foi bem-sucedida!
                logger.info("HTML obtido com sucesso na tentativa {}", tentativaAtual);
                return document;
            
            } catch (IllegalArgumentException e) {
                // ===== ERRO DE CONFIGURAÇÃO: URL INVÁLIDA =====
                // Este erro indica problema no código/config, não no servidor
                // Não adianta fazer retry, então falhamos imediatamente
                logger.error(
                    "URL malformada ou inválida: '{}'. " +
                    "Verifique a configuração da aplicação.",
                    url, e
                );
                
                // Lançamos IllegalStateException para indicar erro de estado/config
                throw new IllegalStateException(
                    "URL configurada é inválida: " + url,
                    e
                );

            } catch (IOException e) {
                // ===== ERRO DE REDE/CONEXÃO =====
                // Pode ser timeout, servidor fora do ar, problema de rede, etc.
                // Estes erros são temporários, então vale a pena retentar
                
                logger.warn(
                    "Falha na tentativa {}/{}: {} - {}",
                    tentativaAtual, maxRetries,
                    e.getClass().getSimpleName(), e.getMessage()
                );

                // Se foi a última tentativa, não adianta mais retentar
                if (tentativaAtual >= maxRetries) {
                    logger.error(
                        "Todas as {} tentativas falharam para URL: {}. " +
                        "Possíveis causas: site fora do ar, problema de rede, firewall, timeout muito curto.",
                        maxRetries, url
                    );

                    throw new IllegalStateException(
                        String.format(
                            "Falha ao conectar após %d tentativas: %s",
                            maxRetries, e.getMessage()
                        ),
                        e
                    );
                }

                // ===== BACKOFF: AGUARDA ANTES DE RETENTAR =====
                // Espera 2 segundos antes da próxima tentativa
                // Isso dá tempo para problemas temporários se resolverem
                try {
                    logger.info(
                        "Aguardando {}ms antes da próxima tentativa...",
                        retryBackoff
                    );
                    Thread.sleep(retryBackoff);

                } catch (InterruptedException ie) {
                    // Se a thread for interrompida durante o sleep,
                    // restauramos o flag de interrupção e continuamos
                    Thread.currentThread().interrupt();
                    
                    logger.warn(
                        "Espera entre tentativas foi interrompida. " +
                        "Prosseguindo para próxima tentativa imediatamente."
                    );
                }
            }
        }

        // ===== FALLBACK DE SEGURANÇA =====
        // Este código só é alcançado se o loop terminar sem return nem throw
        // (não deveria acontecer, mas é uma proteção extra)

        logger.error(
            "Saída inesperada do loop de retry. " +
            "Isto indica um bug no código do fetcher."
        );

        throw new IllegalStateException(
            "Erro inesperado no fetch: loop terminou sem resultado"
        );
    }

    // ===== MÉTODOS AUXILIARES (GETTERS) =====
    // Úteis para testes e debugging
    
    /**
     * Retorna a URL configurada para este fetcher.
     * 
     * @return URL que será acessada pelo método {@link #fetch()}
     */
    public String getUrl() {
        return url;
    }

    /**
     * Retorna o timeout configurado, em milissegundos.
     * 
     * @return Timeout em ms usado nas requisições HTTP
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Retorna o número máximo de tentativas configurado.
     * 
     * @return Número de tentativas antes de desistir (atualmente 3)
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Retorna o tempo de espera entre tentativas, em milissegundos.
     * 
     * @return Backoff em ms entre tentativas de retry (atualmente 2000ms)
     */
    public int getRetryBackoff() {
        return retryBackoff;
    }

}

