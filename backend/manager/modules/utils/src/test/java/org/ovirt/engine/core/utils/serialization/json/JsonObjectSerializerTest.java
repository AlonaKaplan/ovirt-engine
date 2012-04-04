package org.ovirt.engine.core.utils.serialization.json;

import static org.junit.Assert.assertEquals;
import junit.framework.Assert;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network;


/**
 * Tests for {@link JsonObjectSerializer}.
 */
public class JsonObjectSerializerTest {

    @Test
    public void testSerialize() {
        JsonSerializablePojo serializablePojo = new JsonSerializablePojo();

        assertEquals(serializablePojo.toJsonForm(true),
                new JsonObjectSerializer().serialize(serializablePojo).replaceAll("\\s", ""));
    }

    @Test
    public void serializeNetwork() {
        network net = new network();
        JsonObjectSerializer serialezer = new JsonObjectSerializer();
        Assert.assertTrue(serialezer.serialize(net).length() > 0);
    }

    @Test
    public void serializeVdsNetworkInterface() {
        VdsNetworkInterface nic = new VdsNetworkInterface();
        JsonObjectSerializer serialezer = new JsonObjectSerializer();
        Assert.assertTrue(serialezer.serialize(nic).length() > 0);
    }
}
