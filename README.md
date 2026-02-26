# 🚢 API de Movimentação de Navios - Porto de Itajaí

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Javalin](https://img.shields.io/badge/Javalin-6.x-blue.svg)](https://javalin.io/)
[![Jsoup](https://img.shields.io/badge/Jsoup-1.17-green.svg)](https://jsoup.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

API REST para consulta de movimentações de navios no Porto de Itajaí-SC através de web scraping do site da praticagem.

---

## 📋 Índice

- [Sobre o Projeto](#-sobre-o-projeto)
- [Funcionalidades](#-funcionalidades)
- [Tecnologias](#-tecnologias)
- [Arquitetura](#-arquitetura)
- [Pré-requisitos](#-pré-requisitos)
- [Instalação](#-instalação)
- [Configuração](#-configuração)
- [Como Usar](#-como-usar)
- [Endpoints da API](#-endpoints-da-api)
- [Exemplos de Resposta](#-exemplos-de-resposta)
- [Documentação JavaDoc](#-documentação-javadoc)
- [Estratégias de Resiliência](#-estratégias-de-resiliência)
- [Deploy em Produção](#-deploy-em-produção)
- [Testes](#-testes)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Contribuindo](#-contribuindo)
- [Licença](#-licença)
- [Autor](#-autor)

---

## 🎯 Sobre o Projeto

Esta aplicação foi desenvolvida para automatizar a coleta de dados de movimentação de navios do Porto de Itajaí-SC, disponibilizados no site da praticagem (https://praticoszp21.com.br/movimentacao-de-navios/).

A API realiza web scraping resiliente e expõe os dados em formato JSON, facilitando a integração com outras aplicações e dashboards.

### 🌟 Destaques

- ✅ **Web Scraping Resiliente**: Tolera mudanças menores na estrutura do HTML
- ✅ **Retry Automático**: Até 3 tentativas com backoff configurável
- ✅ **Configuração Flexível**: Suporta variáveis de ambiente e arquivo properties
- ✅ **Leve e Rápido**: Usa Javalin (não Spring Boot) para menor consumo de recursos
- ✅ **Documentação Completa**: JavaDoc profissional em todas as classes
- ✅ **Thread-Safe**: Suporta múltiplas requisições simultâneas

---

## 🚀 Funcionalidades

- 📊 **Consulta de Movimentações**: Lista todas as movimentações programadas de navios
- 🔄 **Health Check**: Endpoint para monitoramento de disponibilidade
- 🛡️ **Tratamento de Erros**: Respostas JSON estruturadas mesmo em caso de falha
- ⚙️ **Configuração Dinâmica**: Ajuste timeout, retries e URLs sem recompilar
- 📝 **Logging Estruturado**: Logs informativos usando SLF4J/Logback

---

## 🛠️ Tecnologias

| Tecnologia | Versão | Finalidade |
|------------|--------|------------|
| Java | 21 | Linguagem de programação |
| Gradle | 8.7 | Gerenciador de dependências e build |
| Javalin | 6.x | Framework web leve para API REST |
| Jsoup | 1.17.2 | Parser HTML para web scraping |
| Jackson | 2.x | Serialização JSON |
| SLF4J/Logback | 2.x | Sistema de logs |

---

## 🏗️ Arquitetura

A aplicação segue uma arquitetura em camadas simples e clara:

```
┌─────────────────────────────────────────┐
│      API REST (Javalin/Main)            │  ← Camada de Apresentação
├─────────────────────────────────────────┤
│    MovimentacaoService                  │  ← Camada de Negócio
├─────────────────────────────────────────┤
│  HtmlFetcher  │  HtmlParser             │  ← Camada de Dados
├─────────────────────────────────────────┤
│         ConfigLoader                    │  ← Camada de Configuração
└─────────────────────────────────────────┘
```

### Componentes Principais

- **Main**: Ponto de entrada, configura Javalin e rotas
- **MovimentacaoService**: Orquestra fetcher e parser
- **HtmlFetcher**: Busca HTML com retry automático
- **HtmlParser**: Extrai dados da tabela de forma resiliente
- **ConfigLoader**: Gerencia configurações em cascata
- **NavioMovimentacao**: Model/DTO imutável (Java Record)

---

## 📦 Pré-requisitos

- **Java 21** ou superior ([Download](https://adoptium.net/))
- **Gradle 8.7** ou superior (ou use o Gradle Wrapper incluído)
- Conexão com internet (para acessar o site da praticagem)

### Verificar instalação:

```bash
java -version
# Deve mostrar: java version "21.x.x"

./gradlew -v
# Deve mostrar: Gradle 8.7
```

---

## ⚡ Instalação

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/praticagem-api.git
cd praticagem-api
```

### 2. Compile o projeto

```bash
./gradlew build
```

### 3. Execute a aplicação

```bash
# Opção 1: Usando Gradle
./gradlew run

# Opção 2: Usando JAR gerado
java -jar build/libs/praticagem-api.jar
```

### 4. Verifique se está funcionando

```bash
curl http://localhost:7000/health
# Resposta esperada: OK
```

---

## ⚙️ Configuração

A aplicação usa um sistema de configuração em **cascata** com a seguinte ordem de prioridade:

1. **Variáveis de Ambiente** (mais alta - produção)
2. **System Properties** (-D flags)
3. **application.properties** (fallback - desenvolvimento)

### Arquivo application.properties

Localizado em `src/main/resources/application.properties`:

```properties
# URL do site de praticagem
praticagem.url=https://praticoszp21.com.br/movimentacao-de-navios/

# Timeout HTTP em milissegundos (10 segundos)
praticagem.timeout=10000

# Número máximo de tentativas em caso de falha
praticagem.max.retries=3

# Tempo de espera entre tentativas em milissegundos (2 segundos)
praticagem.retry.backoff=2000

# Porta do servidor HTTP
server.port=7000
```

### Variáveis de Ambiente (Produção)

```bash
# Definir variáveis de ambiente
export PRATICAGEM_URL=https://praticoszp21.com.br/movimentacao-de-navios/
export PRATICAGEM_TIMEOUT=15000
export PRATICAGEM_MAX_RETRIES=5
export PRATICAGEM_RETRY_BACKOFF=3000
export SERVER_PORT=8080

# Executar aplicação
java -jar praticagem-api.jar
```

### Arquivo .env (Recomendado para produção)

Crie um arquivo `.env` na mesma pasta do JAR:

```bash
# .env
export PRATICAGEM_URL=https://praticoszp21.com.br/movimentacao-de-navios/
export PRATICAGEM_TIMEOUT=15000
export PRATICAGEM_MAX_RETRIES=5
export PRATICAGEM_RETRY_BACKOFF=3000
export SERVER_PORT=80
```

Carregue e execute:

```bash
source .env
java -jar praticagem-api.jar
```

### Tabela de Configurações

| Propriedade | Env Var | Padrão | Descrição |
|-------------|---------|--------|-----------|
| `praticagem.url` | `PRATICAGEM_URL` | https://praticoszp21... | URL do site |
| `praticagem.timeout` | `PRATICAGEM_TIMEOUT` | 10000 | Timeout HTTP (ms) |
| `praticagem.max.retries` | `PRATICAGEM_MAX_RETRIES` | 3 | Máx. de tentativas |
| `praticagem.retry.backoff` | `PRATICAGEM_RETRY_BACKOFF` | 2000 | Espera entre tentativas (ms) |
| `server.port` | `SERVER_PORT` | 7000 | Porta do servidor |

---

## 📖 Como Usar

### Desenvolvimento Local

```bash
# Inicie a aplicação
./gradlew run

# Em outro terminal, faça requisições
curl http://localhost:7000/movimentacoes
```

### Produção (VPS/Cloud)

```bash
# 1. Compile o projeto
./gradlew build

# 2. Copie o JAR para o servidor
scp build/libs/praticagem-api.jar usuario@servidor:/opt/praticagem/

# 3. No servidor, configure as variáveis
nano /opt/praticagem/.env

# 4. Execute
cd /opt/praticagem
source .env
java -jar praticagem-api.jar
```

---

## 🌐 Endpoints da API

### GET /movimentacoes

Retorna lista de todas as movimentações de navios.

**Resposta de Sucesso (200 OK):**

```json
[
  {
    "data": "23/02/2026",
    "horario": "08:00",
    "manobra": "Atracação",
    "berco": "201",
    "navio": "MSC MARINA",
    "situacao": "Confirmado"
  },
  {
    "data": "23/02/2026",
    "horario": "14:30",
    "manobra": "Desatracação",
    "berco": "102",
    "navio": "EVER GIVEN",
    "situacao": "Em andamento"
  }
]
```

**Resposta de Erro (500 Internal Server Error):**

```json
{
  "erro": "Falha ao obter dados da praticagem",
  "mensagem": "Falha ao conectar após 3 tentativas: timeout",
  "timestamp": 1708704000000,
  "path": "/movimentacoes"
}
```

### GET /health

Health check para monitoramento.

**Resposta (200 OK):**

```
OK
```

---

## 📚 Exemplos de Resposta

### Exemplo 1: Buscar movimentações

```bash
curl http://localhost:7000/movimentacoes
```

**Resposta:**

```json
[
  {
    "data": "23/02/2026",
    "horario": "08:00",
    "manobra": "Atracação",
    "berco": "201",
    "navio": "MSC MARINA",
    "situacao": "Confirmado"
  }
]
```

### Exemplo 2: Formatar JSON com jq

```bash
curl http://localhost:7000/movimentacoes | jq
```

### Exemplo 3: Salvar resposta em arquivo

```bash
curl http://localhost:7000/movimentacoes > movimentacoes.json
```

### Exemplo 4: Health check

```bash
curl http://localhost:7000/health
# Resposta: OK
```

---

## 📘 Documentação JavaDoc

O projeto possui documentação JavaDoc completa e profissional.

### Gerar documentação HTML:

```bash
./gradlew javadoc
```

### Visualizar documentação:

```bash
# Linux/Mac
open build/docs/javadoc/index.html

# Windows
start build/docs/javadoc/index.html
```

A documentação gerada incluirá:

- Descrição completa de cada classe
- Diagramas ASCII de arquitetura
- Exemplos de uso
- Análise de performance
- Decisões de design explicadas

---

## 🛡️ Estratégias de Resiliência

A aplicação implementa várias estratégias para lidar com falhas:

### 1. Retry Automático (HtmlFetcher)

- **3 tentativas** antes de desistir (configurável)
- **Backoff de 2 segundos** entre tentativas (configurável)
- **Timeout de 10 segundos** por tentativa (configurável)

### 2. Parsing Resiliente (HtmlParser)

- **Seleção dinâmica de colunas**: Não assume posição fixa
- **Normalização de texto**: Remove acentos e unifica case
- **Busca por palavra-chave**: Tolera variações nos nomes das colunas
- **Validação de estrutura**: Falha explicitamente se colunas essenciais não existem

### 3. Tratamento de Erros

- **Erros de conexão**: Retorna HTTP 500 com JSON explicativo
- **Estrutura mudou**: Retorna HTTP 500 indicando necessidade de atualização
- **Timeout**: Retorna HTTP 500 após retries esgotados

### 4. Logging

- **INFO**: Inicialização, tentativas de conexão, requisições
- **WARN**: Falhas temporárias, retries
- **ERROR**: Erros críticos, estrutura mudou, falha total

---

## 🚀 Deploy em Produção

### Opção 1: Systemd Service (Ubuntu/Debian)

Crie o arquivo de serviço:

```bash
sudo nano /etc/systemd/system/praticagem.service
```

Conteúdo:

```ini
[Unit]
Description=API de Movimentação de Navios - Porto de Itajaí
After=network.target

[Service]
Type=simple
User=seu-usuario
WorkingDirectory=/opt/praticagem

# Variáveis de ambiente
Environment="PRATICAGEM_URL=https://praticoszp21.com.br/movimentacao-de-navios/"
Environment="SERVER_PORT=80"
Environment="PRATICAGEM_TIMEOUT=15000"
Environment="PRATICAGEM_MAX_RETRIES=5"
Environment="PRATICAGEM_RETRY_BACKOFF=3000"

# Comando para executar
ExecStart=/usr/bin/java -jar /opt/praticagem/praticagem-api.jar

# Reinicia automaticamente se cair
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Ative o serviço:

```bash
# Recarrega configurações
sudo systemctl daemon-reload

# Inicia o serviço
sudo systemctl start praticagem

# Verifica status
sudo systemctl status praticagem

# Habilita início automático no boot
sudo systemctl enable praticagem

# Ver logs em tempo real
sudo journalctl -u praticagem -f
```

### Opção 2: Executar em Background (nohup)

```bash
nohup java -jar praticagem-api.jar > praticagem.log 2>&1 &
```

### Opção 3: Script de Início

Crie `start.sh`:

```bash
#!/bin/bash

# Carrega variáveis de ambiente
source /opt/praticagem/.env

# Inicia aplicação em background
nohup java -jar /opt/praticagem/praticagem-api.jar \
  > /opt/praticagem/logs/app.log 2>&1 &

# Salva PID
echo $! > /opt/praticagem/praticagem.pid

echo "Aplicação iniciada. PID: $(cat /opt/praticagem/praticagem.pid)"
```

Torne executável:

```bash
chmod +x start.sh
./start.sh
```

---

## 🧪 Testes

### Executar todos os testes:

```bash
./gradlew test
```

### Ver relatório HTML dos testes:

```bash
./gradlew test
open build/reports/tests/test/index.html
```

### Estrutura de Testes

```
src/test/java/
└── br/dev/marcus/praticagem/
    ├── fetcher/
    │   └── HtmlFetcherTest.java
    ├── parser/
    │   └── HtmlParserTest.java
    └── service/
        └── MovimentacaoServiceTest.java
```

---

## 📁 Estrutura do Projeto

```
praticagem-api/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── br/dev/marcus/praticagem/
│   │   │       ├── Main.java                    # Ponto de entrada
│   │   │       ├── config/
│   │   │       │   └── ConfigLoader.java        # Gerenciador de configurações
│   │   │       ├── fetcher/
│   │   │       │   └── HtmlFetcher.java         # Cliente HTTP com retry
│   │   │       ├── parser/
│   │   │       │   └── HtmlParser.java          # Parser HTML resiliente
│   │   │       ├── service/
│   │   │       │   └── MovimentacaoService.java # Orquestrador
│   │   │       └── model/
│   │   │           └── NavioMovimentacao.java   # DTO/Record
│   │   └── resources/
│   │       ├── application.properties           # Configurações padrão
│   │       └── logback.xml                      # Configuração de logs
│   └── test/
│       └── java/
│           └── br/dev/marcus/praticagem/
│               └── ...                          # Testes unitários
├── build.gradle                                 # Configuração Gradle
├── gradle.properties                            # Propriedades Gradle
├── settings.gradle                              # Configurações do projeto
├── .gitignore                                   # Arquivos ignorados pelo Git
├── README.md                                    # Este arquivo
└── LICENSE                                      # Licença do projeto
```

---

## 🤝 Contribuindo

Contribuições são bem-vindas! Para contribuir:

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/NovaFuncionalidade`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/NovaFuncionalidade`)
5. Abra um Pull Request

### Diretrizes

- Mantenha o código documentado (JavaDoc)
- Adicione testes para novas funcionalidades
- Siga o estilo de código existente
- Atualize o README se necessário

---

## 📄 Licença

Este projeto está licenciado sob a Licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.

---

## 👨‍💻 Autor

**Marcus Silva**  
📧 marcus-silva.ms@marinha.mil.br  
🎓 Estudante de ADS - UNIVALI  
🏛️ Grupo de Vistorias e Inspeção de Itajaí (GVI)

---

## 🙏 Agradecimentos

- [Javalin](https://javalin.io/) - Framework web leve e eficiente
- [Jsoup](https://jsoup.org/) - Parser HTML excepcional
- [UNIVALI](https://www.univali.br/) - Universidade do Vale do Itajaí
- Praticagem ZP-21 - Fonte dos dados de movimentação

---

## 📞 Suporte

Se encontrar problemas ou tiver dúvidas:

1. Consulte a [documentação JavaDoc](#-documentação-javadoc)
2. Verifique as [Issues](https://github.com/seu-usuario/praticagem-api/issues) existentes
3. Abra uma nova Issue descrevendo o problema

---

<div align="center">

**Desenvolvido com ❤️ para a comunidade portuária de Itajaí-SC**

⭐ Se este projeto foi útil, considere dar uma estrela no GitHub!

</div>