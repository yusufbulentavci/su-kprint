package kexamprint.printer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.pdfbox.pdmodel.PDPage;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import kexamprint.RecurringPageDocument;
import kexamprint.model.ExamPaperData;
import kexamprint.util.TextResources;

/**
 * Printer for Exam Papers (Soru Kagidi)
 */
public class ExamPaperPrinter extends RecurringPageDocument {

    private final ExamPaperData data;

    public ExamPaperPrinter(ExamPaperData data) {
        this.data = data;
    }

    @Override
    public String getFolder() {
        return data.getOutputFolder();
    }

    @Override
    public String getFile() {
        return data.getFileName();
    }

    @Override
    public boolean isA4() {
        return data.isA4();
    }

    @Override
    public boolean next() {
        return false;
    }

    @Override
    public void addHeader(PdfWriter writer, Document doc, PDPage page) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidths(new float[] { 5, 2 });
        table.setWidthPercentage(100);

        Font bold = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        Font font = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);

        // Row 1
        PdfPCell cell = new PdfPCell(new Paragraph(data.getHeaderLine1(), bold));
        cell.setPadding(5);
        table.addCell(cell);

        cell = new PdfPCell(new Paragraph(data.getHeaderLine2(), bold));
        cell.setPadding(5);
        table.addCell(cell);

        // Row 2
        cell = new PdfPCell(new Paragraph(data.getHeaderLine3(), bold));
        cell.setPadding(5);
        table.addCell(cell);

        cell = new PdfPCell();
        String[] lines = data.getHeaderLine4().split("\n");
        for (String line : lines) {
            cell.addElement(new Paragraph(line, font));
        }
        cell.setPadding(5);
        table.addCell(cell);

        doc.add(table);
    }

    @Override
    public void addBody(PdfWriter writer, Document doc, PDPage page)
            throws DocumentException, MalformedURLException, IOException {

        String imagePath = data.getExamImagePath();
        if (imagePath == null || !new File(imagePath).exists()) {
            System.err.println("Exam image not found: " + imagePath);
            System.err.println("  Room: " + data.getRoomNumber() +
                             ", Seat: " + data.getSeatNumber() +
                             ", Time: " + data.getTimeSlot());
            return;
        }

        Image image = Image.getInstance(imagePath);

        if (data.isA4()) {
            image.scaleToFit(PageSize.A4.getWidth() - 63, PageSize.A4.getHeight() - 50);
        } else {
            image.scaleToFit(PageSize.A3.getWidth() - 63, PageSize.A3.getHeight() - 50);
        }

        image.setAlignment(Image.ALIGN_CENTER);
        doc.add(image);
    }

    @Override
    public void addPageHeader(PdfWriter writer, Document document) {
        // No page header needed
    }

    @Override
    public void addFooter(PdfWriter writer, Document doc, PDPage page) throws Exception {
        // No footer in body
    }

    @Override
    public void addPageFooter(PdfWriter writer, Document document) {
        Font largeFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        Chunk largeText = new Chunk(TextResources.getExamEvaluatorLabel(), largeFont);
        Paragraph paragraph = new Paragraph();
        paragraph.add(largeText);

        PdfContentByte cb = writer.getDirectContent();
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, paragraph,
            document.leftMargin(), document.bottom() - 10, 0);
    }
}
