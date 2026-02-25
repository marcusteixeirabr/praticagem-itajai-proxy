package br.dev.marcus.praticagem;

import io.javalin.Javalin;

import br.dev.marcus.praticagem.config.ConfigLoader;
import br.dev.marcus.praticagem.fetcher.HtmlFetcher;
import br.dev.marcus.praticagem.parser.HtmlParser;
import br.dev.marcus.praticagem.service.MovimentacaoService;
import br.dev.marcus.praticagem.model.NavioMovimentacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Ponto de entrada da aplicação de scraping de movimentação de navios.
 * 
 * <p>Esta aplicação expõe uma API REST simples usando {@link Javalin} para
 * consultar dados de movimentação de navios do Porto de Itajaí-SC, coletados
 * do site da praticagem (https://praticoszp21.com.br).</p>
 * 
 * <h2>Arquitetura da Aplicação</h2>
 * <p>A aplicação segue uma arquitetura em camadas simples:</p>
 * <pre>
 * ┌─────────────────────────────────────────┐
 * │         API REST (Javalin)              │  ← Camada de apresentação
 * ├─────────────────────────────────────────┤
 * │    MovimentacaoService                  │  ← Camada de negócio
 * ├─────────────────────────────────────────┤
 * │  HtmlFetcher  │  HtmlParser             │  ← Camada de dados
 * ├─────────────────────────────────────────┤
 * │         ConfigLoader                    │  ← Camada de configuração
 * └─────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>Endpoints Disponíveis</h2>
 * <table border="1">
 *   <tr>
 *     <th>Método</th>
 *     <th>Endpoint</th>
 *     <th>Descrição</th>
 *     <th>Resposta</th>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/movimentacoes</td>
 *     <td>Lista todas as movimentações de navios</td>
 *     <td>JSON array com objetos NavioMovimentacao</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/health</td>
 *     <td>Health check da aplicação</td>
 *     <td>Texto "OK" com status 200</td>
 *   </tr>
 * </table>
 * 
 * <h2>Exemplo de Resposta - /movimentacoes</h2>
 * <pre>{@code
 * [
 *   {
 *     "data": "23/02/2026",
 *     "horario": "08:00",
 *     "manobra": "Atracação",
 *     "berco": "201",
 *     "navio": "MSC MARINA",
 *     "situacao": "Confirmado"
 *   }
 * ]
 * }</pre>
 * 
 * <h2>Sistema de Configuração em Cascata</h2>
 * <p>A aplicação usa {@link ConfigLoader} que busca configurações em ordem de prioridade:</p>
 * <ol>
 *   <li><b>Variáveis de ambiente</b> (mais alta - produção)</li>
 *   <li><b>System properties</b> (-D flags)</li>
 *   <li><b>application.properties</b> (padrão - desenvolvimento)</li>
 * </ol>
 * 
 * <h3>Configurações Disponíveis</h3>
 * <table border="1">
 *   <tr>
 *     <th>Propriedade</th>
 *     <th>Env Var</th>
 *     <th>Padrão</th>
 *     <th>Descrição</th>
 *   </tr>
 *   <tr>
 *     <td>praticagem.url</td>
 *     <td>PRATICAGEM_URL</td>
 *     <td>https://praticoszp21.com.br/...</td>
 *     <td>URL do site de praticagem</td>
 *   </tr>
 *   <tr>
 *     <td>praticagem.timeout</td>
 *     <td>PRATICAGEM_TIMEOUT</td>
 *     <td>10000</td>
 *     <td>Timeout HTTP em milissegundos</td>
 *   </tr>
 *   <tr>
 *     <td>server.port</td>
 *     <td>SERVER_PORT</td>
 *     <td>7000</td>
 *     <td>Porta do servidor HTTP</td>
 *   </tr>
 * </table>
 * 
 * <h2>Como Executar</h2>
 * <pre>{@code
 * # Desenvolvimento (usa application.properties)
 * ./gradlew run
 * 
 * # Produção (sobrescreve com env vars)
 * export PRATICAGEM_URL=https://outro-site.com
 * export SERVER_PORT=8080
 * java -jar praticagem-api.jar
 * 
 * # Ou com arquivo .env
 * source .env
 * java -jar praticagem-api.jar
 * }</pre>
 * 
 * @author Marcus
 * @version 1.0
 * @since 2026-02-23
 * 
 * @see ConfigLoader
 * @see HtmlFetcher
 * @see HtmlParser
 * @see MovimentacaoService
 */
public class Main {

    /**
     * Logger para registrar eventos da aplicação.
     */
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Ponto de entrada da aplicação.
     * 
     * <p>Fluxo de inicialização:</p>
     * <ol>
     *   <li>Carrega configurações via {@link ConfigLoader}</li>
     *   <li>Inicializa componentes (fetcher, parser, service)</li>
     *   <li>Configura e inicia servidor Javalin</li>
     *   <li>Registra endpoints e handlers de erro</li>
     * </ol>
     * 
     * @param args Argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        
        logger.info("=== Iniciando aplicação de Movimentação de Navios ===");
        
        // ===== CARREGAMENTO DE CONFIGURAÇÕES =====
        // ConfigLoader busca em: env vars → system props → application.properties
        logger.info("Carregando configurações...");
        ConfigLoader config = new ConfigLoader();

        // Extrai configurações com valores de fallback
        String url = config.get(
            "praticagem.url",
            "https://praticoszp21.com.br/movimentacao-de-navios/"
        );

        int timeout = config.getInt("praticagem.timeout", 10000);
        
        int porta = config.getInt("server.port", 7000);

        // Log das configurações carregadas (útil para debug)
        logger.info("Configurações carregadas:");
        logger.info("  └─ URL: {}", url);
        logger.info("  └─ Timeout: {}ms", timeout);
        logger.info("  └─ Porta: {}", porta);

        // ===== INICIALIZAÇÃO DE COMPONENTES =====
        // Padrão de injeção de dependências manual (simples e explícito)
        logger.info("Inicializando componentes...");
        
        HtmlFetcher fetcher = new HtmlFetcher(url, timeout);
        logger.debug("  ✓ HtmlFetcher criado");
        
        HtmlParser parser = new HtmlParser();
        logger.debug("  ✓ HtmlParser criado");
        
        MovimentacaoService service = new MovimentacaoService(fetcher, parser);
        logger.debug("  ✓ MovimentacaoService criado");

        // ===== CONFIGURAÇÃO DO SERVIDOR JAVALIN =====
        logger.info("Configurando servidor Javalin...");
        
        Javalin app = Javalin.create(javalinConfig -> {
            // Desabilita banner do Javalin (mantém logs limpos)
            javalinConfig.showJavalinBanner = false;
            
            // Habilita logs de requisições HTTP (útil para monitoramento)
            javalinConfig.plugins.enableDevLogging();
            
            // Configurações adicionais podem ser adicionadas aqui:
            // javalinConfig.plugins.enableCors(...);
            // javalinConfig.staticFiles.add(...);
        }).start(porta);

        logger.info("✅ Servidor iniciado com sucesso em http://localhost:{}", porta);

        // ===== TRATAMENTO GLOBAL DE EXCEÇÕES =====
        // Captura IllegalStateException (erros de scraping) e retorna JSON
        app.exception(IllegalStateException.class, (exception, ctx) -> {
            logger.error(
                "Erro ao processar requisição {} - {}",
                ctx.path(), exception.getMessage(),
                exception
            );
            
            ctx.status(500);
            ctx.json(Map.of(
                "erro", "Falha ao obter dados da praticagem",
                "mensagem", exception.getMessage(),
                "timestamp", System.currentTimeMillis(),
                "path", ctx.path()
            ));
        });

        // ===== ENDPOINT: /movimentacoes =====
        /**
         * GET /movimentacoes
         * 
         * <p>Retorna lista de movimentações de navios obtidas via web scraping.</p>
         * 
         * <h3>Fluxo:</h3>
         * <ol>
         *   <li>Service → Fetcher (busca HTML com retry)</li>
         *   <li>Service → Parser (extrai dados da tabela)</li>
         *   <li>Javalin serializa para JSON</li>
         * </ol>
         * 
         * <h3>Respostas:</h3>
         * <ul>
         *   <li><b>200 OK:</b> JSON array de movimentações</li>
         *   <li><b>500 Error:</b> Falha no scraping</li>
         * </ul>
         */
        app.get("/movimentacoes", ctx -> {
            logger.info("Requisição recebida: GET /movimentacoes");
            
            // Service coordena fetcher e parser
            List<NavioMovimentacao> dados = service.buscarMovimentacoes();
            
            logger.info(
                "Respondendo com {} movimentações encontradas",
                dados.size()
            );
            
            // Javalin serializa automaticamente para JSON
            ctx.json(dados);
        });

        // ===== ENDPOINT: /health =====
        /**
         * GET /health
         * 
         * <p>Health check para monitoramento (Kubernetes, load balancers, etc).</p>
         * 
         * <h3>Resposta:</h3>
         * <ul>
         *   <li><b>200 OK:</b> Aplicação está rodando</li>
         * </ul>
         */
        app.get("/health", ctx -> {
            logger.debug("Health check recebido");
            ctx.result("OK");
        });

        // ===== LOG DE ENDPOINTS DISPONÍVEIS =====
        logger.info("=== Aplicação pronta para receber requisições ===");
        logger.info("Endpoints disponíveis:");
        logger.info("  └─ GET http://localhost:{}/movimentacoes - Lista movimentações", porta);
        logger.info("  └─ GET http://localhost:{}/health - Health check", porta);
        logger.info("====================================================");
        
        // ===== SHUTDOWN HOOK =====
        // Garante encerramento gracioso ao pressionar Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Sinal de encerramento recebido (Ctrl+C)");
            logger.info("Parando servidor...");
            app.stop();
            logger.info("✓ Aplicação encerrada com sucesso");
        }));
    }
}