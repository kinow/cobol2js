<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/copybook">
		<FileFormat ConversionTable="Cp037" newLineSize="0" dataFileImplementation="IBM i or z System">
			<xsl:attribute name="distinguishFieldSize">
				<xsl:choose>
					<xsl:when test="count(item)=1">0</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="item/item/@display-length" />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
			<xsl:for-each select="item">
				<RecordFormat>
					<xsl:attribute name="distinguishFieldValue">
						<xsl:choose>
							<xsl:when test="item/@value">
								<xsl:value-of select="item/@value" />
							</xsl:when>
							<xsl:otherwise>0</xsl:otherwise>
						</xsl:choose>
					</xsl:attribute>
					<xsl:attribute name="cobolRecordName">
						<xsl:value-of select="@name" />
					</xsl:attribute>
					<xsl:apply-templates select="item"/>
				</RecordFormat>
			</xsl:for-each>
		</FileFormat>
	</xsl:template>
	<xsl:template match="item" name="fieldslist">
		<xsl:choose>
			<xsl:when test="@redefined">
		<!-- skips redefined items --></xsl:when>
			<xsl:when test="@picture or @usage">
				<FieldFormat>
					<xsl:attribute name="Name">
						<xsl:value-of select="@name"/>
					</xsl:attribute>
					<xsl:attribute name="DependingOn">
						<xsl:value-of select="@depending-on"/>
					</xsl:attribute>
					<xsl:attribute name="Occurs">
						<xsl:choose>
							<xsl:when test="@occurs">
								<xsl:value-of select="@occurs"/>
							</xsl:when>
							<xsl:otherwise>1</xsl:otherwise>
						</xsl:choose>
					</xsl:attribute>
					<xsl:attribute name="Size">
						<xsl:value-of select="@display-length"/>
					</xsl:attribute>
					<xsl:choose>
						<xsl:when test="@numeric='true' or substring(@usage, 1, 4)='comp'">
							<xsl:attribute name="Decimal">
								<xsl:choose>
									<xsl:when test="@scale">
										<xsl:value-of select="@scale"/>
									</xsl:when>
									<xsl:otherwise>0</xsl:otherwise>
								</xsl:choose>
							</xsl:attribute>
							<xsl:choose>
								<xsl:when test="@usage='binary' or @usage='computational' or @usage='computational-4' or @usage='computational-5'">
									<xsl:attribute name="Type">B</xsl:attribute>
									<xsl:attribute name="Size">
										<xsl:choose>
											<xsl:when test="@display-length &lt; 5">2</xsl:when>
											<xsl:when test="@display-length &lt; 10">4</xsl:when>
											<xsl:otherwise>8</xsl:otherwise>
										</xsl:choose>
									</xsl:attribute>
								</xsl:when>
								<xsl:when test="@usage='computational-1'">
									<xsl:attribute name="Type">1</xsl:attribute>
									<xsl:attribute name="Size">4</xsl:attribute>
								</xsl:when>
								<xsl:when test="@usage='computational-2'">
									<xsl:attribute name="Type">2</xsl:attribute>
									<xsl:attribute name="Size">8</xsl:attribute>
								</xsl:when>
								<xsl:when test="@usage='computational-3' or @usage='packed-decimal'">
									<xsl:attribute name="Type">3</xsl:attribute>
								</xsl:when>
								<xsl:when test="contains(@picture, 'COMP-6')">
									<xsl:attribute name="Type">6</xsl:attribute>
								</xsl:when>
								<xsl:otherwise>
									<xsl:attribute name="Type">9</xsl:attribute>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:when>
						<xsl:otherwise>
							<xsl:attribute name="Type">X</xsl:attribute>
							<xsl:attribute name="Decimal">0</xsl:attribute>
						</xsl:otherwise>
					</xsl:choose>
					<xsl:attribute name="Signed">
						<xsl:choose>
							<xsl:when test="@signed">
								<xsl:value-of select="@signed"/>
							</xsl:when>
							<xsl:otherwise>false</xsl:otherwise>
						</xsl:choose>
					</xsl:attribute>
					<xsl:attribute name="ImpliedDecimal">
						<xsl:choose>
							<xsl:when test="@insert-decimal-point">
								<xsl:value-of select="not(@insert-decimal-point)"/>
							</xsl:when>
							<xsl:otherwise>true</xsl:otherwise>
						</xsl:choose>
					</xsl:attribute>
					<xsl:attribute name="Value">
						<xsl:value-of select="@value"/>
					</xsl:attribute>
					<xsl:attribute name="Picture">
						<xsl:value-of select="@picture"/>
					</xsl:attribute>
				</FieldFormat>
			</xsl:when>
			<xsl:otherwise>
				<FieldsGroup>
					<xsl:attribute name="Name">
						<xsl:value-of select="@name"/>
					</xsl:attribute>
					<xsl:attribute name="DependingOn">
						<xsl:value-of select="@depending-on"/>
					</xsl:attribute>
					<xsl:attribute name="Occurs">
						<xsl:choose>
							<xsl:when test="@occurs">
								<xsl:value-of select="@occurs"/>
							</xsl:when>
							<xsl:otherwise>1</xsl:otherwise>
						</xsl:choose>
					</xsl:attribute>
					<xsl:apply-templates select="item"/>
				</FieldsGroup>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
