<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:redirect="http://xml.apache.org/xalan/redirect"
	extension-element-prefixes="redirect"
	version="1.0"
>

<xsl:template match="RecordFormat">
	<xsl:variable name="filename" select="concat(@distinguishFieldValue,'.xml')" />
	<redirect:write select="$filename">
		<schema>
			<xsl:apply-templates />
		</schema>
	</redirect:write>
</xsl:template>

<xsl:template match="FieldsGroup">
	<xsl:call-template name="my-loop-fg">
		<xsl:with-param name="my-occurs-fg" select="@Occurs"/>
	</xsl:call-template>
</xsl:template>

<xsl:template name="my-loop-fg">
	<xsl:param name="my-occurs-fg"/>
	
	<xsl:if test="$my-occurs-fg &gt; 0">
		<xsl:call-template name="my-loop-fg">
			<xsl:with-param name="my-occurs-fg" select="$my-occurs-fg - 1"/>
		</xsl:call-template>
	</xsl:if>
	<xsl:if test="$my-occurs-fg &gt; 0">
		<xsl:apply-templates>
			<xsl:with-param name="my-occurs-fg" select="$my-occurs-fg"/>
		</xsl:apply-templates>
	</xsl:if>
</xsl:template>

<xsl:template match="FieldFormat">
	<xsl:param name="my-occurs-fg"/>
	<xsl:call-template name="my-loop-ff">
		<xsl:with-param name="my-occurs-ff" select="@Occurs"/>
		<xsl:with-param name="my-occurs-fg" select="$my-occurs-fg"/>
	</xsl:call-template>
</xsl:template>

<xsl:template name="my-loop-ff">
	<xsl:param name="my-occurs-ff"/>
	<xsl:param name="my-occurs-fg"/>
	
	<xsl:if test="$my-occurs-ff &gt; 0">
		<xsl:call-template name="my-loop-ff">
			<xsl:with-param name="my-occurs-ff" select="$my-occurs-ff - 1"/>
			<xsl:with-param name="my-occurs-fg" select="$my-occurs-fg"/>
		</xsl:call-template>
	</xsl:if>
	<xsl:if test="$my-occurs-ff &gt; 0">
		<xsl:call-template name="FieldFormat-1">
			<xsl:with-param name="my-occurs-ff" select="$my-occurs-ff"/>
			<xsl:with-param name="my-occurs-fg" select="$my-occurs-fg"/>
		</xsl:call-template>
	</xsl:if>
</xsl:template>

<xsl:template name="FieldFormat-1">
	<xsl:param name="my-occurs-ff"/>
	<xsl:param name="my-occurs-fg"/>
	<column key="false" nullable="true" pattern="" >
		<xsl:attribute name="default" >
			<xsl:value-of select="@Value"/>
		</xsl:attribute>
		<xsl:attribute name="comment" >
			<xsl:value-of select="@Picture"/>
		</xsl:attribute>
		<xsl:attribute name="label" >
			<xsl:value-of select="concat(translate(@Name, '-', '_'), '_', $my-occurs-fg, '_', $my-occurs-ff)"/>
		</xsl:attribute>
		<xsl:attribute name="originalDbColumnName" >
			<xsl:value-of select="translate(@Name, '-', '_')"/>
		</xsl:attribute>
		<xsl:attribute name="length" >
			<xsl:value-of select="@Size"/>
		</xsl:attribute>
		<xsl:attribute name="precision" >
			<xsl:value-of select="@Decimal"/>
		</xsl:attribute>
		<xsl:attribute name="talendType" >
			<xsl:choose>
				<xsl:when test="@Type='X'">id_String</xsl:when>
				<xsl:when test="@Type='1'">id_Float</xsl:when>
				<xsl:when test="@Type='2'">id_Double</xsl:when>
				<xsl:when test="@Type='3'">id_BigDecimal</xsl:when>
				<xsl:when test="@Type='9'">id_BigDecimal</xsl:when>
				<xsl:when test="@Type='B'">id_BigDecimal</xsl:when>
				<xsl:when test="@Type='T'">id_byte[]</xsl:when>
			</xsl:choose>
		</xsl:attribute>
	</column>
</xsl:template>

</xsl:stylesheet>
