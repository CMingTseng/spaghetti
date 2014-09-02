package com.prezi.spaghetti.ast;

import com.google.common.collect.Sets;
import com.prezi.spaghetti.ast.internal.DefaultExternInterfaceReference;
import com.prezi.spaghetti.ast.internal.DefaultInterfaceReference;
import com.prezi.spaghetti.ast.internal.DefaultMethodParameterNode;
import com.prezi.spaghetti.ast.internal.DefaultStructReference;
import com.prezi.spaghetti.ast.internal.DefaultTypeChain;
import com.prezi.spaghetti.ast.internal.DefaultTypeMethodNode;

import java.util.Map;
import java.util.Set;

public class AstUtils {
	public static Set<InterfaceNodeBase> getAllInterfaces(InterfaceNode node) {
		Set<InterfaceNodeBase> allInterfaces = Sets.newLinkedHashSet();
		allInterfaces.add(node);
		addAllInterfaces(node, allInterfaces);
		return allInterfaces;
	}

	private static void addAllInterfaces(InterfaceNode node, Set<InterfaceNodeBase> allInterfaces) {
		for (InterfaceReferenceBase<?> superIfaceRef : node.getSuperInterfaces()) {
			InterfaceNodeBase superIface = superIfaceRef.getType();
			if (allInterfaces.add(superIface)) {
				if (superIface instanceof InterfaceNode) {
					addAllInterfaces((InterfaceNode) superIface, allInterfaces);
				}
			}
		}
	}

	public static TypeReference resolveTypeParameters(TypeReference node, Map<TypeParameterNode, TypeReference> bindings) {
		if (node instanceof VoidTypeReference
				|| node instanceof PrimitiveTypeReference
				|| node instanceof EnumReference) {
			return node;
		} else if (node instanceof TypeChain) {
			TypeChain typeChain = (TypeChain) node;
			DefaultTypeChain result = new DefaultTypeChain(typeChain.getArrayDimensions());
			for (TypeReference elem : typeChain.getElements()) {
				result.getElements().add(resolveTypeParameters(elem, bindings));
			}
			return result;
		} else if (node instanceof ParametrizedTypeNodeReference) {
			ParametrizedTypeNodeReference<?> parametrizedRef = (ParametrizedTypeNodeReference<?>) node;
			if (parametrizedRef.getArguments().isEmpty()) {
				return parametrizedRef;
			}
			ParametrizedTypeNodeReference<?> result;
			if (node instanceof StructReference) {
				StructReference structRef = (StructReference) node;
				result = new DefaultStructReference(structRef.getType(), structRef.getArrayDimensions());
			} else if(node instanceof InterfaceReference) {
				InterfaceReference ifaceRef = (InterfaceReference) node;
				result = new DefaultInterfaceReference(ifaceRef.getType(), ifaceRef.getArrayDimensions());
			} else if (node instanceof ExternInterfaceReference) {
				ExternInterfaceReference externRef = (ExternInterfaceReference) node;
				result = new DefaultExternInterfaceReference(externRef.getType(), externRef.getArrayDimensions());
			} else {
				throw new AssertionError("Unknown parametrized type: " + node.getClass());
			}
			for (TypeReference argument : parametrizedRef.getArguments()) {
				result.getArguments().add(resolveTypeParameters(argument, bindings));
			}
			return result;
		} else if (node instanceof TypeParameterReference) {
			TypeParameterReference paramRef = (TypeParameterReference) node;
			TypeReference resolvedReference = bindings.get(paramRef.getType());
			if (resolvedReference == null) {
				return paramRef;
			} else {
				return resolveTypeParameters(resolvedReference, bindings);
			}
		} else {
			throw new AssertionError("Unknown type: " + node.getClass());
		}
	}

	@SuppressWarnings("deprecation")
	public static TypeMethodNode resolveTypeParameters(TypeMethodNode methodNode, Map<TypeParameterNode, TypeReference> bindings) {
		TypeReference returnType = resolveTypeParameters(methodNode.getReturnType(), bindings);
		DefaultTypeMethodNode result = new DefaultTypeMethodNode(methodNode.getName());
		result.setDocumentation(methodNode.getDocumentation());
		result.getAnnotations().addAll(methodNode.getAnnotations());
		result.getTypeParameters().addAll(methodNode.getTypeParameters());
		result.setReturnType(returnType);
		for (MethodParameterNode param : methodNode.getParameters()) {
			TypeReference type = resolveTypeParameters(param.getType(), bindings);
			DefaultMethodParameterNode resultParam = new DefaultMethodParameterNode(param.getName(), type, param.isOptional());
			resultParam.getAnnotations().addAll(param.getAnnotations());
			result.getParameters().add(resultParam);
		}
		return result;
	}
}
