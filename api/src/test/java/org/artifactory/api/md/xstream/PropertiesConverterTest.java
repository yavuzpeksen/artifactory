/*
 * This file is part of Artifactory.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.api.md.xstream;

import com.thoughtworks.xstream.XStream;
import org.artifactory.api.md.Properties;
import org.artifactory.api.xstream.XStreamFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Yoav Landman
 */
public class PropertiesConverterTest {
    @Test
    public void testConversion() {
        Properties properties = new Properties();
        properties.put("a", "aye");
        properties.put("a", "aye2");
        properties.put("b", "bee");
        properties.put("c", "cee");
        properties.put("empty", "");
        properties.put("null", null);
        properties.put("xml", "<xml></xml>");
        XStream xstream = XStreamFactory.create();
        String xml = xstream.toXML(properties);
        Object deserializedMap = xstream.fromXML(xml);
        Assert.assertEquals(deserializedMap, properties);
    }
}