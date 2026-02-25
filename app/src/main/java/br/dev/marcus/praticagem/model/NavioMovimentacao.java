package br.dev.marcus.praticagem.model;

 /**
 * Representa uma única linha da tabela de movimentação.
 *
 * Usamos "record" porque:
 * - É imutável
 * - É mais simples que criar getters manualmente
 * - Ideal para DTO / dados puros
 */
public record NavioMovimentacao(
    String data,
    String horario,
    String manobra,
    String berco,
    String navio,
    String situacao
) {


}
