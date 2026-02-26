<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl='http://www.w3.org/1999/XSL/Transform'${content.getNamespacesAsString()?no_esc}>
<#list content.params as param>
    <xsl:param name="${param}" />
</#list>
    <xsl:template match="/">
        <ns0:Receivers xmlns:ns0='http://sap.com/xi/XI/System'>
            <ReceiverNotDetermined>
                <Type>${content.type?no_esc}</Type>
<#if content.type == "Default">
                <DefaultReceiver>
                    <Service>${content.defaultReceiver?no_esc}</Service>
                </DefaultReceiver>
<#else>
                <DefaultReceiver/>
</#if>
            </ReceiverNotDetermined>
<#list content.hashMapConditionReceiver?keys as receiverCondition>
<#if receiverCondition!="">
            <xsl:if test="${receiverCondition?no_esc}">
<#list content.hashMapConditionReceiver[receiverCondition] as receiverName>
                <Receiver>
                    <Service>${receiverName?no_esc}</Service>
                </Receiver>
</#list>
            </xsl:if>
<#else>
<#list content.hashMapConditionReceiver[receiverCondition] as receiverName>
            <Receiver>
                <Service>${receiverName?no_esc}</Service>
            </Receiver>
</#list>
</#if>
</#list>
        </ns0:Receivers>
    </xsl:template>
</xsl:stylesheet>