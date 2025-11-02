package kexamprint.printer;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.pdfbox.pdmodel.PDPage;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import kexamprint.RecurringPageDocument;
import kexamprint.model.SignatureFormData;
import kexamprint.model.StudentSeatInfo;
import kexamprint.util.TextResources;

/**
 * Printer for Signature Forms (Sinav Cetvel)
 */
public class SignatureFormPrinter extends RecurringPageDocument {

    private final SignatureFormData data;

    public SignatureFormPrinter(SignatureFormData data) {
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
    public boolean next() {
        return false;
    }

    @Override
    public void addHeader(PdfWriter writer, Document doc, PDPage page) throws DocumentException {
        Font largeFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        Chunk largeText = new Chunk(TextResources.getSupervisorLabel(), largeFont);
        Paragraph paragraph = new Paragraph();
        paragraph.add(largeText);
        doc.add(paragraph);
        doc.add(new Paragraph("\n"));
    }

    @Override
    public void addBody(PdfWriter writer, Document doc, PDPage page)
            throws DocumentException, MalformedURLException, IOException {

        // For oral exams: 2 columns (no seat numbers)
        // For written exams: 4 columns (name, seat, name, seat)
        PdfPTable table;
        if (data.isOralExam()) {
            table = new PdfPTable(2);
            table.setWidths(new float[] { 1, 1 });
        } else {
            table = new PdfPTable(4);
            table.setWidths(new float[] { 2, 1, 2, 1 });
        }
        table.setWidthPercentage(100);

        boolean toggle = false;

        for (StudentSeatInfo student : data.getStudents()) {
            Font largeFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
            Chunk largeText = new Chunk(student.getFullName(), largeFont);
            Font smallFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
            Chunk smallText = new Chunk("\n" + student.getGroupCode(), smallFont);

            Paragraph paragraph = new Paragraph();
            paragraph.add(largeText);
            paragraph.add(smallText);

            PdfPCell cell = new PdfPCell(paragraph);
            cell.setPadding(5);
            table.addCell(cell);

            // Add seat number cell only for written exams
            if (!data.isOralExam()) {
                paragraph = new Paragraph();
                Integer seatNum = student.getSeatNumber();
                paragraph.add(seatNum == null || seatNum == 0 ? "" : seatNum.toString());

                cell = new PdfPCell(paragraph);
                cell.setPadding(5);
                table.addCell(cell);
            }

            toggle = !toggle;
        }

        // Add empty cells if odd number of students
        if (toggle) {
            table.addCell(new PdfPCell(new Paragraph("")));
            if (!data.isOralExam()) {
                table.addCell(new PdfPCell(new Paragraph("")));
            }
        }

        doc.add(table);
        System.out.println(getFolder() + "/" + getFile() + ": " + data.getStudents().size() + " students");
    }

    @Override
    public void addFooter(PdfWriter writer, Document doc, PDPage page) throws Exception {
        // No footer needed
    }

    @Override
    public void addPageFooter(PdfWriter writer, Document document) {
        // No page footer needed
    }

    @Override
    public void addPageHeader(PdfWriter writer, Document document) {
        Font largeFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);
        Chunk largeText = new Chunk(data.getHeaderText(), largeFont);

        Font normalFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
        Chunk normalText = new Chunk(data.getSubHeaderText(), normalFont);

        Paragraph pl = new Paragraph();
        pl.add(largeText);

        Paragraph pr = new Paragraph();
        pr.add(normalText);

        PdfContentByte cb = writer.getDirectContent();

        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, pl,
            document.leftMargin(), document.top() + 5, 0);

        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, pr,
            document.right(), document.top() + 5, 0);
    }
}
