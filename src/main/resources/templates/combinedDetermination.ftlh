<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
    <xsl:template match="/">
<#assign receiverDetermination = content.receiverDetermination>
<#assign mapInterfaceDeterminations = content.mapInterfaceDeterminations>
        <ns0:Receivers xmlns:ns0='http://sap.com/xi/XI/System'>
            <ReceiverNotDetermined>
                <Type>${receiverDetermination.type?no_esc}</Type>
<#if receiverDetermination.type == "Default">
                <DefaultReceiver>
                    <Service>${receiverDetermination.defaultReceiver?no_esc}</Service>
                    <Interfaces>
<#assign i = 1>
<#list mapInterfaceDeterminations[receiverDetermination.defaultReceiver].hashMapConditionService?keys as interfaceCondition>
<#if interfaceCondition!="">
                        <xsl:if test="${interfaceCondition?no_esc}">
<#list mapInterfaceDeterminations[receiverDetermination.defaultReceiver].hashMapConditionService[interfaceCondition] as interfaceName>
                            <Interface>
                                <Index>${i}</Index>
                                <Service>${interfaceName?no_esc}</Service>
                            </Interface>
<#assign i = i + 1>
</#list>
                        </xsl:if>
<#else>
<#list mapInterfaceDeterminations[receiverDetermination.defaultReceiver].hashMapConditionService[interfaceCondition] as interfaceName>
                        <Interface>
                            <Index>${i}</Index>
                            <Service>${interfaceName?no_esc}</Service>
                        </Interface>
<#assign i = i + 1>
</#list>
</#if>
</#list>
                    </Interfaces>
                </DefaultReceiver>
<#else>
                <DefaultReceiver/>
</#if>
            </ReceiverNotDetermined>
<#list receiverDetermination.hashMapConditionReceiver?keys as receiverCondition>
<#if receiverCondition!="">
            <xsl:if test="${receiverCondition?no_esc}">
<#list receiverDetermination.hashMapConditionReceiver[receiverCondition] as receiverName>
                <Receiver>
                    <Service>${receiverName?no_esc}</Service>
                    <Interfaces>
<#assign i = 1>
<#list mapInterfaceDeterminations[receiverName].hashMapConditionService?keys as interfaceCondition>
<#if interfaceCondition!="">
                        <xsl:if test="${interfaceCondition?no_esc}">
<#list mapInterfaceDeterminations[receiverName].hashMapConditionService[interfaceCondition] as interfaceName>
                            <Interface>
                                <Index>${i}</Index>
                                <Service>${interfaceName?no_esc}</Service>
                            </Interface>
<#assign i = i + 1>
</#list>
                        </xsl:if>
<#else>
<#list mapInterfaceDeterminations[receiverName].hashMapConditionService[interfaceCondition] as interfaceName>
                        <Interface>
                            <Index>${i}</Index>
                            <Service>${interfaceName?no_esc}</Service>
                        </Interface>
<#assign i = i + 1>
</#list>
</#if>
</#list>
                    </Interfaces>
                </Receiver>
</#list>
            </xsl:if>
<#else>
<#list receiverDetermination.hashMapConditionReceiver[receiverCondition] as receiverName>
            <Receiver>
                <Service>${receiverName?no_esc}</Service>
                <Interfaces>
<#assign i = 1>
<#list mapInterfaceDeterminations[receiverName].hashMapConditionService?keys as interfaceCondition>
<#if interfaceCondition!="">
                    <xsl:if test="${interfaceCondition?no_esc}">
<#list mapInterfaceDeterminations[receiverName].hashMapConditionService[interfaceCondition] as interfaceName>
                        <Interface>
                            <Index>${i}</Index>
                            <Service>${interfaceName?no_esc}</Service>
                        </Interface>
<#assign i = i + 1>
</#list>
                    </xsl:if>
<#else>
<#list mapInterfaceDeterminations[receiverName].hashMapConditionService[interfaceCondition] as interfaceName>
                    <Interface>
                        <Index>${i}</Index>
                        <Service>${interfaceName?no_esc}</Service>
                    </Interface>
<#assign i = i + 1>
</#list>
</#if>
</#list>
                </Interfaces>
            </Receiver>
</#list>
</#if>
</#list>
        </ns0:Receivers>
    </xsl:template>
</xsl:stylesheet>