package com.prezi.gradle.spaghetti

import groovy.transform.EqualsAndHashCode
import prezi.spaghetti.SpaghettiModuleParser.FqNameContext
/**
 * Fully qualified name.
 */
@EqualsAndHashCode
final public class FQName {
	final String namespace
	final String localName

	private FQName(String namespace, String localName) {
		this.localName = localName
		this.namespace = namespace
	}

	public static FQName fromString(String fqName) {
		String _name
		String _namespace
		int lastDot = fqName.lastIndexOf('.')
		if (lastDot == -1)
		{
			_namespace = null
			_name = fqName
		}
		else
		{
			_namespace = fqName.substring(0, lastDot)
			_name = fqName.substring(lastDot + 1)
		}
		return new FQName(_namespace, _name)
	}

	public static FQName fromContext(FqNameContext context) {
		if (context.qualifiedName != null) {
			return FQName.fromString(context.qualifiedName.text)
		} else {
			return FQName.fromString(context.name.text)
		}
	}

	public FQName resolveLocalName(FQName name) {
		if (name.hasNamespace()) {
			return name
		} else {
			return new FQName(namespace, name.localName)
		}
	}

	public String getFullyQualifiedName() {
		return namespacePrefix + localName
	}

	public String getNamespacePrefix() {
		return (namespace == null ? "" : namespace + ".")
	}

	public File createNamespacePath(File root) {
		File result = root
		if (namespace != null) {
			namespace.split(/\./).each { name -> result = new File(result, name) }
		}
		return result
	}

	public boolean hasNamespace() {
		return namespace != null
	}

	@Override
	String toString() {
		return fullyQualifiedName
	}
}
