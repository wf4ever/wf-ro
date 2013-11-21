package org.purl.wf4ever.wf2ro;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/** 
 * Additional methods for {@link UUID}.
 * 
 * @author Stian Soiland-Reyes
 * 
 * See http://www.ietf.org/rfc/rfc4122.txt */
public class UUIDTool {
    private static final Charset ASCII = Charset.forName("ASCII");

    public static UUID NAMESPACE_DNS = UUID
            .fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
    public static UUID NAMESPACE_URI = UUID
            .fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");
    public static UUID NAMESPACE_OID = UUID
            .fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8");
    public static UUID NAMESPACE_X500 = UUID
            .fromString("6ba7b814-9dad-11d1-80b4-00c04fd430c8");

    public static UUID namespaceUUIDv3md5(UUID namespace, String asciiString) {
        byte[] bytes = namespaceAndBytes(namespace, asciiString);        
        return UUID.nameUUIDFromBytes(bytes);
    }

    public static byte[] getBytes(UUID uuid) {
        byte[] bytes = new byte[16];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return bytes;
    }
    
    private static byte[] namespaceAndBytes(UUID namespace, String asciiString) {
        byte[] asciiBytes = asciiString.getBytes(ASCII);
        byte[] bytes = new byte[asciiBytes.length + 16];

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.putLong(namespace.getMostSignificantBits());
        buffer.putLong(namespace.getLeastSignificantBits());
        buffer.put(asciiBytes);
        return bytes;
    }
    
    public static UUID namespaceUUIDv5sha1(UUID namespace, String asciiString) {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        byte[] bytes = namespaceAndBytes(namespace, asciiString);   
        byte[] digest = sha1.digest(bytes);
        
        digest[6] &= 0x0f; // clear version
        digest[6] |= 0x50; // v5 (sha)
        digest[8] &= 0x3f; // clear variant
        digest[8] |= 0x80; // ietf
        
        ByteBuffer digestBuffer = ByteBuffer.wrap(digest);
        return new UUID(digestBuffer.getLong(), digestBuffer.getLong());
    }

    public static UUID namespaceUUID(URI uri) {
        return namespaceUUIDv5sha1(NAMESPACE_URI, uri.toASCIIString());
    }
}
