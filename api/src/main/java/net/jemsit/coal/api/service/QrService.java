package net.jemsit.coal.api.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import net.jemsit.coal.api.dto.QrDtos.QrPassportDto;
import net.jemsit.coal.shared.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class QrService {

    private final BatchService batches;
    private final PileService piles;
    private final MovementService movements;
    private final String baseUrl;

    public QrService(BatchService batches,
                     PileService piles,
                     MovementService movements,
                     @Value("${coal.qr.base-url:http://localhost:5174}") String baseUrl) {
        this.batches = batches;
        this.piles = piles;
        this.movements = movements;
        this.baseUrl = baseUrl;
    }

    public byte[] png(String type, UUID id) {
        // The payload is a bare, absolute URL with no human-readable suffix, so a
        // plain phone camera (not just our APK) recognises it as a link and opens
        // the public passport page. The mobile parsers still accept the old
        // "{url} (label)" shape, so already-printed codes keep working.
        require(type, id);
        String content = baseUrl + "/qr/" + type + "/" + id;
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 320, 320);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException(500, "Не удалось сформировать QR-код: " + e.getMessage());
        }
    }

    /**
     * One anonymous read of everything the passport page shows. Served under
     * /api/qr/** which SecurityConfig leaves open, so a scanner without a session
     * still gets the full picture the APK renders.
     */
    public QrPassportDto passport(String type, UUID id) {
        return switch (type) {
            case "batch" -> new QrPassportDto(type,
                    batches.get(id), null, null,
                    batches.qualityHistory(id), batches.analysesOf(id));
            case "pile" -> new QrPassportDto(type,
                    null, piles.get(id), null, List.of(), List.of());
            case "movement" -> new QrPassportDto(type,
                    null, null, movements.get(id), List.of(), List.of());
            default -> throw ApiException.badRequest("Неизвестный тип QR: " + type);
        };
    }

    /** Validates the id exists for the type without building a payload. */
    private void require(String type, UUID id) {
        switch (type) {
            case "batch" -> batches.get(id);
            case "pile" -> piles.get(id);
            case "movement" -> movements.get(id);
            default -> throw ApiException.badRequest("Неизвестный тип QR: " + type);
        }
    }
}
