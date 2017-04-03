<#macro doI level=1>${StringUtils.repeat(indent, level)}</#macro>
<#macro escapeAndWrapLine rawString><#assign escapedDescription = rawString?js_string>
<#assign oneIndent><@doI/></#assign>
${WordUtils.wrap(escapedDescription, 70, "\n ${oneIndent} * ", false)}</#macro>
<#macro schemaRecordJavadoc schemaRecord>
/**
<#if schemaRecord.getLabel().isPresent()>
<@doI/> * <@escapeAndWrapLine rawString="${schemaRecord.getLabel().get().stringValue()}"/>
<@doI/> * <p>
</#if>
<@doI/> * {@code ${schemaRecord.getIRI().stringValue()?js_string}}
<#if schemaRecord.getDescription().isPresent()>
<@doI/> * <p>
<@doI/> * <@escapeAndWrapLine rawString="${schemaRecord.getDescription().get().stringValue()}"/>
</#if>
<@doI/> * @see <a href="${schemaRecord.getIRI().stringValue()?js_string}">${schemaRecord.getRawRecordKey()?js_string}</a>
<@doI/> */
</#macro>
/*
 * Auto-generated by RDF4JSchemaGenerator
 */
<#if packageName??>
package ${packageName};

</#if>
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * ${title!"No title found"?js_string}
<#if description??>
 * ${description?js_string}
</#if>
 *
 * Namespace ${name?js_string}
 * Prefix: {@code <${prefix?js_string}>}
 *
<#list seeAlsoUrls as seeAlsoUrl>
 * @see <a href="${seeAlsoUrl?js_string}>${seeAlsoUrl?js_string}</a>
</#list>
 */
public class ${className?j_string} <#if metaInfServicesInterface??>implements ${metaInfServicesInterface?j_string} </#if>{

<@doI/>/**
<@doI/> * {@code <${prefix?js_string}>}
<@doI/> */
<@doI/>public static final String NAMESPACE = "${prefix?j_string}"; 

<@doI/>/**
<@doI/> * {@code <${prefix?js_string}>}
<@doI/> */
<@doI/>public static final IRI NAMESPACE_IRI;

<@doI/>/**
<@doI/> * {@code <${name?js_string}>}
<@doI/> */
<@doI/>public static final String PREFIX = "${name?j_string}"; 

<#if stringConstants??>
<@doI/>/**********************
<@doI/> * IRI String Constants
<@doI/> **********************/
<#list stringConstants as stringConstant>

<@doI/><@schemaRecordJavadoc schemaRecord=stringConstant />
<@doI/>public static final String ${stringConstant.getFormattedRecordKey()?j_string} = "${stringConstant.getRawRecordKey()?j_string}";
</#list>
</#if>

<#if localNameStringConstants??>
<@doI/>/*****************************
<@doI/> * Local Name String Constants
<@doI/> *****************************/
<#list localNameStringConstants as localNameStringConstant>

<@doI/><@schemaRecordJavadoc schemaRecord=localNameStringConstant />
<@doI/>public static final String ${localNameStringConstant.getFormattedRecordKey()?j_string} = "${localNameStringConstant.getRawRecordKey()?j_string}";
</#list>
</#if>

<#if iriConstants??>
<@doI/>/***************
<@doI/> * IRI Constants
<@doI/> ***************/
<#list iriConstants as iriConstant>

<@doI/><@schemaRecordJavadoc schemaRecord=iriConstant />
<@doI/>public static final IRI ${iriConstant.getFormattedRecordKey()?j_string};
</#list>
</#if>

<@doI/>/**
<@doI/> * Static initializer
<@doI/> */
<@doI/>static {
<@doI/><@doI/>ValueFactory vf = SimpleValueFactory.getInstance();

<@doI/><@doI/>NAMESPACE_IRI = vf.createIRI(NAMESPACE);

<#if iriConstants??>
<@doI/><@doI/>/***********************
<@doI/><@doI/> * IRI Constant creation
<@doI/><@doI/> ***********************/
<#list iriConstants as iriConstant>
<@doI/><@doI/>${iriConstant.getFormattedRecordKey()?j_string} = vf.createIRI("${iriConstant.getIRI().stringValue()?j_string}");
</#list>
</#if>
<@doI/>}

<#if metaInfServicesInterface??>
<@doI/>@Override
<@doI/>public IRI getIRI() {
<@doI/><@doI/>return NAMESPACE_IRI;
<@doI/>}
</#if>
<@doI/>public ${className?j_string}() {
<@doI/><@doI/>// To enable service discovery to succeed, even though this is a static class
<@doI/>}
}