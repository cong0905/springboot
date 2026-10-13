package vn.techshop.reporting;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReportExportServiceTest {
    @Test void phoneExportUsesTextGuardWithoutAnExcelFormula() {
        assertThat(ReportExportService.escapeCsvText("0901234567")).isEqualTo("'0901234567");
        assertThat(ReportExportService.escapeCsvText("=1+1")).isEqualTo("'=1+1");
        assertThat(ReportExportService.escapeCsvText(null)).isEmpty();
    }

    @Test
    void testFormulaInjectionNeutralized() {
        // Simple formula injection
        assertThat(ReportExportService.escapeCsv("=1+1")).isEqualTo("\"'=1+1\"");
        assertThat(ReportExportService.escapeCsv("+1+1")).isEqualTo("\"'+1+1\"");
        assertThat(ReportExportService.escapeCsv("-5")).isEqualTo("\"'-5\"");
        assertThat(ReportExportService.escapeCsv("@SUM(1,1)")).isEqualTo("\"'@SUM(1,1)\"");

        // Leading whitespace followed by formula character
        assertThat(ReportExportService.escapeCsv("  =1+1")).isEqualTo("\"'  =1+1\"");
        assertThat(ReportExportService.escapeCsv("\t=1+1")).isEqualTo("\"'\t=1+1\"");
        assertThat(ReportExportService.escapeCsv("\r=1+1")).isEqualTo("\"'\r=1+1\"");

        // Normal strings
        assertThat(ReportExportService.escapeCsv("Nguyễn Văn A")).isEqualTo("Nguyễn Văn A");
        assertThat(ReportExportService.escapeCsv("0901234567")).isEqualTo("0901234567");

        // Quotes and commas
        assertThat(ReportExportService.escapeCsv("Hà Nội, Việt Nam")).isEqualTo("\"Hà Nội, Việt Nam\"");
        assertThat(ReportExportService.escapeCsv("Cửa hàng \"TechShop\"")).isEqualTo("\"Cửa hàng \"\"TechShop\"\"\"");

        // Newline
        assertThat(ReportExportService.escapeCsv("Dòng 1\nDòng 2")).isEqualTo("\"Dòng 1\nDòng 2\"");

        // Null and empty
        assertThat(ReportExportService.escapeCsv(null)).isEqualTo("");
        assertThat(ReportExportService.escapeCsv("")).isEqualTo("");
    }
}
