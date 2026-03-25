<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="html" indent="yes" />

    <xsl:template match="/">
        <html>
            <head>
                <title>Estatísticas de Jogos</title>
                <style>
                    table {
                        width: 90%;
                        border-collapse: collapse;
                        margin: 20px auto;
                    }
                    th, td {
                        border: 1px solid black;
                        padding: 8px;
                        text-align: center;
                    }
                    th {
                        background-color: #f2f2f2;
                    }
                    h1 {
                        text-align: center;
                    }
                    .jogos-table {
                        margin-top: 10px;
                        width: 100%;
                    }
                    .jogos-table th {
                        background-color: #d9ead3;
                    }
                </style>
            </head>
            <body>
                <h1>Estatísticas de Jogos</h1>
                <table>
                    <tr>
                        <th>Nickname</th>
                        <th>Total Jogos</th>
                        <th>Vitórias</th>
                        <th>Derrotas</th>
                        <th>Empates</th>
                        <th>Detalhes dos Jogos</th>
                    </tr>
                    <xsl:for-each select="estatisticas/jogador">
                        <tr>
                            <td><xsl:value-of select="@nickname" /></td>
                            <td><xsl:value-of select="totalJogos" /></td>
                            <td><xsl:value-of select="vitorias" /></td>
                            <td><xsl:value-of select="derrotas" /></td>
                            <td><xsl:value-of select="empates" /></td>
                            <td>
                                <table class="jogos-table">
                                    <tr>
                                        <th>ID do Jogo</th>
                                        <th>Tempo Pessoal</th>
                                        <th>Tempo Adversário</th>
                                        <th>Resultado</th>
                                    </tr>
                                    <xsl:for-each select="jogos/jogo">
                                        <tr>
                                            <td><xsl:value-of select="@id" /></td>
                                            <td><xsl:value-of select="tempoPessoal" /></td>
                                            <td><xsl:value-of select="tempoAdversario" /></td>
                                            <td><xsl:value-of select="resultado" /></td>
                                        </tr>
                                    </xsl:for-each>
                                </table>
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
            </body>
        </html>
    </xsl:template>

</xsl:stylesheet>