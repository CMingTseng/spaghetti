package com.prezi.spaghetti.ast.internal;

import com.prezi.spaghetti.ast.StructNode;
import com.prezi.spaghetti.ast.StructReference;

public interface MutableStructNode extends StructNode {
	void setSuperStruct(StructReference superStruct);
}
