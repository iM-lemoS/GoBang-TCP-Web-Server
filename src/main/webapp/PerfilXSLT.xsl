<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="html" indent="yes" />

    <xsl:template match="/">
        <html>
            <head>
                <title>Lista de Jogadores</title>
                <style>
                    table {
                        width: 80%;
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
                </style>
            </head>
            <body>
                <h1>Jogadores</h1>
                <table>
                    <tr>
                        <th>Nickname</th>
                        <th>Nacionalidade</th>
                        <th>Idade</th>
                        <th>Password</th>
                        <th>Foto</th>
                        <th>Sexo</th>
                    </tr>
                    <xsl:for-each select="jogadores/jogador">
                        <tr>
                            <td><xsl:value-of select="@nickname" /></td>
                            <td><xsl:value-of select="nacionalidade" /></td>
                            <td><xsl:value-of select="idade" /></td>
                            <td><xsl:value-of select="password" /></td>
                            <td><xsl:value-of select="foto" /></td>
                            <td><xsl:value-of select="sexo" /></td>
                        </tr>
                    </xsl:for-each>
                </table>
            </body>
        </html>
    </xsl:template>

</xsl:stylesheet>