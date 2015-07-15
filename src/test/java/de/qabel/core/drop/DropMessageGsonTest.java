package de.qabel.core.drop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.config.QblClassLoader;
import de.qabel.core.crypto.QblECKeyPair;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.ArrayList;

public class DropMessageGsonTest {
    String json = null;

    static class TestMessage extends ModelObject{
        public String content;

    }

    @Test(expected = NullPointerException.class)
    public void invalidJsonDeserializeTest()
    {
	// 'time' got a wrong value
	String json = "{\"version\":1,\"time\":\"asdf\",\"sender\":\"foo\",\"acknowledgeID\":\"1234\",\"model\":\"de.qabel.core.drop.DropMessageGsonTest$TestMessage\",\"data\":\"{\\\"content\\\":\\\"bar\\\"}\"}";
	
	GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(DropMessage.class, new DropTypeAdapter<>());
        builder.registerTypeAdapter(DropMessage.class, new DropSerializer());
        builder.registerTypeAdapter(DropMessage.class, new DropDeserializer(new QblClassLoader(ClassLoader.getSystemClassLoader())));
	
	Gson gson = builder.create();
	
	gson.fromJson(json, DropMessage.class);
    }
    
    @Test(expected = JsonSyntaxException.class)
    public void invalidJsonDeserializeTest2()
    {
	// 'time' got a missing value, i.e. wrong syntax
	String json = "{\"version\":1,\"time\":,\"sender\":\"foo\",\"acknowledgeID\":\"1234\",\"model\":\"de.qabel.core.drop.DropMessageGsonTest$TestMessage\",\"data\":\"{\\\"content\\\":\\\"bar\\\"}\"}";
	
	GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(DropMessage.class, new DropTypeAdapter<>());
        builder.registerTypeAdapter(DropMessage.class, new DropSerializer());
        builder.registerTypeAdapter(DropMessage.class, new DropDeserializer(new QblClassLoader(ClassLoader.getSystemClassLoader())));
	
	Gson gson = builder.create();
	
	gson.fromJson(json, DropMessage.class);
    }
    
    @Test
    public void serializeTest() {
        TestMessage m = new TestMessage();

        m.content = "baz";
        Identity sender = new Identity("Bernd", new ArrayList<DropURL>(), new QblECKeyPair());
        Identities identities = new Identities();
        identities.put(sender);
        DropMessage<TestMessage> a = new DropMessage<TestMessage>(sender, m);

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(DropMessage.class, new DropTypeAdapter<TestMessage>());
        builder.registerTypeAdapter(DropMessage.class, new DropSerializer());
        builder.registerTypeAdapter(DropMessage.class, new DropDeserializer(new QblClassLoader(ClassLoader.getSystemClassLoader())));

        Gson gson = builder.create();

        json = gson.toJson(a);
        assertNotNull(json);

        System.out.println("Serialized message: " + gson.toJson(a));
        DropMessage<TestMessage> deserializedJson = gson.fromJson(json, DropMessage.class);
        assertTrue(deserializedJson.registerSender(sender));
        System.out.println("Deserialized message: " + gson.toJson(deserializedJson));
        assertEquals(m.content, deserializedJson.getData().content);
        assertEquals(sender, deserializedJson.getSender());
        assertEquals(a.getAcknowledgeID(), deserializedJson.getAcknowledgeID());
    }
}
