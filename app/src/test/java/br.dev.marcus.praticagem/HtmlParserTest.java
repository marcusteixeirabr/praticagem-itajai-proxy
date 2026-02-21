package br.dev.marcus.praticagem;

import br.dev.marcus.praticagem.scrapping.HtmlParser;
import br.dev.marcus.praticagem.scrapping.NavioMovimentacao;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HtmlParserTest {
    @Test
    void deveExtrairMovimentacoesDoHtmlReal() throws Exception {
        
        // Carrega o arquivo HTML salvo em test/resources
        InputStream input = getClass()
            .getClassLoader()
            .getResourceAsStream("html/praticagem_sample.html");
        
        assertNotNull(input);

        // Parsea o HTML usando Jsoup
        Document document = Jsoup.parse(input, "UTF-8", "");

        HtmlParser parser = new HtmlParser();

        List<NavioMovimentacao> resultado = parser.parse(document);

        // Valida que encontrou pelo menos 1 regisro
        assertFalse(resultado.isEmpty());

        // Exemplo de validação simples
        NavioMovimentacao primeira = resultado.get(0);

        assertNotNull(primeira.navio());
        assertNotNull(primeira.data());
        
    }
    
    @Test
    void deveFuncionarMesmoComOrdemAlteradaDasColunas() {

        String html = """
            <table>
                <tr>
                    <th>Navio</th>
                    <th>Data</th>
                    <th>Situação</th>
                    <th>Horário</th>
                    <th>Berço</th>
                    <th>Manobra</th>
                </tr>
                <tr>
                    <td>NAVIO TESTE</td>
                    <td>21/02/2026</td>
                    <td>CONFIRMADA</td>
                    <td>10:30</td>
                    <td>201</td>
                    <td>ATRACACAO</td>
                </tr>
            </table>
        """;

        Document document = Jsoup.parse(html);

        HtmlParser parser = new HtmlParser();
        List<NavioMovimentacao> resultado = parser.parse(document);

        assertEquals(1, resultado.size());
        assertEquals("NAVIO TESTE", resultado.get(0).navio());
    }

    @Test
    void deveFalharSeColunaEssencialForRemovida() {

        String html = """
            <table>
                <tr>
                    <th>Data</th>
                    <th>Horário</th>
                    <th>Manobra</th>
                    <th>Berço</th>
                    <th>Navio</th>
                </tr>
                <tr>
                    <td>21/02/2026</td>
                    <td>10:30</td>
                    <td>ATRACACAO</td>
                    <td>201</td>
                    <td>NAVIO TESTE</td>
                </tr>
            </table>
        """;

        Document document = Jsoup.parse(html);

        HtmlParser parser = new HtmlParser();

        assertThrows(IllegalStateException.class,
                () -> parser.parse(document));
    }

    @Test
    void deveIgnorarColunasExtras() {

        String html = """
            <table>
                <tr>
                    <th>Data</th>
                    <th>Horário</th>
                    <th>Manobra</th>
                    <th>Berço</th>
                    <th>Navio</th>
                    <th>Situação</th>
                    <th>Nova Coluna Inesperada</th>
                </tr>
                <tr>
                    <td>21/02/2026</td>
                    <td>10:30</td>
                    <td>ATRACACAO</td>
                    <td>201</td>
                    <td>NAVIO TESTE</td>
                    <td>CONFIRMADA</td>
                    <td>Qualquer coisa</td>
                </tr>
            </table>
        """;

        Document document = Jsoup.parse(html);

        HtmlParser parser = new HtmlParser();
        List<NavioMovimentacao> resultado = parser.parse(document);

        assertEquals(1, resultado.size());
    }

    @Test
    void deveAceitarVariacaoNosNomesDasColunas() {

        String html = """
            <table>
                <tr>
                    <th>Data Prevista</th>
                    <th>Horario Estimado</th>
                    <th>Tipo de Manobra</th>
                    <th>Berco de Atracacao</th>
                    <th>Nome do Navio</th>
                    <th>Situacao da Manobra</th>
                </tr>
                <tr>
                    <td>21/02/2026</td>
                    <td>10:30</td>
                    <td>ATRACACAO</td>
                    <td>201</td>
                    <td>NAVIO TESTE</td>
                    <td>CONFIRMADA</td>
                </tr>
            </table>
        """;

        Document document = Jsoup.parse(html);

        HtmlParser parser = new HtmlParser();
        List<NavioMovimentacao> resultado = parser.parse(document);

        assertEquals(1, resultado.size());
    }


}
