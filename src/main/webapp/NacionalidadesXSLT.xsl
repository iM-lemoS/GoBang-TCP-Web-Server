<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="html" encoding="UTF-8" indent="yes"/>

<xsl:template match="/">
  <html>
  <head>
    <title>Lista de Nacionalidades</title>
    <style>
      body { font-family: Arial, sans-serif; margin: 20px; }
      table { border-collapse: collapse; width: 100%; margin-top: 20px; }
      th, td { border: 1px solid #dddddd; text-align: left; padding: 8px; }
      th { background-color: #f2f2f2; }
      tr:nth-child(even) { background-color: #f9f9f9; }
    </style>
  </head>
  <body>
    <h1>Lista de Nacionalidades do Mundo (Português de Portugal)</h1>
    <table>
      <thead>
        <tr>
          <th>País</th>
          <th>Nacionalidade (Masculino)</th>
          <th>Nacionalidade (Feminino)</th>
          <th>Código ISO Alpha-2</th>
        </tr>
      </thead>
      <tbody>
        <xsl:apply-templates select="nacionalidades/nacionalidade">
          <xsl:sort select="pais"/> 
        </xsl:apply-templates>
      </tbody>
    </table>
  </body>
  </html>
</xsl:template>

<xsl:template match="nacionalidade">
  <tr>
    <td><xsl:value-of select="pais"/></td>
    <td><xsl:value-of select="masculino"/></td>
    <td><xsl:value-of select="feminino"/></td>
    <td><xsl:value-of select="codigo_ISO_alpha2"/></td>
  </tr>
</xsl:template>

</xsl:stylesheet>