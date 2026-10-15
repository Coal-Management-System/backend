package net.jemsit.coal.api.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import net.jemsit.coal.shared.api.ApiException;
import net.jemsit.coal.shared.repo.CoalBatchRepository;
import net.jemsit.coal.shared.repo.CoalPileRepository;
import net.jemsit.coal.shared.repo.MovementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QrService {

    private final CoalBatchRepository batches;
    private final CoalPileRepository piles;
    private final MovementRepository movements;
    private final String baseUrl;

    public QrService(CoalBatchRepository batches,
                     CoalPileRepository piles,
                     MovementRepository movements,
                     @Value("${coal.qr.base-url:http://localhost:5174}") String baseUrl) {
        this.batches = batches;
        this.piles = piles;
        this.movements = movements;
        this.baseUrl = baseUrl;
    }

    public byte[] png(String type, UUID id) {
        String label = switch (type) {
            case "batch" -> batches.findById(id)
                    .orElseThrow(() -> ApiException.notFound("Партия не найдена: " + id)).getBatchNumber();
            case "pile" -> piles.findById(id)
                    .orElseThrow(() -> ApiException.notFound("Штабель не найден: " + id)).getPileNumber();
            case "movement" -> movements.findById(id)
                    .orElseThrow(() -> ApiException.notFound("Движение не найдено: " + id)).getId().toString();
            default -> throw ApiException.badRequest("Неизвестный тип QR: " + type);
        };
        String content = baseUrl + "/qr/" + type + "/" + id + " (" + label + ")";
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 320, 320);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException(500, "Не удалось сформировать QR-код: " + e.getMessage());
        }
    }
}
