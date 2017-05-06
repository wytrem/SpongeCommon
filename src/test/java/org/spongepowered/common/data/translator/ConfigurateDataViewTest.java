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
package org.spongepowered.common.data.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.SimpleConfigurationNode;
import org.junit.Test;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.common.data.MemoryDataContainer;
import org.spongepowered.common.data.persistence.ConfigurateTranslator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigurateDataViewTest {

    @Test
    public void testNodeToData() {
        ConfigurationNode node = SimpleConfigurationNode.root();
        node.getNode("foo","int").setValue(1);
        node.getNode("foo", "double").setValue(10.0D);
        node.getNode("foo", "long").setValue(Long.MAX_VALUE);
        List<String> stringList = Lists.newArrayList();
        for (int i = 0; i < 100; i ++) {
            stringList.add("String" + i);
        }
        node.getNode("foo", "stringList").setValue(stringList);
        List<SimpleData> dataList = Lists.newArrayList();
        for (int i = 0; i < 100; i++) {
            dataList.add(new SimpleData(i, 10.0 + i, "String" + i, Collections.<String>emptyList()));
        }
        node.getNode("foo", "nested", "Data").setValue(dataList);

        DataContainer manual = DataContainer.createNew();
        manual.set(DataQuery.of("foo", "int"), 1)
                .set(DataQuery.of("foo", "double"), 10.0D)
                .set(DataQuery.of("foo", "long"), Long.MAX_VALUE)
                .set(DataQuery.of("foo", "stringList"), stringList)
                .set(DataQuery.of("foo", "nested", "Data"), dataList);

        DataView container = ConfigurateTranslator.instance().translate(node);
        assertTrue(manual.equals(container));
        ConfigurationNode translated = ConfigurateTranslator.instance().translate(container);
        // assertTrue(node.equals(translated)); // TODO Pending Configurate equals implementation
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNodeWithListOfMapsGetsConvertedToDataView() {
        ConfigurationNode node = SimpleConfigurationNode.root();

        ConfigurationNode n1 = SimpleConfigurationNode.root();
        n1.getNode("entryone").setValue(1);
        n1.getNode(ConfigurateTranslator.DATA_VIEW_IDENTIFIER).setValue(true);

        ConfigurationNode n2 = SimpleConfigurationNode.root();
        n2.getNode("entrytwo").setValue(2);
        n2.getNode("entrythree").setValue("three");
        n2.getNode(ConfigurateTranslator.DATA_VIEW_IDENTIFIER).setValue(true);

        node.getNode("list").setValue(Lists.newArrayList(n1, n2));

        // Send it back again
        DataContainer sut = ConfigurateTranslator.instance().translate(node);

        List<?> list = sut.getList(DataQuery.of("list")).get();
        assertEquals(2, list.size());
        assertTrue(list.stream().allMatch(x -> x instanceof DataView));
        assertTrue(((List<DataView>)list).stream().anyMatch(x -> x.getInt(DataQuery.of("entryone")).map(y -> y == 1).isPresent()));
        assertTrue(((List<DataView>)list).stream()
                .anyMatch(x -> x.getInt(DataQuery.of("entrytwo")).map(y -> y == 2).isPresent() &&
                        x.getString(DataQuery.of("entrythree")).map(y -> y.equals("three")).isPresent()));
    }

    @Test
    public void testDataViewWithinADataViewSerializesCorrectly() {
        // We start with a DataView
        DataContainer dataView = new MemoryDataContainer(DataView.SafetyMode.NO_DATA_CLONED);

        // DataView one
        DataContainer dataViewChildOne = new MemoryDataContainer(DataView.SafetyMode.NO_DATA_CLONED);
        dataViewChildOne.set(DataQuery.of("entryone"), 1);

        // DataView two
        DataContainer dataViewChildTwo = new MemoryDataContainer(DataView.SafetyMode.NO_DATA_CLONED);
        dataViewChildTwo.set(DataQuery.of("entrytwo"), 2).set(DataQuery.of("entrythree"), "three");

        dataView.set(DataQuery.of("list"), ImmutableList.of(dataViewChildOne, dataViewChildTwo));

        // Send it through the Configurate Translator
        ConfigurationNode node = ConfigurateTranslator.instance().translate(dataView);

        List<? extends ConfigurationNode> nodes = node.getNode("list").getChildrenList();
        assertEquals(2, nodes.size());
        assertTrue(nodes.stream().anyMatch(x -> x.getNode("entrytwo").getInt() == 2
                && x.getNode("entrythree").getString().equals("three")
                && !x.getNode(ConfigurateTranslator.DATA_VIEW_IDENTIFIER).isVirtual()));
        assertTrue(nodes.stream().anyMatch(x -> x.getNode("entryone").getInt() == 1
                && !x.getNode(ConfigurateTranslator.DATA_VIEW_IDENTIFIER).isVirtual()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testComplexNodeWithDataViewIsDeserializedCorrectly() {
        ConfigurationNode node = SimpleConfigurationNode.root();
        node.getNode("foo", "int").setValue(1);
        node.getNode("foo", "dataview", ConfigurateTranslator.DATA_VIEW_IDENTIFIER).setValue(true);
        node.getNode("foo", "dataview", "entrytwo").setValue(2);
        node.getNode("foo", "dataview", "entrythree").setValue("three");

        DataContainer sut = ConfigurateTranslator.instance().translate(node);
        assertEquals(1, (int)sut.getInt(DataQuery.of("foo", "int")).get());
        DataView dataView = sut.getView(DataQuery.of("foo", "dataview")).get();
        assertTrue(dataView.getInt(DataQuery.of("entrytwo")).map(y -> y == 2).isPresent());
        assertTrue(dataView.getString(DataQuery.of("entrythree")).map(y -> y.equals("three")).isPresent());
    }

    @Test
    public void testNodeWithListOfSimpleDataDoesNotGetConvertedToDataView() {
        // We start with a DataView
        DataContainer dataView = new MemoryDataContainer(DataView.SafetyMode.NO_DATA_CLONED);

        List<String> testList = ImmutableList.of("test1", "test2");
        dataView.set(DataQuery.of("list"), testList);

        // Send it through the Configurate Translator
        ConfigurationNode node = ConfigurateTranslator.instance().translate(dataView);

        // Send it back again
        DataContainer sut = ConfigurateTranslator.instance().translate(node);

        List<?> list = sut.getList(DataQuery.of("list")).get();
        assertEquals(2, list.size());
        assertTrue(list.contains("test1"));
        assertTrue(list.contains("test2"));
    }

    @Test
    public void testNonMapEntryReturnsDataViewCorrectly() {
        DataContainer container = ConfigurateTranslator.instance().translate(SimpleConfigurationNode.root().setValue("test"));
        assertEquals("test", container.getString(DataQuery.of("value")).get());
    }

}
