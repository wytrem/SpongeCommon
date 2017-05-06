/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.data.persistence;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spongepowered.api.data.DataQuery.of;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.persistence.DataTranslator;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.common.data.MemoryDataView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * A translator for translating {@link DataView}s into
 * {@link ConfigurationNode}s.
 */
public class ConfigurateTranslator implements DataTranslator<ConfigurationNode> {

    @VisibleForTesting
    public static final String DATA_VIEW_IDENTIFIER = "$DataView-8f3d5a9"; // random junk, just in case of clashes
    private static final ConfigurateTranslator instance = new ConfigurateTranslator();
    private static final TypeToken<ConfigurationNode> TOKEN = TypeToken.of(ConfigurationNode.class);

    private ConfigurateTranslator() {
    }

    /**
     * Get the instance of this translator.
     *
     * @return The instance of this translator
     */
    public static ConfigurateTranslator instance() {
        return instance;
    }

    private void populateNode(ConfigurationNode node, DataView container) {
        checkNotNull(node, "node");
        checkNotNull(container, "container");

        // Configurate won't serialize this for us with a wildcard type, so we'll walk the map ourselves.
        if (container instanceof MemoryDataView) {
            walk(node, ((MemoryDataView) container).getMap(of(), false).get());
        } else {
            walk(node, container.getMap(of()).get());
        }
    }

    private void walk(ConfigurationNode node, Map<?, ?> map) {
        map.forEach((key, value) -> keyValueToNode(node.getNode(key), value));
    }

    private void walk(ConfigurationNode node, @Nullable Collection<?> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        List<ConfigurationNode> listOfNodes = new ArrayList<>();
        for (Object obj : list) {
            ConfigurationNode newNode = SimpleCommentedConfigurationNode.root();
            keyValueToNode(newNode, obj);
            if (newNode.getValue() != null) {
                listOfNodes.add(newNode);
            }
        }

        node.setValue(listOfNodes);
    }

    private void keyValueToNode(ConfigurationNode node, @Nullable Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof Map) {
            walk(node, (Map<?, ?>) value);
        } else if (value instanceof Collection) {
            walk(node, (List<?>) value);
        } else if (value instanceof DataView) {
            node.setValue(translate((DataView) value));
            ConfigurationNode idNode = node.getNode(DATA_VIEW_IDENTIFIER);
            idNode.setValue(true);
            if (idNode instanceof CommentedConfigurationNode) {
                ((CommentedConfigurationNode) idNode).setComment("Do not edit this node, this marks this entry as a DataView.");
            }
        } else {
            node.setValue(value);
        }
    }

    private DataContainer translateFromNode(ConfigurationNode node) {
        checkNotNull(node, "node");
        DataContainer dataContainer = DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED);
        ConfigurateTranslator.instance().addTo(node, dataContainer);
        return dataContainer;
    }

    private DataQuery dataQueryFromPathStack(Stack<Object> key) {
        if (key.isEmpty()) {
            return of("value"); // TODO: What do we do about singular values?
        }

        return of(key.stream().map(Object::toString).collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    private void translateNodeToData(ConfigurationNode node, DataView container, @Nullable Stack<Object> path) {

        if (path == null) {
            // We don't populate this here, an empty stack indicates the root of a DataView
            // and so we never check to see if it's a child DataView.
            path = new Stack<>();
        }

        if (node.hasMapChildren()) {
            // If we're on the top level, we are creating a DataView for this level as it is,
            // otherwise, if we have a data view, we translate that separately.
            if (!path.isEmpty() && !node.getNode(DATA_VIEW_IDENTIFIER).isVirtual()) {
                // This needs to be deserialized into a DataView - send it through the translator.
                container.set(dataQueryFromPathStack(path), translateFromNode(node));
            } else {
                for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.getChildrenMap().entrySet()) {
                    if (entry.getKey().toString().equals(DATA_VIEW_IDENTIFIER)) {
                        // We ignore this - it's our marker.
                        continue;
                    }

                    path.push(entry.getKey());
                    translateNodeToData(entry.getValue(), container, path);
                    path.pop();
                }
            }
        } else if (node.hasListChildren()) {
            List<Object> list = new ArrayList<>();

            for (ConfigurationNode childNode : node.getChildrenList()) {
                // TODO: Should this be a DataView regardless if this is a map?
                if (childNode.hasMapChildren() && !childNode.getNode(DATA_VIEW_IDENTIFIER).isVirtual()) {
                    list.add(instance.translate(childNode));
                } else {
                    list.add(childNode.getValue());
                }
            }

            container.set(dataQueryFromPathStack(path), list);
        } else {
            container.set(dataQueryFromPathStack(path), node.getValue());
        }

    }

    void translateContainerToData(ConfigurationNode node, DataView container) {
        populateNode(node, container);
    }

    @Override
    public TypeToken<ConfigurationNode> getToken() {
        return TOKEN;
    }

    @Override
    public ConfigurationNode translate(DataView view) throws InvalidDataException {
        ConfigurationNode node = SimpleCommentedConfigurationNode.root();
        populateNode(node, view);
        return node;
    }

    @Override
    public DataContainer translate(ConfigurationNode obj) throws InvalidDataException {
        return translateFromNode(obj);
    }

    @Override
    public DataView addTo(ConfigurationNode node, DataView dataView) {
        Object value = node.getValue();
        Object key = node.getKey();
        if (value != null) {
            if (key == null || value instanceof Map || value instanceof List) {
                translateNodeToData(node, dataView, null);
            } else {
                dataView.set(of('.', key.toString()), value);
            }
        }
        return dataView;
    }

    @Override
    public String getId() {
        return "sponge:configuration_node";
    }

    @Override
    public String getName() {
        return "ConfigurationNodeTranslator";
    }

}