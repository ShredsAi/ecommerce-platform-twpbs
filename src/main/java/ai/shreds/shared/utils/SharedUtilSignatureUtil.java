package ai.shreds.shared.utils;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacAlgorithms;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Utility class for signature computation and verification operations used across payment processors.
 * Supports HMAC-SHA256, SHA-1, and secure comparison operations for webhook signature validation.
 */
public final class SharedUtilSignatureUtil {

    private SharedUtilSignatureUtil() {
        // Utility class should not be instantiated
    }

    /**
     * Computes HMAC-SHA256 signature for the given data using the provided secret.
     *
     * @param data   The data to sign (must not be null)
     * @param secret The secret key for HMAC computation (must not be null)
     * @return Hex-encoded HMAC-SHA256 signature
     * @throws IllegalArgumentException if data or secret is null
     * @throws RuntimeException         if cryptographic operations fail
     */
    public static String computeHmacSha256(String data, String secret) {
        Objects.requireNonNull(data, "Data must not be null");
        Objects.requireNonNull(secret, "Secret must not be null");
        
        try {
            Mac sha256Hmac = Mac.getInstance(HmacAlgorithms.HMAC_SHA_256.getName());
            SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), 
                HmacAlgorithms.HMAC_SHA_256.getName()
            );
            sha256Hmac.init(secretKey);
            byte[] hmacData = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hmacData);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error computing HMAC-SHA256: " + e.getMessage(), e);
        }
    }

    /**
     * Computes HMAC-SHA1 signature for the given data using the provided secret.
     * Used by some payment processors like PayPal for certain operations.
     *
     * @param data   The data to sign (must not be null)
     * @param secret The secret key for HMAC computation (must not be null)
     * @return Hex-encoded HMAC-SHA1 signature
     * @throws IllegalArgumentException if data or secret is null
     * @throws RuntimeException         if cryptographic operations fail
     */
    public static String computeHmacSha1(String data, String secret) {
        Objects.requireNonNull(data, "Data must not be null");
        Objects.requireNonNull(secret, "Secret must not be null");
        
        try {
            Mac sha1Hmac = Mac.getInstance(HmacAlgorithms.HMAC_SHA_1.getName());
            SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                HmacAlgorithms.HMAC_SHA_1.getName()
            );
            sha1Hmac.init(secretKey);
            byte[] hmacData = sha1Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hmacData);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error computing HMAC-SHA1: " + e.getMessage(), e);
        }
    }

    /**
     * Computes SHA-1 hash of the given data.
     * Used by Square for webhook signature verification.
     *
     * @param data The data to hash (must not be null)
     * @return Hex-encoded SHA-1 hash
     * @throws IllegalArgumentException if data is null
     * @throws RuntimeException         if hashing operation fails
     */
    public static String computeSha1(String data) {
        Objects.requireNonNull(data, "Data must not be null");
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error computing SHA-1: " + e.getMessage(), e);
        }
    }

    /**
     * Computes SHA-256 hash of the given data.
     *
     * @param data The data to hash (must not be null)
     * @return Hex-encoded SHA-256 hash
     * @throws IllegalArgumentException if data is null
     * @throws RuntimeException         if hashing operation fails
     */
    public static String computeSha256(String data) {
        Objects.requireNonNull(data, "Data must not be null");
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error computing SHA-256: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies HMAC signature using constant-time comparison to prevent timing attacks.
     *
     * @param data      The original data that was signed
     * @param signature The signature to verify (hex-encoded)
     * @param secret    The secret key used for signing
     * @return true if signature is valid, false otherwise
     * @throws IllegalArgumentException if any parameter is null
     */
    public static boolean verifyHmac(String data, String signature, String secret) {
        Objects.requireNonNull(data, "Data must not be null");
        Objects.requireNonNull(signature, "Signature must not be null");
        Objects.requireNonNull(secret, "Secret must not be null");
        
        try {
            String computedSignature = computeHmacSha256(data, secret);
            return constantTimeEquals(computedSignature, signature);
        } catch (Exception e) {
            // Log the exception but don't reveal details to prevent information leakage
            return false;
        }
    }

    /**
     * Verifies HMAC-SHA1 signature using constant-time comparison.
     *
     * @param data      The original data that was signed
     * @param signature The signature to verify (hex-encoded)
     * @param secret    The secret key used for signing
     * @return true if signature is valid, false otherwise
     * @throws IllegalArgumentException if any parameter is null
     */
    public static boolean verifyHmacSha1(String data, String signature, String secret) {
        Objects.requireNonNull(data, "Data must not be null");
        Objects.requireNonNull(signature, "Signature must not be null");
        Objects.requireNonNull(secret, "Secret must not be null");
        
        try {
            String computedSignature = computeHmacSha1(data, secret);
            return constantTimeEquals(computedSignature, signature);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Performs constant-time string comparison to prevent timing attacks.
     * This is crucial for cryptographic signature verification.
     *
     * @param a First string to compare
     * @param b Second string to compare
     * @return true if strings are equal, false otherwise
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        // Convert to bytes for MessageDigest.isEqual which provides constant-time comparison
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        
        return MessageDigest.isEqual(aBytes, bBytes);
    }

    /**
     * Encodes binary data to a hex string.
     *
     * @param data The binary data to encode
     * @return Hex-encoded string representation
     * @throws IllegalArgumentException if data is null
     */
    public static String toHexString(byte[] data) {
        Objects.requireNonNull(data, "Data must not be null");
        return Hex.encodeHexString(data);
    }

    /**
     * Decodes a hex string to binary data.
     *
     * @param hexString The hex string to decode
     * @return Binary data representation
     * @throws IllegalArgumentException if hexString is null or invalid
     * @throws RuntimeException         if decoding fails
     */
    public static byte[] fromHexString(String hexString) {
        Objects.requireNonNull(hexString, "Hex string must not be null");
        
        try {
            return Hex.decodeHex(hexString);
        } catch (Exception e) {
            throw new RuntimeException("Error decoding hex string: " + e.getMessage(), e);
        }
    }
}
