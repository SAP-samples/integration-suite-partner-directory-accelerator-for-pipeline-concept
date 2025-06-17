<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
<#list content.params as param>
    <xsl:param name="${param}" />
</#list>
    <xsl:template match="/">
        <ns0:Interfaces xmlns:ns0='http://sap.com/xi/XI/System'>
<#assign i = 1>
<#list content.hashMapConditionService?keys as interfaceCondition>
<#if interfaceCondition!="">
            <xsl:if test="${interfaceCondition?no_esc}">
<#list content.hashMapConditionService[interfaceCondition] as interfaceName>
                <Interface>
                    <Index>${i}</Index>
                    <Service>${interfaceName?no_esc}</Service>
                </Interface>
<#assign i = i + 1>
</#list>
            </xsl:if>
<#else>
<#list content.hashMapConditionService[interfaceCondition] as interfaceName>
            <Interface>
                <Index>${i}</Index>
                <Service>${interfaceName?no_esc}</Service>
            </Interface>
<#assign i = i + 1>
</#list>
</#if>
</#list>
        </ns0:Interfaces>
    </xsl:template>
</xsl:stylesheet>