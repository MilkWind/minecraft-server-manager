package minecraft.milkwind.manager.auth.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.time.Instant;

@Service
public class TotpService {

    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;

    public boolean verify(String secret, String code) {
        if (secret == null || code == null || code.isBlank()) {
            return false;
        }

        String normalizedCode = code.trim();
        if (!normalizedCode.matches("\\d{" + CODE_DIGITS + "}")) {
            return false;
        }

        long currentStep = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        return normalizedCode.equals(generate(secret, currentStep))
                || normalizedCode.equals(generate(secret, currentStep - 1))
                || normalizedCode.equals(generate(secret, currentStep + 1));
    }

    private String generate(String secret, long timeStep) {
        try {
            byte[] key = decodeBase32(secret);
            byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception exception) {
            return "";
        }
    }

    private byte[] decodeBase32(String secret) {
        String normalized = secret.replace("=", "").replace(" ", "").trim().toUpperCase();
        ByteBuffer output = ByteBuffer.allocate(normalized.length() * 5 / 8 + 1);
        int buffer = 0;
        int bitsLeft = 0;

        for (char character : normalized.toCharArray()) {
            int value = decodeBase32Character(character);
            if (value < 0) {
                throw new IllegalArgumentException("Invalid base32 character");
            }

            buffer = (buffer << 5) | value;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output.put((byte) ((buffer >> (bitsLeft - 8)) & 0xFF));
                bitsLeft -= 8;
            }
        }

        byte[] result = new byte[output.position()];
        output.flip();
        output.get(result);
        return result;
    }

    private int decodeBase32Character(char character) {
        if (character >= 'A' && character <= 'Z') {
            return character - 'A';
        }
        if (character >= '2' && character <= '7') {
            return character - '2' + 26;
        }
        return -1;
    }
}
