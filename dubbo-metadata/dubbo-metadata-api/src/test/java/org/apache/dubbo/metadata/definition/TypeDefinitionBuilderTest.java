package org.apache.dubbo.metadata.definition;

import org.apache.dubbo.metadata.definition.builder.TypeBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TypeDefinitionBuilderTest {

    @Test
    public void testSortTypeBuilder(){
        TypeBuilder tb = TypeDefinitionBuilder.BUILDERS.get(0);
        Assertions.assertTrue(tb instanceof TestTypeBuilder);

        tb = TypeDefinitionBuilder.BUILDERS.get(TypeDefinitionBuilder.BUILDERS.size()-1);
        Assertions.assertTrue(tb instanceof Test3TypeBuilder);
    }
}
