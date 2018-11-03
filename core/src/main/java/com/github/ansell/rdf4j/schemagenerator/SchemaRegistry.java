package com.github.ansell.rdf4j.schemagenerator;

import java.util.Collection;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;

import com.github.ansell.abstractserviceloader.AbstractServiceLoader;
import com.github.ansell.jdefaultdict.JDefaultDict;

/**
 * A META-INF/services based service registry for Schema's.
 *
 * @author Peter Ansell p_ansell@yahoo.com
 */
public abstract class SchemaRegistry<T extends Schema> extends AbstractServiceLoader<IRI, T> {

    private static final long serialVersionUID = -5649091002854294326L;

    public SchemaRegistry(Class<T> serviceClass, Function<T, IRI> keyLambda) {
        super(serviceClass, keyLambda);
    }

    public SchemaRegistry(Class<T> serviceClass, ClassLoader classLoader,
            Function<T, IRI> keyLambda) {
        super(serviceClass, classLoader, keyLambda);
    }

    public SchemaRegistry(final Class<T> serviceClass, final ClassLoader classLoader,
            final Function<T, IRI> keyLambda, final JDefaultDict<IRI, Collection<T>> services) {
        super(serviceClass, classLoader, keyLambda, services);
    }

}
