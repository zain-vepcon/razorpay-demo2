package com.example.razorpay;

import com.example.razorpay.util.SignatureUtil;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureUtilTest {

    private static final String SECRET = "test_secret_123";

    private String sign(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes()));
    }

    @Test
    void validSignature_shouldPass() throws Exception {
        String payload = "order_ABC123|pay_XYZ789";
        String correctSignature = sign(payload, SECRET);

        assertTrue(SignatureUtil.verifySignature(payload, correctSignature, SECRET));
    }

    @Test
    void tamperedPayload_shouldFail() throws Exception {
        String originalPayload = "order_ABC123|pay_XYZ789";
        String signatureForOriginal = sign(originalPayload, SECRET);

        // Attacker changes the payment id after the signature was generated
        String tamperedPayload = "order_ABC123|pay_HACKED00";

        assertFalse(SignatureUtil.verifySignature(tamperedPayload, signatureForOriginal, SECRET));
    }

    @Test
    void wrongSecret_shouldFail() throws Exception {
        String payload = "order_ABC123|pay_XYZ789";
        String signatureFromWrongSecret = sign(payload, "wrong_secret");

        assertFalse(SignatureUtil.verifySignature(payload, signatureFromWrongSecret, SECRET));
    }

    @Test
    void garbageSignature_shouldFail() {
        assertFalse(SignatureUtil.verifySignature("order_1|pay_1", "not-a-valid-hex-signature", SECRET));
    }
}
