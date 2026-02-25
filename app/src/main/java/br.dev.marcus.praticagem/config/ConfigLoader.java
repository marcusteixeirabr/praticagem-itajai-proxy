package br.dev.marcus.praticagem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Carregador centralizado de configurações da aplicação.
 * 
 * <p>Esta classe implementa um sistema de configuração em cascata que busca
 * valores na seguinte ordem de prioridade (do maior para o menor):</p>
 * <ol>
 *   <li><b>Variáveis de ambiente</b> - Maior prioridade (deployment/produção)</li>
 *   <li><b>Propriedades do sistema</b> - Java -D flags</li>
 *   <li><b>Arquivo application.properties</b> - Valores padrão (desenvolvimento)</li>
 * </ol>
 * 
 * <h2>Por que esta abordagem?</h2>
 * <ul>
 *   <li><b>Desenvolvimento:</b> Usa valores padrão do .properties (sem configurar nada)</li>
 *   <li><b>CI/CD:</b> Sobrescreve via variáveis de ambiente</li>
 *   <li><b>Segredos:</b> Nunca vão para o Git (apenas via env vars)</li>
 *   <li><b>Documentação:</b> Arquivo .properties documenta todas as opções</li>
 * </ul>
 * 
 * <h2>Exemplo de Uso</h2>
 * <pre>{@code
 * // Criar loader (carrega application.properties automaticamente)
 * ConfigLoader config = new ConfigLoader();
 * 
 * // Buscar valores (com fallback para variáveis de ambiente)
 * String url = config.get("praticagem.url");
 * int timeout = config.getInt("praticagem.timeout", 10000);
 * int port = config.getInt("server.port", 7000);
 * 
 * // Em produção, sobrescrever via env var:
 * // export PRATICAGEM_URL=https://outro-site.com
 * // export SERVER_PORT=8080
 * }</pre>
 * 
 * <h2>Convenção de Nomes</h2>
 * <p>Para mapear entre properties e env vars, usamos esta conversão:</p>
 * <ul>
 *   <li>Properties: {@code praticagem.url} → Env var: {@code PRATICAGEM_URL}</li>
 *   <li>Properties: {@code server.port} → Env var: {@code SERVER_PORT}</li>
 *   <li>Regra: pontos viram underscores, tudo em maiúsculas</li>
 * </ul>
 * 
 * @author Marcus
 * @version 1.0
 * @since 2026-02-23
 */
public class ConfigLoader {

    /**
     * Logger para registrar eventos da aplicação.
     */
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    
    /**
     * Nome do arquivo de propriedades padrão.
     * Deve estar em src/main/resources/
     */
    private static final String PROPERTIES_FILE = "application.properties";
    
    /**
     * Propriedades carregadas do arquivo.
     * Serve como fallback quando env vars não estão definidas.
     */
    private final Properties properties;

    /**
     * Constrói o loader e carrega o arquivo de propriedades.
     * 
     * <p>Se o arquivo não existir ou houver erro ao ler, continua com
     * properties vazio (usa apenas variáveis de ambiente).</p>
     */
    public ConfigLoader() {
        this.properties = new Properties();
        carregarPropertiesFile();
    }

    /**
     * Carrega o arquivo application.properties do classpath.
     * 
     * <p>Procura o arquivo em src/main/resources/ (em desenvolvimento)
     * ou dentro do JAR (em produção).</p>
     */
    private void carregarPropertiesFile() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {
            
            if (input == null) {
                logger.warn(
                    "Arquivo {} não encontrado no classpath. " +
                    "Usando apenas variáveis de ambiente.",
                    PROPERTIES_FILE
                );
                return;
            }

            properties.load(input);
            logger.info(
                "Arquivo {} carregado com sucesso ({} propriedades)",
                PROPERTIES_FILE, properties.size()
            );
            
        } catch (IOException e) {
            logger.error("Erro ao carregar {}", PROPERTIES_FILE, e);
        }
    }

    /**
     * Busca valor de configuração com cascata de prioridades.
     * 
     * <h3>Ordem de busca:</h3>
     * <ol>
     *   <li>Variável de ambiente (ex: PRATICAGEM_URL)</li>
     *   <li>System property (ex: -Dpraticagem.url=...)</li>
     *   <li>Arquivo application.properties</li>
     *   <li>Valor padrão fornecido</li>
     * </ol>
     * 
     * @param key Chave da propriedade (ex: "praticagem.url")
     * @param defaultValue Valor usado se nenhuma fonte tiver a propriedade
     * @return Valor encontrado ou defaultValue
     */
    public String get(String key, String defaultValue) {
        // 1. Tenta variável de ambiente (PRATICAGEM_URL)
        String envKey = toEnvVarName(key);
        String envValue = System.getenv(envKey);
        if (envValue != null) {
            logger.debug("Config '{}' obtida de env var {}: {}", key, envKey, envValue);
            return envValue;
        }

        // 2. Tenta system property (-Dpraticagem.url=...)
        String sysProp = System.getProperty(key);
        if (sysProp != null) {
            logger.debug("Config '{}' obtida de system property: {}", key, sysProp);
            return sysProp;
        }

        // 3. Tenta arquivo properties
        String propValue = properties.getProperty(key);
        if (propValue != null) {
            logger.debug("Config '{}' obtida de {}: {}", key, PROPERTIES_FILE, propValue);
            return propValue;
        }

        // 4. Fallback para valor padrão
        logger.debug("Config '{}' usando valor padrão: {}", key, defaultValue);
        return defaultValue;
    }

    /**
     * Busca valor de configuração sem valor padrão.
     * 
     * @param key Chave da propriedade
     * @return Valor encontrado ou null
     */
    public String get(String key) {
        return get(key, null);
    }

    /**
     * Busca valor inteiro com valor padrão.
     * 
     * @param key Chave da propriedade
     * @param defaultValue Valor padrão se não encontrar ou não for número válido
     * @return Valor inteiro parseado ou defaultValue
     */
    public int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn(
                "Valor inválido para '{}': '{}'. Usando padrão: {}",
                key, value, defaultValue
            );
            return defaultValue;
        }
    }

    /**
     * Busca valor booleano com valor padrão.
     * 
     * @param key Chave da propriedade
     * @param defaultValue Valor padrão se não encontrar
     * @return true/false parseado ou defaultValue
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }

    /**
     * Converte nome de propriedade em nome de variável de ambiente.
     * 
     * <p>Regras de conversão:</p>
     * <ul>
     *   <li>Converte para maiúsculas</li>
     *   <li>Substitui pontos (.) por underscores (_)</li>
     *   <li>Substitui hífens (-) por underscores (_)</li>
     * </ul>
     * 
     * <p>Exemplos:</p>
     * <ul>
     *   <li>"praticagem.url" → "PRATICAGEM_URL"</li>
     *   <li>"server.port" → "SERVER_PORT"</li>
     *   <li>"retry.max-tentativas" → "RETRY_MAX_TENTATIVAS"</li>
     * </ul>
     * 
     * @param propertyKey Nome da propriedade
     * @return Nome da variável de ambiente equivalente
     */
    private String toEnvVarName(String propertyKey) {
        return propertyKey
            .toUpperCase()
            .replace('.', '_')
            .replace('-', '_');
    }

    /**
     * Lista todas as configurações carregadas (útil para debug).
     * 
     * @return Cópia das propriedades carregadas do arquivo
     */
    public Properties getAllProperties() {
        return (Properties) properties.clone();
    }
}