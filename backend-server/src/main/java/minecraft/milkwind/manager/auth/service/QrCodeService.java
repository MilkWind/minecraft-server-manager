package minecraft.milkwind.manager.auth.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class QrCodeService {

    public String generateSvgDataUri(String content) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    280,
                    280,
                    Map.of(EncodeHintType.MARGIN, 1)
            );
            String svg = toSvg(matrix);
            return "data:image/svg+xml;base64," + Base64.getEncoder()
                    .encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        } catch (WriterException exception) {
            throw new IllegalStateException("生成二维码失败", exception);
        }
    }

    private String toSvg(BitMatrix matrix) {
        StringBuilder builder = new StringBuilder();
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        builder.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
                .append(width)
                .append(' ')
                .append(height)
                .append("\" shape-rendering=\"crispEdges\">")
                .append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>");

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix.get(x, y)) {
                    builder.append("<rect x=\"")
                            .append(x)
                            .append("\" y=\"")
                            .append(y)
                            .append("\" width=\"1\" height=\"1\" fill=\"#111111\"/>");
                }
            }
        }

        builder.append("</svg>");
        return builder.toString();
    }
}
