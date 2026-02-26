package br.dev.marcus.praticagem.fetcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Testes unitários para a classe {@link HtmlFetcher}.
 * 
 * <p>Estes testes usam Mockito para simular o comportamento do Jsoup,
 * permitindo testar a lógica de retry sem fazer requisições HTTP reais.</p>
 * 
 * <h4>Cenários Testados</h4>
 * <ul>
 *   <li>Sucesso na primeira tentativa</li>
 *   <li>Falha seguida de sucesso (resiliência)</li>
 *   <li>Falha em todas as tentativas</li>
 *   <li>URL inválida (falha imediata)</li>
 *   <li>Timeout configurável</li>
 *   <li>Configurações de retry personalizadas</li>
 * </ul>
 * 
 * @author Marcus
 * @version 1.0
 * @since 2026-02-23
 */
class HtmlFetcherTest {

    private static final String URL_TESTE = "https://example.com";
    private static final int TIMEOUT_TESTE = 5000;
    private static final int MAX_RETRIES_TESTE = 3;
    private static final int BACKOFF_TESTE = 2000;

    /**
     * Testa o cenário ideal: requisição bem-sucedida na primeira tentativa.
     * 
     * <p>Verifica que:</p>
     * <ul>
     *   <li>O documento é retornado corretamente</li>
     *   <li>Apenas uma tentativa é feita</li>
     *   <li>Não há delays desnecessários</li>
     * </ul>
     */
    @Test
    @DisplayName("Deve buscar HTML com sucesso na primeira tentativa")
    void deveBuscarHtmlComSucesso() throws IOException {
        // Arrange: Preparar mock do documento
        Document docMock = mock(Document.class);
        when(docMock.title()).thenReturn("Página de Teste");

        HtmlFetcher fetcher = new HtmlFetcher(
            URL_TESTE, 
            TIMEOUT_TESTE, 
            MAX_RETRIES_TESTE, 
            BACKOFF_TESTE
        );

        // Act & Assert
        try (MockedStatic<org.jsoup.Jsoup> jsoupMock = mockStatic(org.jsoup.Jsoup.class)) {
            // Simula Jsoup.connect().timeout().userAgent().get()
            org.jsoup.Connection connectionMock = mock(org.jsoup.Connection.class);
            
            jsoupMock.when(() -> org.jsoup.Jsoup.connect(URL_TESTE))
                     .thenReturn(connectionMock);
            
            when(connectionMock.timeout(TIMEOUT_TESTE)).thenReturn(connectionMock);
            when(connectionMock.userAgent(anyString())).thenReturn(connectionMock);
            when(connectionMock.get()).thenReturn(docMock);

            // Executa
            Document resultado = fetcher.fetch();

            // Verifica
            assertNotNull(resultado);
            assertEquals("Página de Teste", resultado.title());
            
            // Verifica que Jsoup.connect foi chamado apenas 1 vez
            jsoupMock.verify(() -> org.jsoup.Jsoup.connect(URL_TESTE), times(1));
        }
    }

    /**
     * Testa resiliência: falha na primeira tentativa, sucesso na segunda.
     * 
     * <p>Simula cenário real de instabilidade de rede onde o retry resolve.</p>
     */
    @Test
    @DisplayName("Deve retentar após falha e ter sucesso na segunda tentativa")
    void deveRetentarAposFalha() throws IOException {
        // Arrange
        Document docMock = mock(Document.class);
        HtmlFetcher fetcher = new HtmlFetcher(
            URL_TESTE, 
            TIMEOUT_TESTE, 
            MAX_RETRIES_TESTE, 
            BACKOFF_TESTE
        );

        try (MockedStatic<org.jsoup.Jsoup> jsoupMock = mockStatic(org.jsoup.Jsoup.class)) {
            org.jsoup.Connection connectionMock = mock(org.jsoup.Connection.class);
            
            jsoupMock.when(() -> org.jsoup.Jsoup.connect(URL_TESTE))
                     .thenReturn(connectionMock);
            
            when(connectionMock.timeout(TIMEOUT_TESTE)).thenReturn(connectionMock);
            when(connectionMock.userAgent(anyString())).thenReturn(connectionMock);
            
            // Primeira chamada: falha com IOException
            // Segunda chamada: sucesso
            when(connectionMock.get())
                .thenThrow(new IOException("Timeout"))
                .thenReturn(docMock);

            // Act
            Document resultado = fetcher.fetch();

            // Assert
            assertNotNull(resultado);
            
            // Verifica que houve exatamente 2 tentativas
            jsoupMock.verify(() -> org.jsoup.Jsoup.connect(URL_TESTE), times(2));
        }
    }

    /**
     * Testa limite de retry: todas as tentativas falham.
     * 
     * <p>Verifica que:</p>
     * <ul>
     *   <li>Faz exatamente o número configurado de tentativas</li>
     *   <li>Lança IllegalStateException após última falha</li>
     *   <li>Mensagem de erro é descritiva</li>
     * </ul>
     */
    @Test
    @DisplayName("Deve falhar após todas as tentativas configuradas")
    void deveFalharAposTodasTentativas() throws IOException {
        // Arrange
        HtmlFetcher fetcher = new HtmlFetcher(
            URL_TESTE, 
            TIMEOUT_TESTE, 
            MAX_RETRIES_TESTE, 
            BACKOFF_TESTE
        );

        try (MockedStatic<org.jsoup.Jsoup> jsoupMock = mockStatic(org.jsoup.Jsoup.class)) {
            org.jsoup.Connection connectionMock = mock(org.jsoup.Connection.class);
            
            jsoupMock.when(() -> org.jsoup.Jsoup.connect(URL_TESTE))
                     .thenReturn(connectionMock);
            
            when(connectionMock.timeout(TIMEOUT_TESTE)).thenReturn(connectionMock);
            when(connectionMock.userAgent(anyString())).thenReturn(connectionMock);
            
            // Todas as tentativas falham
            when(connectionMock.get())
                .thenThrow(new IOException("Site fora do ar"));

            // Act & Assert
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> fetcher.fetch()
            );

            // Verifica mensagem de erro
            assertTrue(exception.getMessage().contains("Falha ao conectar após"));
            assertTrue(exception.getMessage().contains("tentativas"));

            // Verifica que tentou exatamente MAX_RETRIES_TESTE vezes
            jsoupMock.verify(
                () -> org.jsoup.Jsoup.connect(URL_TESTE), 
                times(MAX_RETRIES_TESTE)
            );
        }
    }

    /**
     * Testa tratamento de URL inválida.
     * 
     * <p>URL malformada deve falhar imediatamente sem retry, pois é erro
     * de configuração e não problema temporário de rede.</p>
     */
    @Test
    @DisplayName("Deve falhar imediatamente com URL inválida (sem retry)")
    void deveFalharImediatamenteComUrlInvalida() throws IOException {
        // Arrange
        String urlInvalida = "url-sem-protocolo";
        HtmlFetcher fetcher = new HtmlFetcher(
            urlInvalida, 
            TIMEOUT_TESTE, 
            MAX_RETRIES_TESTE, 
            BACKOFF_TESTE
        );

        try (MockedStatic<org.jsoup.Jsoup> jsoupMock = mockStatic(org.jsoup.Jsoup.class)) {
            org.jsoup.Connection connectionMock = mock(org.jsoup.Connection.class);
            
            jsoupMock.when(() -> org.jsoup.Jsoup.connect(urlInvalida))
                     .thenReturn(connectionMock);
            
            when(connectionMock.timeout(TIMEOUT_TESTE)).thenReturn(connectionMock);
            when(connectionMock.userAgent(anyString())).thenReturn(connectionMock);
            
            // Jsoup lança IllegalArgumentException para URL inválida
            when(connectionMock.get())
                .thenThrow(new IllegalArgumentException("Malformed URL"));

            // Act & Assert
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> fetcher.fetch()
            );

            // Verifica que não fez retry (apenas 1 tentativa)
            jsoupMock.verify(() -> org.jsoup.Jsoup.connect(urlInvalida), times(1));
            
            // Verifica mensagem indica problema de configuração
            assertTrue(exception.getMessage().contains("inválida"));
        }
    }

    /**
     * Testa que timeout configurado é passado corretamente para o Jsoup.
     */
    @Test
    @DisplayName("Deve usar timeout configurado corretamente")
    void deveUsarTimeoutConfigurado() throws IOException {
        // Arrange
        int timeoutCustomizado = 15000; // 15 segundos
        Document docMock = mock(Document.class);
        HtmlFetcher fetcher = new HtmlFetcher(
            URL_TESTE, 
            timeoutCustomizado, 
            MAX_RETRIES_TESTE, 
            BACKOFF_TESTE
        );

        try (MockedStatic<org.jsoup.Jsoup> jsoupMock = mockStatic(org.jsoup.Jsoup.class)) {
            org.jsoup.Connection connectionMock = mock(org.jsoup.Connection.class);
            
            jsoupMock.when(() -> org.jsoup.Jsoup.connect(URL_TESTE))
                     .thenReturn(connectionMock);
            
            when(connectionMock.timeout(timeoutCustomizado)).thenReturn(connectionMock);
            when(connectionMock.userAgent(anyString())).thenReturn(connectionMock);
            when(connectionMock.get()).thenReturn(docMock);

            // Act
            fetcher.fetch();

            // Assert: Verifica que timeout correto foi usado
            verify(connectionMock).timeout(timeoutCustomizado);
        }
    }

    /**
     * Testa configurações personalizadas de retry.
     * 
     * <p>Verifica que maxRetries e retryBackoff configurados são
     * respeitados durante a execução.</p>
     */
    @Test
    @DisplayName("Deve respeitar configurações personalizadas de retry")
    void deveRespeitarConfiguracoesPersonalizadas() throws IOException {
        // Arrange
        int maxRetriesPersonalizado = 5;
        int backoffPersonalizado = 1000; // 1 segundo
        
        HtmlFetcher fetcher = new HtmlFetcher(
            URL_TESTE,
            TIMEOUT_TESTE,
            maxRetriesPersonalizado,
            backoffPersonalizado
        );

        try (MockedStatic<org.jsoup.Jsoup> jsoupMock = mockStatic(org.jsoup.Jsoup.class)) {
            org.jsoup.Connection connectionMock = mock(org.jsoup.Connection.class);
            
            jsoupMock.when(() -> org.jsoup.Jsoup.connect(URL_TESTE))
                     .thenReturn(connectionMock);
            
            when(connectionMock.timeout(TIMEOUT_TESTE)).thenReturn(connectionMock);
            when(connectionMock.userAgent(anyString())).thenReturn(connectionMock);
            
            // Todas as tentativas falham
            when(connectionMock.get())
                .thenThrow(new IOException("Falha de rede"));

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> fetcher.fetch());

            // Verifica que tentou exatamente maxRetriesPersonalizado vezes
            jsoupMock.verify(
                () -> org.jsoup.Jsoup.connect(URL_TESTE), 
                times(maxRetriesPersonalizado)
            );
        }
    }

    /**
     * Testa os getters da classe.
     * 
     * <p>Verifica que todos os valores configurados no construtor
     * são retornados corretamente pelos métodos getter.</p>
     */
    @Test
    @DisplayName("Getters devem retornar valores configurados")
    void gettersDevemRetornarValoresCorretos() {
        // Arrange & Act
        HtmlFetcher fetcher = new HtmlFetcher(
            URL_TESTE, 
            TIMEOUT_TESTE, 
            MAX_RETRIES_TESTE, 
            BACKOFF_TESTE
        );

        // Assert
        assertEquals(URL_TESTE, fetcher.getUrl());
        assertEquals(TIMEOUT_TESTE, fetcher.getTimeout());
        assertEquals(MAX_RETRIES_TESTE, fetcher.getMaxRetries());
        assertEquals(BACKOFF_TESTE, fetcher.getRetryBackoff());
    }

    /**
     * Testa que o construtor aceita valores mínimos válidos.
     */
    @Test
    @DisplayName("Deve aceitar valores mínimos de configuração")
    void deveAceitarValoresMinimos() {
        // Arrange & Act
        HtmlFetcher fetcher = new HtmlFetcher(
            URL_TESTE,
            1000,    // 1 segundo de timeout
            1,       // 1 tentativa apenas
            0        // Sem backoff
        );

        // Assert
        assertNotNull(fetcher);
        assertEquals(1, fetcher.getMaxRetries());
        assertEquals(0, fetcher.getRetryBackoff());
    }

    /**
     * Testa que o User-Agent customizado é usado nas requisições.
     */
    @Test
    @DisplayName("Deve usar User-Agent customizado")
    void deveUsarUserAgentCustomizado() throws IOException {
        // Arrange
        Document docMock = mock(Document.class);
        HtmlFetcher fetcher = new HtmlFetcher(
            URL_TESTE, 
            TIMEOUT_TESTE, 
            MAX_RETRIES_TESTE, 
            BACKOFF_TESTE
        );

        try (MockedStatic<org.jsoup.Jsoup> jsoupMock = mockStatic(org.jsoup.Jsoup.class)) {
            org.jsoup.Connection connectionMock = mock(org.jsoup.Connection.class);
            
            jsoupMock.when(() -> org.jsoup.Jsoup.connect(URL_TESTE))
                     .thenReturn(connectionMock);
            
            when(connectionMock.timeout(TIMEOUT_TESTE)).thenReturn(connectionMock);
            when(connectionMock.userAgent(anyString())).thenReturn(connectionMock);
            when(connectionMock.get()).thenReturn(docMock);

            // Act
            fetcher.fetch();

            // Assert: Verifica que algum User-Agent foi configurado
            verify(connectionMock).userAgent(argThat(ua -> 
                ua != null && !ua.isEmpty()
            ));
        }
    }
}