package org.apache.dubbo.rpc.cluster.router.expression.model;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;

import java.util.stream.Collectors;

/**
 * A yaml constructor for parsing RuleSets which should be a map.
 */
public class ExpressionRuleConstructor extends Constructor {

    private TypeDescription itemType = new TypeDescription(RuleSet.class);

    private static final String ROOT_NAME = "ruleSetRoot";

    public ExpressionRuleConstructor() {
        this.rootTag = new Tag(ROOT_NAME);
        this.addTypeDescription(itemType);
    }

    @Override
    protected Object constructObject(Node node) {
        if (ROOT_NAME.equals(node.getTag().getValue()) && node instanceof MappingNode) {
            MappingNode mNode = (MappingNode) node;
            return mNode.getValue().stream().collect(
                Collectors.toMap(
                    t -> super.constructObject(t.getKeyNode()),
                    t -> {
                        Node child = t.getValueNode();
                        child.setType(itemType.getType());
                        return super.constructObject(child);
                    }
                )
            );
        } else {
            return super.constructObject(node);
        }
    }
}
