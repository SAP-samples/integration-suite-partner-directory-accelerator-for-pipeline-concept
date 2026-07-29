<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl='http://www.w3.org/1999/XSL/Transform'${content.getNamespacesAsString()?no_esc}>
<#list content.params as param>
    <xsl:param name="${param}" />
</#list>
    <xsl:template match="/">
        <ns0:Interfaces xmlns:ns0='http://sap.com/xi/XI/System'>
<#assign i = 1>
<#assign nameIndex = 0>
<#list content.hashMapConditionService?keys as interfaceCondition>
<#if interfaceCondition!="">
            <xsl:if test="${interfaceCondition?no_esc}">
<#list content.hashMapConditionService[interfaceCondition] as interfaceName>
                <Interface>
                    <Index>${i}</Index>
                    <Service>${interfaceName?no_esc}</Service>
<#if nameIndex < content.receiverInterfaceNames?size && content.receiverInterfaceNames[nameIndex]?has_content>
                    <Name>${content.receiverInterfaceNames[nameIndex]?no_esc}</Name>
</#if>
                </Interface>
<#assign nameIndex = nameIndex + 1>
<#assign i = i + 1>
</#list>
            </xsl:if>
<#else>
<#list content.hashMapConditionService[interfaceCondition] as interfaceName>
            <Interface>
                <Index>${i}</Index>
                <Service>${interfaceName?no_esc}</Service>
<#if nameIndex < content.receiverInterfaceNames?size && content.receiverInterfaceNames[nameIndex]?has_content>
                <Name>${content.receiverInterfaceNames[nameIndex]?no_esc}</Name>
</#if>
            </Interface>
<#assign nameIndex = nameIndex + 1>
<#assign i = i + 1>
</#list>
</#if>
</#list>
        </ns0:Interfaces>
    </xsl:template>
</xsl:stylesheet>