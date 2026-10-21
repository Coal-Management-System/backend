package net.jemsit.coal.api.dto;

import java.util.List;

/**
 * Read-only projection served by the public QR endpoints.
 *
 * A plain phone camera has no login, so the QR web page (and anything else that
 * follows the printed URL anonymously) reads its whole passport from this single
 * payload instead of the authenticated per-section endpoints the SPA uses.
 */
public final class QrDtos {

    private QrDtos() {
    }

    public record QrPassportDto(
            String type,
            BatchDtos.BatchDto batch,
            PileDtos.PileDto pile,
            MovementDtos.MovementDto movement,
            List<BatchDtos.QualityHistoryRow> quality,
            List<LabDtos.AnalysisDto> analyses) {
    }
}
