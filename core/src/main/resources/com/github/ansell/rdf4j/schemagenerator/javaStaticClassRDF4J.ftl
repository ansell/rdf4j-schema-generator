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
 * ${title!"No title found"}
<#if description??>
 * ${description!""}
</#if>
 *
 * Namespace ${name}
 * Prefix: {@code <${prefix}>}
 *
<#list seeAlsoUrls as seeAlsoUrl>
 * @see <a href="${seeAlsoUrl}>${seeAlsoUrl}</a>
</#list>
 */
public class ${className} {

    /**
     * {@code <${prefix}>}
     */
    public static final String NAMESPACE = "${prefix}"; 

    /**
     * {@code <${name}>}
     */
    public static final String PREFIX = "${name}"; 

<#if stringConstants??>
<#list stringConstants as stringConstant>

</#list>
</#if>

<#if localNameStringConstants??>
<#list localNameStringConstants as localNameStringConstants>

</#list>
</#if>

<#if iriConstants??>
<#list iriConstants as iriConstants>

</#list>
</#if>
}