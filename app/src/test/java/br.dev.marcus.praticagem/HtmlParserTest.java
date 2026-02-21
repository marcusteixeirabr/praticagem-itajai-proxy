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
}
