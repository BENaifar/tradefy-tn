package tn.esprit.pi5eme.dto.marketdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvImportResult {

    private int totalRows;
    private int inserted;
    private int updated;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
